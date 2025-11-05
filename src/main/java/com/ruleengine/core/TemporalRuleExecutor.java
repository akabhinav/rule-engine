package com.ruleengine.core;

import com.ruleengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporal Rule Executor
 *
 * Handles time-based rules with sliding/tumbling windows
 * Examples:
 * - "More than 5 transactions in 10 minutes"
 * - "Average amount > 1000 in last hour"
 * - "No activity in last 24 hours"
 */
@Component
public class TemporalRuleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TemporalRuleExecutor.class);

    // Store events per rule for window-based evaluation
    private final Map<String, List<TemporalEvent>> eventStore;

    public TemporalRuleExecutor() {
        this.eventStore = new ConcurrentHashMap<>();
    }

    /**
     * Execute temporal rule
     */
    public boolean evaluateTemporalRule(Rule rule, RuleContext context) {
        TemporalWindow window = rule.getTemporalWindow();
        if (window == null) {
            return true; // No temporal constraints
        }

        // Check if rule is currently valid
        Instant now = Instant.now();
        if (!window.isValid(now)) {
            logger.debug("Rule {} not valid at current time", rule.getId());
            return false;
        }

        // Get or create event list for this rule
        List<TemporalEvent> events = eventStore.computeIfAbsent(
                rule.getId(),
                k -> new ArrayList<>()
        );

        // Add current event
        TemporalEvent currentEvent = new TemporalEvent(
                now,
                context.getFacts()
        );
        events.add(currentEvent);

        // Clean old events outside window
        cleanOldEvents(events, window, now);

        // Evaluate based on window type
        return switch (window.getType()) {
            case SLIDING -> evaluateSlidingWindow(events, window, now);
            case TUMBLING -> evaluateTumblingWindow(events, window, now);
            case SESSION -> evaluateSessionWindow(events, window, now);
            case COUNT -> evaluateCountWindow(events, window);
        };
    }

    private boolean evaluateSlidingWindow(List<TemporalEvent> events, TemporalWindow window, Instant now) {
        Instant windowStart = now.minus(window.getWindowDuration());

        // Get events in window
        List<TemporalEvent> windowEvents = events.stream()
                .filter(e -> e.timestamp().isAfter(windowStart))
                .toList();

        return evaluateWindowCondition(windowEvents, window);
    }

    private boolean evaluateTumblingWindow(List<TemporalEvent> events, TemporalWindow window, Instant now) {
        // For tumbling windows, only consider events in current window
        long windowMillis = window.getWindowDuration().toMillis();
        long currentWindowStart = (now.toEpochMilli() / windowMillis) * windowMillis;
        Instant windowStart = Instant.ofEpochMilli(currentWindowStart);

        List<TemporalEvent> windowEvents = events.stream()
                .filter(e -> e.timestamp().isAfter(windowStart))
                .toList();

        return evaluateWindowCondition(windowEvents, window);
    }

    private boolean evaluateSessionWindow(List<TemporalEvent> events, TemporalWindow window, Instant now) {
        // Session window: events grouped by inactivity gap
        if (events.isEmpty()) {
            return false;
        }

        Duration sessionGap = window.getSlideInterval() != null
                ? window.getSlideInterval()
                : Duration.ofMinutes(30); // Default 30 min inactivity

        // Find current session (events with < sessionGap between them)
        List<TemporalEvent> currentSession = new ArrayList<>();
        Instant lastEventTime = now;

        for (int i = events.size() - 1; i >= 0; i--) {
            TemporalEvent event = events.get(i);
            Duration gap = Duration.between(event.timestamp(), lastEventTime);

            if (gap.compareTo(sessionGap) <= 0) {
                currentSession.add(0, event); // Add at beginning
                lastEventTime = event.timestamp();
            } else {
                break; // Gap too large, session ended
            }
        }

        return evaluateWindowCondition(currentSession, window);
    }

    private boolean evaluateCountWindow(List<TemporalEvent> events, TemporalWindow window) {
        // Count-based window: last N events
        Integer eventCount = window.getEventCount();
        if (eventCount == null) {
            return false;
        }

        int startIndex = Math.max(0, events.size() - eventCount);
        List<TemporalEvent> windowEvents = events.subList(startIndex, events.size());

        return evaluateWindowCondition(windowEvents, window);
    }

    private boolean evaluateWindowCondition(List<TemporalEvent> windowEvents, TemporalWindow window) {
        Integer requiredCount = window.getEventCount();
        if (requiredCount != null && windowEvents.size() < requiredCount) {
            return false;
        }

        // If aggregation specified, evaluate it
        if (window.getAggregation() != null && window.getAggregationField() != null) {
            return evaluateAggregation(windowEvents, window);
        }

        // Default: just check if we have enough events
        return requiredCount == null || windowEvents.size() >= requiredCount;
    }

    private boolean evaluateAggregation(List<TemporalEvent> events, TemporalWindow window) {
        String field = window.getAggregationField();
        TemporalWindow.AggregationType aggType = window.getAggregation();

        List<Double> values = events.stream()
                .map(e -> extractNumericValue(e.facts(), field))
                .filter(v -> v != null)
                .toList();

        if (values.isEmpty()) {
            return false;
        }

        double result = switch (aggType) {
            case COUNT -> values.size();
            case SUM -> values.stream().mapToDouble(Double::doubleValue).sum();
            case AVG -> values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MIN -> values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case MAX -> values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case FIRST -> values.get(0);
            case LAST -> values.get(values.size() - 1);
        };

        logger.debug("Aggregation {} on field {} = {}", aggType, field, result);

        // For now, just return true if aggregation succeeded
        // In real use, would compare against threshold
        return true;
    }

    private Double extractNumericValue(Map<String, Fact> facts, String field) {
        // Parse field path (e.g., "transaction.amount")
        String[] parts = field.split("\\.", 2);
        if (parts.length < 2) {
            return null;
        }

        Fact fact = facts.get(parts[0]);
        if (fact == null) {
            return null;
        }

        Object value = fact.getValue(parts[1]);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        return null;
    }

    private void cleanOldEvents(List<TemporalEvent> events, TemporalWindow window, Instant now) {
        if (window.getWindowDuration() == null) {
            return;
        }

        Instant cutoff = now.minus(window.getWindowDuration().multipliedBy(2)); // Keep 2x window for safety
        events.removeIf(e -> e.timestamp().isBefore(cutoff));
    }

    /**
     * Clear event store
     */
    public void clear() {
        eventStore.clear();
    }

    /**
     * Clear events for specific rule
     */
    public void clearRule(String ruleId) {
        eventStore.remove(ruleId);
    }

    /**
     * Get statistics
     */
    public TemporalStatistics getStatistics() {
        int totalRules = eventStore.size();
        long totalEvents = eventStore.values().stream()
                .mapToLong(List::size)
                .sum();

        return new TemporalStatistics(totalRules, totalEvents);
    }

    record TemporalEvent(Instant timestamp, Map<String, Fact> facts) {}

    public record TemporalStatistics(int trackedRules, long totalEvents) {}
}
