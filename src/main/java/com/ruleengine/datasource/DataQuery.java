package com.ruleengine.datasource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Query - Universal query abstraction
 *
 * Can represent:
 * - SQL queries
 * - NoSQL queries
 * - REST API calls
 * - File filters
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQuery {

    // Query string (SQL, MongoDB query, etc.)
    private String query;

    // Parameters for parameterized queries
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    // For REST APIs
    private String endpoint;
    private String method; // GET, POST, etc.

    // For file systems
    private String filePath;
    private String filePattern;

    // Pagination
    private Integer limit;
    private Integer offset;

    // Filters (for NoSQL, APIs)
    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    // Sorting
    private String sortBy;
    private SortOrder sortOrder;

    // Timeout
    private Long timeoutMs;

    public enum SortOrder {
        ASC, DESC
    }

    /**
     * Create SQL query
     */
    public static DataQuery sql(String sql) {
        return DataQuery.builder().query(sql).build();
    }

    /**
     * Create SQL query with parameters
     */
    public static DataQuery sql(String sql, Map<String, Object> params) {
        return DataQuery.builder().query(sql).parameters(params).build();
    }

    /**
     * Create REST API query
     */
    public static DataQuery rest(String endpoint, String method) {
        return DataQuery.builder().endpoint(endpoint).method(method).build();
    }

    /**
     * Create MongoDB query
     */
    public static DataQuery mongo(Map<String, Object> filters) {
        return DataQuery.builder().filters(filters).build();
    }
}
