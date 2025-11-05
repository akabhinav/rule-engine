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
public class RuleSummary {
    private String id;
    private String name;
    private String description;
    private Integer priority;
    private Integer salience;
    private Boolean enabled;
    private String ruleGroup;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private long executionCount;
    private double avgExecutionTimeMs;
}
