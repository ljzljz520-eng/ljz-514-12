package com.cqu.model;

/**
 * 路径中相邻两个景点之间一段连接的完整指标与耗时拆解，
 * 供页面解释“每一段路为什么花这么多时间”。
 */
public class SegmentInfo {
    private String fromId;
    private String toId;
    private String fromName;
    private String toName;
    private String mode;
    private String modeLabel;
    private double distanceMeters;
    /** CSV 中记录的基础步行/乘车耗时（秒） */
    private int baseSeconds;
    /** 爬坡附加（秒） */
    private double slopeSeconds;
    /** 拥挤延误（秒） */
    private double crowdSeconds;
    /** 换乘等候（秒） */
    private double transferSeconds;
    /** 综合耗时（秒）= 以上之和 */
    private double totalSeconds;
    private double slopePercent;
    /** 沿行进方向的爬升高度（米） */
    private double climbMeters;
    /** 沿行进方向的下降高度（米） */
    private double descentMeters;
    private boolean transfer;
    private int crowdLevel;
    private String crowdLabel;
    private String note;

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

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getModeLabel() {
        return modeLabel;
    }

    public void setModeLabel(String modeLabel) {
        this.modeLabel = modeLabel;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public int getBaseSeconds() {
        return baseSeconds;
    }

    public void setBaseSeconds(int baseSeconds) {
        this.baseSeconds = baseSeconds;
    }

    public double getSlopeSeconds() {
        return slopeSeconds;
    }

    public void setSlopeSeconds(double slopeSeconds) {
        this.slopeSeconds = slopeSeconds;
    }

    public double getCrowdSeconds() {
        return crowdSeconds;
    }

    public void setCrowdSeconds(double crowdSeconds) {
        this.crowdSeconds = crowdSeconds;
    }

    public double getTransferSeconds() {
        return transferSeconds;
    }

    public void setTransferSeconds(double transferSeconds) {
        this.transferSeconds = transferSeconds;
    }

    public double getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(double totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public double getSlopePercent() {
        return slopePercent;
    }

    public void setSlopePercent(double slopePercent) {
        this.slopePercent = slopePercent;
    }

    public double getClimbMeters() {
        return climbMeters;
    }

    public void setClimbMeters(double climbMeters) {
        this.climbMeters = climbMeters;
    }

    public double getDescentMeters() {
        return descentMeters;
    }

    public void setDescentMeters(double descentMeters) {
        this.descentMeters = descentMeters;
    }

    public boolean isTransfer() {
        return transfer;
    }

    public void setTransfer(boolean transfer) {
        this.transfer = transfer;
    }

    public int getCrowdLevel() {
        return crowdLevel;
    }

    public void setCrowdLevel(int crowdLevel) {
        this.crowdLevel = crowdLevel;
    }

    public String getCrowdLabel() {
        return crowdLabel;
    }

    public void setCrowdLabel(String crowdLabel) {
        this.crowdLabel = crowdLabel;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
