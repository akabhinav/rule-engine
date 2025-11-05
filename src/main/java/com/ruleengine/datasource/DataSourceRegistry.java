package com.ruleengine.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Source Registry
 *
 * Central registry for all data sources
 */
@Component
public class DataSourceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * Register a data source
     */
    public void registerDataSource(String name, DataSource dataSource) {
        dataSources.put(name, dataSource);
        logger.info("Registered data source: {} ({})", name, dataSource.getType());
    }

    /**
     * Get data source by name
     */
    public DataSource getDataSource(String name) {
        return dataSources.get(name);
    }

    /**
     * Remove data source
     */
    public void removeDataSource(String name) {
        DataSource removed = dataSources.remove(name);
        if (removed != null) {
            removed.close();
            logger.info("Removed data source: {}", name);
        }
    }

    /**
     * Get all data sources
     */
    public Map<String, DataSource> getAllDataSources() {
        return new ConcurrentHashMap<>(dataSources);
    }

    /**
     * Test all connections
     */
    public Map<String, Boolean> testAllConnections() {
        Map<String, Boolean> results = new ConcurrentHashMap<>();
        dataSources.forEach((name, ds) -> results.put(name, ds.testConnection()));
        return results;
    }

    /**
     * Close all data sources
     */
    public void closeAll() {
        dataSources.values().forEach(DataSource::close);
        dataSources.clear();
        logger.info("All data sources closed");
    }
}
