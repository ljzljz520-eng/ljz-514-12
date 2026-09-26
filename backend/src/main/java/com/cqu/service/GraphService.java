package com.cqu.service;

import com.cqu.model.Node;
import com.cqu.model.Edge;
import com.cqu.model.PathResult;
import com.cqu.model.RouteSummary;
import com.cqu.model.Segment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 图服务：默认按"综合耗时"寻找最优路线。
 *
 * <p>综合耗时由每条边记录的属性折算：</p>
 * <pre>
 *   综合耗时 = 步行耗时 × (1 + 坡度惩罚系数 × 坡度%) × (1 + 拥挤惩罚系数 × (拥挤度-1)) + 换乘次数 × 换乘惩罚
 * </pre>
 * <p>同时基于 Yen 算法计算若干条备选路线，便于在页面上对比说明推荐路线为什么更合适。</p>
 */
public class GraphService {
    /** 步行速度（米/分钟），用于步行耗时缺失时按距离推算 */
    private static final double WALK_SPEED_M_PER_MIN =
            Double.parseDouble(System.getenv().getOrDefault("ROUTE_WALK_SPEED_M_PER_MIN", "75"));
    /** 每 1% 平均坡度增加的耗时比例 */
    private static final double SLOPE_PENALTY_PER_PERCENT =
            Double.parseDouble(System.getenv().getOrDefault("ROUTE_SLOPE_PENALTY_PER_PERCENT", "0.06"));
    /** 每级拥挤度增加的耗时比例 */
    private static final double CROWD_PENALTY_PER_LEVEL =
            Double.parseDouble(System.getenv().getOrDefault("ROUTE_CROWD_PENALTY_PER_LEVEL", "0.06"));
    /** 每次换乘的惩罚分钟数 */
    private static final double TRANSFER_PENALTY_MINUTES =
            Double.parseDouble(System.getenv().getOrDefault("ROUTE_TRANSFER_PENALTY_MINUTES", "10"));
    /** 返回的备选路线数量 */
    private static final int ALTERNATIVE_COUNT =
            Integer.parseInt(System.getenv().getOrDefault("ROUTE_ALTERNATIVES", "2"));

    private final Map<String, Node> nodes;
    private final Map<String, List<Neighbor>> adjacency;

    public GraphService(Map<String, Node> nodes) {
        this.nodes = Map.copyOf(nodes);
        this.adjacency = buildGraph(this.nodes);
    }

    public GraphService(Map<String, Node> nodes, List<Edge> edges) {
        this.nodes = Map.copyOf(nodes);
        if (edges != null && !edges.isEmpty()) {
            this.adjacency = buildGraphFromEdges(this.nodes, edges);
        } else {
            this.adjacency = buildGraph(this.nodes);
        }
    }

    public List<Node> listNodes() {
        return nodes.values().stream().sorted(Comparator.comparing(Node::getName)).toList();
    }

    public PathResult shortestPath(String fromId, String toId) {
        if (fromId == null || toId == null || !nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            throw new IllegalArgumentException("起点或终点不存在");
        }
        if (fromId.equals(toId)) {
            PathResult r = new PathResult(fromId, toId, 0.0, List.of(fromId), List.of(nodes.get(fromId)), List.of());
            r.setSegments(List.of());
            r.setAlternatives(List.of());
            return r;
        }

        List<String> best = dijkstra(fromId, toId, Set.of(), Set.of());
        if (best == null) {
            throw new IllegalStateException("未找到可达路径");
        }

        PathResult result = buildResult(fromId, toId, best);
        result.setAlternatives(findAlternatives(fromId, toId, best));
        return result;
    }

    /** 由路径节点序列构建完整结果（分段明细 + 汇总指标） */
    private PathResult buildResult(String fromId, String toId, List<String> pathIds) {
        List<Node> pathNodes = pathIds.stream().map(nodes::get).toList();
        List<Segment> segments = buildSegments(pathIds);

        List<Double> segmentDistances = new ArrayList<>();
        double totalDistance = 0.0;
        for (Segment s : segments) {
            segmentDistances.add(s.getDistanceMeters());
            totalDistance += s.getDistanceMeters();
        }

        PathResult result = new PathResult(fromId, toId, totalDistance, pathIds, pathNodes, segmentDistances);
        result.setSegments(segments);

        RouteSummary summary = summarize(pathIds, segments);
        result.setTotalWalkMinutes(summary.getTotalWalkMinutes());
        result.setTotalCostMinutes(summary.getTotalCostMinutes());
        result.setTotalTransfers(summary.getTotalTransfers());
        result.setMaxSlopePercent(summary.getMaxSlopePercent());
        result.setAvgCrowding(summary.getAvgCrowding());
        return result;
    }

    /** 把路径节点序列展开为每段路的详细属性 */
    private List<Segment> buildSegments(List<String> pathIds) {
        List<Segment> segments = new ArrayList<>();
        for (int i = 1; i < pathIds.size(); i++) {
            String a = pathIds.get(i - 1);
            String b = pathIds.get(i);
            Edge e = findEdge(a, b);
            Node na = nodes.get(a);
            Node nb = nodes.get(b);
            double distance = resolveDistance(e, na, nb);
            segments.add(new Segment(
                    a, b,
                    distance,
                    resolveWalkMinutes(e, distance),
                    resolveSlope(e),
                    resolveTransfers(e),
                    resolveCrowding(e),
                    edgeCostMinutes(e, na, nb)
            ));
        }
        return segments;
    }

    private static RouteSummary summarize(List<String> pathIds, List<Segment> segments) {
        double distance = 0.0;
        double walk = 0.0;
        double cost = 0.0;
        int transfers = 0;
        double maxSlope = 0.0;
        double crowdingSum = 0.0;
        for (Segment s : segments) {
            distance += s.getDistanceMeters();
            walk += s.getWalkMinutes();
            cost += s.getCostMinutes();
            transfers += s.getTransfers();
            maxSlope = Math.max(maxSlope, s.getSlopePercent());
            crowdingSum += s.getCrowding();
        }
        double avgCrowding = segments.isEmpty() ? 0.0 : crowdingSum / segments.size();
        return new RouteSummary(pathIds, distance, walk, cost, transfers, maxSlope, avgCrowding);
    }

    /**
     * Yen 算法：在综合耗时权重下求 K 条最短简单路径，去掉最优的一条后作为备选路线。
     */
    private List<RouteSummary> findAlternatives(String fromId, String toId, List<String> bestPath) {
        int wanted = 1 + Math.max(0, ALTERNATIVE_COUNT);

        List<List<String>> accepted = new ArrayList<>();
        Set<List<String>> acceptedSet = new HashSet<>();
        accepted.add(bestPath);
        acceptedSet.add(bestPath);

        PriorityQueue<CandidatePath> candidates = new PriorityQueue<>(Comparator.comparingDouble(c -> c.cost));
        Set<List<String>> inCandidates = new HashSet<>();

        for (int k = 1; k < wanted; k++) {
            List<String> prevPath = accepted.get(k - 1);
            for (int i = 0; i < prevPath.size() - 1; i++) {
                String spurNode = prevPath.get(i);
                List<String> rootPath = prevPath.subList(0, i + 1);

                // 禁止与已选路径共享前缀后走同一条边，迫使算法探索分叉
                Set<String> bannedEdgeKeys = new HashSet<>();
                for (List<String> p : accepted) {
                    if (p.size() > i + 1 && p.subList(0, i + 1).equals(rootPath)) {
                        bannedEdgeKeys.add(pairKey(p.get(i), p.get(i + 1)));
                    }
                }
                Set<String> bannedNodes = new HashSet<>(rootPath);
                bannedNodes.remove(spurNode);

                List<String> spurPath = dijkstra(spurNode, toId, bannedNodes, bannedEdgeKeys);
                if (spurPath == null) {
                    continue;
                }
                List<String> totalPath = new ArrayList<>(rootPath);
                totalPath.addAll(spurPath.subList(1, spurPath.size()));
                if (acceptedSet.contains(totalPath) || !inCandidates.add(totalPath)) {
                    continue;
                }
                candidates.add(new CandidatePath(totalPath, pathCostMinutes(totalPath)));
            }
            if (candidates.isEmpty()) {
                break;
            }
            CandidatePath next = candidates.poll();
            inCandidates.remove(next.pathIds);
            accepted.add(next.pathIds);
            acceptedSet.add(next.pathIds);
        }

        List<RouteSummary> alternatives = new ArrayList<>();
        for (int i = 1; i < accepted.size(); i++) {
            List<String> ids = accepted.get(i);
            alternatives.add(summarize(ids, buildSegments(ids)));
        }
        return alternatives;
    }

    private double pathCostMinutes(List<String> pathIds) {
        double total = 0.0;
        for (int i = 1; i < pathIds.size(); i++) {
            String a = pathIds.get(i - 1);
            String b = pathIds.get(i);
            total += edgeCostMinutes(findEdge(a, b), nodes.get(a), nodes.get(b));
        }
        return total;
    }

    /** Dijkstra：按综合耗时求 fromId 到 toId 的最优路径，返回节点 id 序列；不可达返回 null */
    private List<String> dijkstra(String fromId, String toId, Set<String> bannedNodes, Set<String> bannedEdgeKeys) {
        if (fromId.equals(toId)) {
            return List.of(fromId);
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<State> pq = new PriorityQueue<>(Comparator.comparingDouble(s -> s.distance));

        dist.put(fromId, 0.0);
        pq.add(new State(fromId, 0.0));

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            if (cur.distance > dist.getOrDefault(cur.id, Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (cur.id.equals(toId)) {
                break;
            }
            for (Neighbor nb : adjacency.getOrDefault(cur.id, List.of())) {
                if (bannedNodes.contains(nb.toId) || bannedEdgeKeys.contains(pairKey(cur.id, nb.toId))) {
                    continue;
                }
                double nd = cur.distance + nb.costMinutes;
                if (nd < dist.getOrDefault(nb.toId, Double.POSITIVE_INFINITY)) {
                    dist.put(nb.toId, nd);
                    prev.put(nb.toId, cur.id);
                    pq.add(new State(nb.toId, nd));
                }
            }
        }

        if (!prev.containsKey(toId)) {
            return null;
        }

        List<String> pathIds = new ArrayList<>();
        String cur = toId;
        pathIds.add(cur);
        while (!cur.equals(fromId)) {
            cur = prev.get(cur);
            if (cur == null) {
                return null;
            }
            pathIds.add(cur);
        }
        java.util.Collections.reverse(pathIds);
        return pathIds;
    }

    private Edge findEdge(String fromId, String toId) {
        for (Neighbor nb : adjacency.getOrDefault(fromId, List.of())) {
            if (nb.toId.equals(toId)) {
                return nb.edge;
            }
        }
        return null;
    }

    /** 单条边的综合耗时（分钟） */
    private static double edgeCostMinutes(Edge e, Node a, Node b) {
        double distance = resolveDistance(e, a, b);
        double walk = resolveWalkMinutes(e, distance);
        double slope = resolveSlope(e);
        int crowding = resolveCrowding(e);
        int transfers = resolveTransfers(e);
        return walk * (1 + SLOPE_PENALTY_PER_PERCENT * slope) * (1 + CROWD_PENALTY_PER_LEVEL * (crowding - 1))
                + TRANSFER_PENALTY_MINUTES * transfers;
    }

    private static double resolveDistance(Edge e, Node a, Node b) {
        if (e != null && e.getDistanceMeters() != null) {
            return e.getDistanceMeters();
        }
        return GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
    }

    private static double resolveWalkMinutes(Edge e, double distanceMeters) {
        if (e != null && e.getWalkMinutes() != null) {
            return e.getWalkMinutes();
        }
        return distanceMeters / WALK_SPEED_M_PER_MIN;
    }

    private static double resolveSlope(Edge e) {
        return e != null && e.getSlopePercent() != null ? e.getSlopePercent() : 0.0;
    }

    private static int resolveTransfers(Edge e) {
        return e != null && e.getTransfers() != null ? Math.max(0, e.getTransfers()) : 0;
    }

    private static int resolveCrowding(Edge e) {
        if (e == null || e.getCrowding() == null) {
            return 1;
        }
        return Math.max(1, Math.min(5, e.getCrowding()));
    }

    private static Map<String, List<Neighbor>> buildGraph(Map<String, Node> nodes) {
        int k = Integer.parseInt(System.getenv().getOrDefault("GRAPH_K", "6"));
        double maxDist = Double.parseDouble(System.getenv().getOrDefault("GRAPH_MAX_DISTANCE_METERS", "15000"));

        Map<String, List<Neighbor>> adj = new HashMap<>();
        for (String id : nodes.keySet()) {
            adj.put(id, new ArrayList<>());
        }

        Set<String> undirected = new HashSet<>();
        List<Node> all = new ArrayList<>(nodes.values());

        for (Node a : all) {
            List<Node> candidates = new ArrayList<>();
            for (Node b : all) {
                if (a.getId().equals(b.getId())) {
                    continue;
                }
                double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
                if (d <= maxDist) {
                    candidates.add(b);
                }
            }
            candidates.sort(Comparator.comparingDouble(b -> GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng())));
            int limit = Math.min(k, candidates.size());
            for (int i = 0; i < limit; i++) {
                Node b = candidates.get(i);
                String key = pairKey(a.getId(), b.getId());
                if (undirected.add(key)) {
                    double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
                    Edge edge = new Edge(a.getId(), b.getId(), d);
                    double cost = edgeCostMinutes(edge, a, b);
                    adj.get(a.getId()).add(new Neighbor(b.getId(), edge, cost));
                    adj.get(b.getId()).add(new Neighbor(a.getId(), edge, cost));
                }
            }
        }

        ensureConnectivity(adj, nodes);
        return adj;
    }

    private static Map<String, List<Neighbor>> buildGraphFromEdges(Map<String, Node> nodes, List<Edge> edges) {
        Map<String, List<Neighbor>> adj = new HashMap<>();
        for (String id : nodes.keySet()) {
            adj.put(id, new ArrayList<>());
        }

        for (Edge e : edges) {
            if (e == null) {
                continue;
            }
            String from = e.getFromId();
            String to = e.getToId();
            if (from == null || to == null || !nodes.containsKey(from) || !nodes.containsKey(to) || from.equals(to)) {
                continue;
            }

            Node a = nodes.get(from);
            Node b = nodes.get(to);
            double cost = edgeCostMinutes(e, a, b);

            adj.get(from).add(new Neighbor(to, e, cost));
            adj.get(to).add(new Neighbor(from, e, cost));
        }

        boolean ensure = Boolean.parseBoolean(System.getenv().getOrDefault("GRAPH_ENSURE_CONNECTIVITY", "true"));
        if (ensure) {
            ensureConnectivity(adj, nodes);
        }
        return adj;
    }

    private static void ensureConnectivity(Map<String, List<Neighbor>> adj, Map<String, Node> nodes) {
        if (nodes.isEmpty()) {
            return;
        }
        Set<String> visited = new HashSet<>();
        String start = nodes.keySet().iterator().next();
        dfs(start, adj, visited);
        if (visited.size() == nodes.size()) {
            return;
        }

        List<String> remaining = nodes.keySet().stream().filter(id -> !visited.contains(id)).toList();
        Set<String> allVisited = new HashSet<>(visited);
        for (String id : remaining) {
            String connectTo = nearestInSet(id, allVisited, nodes);
            Node a = nodes.get(id);
            Node b = nodes.get(connectTo);
            double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            Edge edge = new Edge(id, connectTo, d);
            double cost = edgeCostMinutes(edge, a, b);
            adj.get(id).add(new Neighbor(connectTo, edge, cost));
            adj.get(connectTo).add(new Neighbor(id, edge, cost));
            dfs(id, adj, allVisited);
        }
    }

    private static String nearestInSet(String fromId, Set<String> set, Map<String, Node> nodes) {
        Node a = nodes.get(fromId);
        String bestId = null;
        double best = Double.POSITIVE_INFINITY;
        for (String candidate : set) {
            Node b = nodes.get(candidate);
            double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
            if (d < best) {
                best = d;
                bestId = candidate;
            }
        }
        if (bestId == null) {
            throw new IllegalStateException("无法确保连通性");
        }
        return bestId;
    }

    private static void dfs(String id, Map<String, List<Neighbor>> adj, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        for (Neighbor nb : adj.getOrDefault(id, List.of())) {
            dfs(nb.toId, adj, visited);
        }
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }

    private static final class Neighbor {
        private final String toId;
        private final Edge edge;
        private final double costMinutes;

        private Neighbor(String toId, Edge edge, double costMinutes) {
            this.toId = toId;
            this.edge = edge;
            this.costMinutes = costMinutes;
        }
    }

    private static final class State {
        private final String id;
        private final double distance;

        private State(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }
    }

    private static final class CandidatePath {
        private final List<String> pathIds;
        private final double cost;

        private CandidatePath(List<String> pathIds, double cost) {
            this.pathIds = pathIds;
            this.cost = cost;
        }
    }
}
