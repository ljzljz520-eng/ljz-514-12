package com.cqu.service;

import com.cqu.model.Edge;
import com.cqu.model.Node;
import com.cqu.model.PathResult;
import com.cqu.model.RouteSummary;
import com.cqu.model.Segment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphServiceTest {
    @Test
    void shortestPathReturnsValidResult() {
        Map<String, Node> nodes = Map.of(
                "A", new Node("A", "A", 29.56301, 106.57577, "t", "d"),
                "B", new Node("B", "B", 29.56470, 106.58169, "t", "d"),
                "C", new Node("C", "C", 29.56336, 106.58713, "t", "d")
        );
        GraphService g = new GraphService(nodes);
        PathResult r = g.shortestPath("A", "C");
        assertNotNull(r);
        assertEquals("A", r.getStartId());
        assertEquals("C", r.getEndId());
        assertFalse(r.getPathNodeIds().isEmpty());
        assertEquals(r.getPathNodeIds().size(), r.getPathNodes().size());
    }

    @Test
    void prefersCompositeTimeOverRawDistance() {
        // S-A-T：距离更长但平坦、不拥挤、无需换乘；S-B-T：距离更短但坡陡、拥挤、需换乘
        Map<String, Node> nodes = Map.of(
                "S", new Node("S", "S", 0.0, 0.0, "t", "d"),
                "A", new Node("A", "A", 0.01, 0.0, "t", "d"),
                "B", new Node("B", "B", -0.01, 0.0, "t", "d"),
                "T", new Node("T", "T", 0.02, 0.0, "t", "d")
        );
        List<Edge> edges = List.of(
                new Edge("S", "A", 1000.0, 14.0, 0.0, 0, 1),
                new Edge("A", "T", 1000.0, 14.0, 0.0, 0, 1),
                new Edge("S", "B", 900.0, 13.0, 9.0, 1, 5),
                new Edge("B", "T", 900.0, 13.0, 9.0, 1, 5)
        );
        GraphService g = new GraphService(nodes, edges);
        PathResult r = g.shortestPath("S", "T");

        assertEquals(List.of("S", "A", "T"), r.getPathNodeIds());
        // 更短的距离（1800m）被放弃，选择综合耗时更低的路线（2000m）
        assertEquals(2000.0, r.getTotalDistanceMeters(), 0.01);
        assertEquals(28.0, r.getTotalWalkMinutes(), 0.01);
        assertEquals(28.0, r.getTotalCostMinutes(), 0.01);
        assertEquals(0, r.getTotalTransfers());
        assertEquals(2, r.getSegments().size());

        boolean sawShortButBadAlternative = false;
        for (RouteSummary alt : r.getAlternatives()) {
            assertEquals(List.of("S", "B", "T"), alt.getPathNodeIds());
            assertTrue(alt.getTotalCostMinutes() > r.getTotalCostMinutes());
            assertEquals(2, alt.getTotalTransfers());
            sawShortButBadAlternative = true;
        }
        assertTrue(sawShortButBadAlternative, "应返回距离更短但综合耗时更高的备选路线");
    }

    @Test
    void segmentsCarryFullAttributesAndPenalty() {
        Map<String, Node> nodes = Map.of(
                "A", new Node("A", "A", 0.0, 0.0, "t", "d"),
                "B", new Node("B", "B", 0.01, 0.0, "t", "d")
        );
        List<Edge> edges = List.of(new Edge("A", "B", 1000.0, 15.0, 5.0, 1, 4));
        GraphService g = new GraphService(nodes, edges);
        PathResult r = g.shortestPath("A", "B");

        Segment s = r.getSegments().get(0);
        assertEquals(1000.0, s.getDistanceMeters(), 0.01);
        assertEquals(15.0, s.getWalkMinutes(), 0.01);
        assertEquals(5.0, s.getSlopePercent(), 0.01);
        assertEquals(1, s.getTransfers());
        assertEquals(4, s.getCrowding());
        // 15 * (1 + 0.06*5) * (1 + 0.06*3) + 10（换乘惩罚） = 33.01
        assertEquals(33.01, s.getCostMinutes(), 0.001);
        assertEquals(1, r.getTotalTransfers());
        assertEquals(5.0, r.getMaxSlopePercent(), 0.01);
        assertEquals(4.0, r.getAvgCrowding(), 0.01);
    }
}
