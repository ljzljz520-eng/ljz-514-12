package com.cqu.service;

import com.cqu.model.Edge;

/**
 * 把一条边的多维度属性换算成可比较的权重。
 *
 * 综合耗时模型（COMPREHENSIVE）：
 *   综合耗时 = 基础耗时 + 坡度附加 + 拥挤延误 + 换乘等候
 *
 * 设计依据（页面上也按同样口径向用户解释）：
 * - 基础耗时：edges.csv 中给出的真实步行/乘车耗时；未给出时按 1.3 m/s 的步行速度估算
 * - 坡度附加：有方向性。沿行进方向每爬升 1 米约多花 4 秒（重庆山城步道经验值）；
 *   下行台阶/坡道虽不爬坡但仍消耗体力与制动时间，按 0.4 系数折算。
 *   一对相反方向的平均代价等价于坡度绝对值 × 4 × 0.7
 * - 拥挤延误：按基础耗时的一定比例增加（0%/10%/20%/35%/55%），
 *   对应 畅通/轻微/中度/拥挤/非常拥挤
 * - 换乘等候：公交/索道/扶梯等换乘带来的等车、绕行、上下楼时间
 *
 * DISTANCE 模式只看 distanceMeters；LESS_TRANSFER 模式给每次换乘一个大惩罚。
 */
public final class EdgeCost {
    /** 未显式给出步行耗时时的默认平地步行速度（m/s） */
    public static final double DEFAULT_WALK_SPEED_MPS = 1.3;
    /** 上行：每爬升 1 米折合的额外秒数 */
    public static final double SECONDS_PER_METER_CLIMB = 4.0;
    /** 下行：相对上行的体力代价系数（台阶/坡道仍需制动，不能忽略） */
    public static final double DOWNHILL_EFFORT_FACTOR = 0.4;
    /** 少换乘模式下，每次换乘折合的惩罚秒数 */
    public static final double TRANSFER_PENALTY_SECONDS = 600.0;

    private EdgeCost() {
    }

    public static int crowdFactorPercent(int crowdLevel) {
        switch (clampCrowd(crowdLevel)) {
            case 1:
                return 10;
            case 2:
                return 20;
            case 3:
                return 35;
            case 4:
                return 55;
            case 0:
            default:
                return 0;
        }
    }

    public static String crowdLabel(int crowdLevel) {
        switch (clampCrowd(crowdLevel)) {
            case 1:
                return "轻微拥挤";
            case 2:
                return "中度拥挤";
            case 3:
                return "拥挤";
            case 4:
                return "非常拥挤";
            case 0:
            default:
                return "畅通";
        }
    }

    public static String modeLabel(String mode) {
        if (mode == null) {
            return "步行";
        }
        switch (mode.trim().toLowerCase()) {
            case "bus":
                return "公交/大巴";
            case "rail":
                return "轨道交通";
            case "cableway":
                return "过江索道";
            case "escalator":
                return "大扶梯";
            case "walk":
            default:
                return "步行";
        }
    }

    public static int clampCrowd(int crowdLevel) {
        if (crowdLevel < 0) {
            return 0;
        }
        if (crowdLevel > 4) {
            return 4;
        }
        return crowdLevel;
    }

    public static double distanceMeters(Edge edge) {
        Double d = edge.getDistanceMeters();
        return d == null || d <= 0 ? 0.0 : d;
    }

    /** 基础耗时（秒）：显式配置优先，否则按默认步行速度估算 */
    public static double baseSeconds(Edge edge) {
        Integer walk = edge.getWalkSeconds();
        if (walk != null && walk > 0) {
            return walk;
        }
        return distanceMeters(edge) / DEFAULT_WALK_SPEED_MPS;
    }

    /**
     * 坡度附加耗时（秒），有方向性：
     * 正坡度（沿行进方向爬升）按 4 秒/米；负坡度（下行）按 0.4 系数折算。
     */
    public static double slopeSeconds(Edge edge) {
        Double slope = edge.getSlopePercent();
        if (slope == null || distanceMeters(edge) <= 0) {
            return 0.0;
        }
        double elevation = distanceMeters(edge) * Math.abs(slope) / 100.0;
        double factor = slope >= 0 ? 1.0 : DOWNHILL_EFFORT_FACTOR;
        return elevation * SECONDS_PER_METER_CLIMB * factor;
    }

    /** 拥挤延误（秒） */
    public static double crowdSeconds(Edge edge) {
        Integer crowd = edge.getCrowdLevel();
        if (crowd == null) {
            return 0.0;
        }
        return baseSeconds(edge) * crowdFactorPercent(clampCrowd(crowd)) / 100.0;
    }

    /** 换乘等候（秒） */
    public static double transferSeconds(Edge edge) {
        if (!edge.isTransfer()) {
            return 0.0;
        }
        Integer wait = edge.getTransferSeconds();
        return wait != null && wait > 0 ? wait : 300.0;
    }

    /** 综合耗时（秒） */
    public static double totalSeconds(Edge edge) {
        return baseSeconds(edge) + slopeSeconds(edge) + crowdSeconds(edge) + transferSeconds(edge);
    }

    /** 按寻路偏好取边权重 */
    public static double weight(Edge edge, RouteProfile profile) {
        switch (profile) {
            case DISTANCE:
                return distanceMeters(edge);
            case LESS_TRANSFER:
                return totalSeconds(edge) + (edge.isTransfer() ? TRANSFER_PENALTY_SECONDS : 0.0);
            case COMPREHENSIVE:
            default:
                return totalSeconds(edge);
        }
    }
}
