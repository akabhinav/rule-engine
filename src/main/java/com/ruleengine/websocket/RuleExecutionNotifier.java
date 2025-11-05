package com.ruleengine.websocket;

import com.ruleengine.model.RuleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Rule Execution Notifier
 *
 * Broadcasts rule execution results via WebSocket
 * to connected dashboard clients for real-time monitoring
 */
@Component
public class RuleExecutionNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Notify about rule execution
     *
     * Sends execution result to all subscribed clients
     */
    public void notifyExecution(RuleResult result) {
        ExecutionNotification notification = ExecutionNotification.builder()
            .ruleId(result.getRuleId())
            .ruleName(result.getRuleName())
            .matched(result.isMatched())
            .status(result.getStatus().name())
            .executionTimeMs(result.getExecutionTimeMs())
            .timestamp(Instant.now())
            .build();

        // Send to /topic/executions
        messagingTemplate.convertAndSend("/topic/executions", notification);
    }

    /**
     * Notify about metrics update
     *
     * Sends updated metrics to dashboard
     */
    public void notifyMetricsUpdate(Map<String, Object> metrics) {
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }

    /**
     * Notify about rule change
     *
     * Notifies clients when a rule is created, updated, or deleted
     */
    public void notifyRuleChange(String eventType, String ruleId, String ruleName) {
        RuleChangeNotification notification = RuleChangeNotification.builder()
            .eventType(eventType)
            .ruleId(ruleId)
            .ruleName(ruleName)
            .timestamp(Instant.now())
            .build();

        messagingTemplate.convertAndSend("/topic/rule-changes", notification);
    }

    /**
     * Execution notification DTO
     */
    public static class ExecutionNotification {
        private String ruleId;
        private String ruleName;
        private boolean matched;
        private String status;
        private long executionTimeMs;
        private Instant timestamp;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String ruleId;
            private String ruleName;
            private boolean matched;
            private String status;
            private long executionTimeMs;
            private Instant timestamp;

            public Builder ruleId(String ruleId) {
                this.ruleId = ruleId;
                return this;
            }

            public Builder ruleName(String ruleName) {
                this.ruleName = ruleName;
                return this;
            }

            public Builder matched(boolean matched) {
                this.matched = matched;
                return this;
            }

            public Builder status(String status) {
                this.status = status;
                return this;
            }

            public Builder executionTimeMs(long executionTimeMs) {
                this.executionTimeMs = executionTimeMs;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public ExecutionNotification build() {
                ExecutionNotification notification = new ExecutionNotification();
                notification.ruleId = this.ruleId;
                notification.ruleName = this.ruleName;
                notification.matched = this.matched;
                notification.status = this.status;
                notification.executionTimeMs = this.executionTimeMs;
                notification.timestamp = this.timestamp;
                return notification;
            }
        }

        // Getters
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public boolean isMatched() { return matched; }
        public String getStatus() { return status; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Rule change notification DTO
     */
    public static class RuleChangeNotification {
        private String eventType; // CREATED, UPDATED, DELETED
        private String ruleId;
        private String ruleName;
        private Instant timestamp;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String eventType;
            private String ruleId;
            private String ruleName;
            private Instant timestamp;

            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }

            public Builder ruleId(String ruleId) {
                this.ruleId = ruleId;
                return this;
            }

            public Builder ruleName(String ruleName) {
                this.ruleName = ruleName;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public RuleChangeNotification build() {
                RuleChangeNotification notification = new RuleChangeNotification();
                notification.eventType = this.eventType;
                notification.ruleId = this.ruleId;
                notification.ruleName = this.ruleName;
                notification.timestamp = this.timestamp;
                return notification;
            }
        }

        // Getters
        public String getEventType() { return eventType; }
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public Instant getTimestamp() { return timestamp; }
    }
}
