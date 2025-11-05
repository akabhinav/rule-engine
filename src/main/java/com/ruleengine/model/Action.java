package com.ruleengine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Action Model - Represents an action to execute when rule matches
 *
 * Supports:
 * - Setting fact values
 * - Triggering other rules
 * - Executing scripts (sandboxed)
 * - Calling external services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    private String id;
    private ActionType type;

    // For SET_FACT type
    private String factKey;
    private Object factValue;
    private String valueExpression; // Dynamic value calculation

    // For TRIGGER_RULE type
    private String ruleId;

    // For EXECUTE_SCRIPT type
    private String script;
    private ScriptLanguage scriptLanguage;

    // For CALL_SERVICE type
    private String serviceUrl;
    private String serviceMethod;
    @Builder.Default
    private Map<String, Object> serviceParams = new HashMap<>();

    // For EMIT_EVENT type
    private String eventTopic;
    private Object eventPayload;

    // Metadata
    private String description;
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public enum ActionType {
        SET_FACT,       // Modify fact in current context
        TRIGGER_RULE,   // Trigger another rule
        EXECUTE_SCRIPT, // Execute script (JS/Groovy)
        CALL_SERVICE,   // Call external service
        EMIT_EVENT,     // Emit event to Kafka/message bus
        ENRICH_RESULT,  // Add data to rule result
        LOG             // Log message
    }

    public enum ScriptLanguage {
        JAVASCRIPT,
        GROOVY,
        MVEL
    }
}
