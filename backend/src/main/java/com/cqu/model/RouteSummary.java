package com.cqu.model;

import java.util.List;

/**
 * 一条候选路线的汇总指标，用于在页面上与推荐路线对比。
 */
public class RouteSummary {
    private List<String> pathNodeIds;
    private double totalDistanceMeters;
    private double totalWalkMinutes;
    private double totalCostMinutes;
    private int totalTransfers;
    private double maxSlopePercent;
    private double avgCrowding;

    public RouteSummary() {
    }

    public RouteSummary(List<String> pathNodeIds, double totalDistanceMeters, double totalWalkMinutes,
                        double totalCostMinutes, int totalTransfers, double maxSlopePercent, double avgCrowding) {
        this.pathNodeIds = pathNodeIds;
        this.totalDistanceMeters = totalDistanceMeters;
        this.totalWalkMinutes = totalWalkMinutes;
        this.totalCostMinutes = totalCostMinutes;
        this.totalTransfers = totalTransfers;
        this.maxSlopePercent = maxSlopePercent;
        this.avgCrowding = avgCrowding;
    }

    public List<String> getPathNodeIds() {
        return pathNodeIds;
    }

    public void setPathNodeIds(List<String> pathNodeIds) {
        this.pathNodeIds = pathNodeIds;
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
}
