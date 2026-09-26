package com.cqu.service;

import com.cqu.model.Edge;
import com.cqu.model.Node;
import com.cqu.model.PathResult;
import com.cqu.model.RouteOption;
import com.cqu.model.SegmentInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphServiceTest {

    private Map<String, Node> threeNodes() {
        return Map.of(
                "A", new Node("A", "甲", 29.56301, 106.57577, "t", "d"),
                "B", new Node("B", "乙", 29.56470, 106.58169, "t", "d"),
                "C", new Node("C", "丙", 29.56336, 106.58713, "t", "d")
        );
    }

    @Test
    void shortestPathReturnsValidResult() {
        GraphService g = new GraphService(threeNodes());
        PathResult r = g.shortestPath("A", "C");
        assertNotNull(r);
        assertEquals("A", r.getStartId());
        assertEquals("C", r.getEndId());
        assertFalse(r.getPathNodeIds().isEmpty());
        assertEquals(r.getPathNodeIds().size(), r.getPathNodes().size());
        assertFalse(r.getOptions().isEmpty());
        assertTrue(r.getTotalSeconds() >= 0);
    }

    @Test
    void comprehensiveTimeAvoidsSteepCrowdedShortcut() {
        // 直连 A-C：距离短，但 15% 陡坡 + 非常拥挤
        Edge direct = new Edge("A", "C", 800.0, 900, 15.0, false, null, 4, "walk", "陡坡直连");
        // 绕行 A-B-C：距离更长，但平缓畅通
        Edge ab = new Edge("A", "B", 600.0, 480, 1.0, false, null, 0, "walk", "平路");
        Edge bc = new Edge("B", "C", 600.0, 480, 1.0, false, null, 0, "walk", "平路");

        GraphService g = new GraphService(threeNodes(), List.of(direct, ab, bc));

        PathResult byTime = g.shortestPath("A", "C", RouteProfile.COMPREHENSIVE);
        assertEquals(List.of("A", "B", "C"), byTime.getPathNodeIds(),
                "综合耗时应选择平缓畅通的绕行路线");
        assertTrue(byTime.getOptions().size() >= 2, "应至少给出两种不同走法");

        PathResult byDistance = g.shortestPath("A", "C", RouteProfile.DISTANCE);
        assertEquals(List.of("A", "C"), byDistance.getPathNodeIds(),
                "最短距离应选择陡坡直连");

        // 推荐方案固定为综合耗时最优
        RouteOption recommended = byTime.getOptions().stream()
                .filter(RouteOption::isRecommended).findFirst().orElseThrow();
        assertEquals(List.of("A", "B", "C"), recommended.getPathNodeIds());

        // 段明细包含各维度耗时拆解，可用于解释
        SegmentInfo seg = byTime.getOptions().get(0).getSegments().get(0);
        assertTrue(seg.getTotalSeconds() > 0);
        assertEquals("甲", seg.getFromName());
        assertNotNull(seg.getCrowdLabel());
    }

    @Test
    void transferPenaltyFavorsDirectWalk() {
        // A-C 直达平路但较长（约 3500m，步行 45 分钟）；A-B-C 公交需换乘，但更快
        Edge direct = new Edge("A", "C", 3500.0, 2700, 1.0, false, null, 1, "walk", "步行直达");
        Edge ab = new Edge("A", "B", 2200.0, 600, 0.0, true, 300, 1, "bus", "公交");
        Edge bc = new Edge("B", "C", 2200.0, 600, 0.0, true, 300, 1, "bus", "公交");

        GraphService g = new GraphService(threeNodes(), List.of(direct, ab, bc));

        PathResult lessTransfer = g.shortestPath("A", "C", RouteProfile.LESS_TRANSFER);
        assertEquals(List.of("A", "C"), lessTransfer.getPathNodeIds(),
                "最少换乘偏好应选择步行直达");

        PathResult byTime = g.shortestPath("A", "C", RouteProfile.COMPREHENSIVE);
        assertEquals(List.of("A", "B", "C"), byTime.getPathNodeIds(),
                "综合耗时下公交更快");
    }

    @Test
    void edgeCostModelSanity() {
        Edge steep = new Edge("A", "B", 1000.0, 800, 10.0, false, null, 2, "walk", null);
        // 上行 1000m * 10% = 100m 爬升 * 4s = 400s
        assertEquals(400.0, EdgeCost.slopeSeconds(steep), 0.1);
        // 下行方向取反，按 0.4 系数 = 160s
        Edge downhill = new Edge("A", "B", 1000.0, 800, -10.0, false, null, 2, "walk", null);
        assertEquals(160.0, EdgeCost.slopeSeconds(downhill), 0.1);
        // 拥挤 2 级 => 基础耗时 * 20% = 160s
        assertEquals(160.0, EdgeCost.crowdSeconds(steep), 0.1);
        assertEquals(800 + 400 + 160, EdgeCost.totalSeconds(steep), 0.1);

        Edge bus = new Edge("A", "B", 5000.0, 900, null, true, 420, 1, "bus", null);
        assertEquals(420.0, EdgeCost.transferSeconds(bus), 0.1);
        assertTrue(EdgeCost.weight(bus, RouteProfile.LESS_TRANSFER)
                > EdgeCost.weight(bus, RouteProfile.COMPREHENSIVE));
    }
}
