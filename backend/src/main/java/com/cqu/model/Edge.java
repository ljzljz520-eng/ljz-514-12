package com.cqu.model;

/**
 * 景点之间的连接边。
 *
 * <p>除距离外，还记录步行耗时、平均坡度、换乘次数与拥挤程度，
 * 路径规划默认按这些属性折算出的"综合耗时"寻路。</p>
 */
public class Edge {
    private final String fromId;
    private final String toId;
    private final Double distanceMeters;
    private final Double walkMinutes;
    private final Double slopePercent;
    private final Integer transfers;
    private final Integer crowding;

    public Edge(String fromId, String toId, Double distanceMeters) {
        this(fromId, toId, distanceMeters, null, null, null, null);
    }

    public Edge(String fromId, String toId, Double distanceMeters, Double walkMinutes,
                Double slopePercent, Integer transfers, Integer crowding) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceMeters = distanceMeters;
        this.walkMinutes = walkMinutes;
        this.slopePercent = slopePercent;
        this.transfers = transfers;
        this.crowding = crowding;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    /** 步行耗时（分钟），null 表示未记录，可按距离推算 */
    public Double getWalkMinutes() {
        return walkMinutes;
    }

    /** 平均坡度（百分比），null 表示未知（按 0 处理） */
    public Double getSlopePercent() {
        return slopePercent;
    }

    /** 通过该段通常需要的换乘次数，null 表示未知（按 0 处理） */
    public Integer getTransfers() {
        return transfers;
    }

    /** 拥挤程度 1（清净）~ 5（非常拥挤），null 表示未知（按 1 处理） */
    public Integer getCrowding() {
        return crowding;
    }
}
