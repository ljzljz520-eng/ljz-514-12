package com.cqu.service;

import com.cqu.model.Edge;
import com.cqu.model.Node;
import com.cqu.model.PathResult;
import com.cqu.model.RouteOption;
import com.cqu.model.SegmentInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class GraphService {
    private final Map<String, Node> nodes;
    private final Map<String, List<Edge>> adjacency;

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

    /** 兼容旧接口：默认按综合耗时规划 */
    public PathResult shortestPath(String fromId, String toId) {
        return shortestPath(fromId, toId, RouteProfile.COMPREHENSIVE);
    }

    public PathResult shortestPath(String fromId, String toId, RouteProfile profile) {
        if (fromId == null || toId == null || !nodes.containsKey(fromId) || !nodes.containsKey(toId)) {
            throw new IllegalArgumentException("起点或终点不存在");
        }
        if (profile == null) {
            profile = RouteProfile.COMPREHENSIVE;
        }

        PathResult result = new PathResult();
        result.setStartId(fromId);
        result.setEndId(toId);
        result.setProfile(profile.name().toLowerCase());
        result.setProfileLabel(profile.getLabel());

        if (fromId.equals(toId)) {
            RouteOption only = buildOption(new Trace(List.of(fromId), List.of()), List.of(profile));
            only.setSelected(true);
            only.setRecommended(true);
            result.setOptions(List.of(only));
            result.setSummary("起点与终点相同，无需出行。");
            applySelected(result, only);
            return result;
        }

        // 三种偏好各算一遍，再按节点序列去重（不同偏好可能走出同一条路）
        Map<List<String>, RouteOption> distinct = new LinkedHashMap<>();
        Map<List<String>, List<RouteProfile>> profilesByPath = new LinkedHashMap<>();
        Map<List<String>, Trace> tracesByPath = new LinkedHashMap<>();

        for (RouteProfile p : List.of(RouteProfile.COMPREHENSIVE, RouteProfile.DISTANCE, RouteProfile.LESS_TRANSFER)) {
            Trace trace = dijkstra(fromId, toId, p);
            profilesByPath.computeIfAbsent(trace.ids, k -> new ArrayList<>()).add(p);
            tracesByPath.putIfAbsent(trace.ids, trace);
        }

        // 推荐方案固定为综合耗时最优（第一个计算结果）
        List<String> recommendedKey = null;
        RouteOption selected = null;
        for (Map.Entry<List<String>, List<RouteProfile>> entry : profilesByPath.entrySet()) {
            List<RouteProfile> ps = entry.getValue();
            boolean recommended = ps.contains(RouteProfile.COMPREHENSIVE);
            RouteOption option = buildOption(tracesByPath.get(entry.getKey()), ps);
            option.setRecommended(recommended);
            if (recommended) {
                recommendedKey = entry.getKey();
            }
            if (ps.contains(profile)) {
                option.setSelected(true);
                selected = option;
            }
            distinct.put(entry.getKey(), option);
        }

        List<RouteOption> options = new ArrayList<>(distinct.values());
        RouteOption recommended = distinct.get(recommendedKey);

        for (RouteOption option : options) {
            option.setReasons(compare(option, recommended));
        }

        result.setOptions(options);
        result.setSummary(buildSummary(profile, selected, recommended, options));
        applySelected(result, selected != null ? selected : recommended);
        return result;
    }

    private void applySelected(PathResult result, RouteOption selected) {
        result.setTotalDistanceMeters(selected.getTotalDistanceMeters());
        result.setTotalSeconds(selected.getTotalSeconds());
        result.setPathNodeIds(selected.getPathNodeIds());
        result.setPathNodes(selected.getPathNodes());
        List<Double> segDist = new ArrayList<>();
        for (SegmentInfo s : selected.getSegments()) {
            segDist.add(s.getDistanceMeters());
        }
        result.setSegmentDistanceMeters(segDist);
    }

    private Trace dijkstra(String fromId, String toId, RouteProfile profile) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prevNode = new HashMap<>();
        Map<String, Edge> prevEdge = new HashMap<>();
        PriorityQueue<State> pq = new PriorityQueue<>(
                Comparator.comparingDouble((State s) -> s.weight).thenComparing(s -> s.id));

        for (String id : nodes.keySet()) {
            dist.put(id, Double.POSITIVE_INFINITY);
        }
        dist.put(fromId, 0.0);
        pq.add(new State(fromId, 0.0));

        while (!pq.isEmpty()) {
            State cur = pq.poll();
            if (cur.weight > dist.get(cur.id)) {
                continue;
            }
            if (cur.id.equals(toId)) {
                break;
            }
            for (Edge edge : adjacency.getOrDefault(cur.id, List.of())) {
                double nd = cur.weight + EdgeCost.weight(edge, profile);
                if (nd < dist.get(edge.getToId()) - 1e-9) {
                    dist.put(edge.getToId(), nd);
                    prevNode.put(edge.getToId(), cur.id);
                    prevEdge.put(edge.getToId(), edge);
                    pq.add(new State(edge.getToId(), nd));
                }
            }
        }

        if (!prevNode.containsKey(toId)) {
            throw new IllegalStateException("未找到可达路径");
        }

        List<String> ids = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        String cur = toId;
        ids.add(cur);
        while (!cur.equals(fromId)) {
            Edge edge = prevEdge.get(cur);
            cur = prevNode.get(cur);
            if (cur == null || edge == null) {
                throw new IllegalStateException("未找到可达路径");
            }
            ids.add(cur);
            edges.add(edge);
        }
        Collections.reverse(ids);
        Collections.reverse(edges);
        return new Trace(List.copyOf(ids), List.copyOf(edges));
    }

    private RouteOption buildOption(Trace trace, List<RouteProfile> profiles) {
        RouteOption option = new RouteOption();
        option.setProfile(profiles.stream().map(p -> p.name().toLowerCase()).reduce((a, b) -> a + "+" + b).orElse(""));
        option.setProfileLabel(profiles.get(0).getLabel());
        if (profiles.size() > 1) {
            List<String> labels = profiles.stream().map(RouteProfile::getLabel).toList();
            option.setProfileLabel(String.join("/", labels));
        }

        List<Node> pathNodes = trace.ids.stream().map(nodes::get).toList();
        List<SegmentInfo> segments = new ArrayList<>();
        double distance = 0;
        int base = 0;
        int slope = 0;
        int crowd = 0;
        int transferWait = 0;
        int transferCount = 0;
        int crowdedCount = 0;
        double climb = 0;
        double descent = 0;

        for (int i = 0; i < trace.edges.size(); i++) {
            Edge edge = trace.edges.get(i);
            Node a = nodes.get(edge.getFromId());
            Node b = nodes.get(edge.getToId());

            int segBase = (int) Math.round(EdgeCost.baseSeconds(edge));
            int segSlope = (int) Math.round(EdgeCost.slopeSeconds(edge));
            int segCrowd = (int) Math.round(EdgeCost.crowdSeconds(edge));
            int segTransfer = (int) Math.round(EdgeCost.transferSeconds(edge));
            double segDistance = EdgeCost.distanceMeters(edge);
            double segSlopePct = edge.getSlopePercent() == null ? 0.0 : edge.getSlopePercent();
            int segCrowdLevel = edge.getCrowdLevel() == null ? 0 : EdgeCost.clampCrowd(edge.getCrowdLevel());

            SegmentInfo seg = new SegmentInfo();
            seg.setFromId(a.getId());
            seg.setToId(b.getId());
            seg.setFromName(a.getName());
            seg.setToName(b.getName());
            String mode = edge.getMode() == null ? "walk" : edge.getMode().trim().toLowerCase();
            seg.setMode(mode);
            seg.setModeLabel(EdgeCost.modeLabel(mode));
            seg.setDistanceMeters(Math.round(segDistance * 10) / 10.0);
            seg.setBaseSeconds(segBase);
            seg.setSlopeSeconds(segSlope);
            seg.setCrowdSeconds(segCrowd);
            seg.setTransferSeconds(segTransfer);
            seg.setTotalSeconds(segBase + segSlope + segCrowd + segTransfer);
            seg.setSlopePercent(segSlopePct);
            double elevationChange = segDistance * Math.abs(segSlopePct) / 100.0;
            if (segSlopePct >= 0) {
                climb += elevationChange;
                seg.setClimbMeters(Math.round(elevationChange * 10) / 10.0);
            } else {
                descent += elevationChange;
                seg.setDescentMeters(Math.round(elevationChange * 10) / 10.0);
            }
            seg.setTransfer(edge.isTransfer());
            seg.setCrowdLevel(segCrowdLevel);
            seg.setCrowdLabel(EdgeCost.crowdLabel(segCrowdLevel));
            seg.setNote(edge.getNote());
            segments.add(seg);

            distance += segDistance;
            base += segBase;
            slope += segSlope;
            crowd += segCrowd;
            transferWait += segTransfer;
            if (edge.isTransfer()) {
                transferCount++;
            }
            if (segCrowdLevel >= 3) {
                crowdedCount++;
            }
        }

        option.setPathNodeIds(trace.ids);
        option.setPathNodes(pathNodes);
        option.setSegments(segments);
        option.setTotalDistanceMeters(Math.round(distance));
        option.setBaseSeconds(base);
        option.setSlopeSeconds(slope);
        option.setCrowdSeconds(crowd);
        option.setTransferSeconds(transferWait);
        option.setTotalSeconds(base + slope + crowd + transferWait);
        option.setTransferCount(transferCount);
        option.setCrowdedSegmentCount(crowdedCount);
        option.setTotalClimbMeters(Math.round(climb));
        option.setTotalDescentMeters(Math.round(descent));
        option.setHighlight(buildHighlight(option));
        return option;
    }

    private static String buildHighlight(RouteOption o) {
        List<String> parts = new ArrayList<>();
        if (o.getTransferCount() == 0) {
            parts.add("全程步行，无需换乘");
        } else {
            parts.add("仅" + o.getTransferCount() + "次换乘");
        }
        if (o.getTotalClimbMeters() >= 200) {
            parts.add("累计爬升约" + roundedFifty(o.getTotalClimbMeters()) + "米");
        }
        if (o.getCrowdedSegmentCount() == 0) {
            parts.add("沿途不拥挤");
        }
        return String.join("，", parts);
    }

    /** 生成“该方案相对推荐方案”的对比说明 */
    private List<String> compare(RouteOption o, RouteOption recommended) {
        if (recommended == null || o == recommended) {
            List<String> self = new ArrayList<>();
            self.add("综合耗时 " + formatMinutes(o.getTotalSeconds()) + "，为默认推荐路线。");
            if (o.getTransferCount() > 0) {
                self.add("含 " + o.getTransferCount() + " 次换乘，换乘等候约 "
                        + formatMinutes(o.getTransferSeconds()) + "。");
            }
            if (o.getSlopeSeconds() > 0) {
                StringBuilder sr = new StringBuilder("坡度附加约 ")
                        .append(formatMinutes(o.getSlopeSeconds())).append("（");
                if (o.getTotalClimbMeters() >= 50) {
                    sr.append("累计爬升约 ").append(Math.round(o.getTotalClimbMeters())).append(" 米");
                }
                if (o.getTotalClimbMeters() >= 50 && o.getTotalDescentMeters() >= 50) {
                    sr.append("、");
                }
                if (o.getTotalDescentMeters() >= 50) {
                    sr.append("累计下降约 ").append(Math.round(o.getTotalDescentMeters())).append(" 米");
                }
                sr.append("）。");
                self.add(sr.toString());
            }
            return self;
        }

        List<String> reasons = new ArrayList<>();
        int deltaSec = o.getTotalSeconds() - recommended.getTotalSeconds();
        if (deltaSec > 30) {
            reasons.add("综合耗时多约 " + formatMinutes(deltaSec)
                    + "（" + formatMinutes(o.getTotalSeconds()) + " vs "
                    + formatMinutes(recommended.getTotalSeconds()) + "）");
        } else if (deltaSec < -30) {
            reasons.add("综合耗时反而少约 " + formatMinutes(-deltaSec));
        }

        long deltaDist = Math.round(o.getTotalDistanceMeters() - recommended.getTotalDistanceMeters());
        if (Math.abs(deltaDist) >= 200) {
            reasons.add((deltaDist > 0 ? "多走约 " : "少走约 ")
                    + Math.abs(deltaDist) + " 米");
        }

        int deltaTransfer = o.getTransferCount() - recommended.getTransferCount();
        if (deltaTransfer > 0) {
            reasons.add("多 " + deltaTransfer + " 次换乘，候车更久");
        } else if (deltaTransfer < 0) {
            reasons.add("少 " + (-deltaTransfer) + " 次换乘，不用等车");
        }

        double deltaClimb = o.getTotalClimbMeters() - recommended.getTotalClimbMeters();
        if (Math.abs(deltaClimb) >= 80) {
            reasons.add((deltaClimb > 0 ? "多爬坡约 " : "少爬坡约 ")
                    + Math.round(Math.abs(deltaClimb)) + " 米");
        }
        double deltaDescent = o.getTotalDescentMeters() - recommended.getTotalDescentMeters();
        if (deltaDescent >= 80) {
            reasons.add("多下坡约 " + Math.round(deltaDescent) + " 米（台阶下行也耗时耗力）");
        }

        int deltaCrowd = o.getCrowdedSegmentCount() - recommended.getCrowdedSegmentCount();
        if (deltaCrowd > 0) {
            reasons.add("多经过 " + deltaCrowd + " 段拥挤道路");
        } else if (deltaCrowd < 0) {
            reasons.add("少经过 " + (-deltaCrowd) + " 段拥挤道路");
        }

        if (reasons.isEmpty()) {
            reasons.add("与推荐路线体验接近，仅途经景点不同");
        }
        return reasons;
    }

    private String buildSummary(RouteProfile requested, RouteOption selected,
                                RouteOption recommended, List<RouteOption> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("默认按“综合耗时”找路 = 基础步行/乘车时间 + 爬坡附加（每爬升 1 米约 4 秒）")
                .append("+ 拥挤延误 + 换乘等候。");
        sb.append("推荐路线全程约 ").append(Math.round(recommended.getTotalDistanceMeters()))
                .append(" 米，综合耗时 ").append(formatMinutes(recommended.getTotalSeconds()));
        if (recommended.getTransferCount() > 0) {
            sb.append("，含 ").append(recommended.getTransferCount()).append(" 次换乘");
        } else {
            sb.append("，无需换乘");
        }
        if (recommended.getSlopeSeconds() > 0) {
            if (recommended.getTotalClimbMeters() >= 50) {
                sb.append("，爬坡代价约 ").append(formatMinutes(recommended.getSlopeSeconds()));
            } else {
                sb.append("，下坡代价约 ").append(formatMinutes(recommended.getSlopeSeconds()));
            }
        }
        sb.append("。");

        if (options.size() > 1) {
            sb.append(" 对比其他走法：");
            List<String> clauses = new ArrayList<>();
            for (RouteOption o : options) {
                if (o == recommended) {
                    continue;
                }
                String tag = o.getProfileLabel() + "方案";
                List<String> rs = compare(o, recommended);
                clauses.add(tag + String.join("、", rs));
            }
            sb.append(String.join("；", clauses)).append("。");
        }

        if (selected != null && selected != recommended) {
            sb.append(" 当前按“").append(requested.getLabel())
                    .append("”偏好选中的是另一条路线，可在上方案卡间切换对比。");
        }
        return sb.toString();
    }

    private static long roundedFifty(double meters) {
        return Math.round(meters / 50.0) * 50;
    }

    private static String formatMinutes(int seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        }
        long min = Math.round(seconds / 60.0);
        return min + " 分钟";
    }

    // ---- 图构建 ----

    private static Map<String, List<Edge>> buildGraph(Map<String, Node> nodes) {
        int k = Integer.parseInt(System.getenv().getOrDefault("GRAPH_K", "6"));
        double maxDist = Double.parseDouble(System.getenv().getOrDefault("GRAPH_MAX_DISTANCE_METERS", "15000"));

        Map<String, List<Edge>> adj = new HashMap<>();
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
            candidates.sort(Comparator.comparingDouble(b ->
                    GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng())));
            int limit = Math.min(k, candidates.size());
            for (int i = 0; i < limit; i++) {
                Node b = candidates.get(i);
                String key = pairKey(a.getId(), b.getId());
                if (undirected.add(key)) {
                    Edge e = syntheticEdge(a, b);
                    adj.get(a.getId()).add(e);
                    adj.get(b.getId()).add(reverseEdge(e, b, a));
                }
            }
        }

        if (Boolean.parseBoolean(System.getenv().getOrDefault("GRAPH_ENSURE_CONNECTIVITY", "true"))) {
            ensureConnectivity(adj, nodes);
        }
        sortAdjacency(adj);
        return adj;
    }

    private static Map<String, List<Edge>> buildGraphFromEdges(Map<String, Node> nodes, List<Edge> edges) {
        Map<String, List<Edge>> adj = new HashMap<>();
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
            Edge forward = normalize(e, a, b);
            adj.get(from).add(forward);
            adj.get(to).add(reverseEdge(forward, b, a));
        }

        if (Boolean.parseBoolean(System.getenv().getOrDefault("GRAPH_ENSURE_CONNECTIVITY", "true"))) {
            ensureConnectivity(adj, nodes);
        }
        sortAdjacency(adj);
        return adj;
    }

    /** CSV 未给出的字段用模型默认值补齐 */
    private static Edge normalize(Edge e, Node a, Node b) {
        double distance = e.getDistanceMeters() != null && e.getDistanceMeters() > 0
                ? e.getDistanceMeters()
                : GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        return new Edge(e.getFromId(), e.getToId(), distance, e.getWalkSeconds(),
                e.getSlopePercent(), e.isTransfer(), e.getTransferSeconds(),
                e.getCrowdLevel(), e.getMode(), e.getNote());
    }

    private static Edge syntheticEdge(Node a, Node b) {
        double d = GeoUtils.haversineMeters(a.getLat(), a.getLng(), b.getLat(), b.getLng());
        return new Edge(a.getId(), b.getId(), d, null, 0.0, false, null, 0, "walk", null);
    }

    private static Edge reverseEdge(Edge e, Node from, Node to) {
        Double reverseSlope = e.getSlopePercent() == null ? null : -e.getSlopePercent();
        return new Edge(from.getId(), to.getId(), e.getDistanceMeters(), e.getWalkSeconds(),
                reverseSlope, e.isTransfer(), e.getTransferSeconds(), e.getCrowdLevel(),
                e.getMode(), e.getNote());
    }

    private static void sortAdjacency(Map<String, List<Edge>> adj) {
        for (List<Edge> list : adj.values()) {
            list.sort(Comparator.comparing(Edge::getToId));
        }
    }

    private static void ensureConnectivity(Map<String, List<Edge>> adj, Map<String, Node> nodes) {
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
            Edge e = syntheticEdge(a, b);
            adj.get(id).add(e);
            adj.get(connectTo).add(reverseEdge(e, b, a));
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

    private static void dfs(String id, Map<String, List<Edge>> adj, Set<String> visited) {
        if (!visited.add(id)) {
            return;
        }
        for (Edge nb : adj.getOrDefault(id, List.of())) {
            dfs(nb.getToId(), adj, visited);
        }
    }

    private static String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }

    private static final class Trace {
        private final List<String> ids;
        private final List<Edge> edges;

        private Trace(List<String> ids, List<Edge> edges) {
            this.ids = ids;
            this.edges = edges;
        }
    }

    private static final class State {
        private final String id;
        private final double weight;

        private State(String id, double weight) {
            this.id = id;
            this.weight = weight;
        }
    }
}
