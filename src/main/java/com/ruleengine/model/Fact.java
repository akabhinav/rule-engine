package com.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fact - Schema-less data container
 *
 * Features:
 * - No predefined schema (accepts any JSON/Map)
 * - Nested field access (e.g., "user.profile.age")
 * - Type-safe value retrieval
 * - Metadata tracking
 */
@Data
@NoArgsConstructor
public class Fact {

    private String id;
    private String type; // e.g., "Transaction", "User", "Event"
    private Instant timestamp;

    // Schema-less data storage
    private Map<String, Object> data = new HashMap<>();

    // Metadata
    private Map<String, Object> metadata = new HashMap<>();

    public Fact(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.timestamp = Instant.now();
    }

    /**
     * Get value by simple or nested path
     * Examples:
     *   - "amount" -> data.get("amount")
     *   - "user.name" -> data.get("user").get("name")
     *   - "transaction.details.currency" -> nested navigation
     */
    @SuppressWarnings("unchecked")
    public Object getValue(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                // Try reflection for POJO access
                try {
                    current = getFieldValue(current, part);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return current;
    }

    /**
     * Set value by path
     */
    @SuppressWarnings("unchecked")
    public void setValue(String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);

            if (!(next instanceof Map)) {
                next = new HashMap<String, Object>();
                current.put(part, next);
            }

            current = (Map<String, Object>) next;
        }

        current.put(parts[parts.length - 1], value);
    }

    /**
     * Type-safe getters
     */
    public String getAsString(String path) {
        Object value = getValue(path);
        return value != null ? value.toString() : null;
    }

    public Integer getAsInteger(String path) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    public Long getAsLong(String path) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public Double getAsDouble(String path) {
        Object value = getValue(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    public Boolean getAsBoolean(String path) {
        Object value = getValue(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAsType(String path, Class<T> type) {
        Object value = getValue(path);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }

    /**
     * JSON serialization support
     */
    @JsonAnyGetter
    public Map<String, Object> getData() {
        return data;
    }

    @JsonAnySetter
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Reflection helper for POJO field access
     */
    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        if (obj == null) {
            return null;
        }

        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            // Try getter method
            String methodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                var method = obj.getClass().getMethod(methodName);
                return method.invoke(obj);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("Field or getter not found: " + fieldName);
            }
        }
    }

    /**
     * Create Fact from Map
     */
    public static Fact fromMap(String type, Map<String, Object> data) {
        return new Fact(type, data);
    }

    /**
     * Merge another fact into this one
     */
    public void merge(Fact other) {
        if (other != null && other.data != null) {
            this.data.putAll(other.data);
        }
    }
}
