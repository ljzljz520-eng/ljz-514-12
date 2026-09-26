package com.cqu.model;

/**
 * 路径中相邻两个景点之间的一段路，携带该段的全部权重属性与折算耗时。
 */
public class Segment {
    private String fromId;
    private String toId;
    private double distanceMeters;
    private double walkMinutes;
    private double slopePercent;
    private int transfers;
    private int crowding;
    /** 该段的综合耗时（分钟）：步行耗时经坡度、拥挤修正并叠加换乘惩罚 */
    private double costMinutes;

    public Segment() {
    }

    public Segment(String fromId, String toId, double distanceMeters, double walkMinutes,
                   double slopePercent, int transfers, int crowding, double costMinutes) {
        this.fromId = fromId;
        this.toId = toId;
        this.distanceMeters = distanceMeters;
        this.walkMinutes = walkMinutes;
        this.slopePercent = slopePercent;
        this.transfers = transfers;
        this.crowding = crowding;
        this.costMinutes = costMinutes;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public double getWalkMinutes() {
        return walkMinutes;
    }

    public void setWalkMinutes(double walkMinutes) {
        this.walkMinutes = walkMinutes;
    }

    public double getSlopePercent() {
        return slopePercent;
    }

    public void setSlopePercent(double slopePercent) {
        this.slopePercent = slopePercent;
    }

    public int getTransfers() {
        return transfers;
    }

    public void setTransfers(int transfers) {
        this.transfers = transfers;
    }

    public int getCrowding() {
        return crowding;
    }

    public void setCrowding(int crowding) {
        this.crowding = crowding;
    }

    public double getCostMinutes() {
        return costMinutes;
    }

    public void setCostMinutes(double costMinutes) {
        this.costMinutes = costMinutes;
    }
}
