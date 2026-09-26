package com.cqu.model;

import java.util.List;

/**
 * 路径规划结果。
 *
 * 顶层字段对应当前请求偏好（profile）选中的路线，保持与旧版本兼容；
 * options 给出不同偏好下的候选路线及其差异解释，供页面对比展示。
 */
public class PathResult {
    private String startId;
    private String endId;
    private String profile;
    private String profileLabel;
    private List<RouteOption> options;
    /** 为什么默认推荐这条路（汇总说明） */
    private String summary;

    // ---- 以下为选中路线的扁平字段（向后兼容） ----
    private double totalDistanceMeters;
    private int totalSeconds;
    private List<String> pathNodeIds;
    private List<Node> pathNodes;
    /** @deprecated 使用 options/segments，仅为兼容旧前端保留 */
    @Deprecated
    private List<Double> segmentDistanceMeters;

    public PathResult() {
    }

    public String getStartId() {
        return startId;
    }

    public void setStartId(String startId) {
        this.startId = startId;
    }

    public String getEndId() {
        return endId;
    }

    public void setEndId(String endId) {
        this.endId = endId;
    }

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

    public List<RouteOption> getOptions() {
        return options;
    }

    public void setOptions(List<RouteOption> options) {
        this.options = options;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public List<Double> getSegmentDistanceMeters() {
        return segmentDistanceMeters;
    }

    public void setSegmentDistanceMeters(List<Double> segmentDistanceMeters) {
        this.segmentDistanceMeters = segmentDistanceMeters;
    }
}
