package com.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Temporal Window - For time-based rule evaluation
 *
 * Examples:
 * - "More than 5 transactions in 10 minutes"
 * - "Average amount > 1000 in last hour"
 * - "No activity in last 24 hours"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalWindow {

    private WindowType type;

    // Time Window
    private Duration windowDuration; // e.g., 10 minutes
    private Integer eventCount; // e.g., count > 5 in window

    // Sliding vs Tumbling
    private Duration slideInterval;

    // Time constraints
    private Instant validFrom;
    private Instant validUntil;

    // Aggregation
    private AggregationType aggregation;
    private String aggregationField;

    public enum WindowType {
        SLIDING,   // Continuous window (e.g., last 10 minutes)
        TUMBLING,  // Fixed intervals (e.g., every hour)
        SESSION,   // Based on activity (e.g., 30 min inactivity = new session)
        COUNT      // Based on event count
    }

    public enum AggregationType {
        COUNT,
        SUM,
        AVG,
        MIN,
        MAX,
        FIRST,
        LAST
    }

    /**
     * Check if current time is within valid range
     */
    public boolean isValid(Instant now) {
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }
        return true;
    }
}
