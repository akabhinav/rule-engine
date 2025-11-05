package com.ruleengine.datasource;

import java.util.List;
import java.util.Map;

/**
 * Data Source Interface
 *
 * Universal abstraction for all data sources:
 * - Databases (SQL, NoSQL)
 * - REST APIs
 * - Files (CSV, JSON, XML)
 * - Cloud Storage (S3, Azure Blob)
 * - Message Queues
 * - Real-time databases
 */
public interface DataSource {

    /**
     * Execute a query and return results
     */
    List<Map<String, Object>> query(DataQuery query);

    /**
     * Fetch a single record by ID
     */
    Map<String, Object> fetchById(String id);

    /**
     * Check if connection is alive
     */
    boolean isConnected();

    /**
     * Close connection and release resources
     */
    void close();

    /**
     * Get data source type
     */
    DataSourceType getType();

    /**
     * Get data source name/identifier
     */
    String getName();

    /**
     * Test connection
     */
    boolean testConnection();

    /**
     * Data source types
     */
    enum DataSourceType {
        JDBC_DATABASE,
        MONGODB,
        REST_API,
        FILE_SYSTEM,
        CLOUD_STORAGE,
        KAFKA,
        REDIS,
        ELASTICSEARCH,
        CUSTOM
    }
}
