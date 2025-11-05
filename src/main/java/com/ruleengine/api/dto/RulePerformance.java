package com.ruleengine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulePerformance {
    private String ruleId;
    private String ruleName;
    private long executionCount;
    private long matchCount;
    private double avgExecutionTimeMs;
    private double matchRate;
    private long errorCount;
}
