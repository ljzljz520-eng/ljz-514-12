package com.cqu.model;

import java.util.List;

/**
 * 一条完整的候选路线：同一起终点下，不同寻路偏好可能给出不同的路线。
 */
public class RouteOption {
    /** 该路线由哪种偏好计算得到 */
    private String profile;
    private String profileLabel;
    /** 是否为当前请求偏好对应的结果 */
    private boolean selected;
    /** 是否为系统默认推荐（综合耗时最短） */
    private boolean recommended;
    /** 一句话亮点，如“全程步行无需换乘” */
    private String highlight;
    /** 备选路线相对推荐路线差异的中文解释 */
    private List<String> reasons;

    private double totalDistanceMeters;
    private int totalSeconds;
    private int baseSeconds;
    private int slopeSeconds;
    private int crowdSeconds;
    private int transferSeconds;
    private int transferCount;
    private int crowdedSegmentCount;
    /** 全程累计爬升（米） */
    private double totalClimbMeters;
    /** 全程累计下降（米） */
    private double totalDescentMeters;

    private List<String> pathNodeIds;
    private List<Node> pathNodes;
    private List<SegmentInfo> segments;

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getProfileLabel() {
        return profileLabel;
    }

    public void setProfileLabel(String profileLabel) {
        this.profileLabel = profileLabel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(int totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public int getBaseSeconds() {
        return baseSeconds;
    }

    public void setBaseSeconds(int baseSeconds) {
        this.baseSeconds = baseSeconds;
    }

    public int getSlopeSeconds() {
        return slopeSeconds;
    }

    public void setSlopeSeconds(int slopeSeconds) {
        this.slopeSeconds = slopeSeconds;
    }

    public int getCrowdSeconds() {
        return crowdSeconds;
    }

    public void setCrowdSeconds(int crowdSeconds) {
        this.crowdSeconds = crowdSeconds;
    }

    public int getTransferSeconds() {
        return transferSeconds;
    }

    public void setTransferSeconds(int transferSeconds) {
        this.transferSeconds = transferSeconds;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }

    public int getCrowdedSegmentCount() {
        return crowdedSegmentCount;
    }

    public void setCrowdedSegmentCount(int crowdedSegmentCount) {
        this.crowdedSegmentCount = crowdedSegmentCount;
    }

    public double getTotalClimbMeters() {
        return totalClimbMeters;
    }

    public void setTotalClimbMeters(double totalClimbMeters) {
        this.totalClimbMeters = totalClimbMeters;
    }

    public double getTotalDescentMeters() {
        return totalDescentMeters;
    }

    public void setTotalDescentMeters(double totalDescentMeters) {
        this.totalDescentMeters = totalDescentMeters;
    }

    public List<String> getPathNodeIds() {
        return pathNodeIds;
    }

    public void setPathNodeIds(List<String> pathNodeIds) {
        this.pathNodeIds = pathNodeIds;
    }

    public List<Node> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<Node> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<SegmentInfo> getSegments() {
        return segments;
    }

    public void setSegments(List<SegmentInfo> segments) {
        this.segments = segments;
    }
}
