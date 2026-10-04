package com.marketflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardMetricsResponse {

    private long totalWorkflows;
    private long activeWorkflows;
    private long totalExecutions;
    private long successfulExecutions;
    private long failedExecutions;
    private double successRatePercentage;

    public DashboardMetricsResponse() {}

    public DashboardMetricsResponse(long totalWorkflows, long activeWorkflows, long totalExecutions,
                                    long successfulExecutions, long failedExecutions, double successRatePercentage) {
        this.totalWorkflows = totalWorkflows;
        this.activeWorkflows = activeWorkflows;
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.successRatePercentage = successRatePercentage;
    }

    public long getTotalWorkflows() {
        return totalWorkflows;
    }

    public void setTotalWorkflows(long totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }

    public long getActiveWorkflows() {
        return activeWorkflows;
    }

    public void setActiveWorkflows(long activeWorkflows) {
        this.activeWorkflows = activeWorkflows;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public void setSuccessfulExecutions(long successfulExecutions) {
        this.successfulExecutions = successfulExecutions;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public double getSuccessRatePercentage() {
        return successRatePercentage;
    }

    public void setSuccessRatePercentage(double successRatePercentage) {
        this.successRatePercentage = successRatePercentage;
    }
}
