package com.ruleengine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetrics {
    private long totalRules;
    private long activeRules;
    private long disabledRules;
    private long totalExecutions;
    private long totalMatches;
    private long totalErrors;
    private double avgExecutionTimeMs;
    private double successRate;
    private double matchRate;
    private long cacheHits;
    private long cacheMisses;
    private double cacheHitRate;
    private Map<String, Long> executionsByHour;
    private Map<String, Long> executionsByStatus;
}
