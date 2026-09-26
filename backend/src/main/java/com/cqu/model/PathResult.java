package com.cqu.model;

import java.util.List;

public class PathResult {
    private String startId;
    private String endId;
    private double totalDistanceMeters;
    /** 全程纯步行耗时合计（分钟，未含坡度/拥挤/换乘修正） */
    private double totalWalkMinutes;
    /** 全程综合耗时（分钟），即寻路时使用的权重 */
    private double totalCostMinutes;
    private int totalTransfers;
    private double maxSlopePercent;
    private double avgCrowding;
    private List<String> pathNodeIds;
    private List<Node> pathNodes;
    private List<Double> segmentDistanceMeters;
    /** 每一段路的详细属性（距离、步行耗时、坡度、换乘、拥挤、折算耗时） */
    private List<Segment> segments;
    /** 备选路线（按综合耗时升序），用于对比说明推荐路线为什么更合适 */
    private List<RouteSummary> alternatives;

    public PathResult() {
    }

    public PathResult(String startId, String endId, double totalDistanceMeters, List<String> pathNodeIds, List<Node> pathNodes, List<Double> segmentDistanceMeters) {
        this.startId = startId;
        this.endId = endId;
        this.totalDistanceMeters = totalDistanceMeters;
        this.pathNodeIds = pathNodeIds;
        this.pathNodes = pathNodes;
        this.segmentDistanceMeters = segmentDistanceMeters;
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

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public double getTotalWalkMinutes() {
        return totalWalkMinutes;
    }

    public void setTotalWalkMinutes(double totalWalkMinutes) {
        this.totalWalkMinutes = totalWalkMinutes;
    }

    public double getTotalCostMinutes() {
        return totalCostMinutes;
    }

    public void setTotalCostMinutes(double totalCostMinutes) {
        this.totalCostMinutes = totalCostMinutes;
    }

    public int getTotalTransfers() {
        return totalTransfers;
    }

    public void setTotalTransfers(int totalTransfers) {
        this.totalTransfers = totalTransfers;
    }

    public double getMaxSlopePercent() {
        return maxSlopePercent;
    }

    public void setMaxSlopePercent(double maxSlopePercent) {
        this.maxSlopePercent = maxSlopePercent;
    }

    public double getAvgCrowding() {
        return avgCrowding;
    }

    public void setAvgCrowding(double avgCrowding) {
        this.avgCrowding = avgCrowding;
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

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }

    public List<RouteSummary> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<RouteSummary> alternatives) {
        this.alternatives = alternatives;
    }
}
