package com.ruleengine.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionSummary {
    private String ruleId;
    private String ruleName;
    private Instant timestamp;
    private boolean matched;
    private String status;
    private long executionTimeMs;
    private String errorMessage;
}
