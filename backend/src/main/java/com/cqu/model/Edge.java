package com.cqu.model;

/**
 * 两个景点之间的一条连接（边）。
 *
 * 除真实步行距离外，还记录影响出行体验的多个维度：
 * - walkSeconds  步行/乘车基础耗时（换乘边指在途时间，不含等候）
 * - slopePercent 平均坡度百分比（爬坡为正，下行为负），用于估算爬坡体力代价
 * - transfer     是否需要换乘（公交/索道/扶梯等）
 * - transferSeconds 换乘的候车、绕行等附加时间（transfer=true 时生效）
 * - crowdLevel   拥挤程度 0-4（畅通/轻微/中度/拥挤/非常拥挤）
 * - mode         通行方式：walk（步行）、bus（公交/大巴）、rail（轨道交通）、
 *                cableway（索道）、escalator（扶梯）
 * - note         备注，用于页面解释这条路的真实情况
 */
public class Edge {
    private final String fromId;
    private final String toId;
    private final Double distanceMeters;
    private final Integer walkSeconds;
    private final Double slopePercent;
    private final boolean transfer;
    private final Integer transferSeconds;
    private final Integer crowdLevel;
    private final String mode;
    private final String note;

    public Edge(String fromId, String toId, Double distanceMeters) {
        this(fromId, toId, distanceMeters, null, null, false, null, null, null, null);
    }

    public Edge(String fromId, String toId, Double distanceMeters, Integer walkSeconds,
                Double slopePercent, boolean transfer, Integer transferSeconds,
                Integer crowdLevel, String mode, String note) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceMeters = distanceMeters;
        this.walkSeconds = walkSeconds;
        this.slopePercent = slopePercent;
        this.transfer = transfer;
        this.transferSeconds = transferSeconds;
        this.crowdLevel = crowdLevel;
        this.mode = mode;
        this.note = note;
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

    public Integer getWalkSeconds() {
        return walkSeconds;
    }

    public Double getSlopePercent() {
        return slopePercent;
    }

    public boolean isTransfer() {
        return transfer;
    }

    public Integer getTransferSeconds() {
        return transferSeconds;
    }

    public Integer getCrowdLevel() {
        return crowdLevel;
    }

    public String getMode() {
        return mode;
    }

    public String getNote() {
        return note;
    }
}
