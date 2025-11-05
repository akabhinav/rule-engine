package com.ruleengine.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * JDBC Data Source Connector
 *
 * Supports:
 * - PostgreSQL
 * - MySQL
 * - Oracle
 * - SQL Server
 * - H2
 * - Any JDBC-compliant database
 */
@Component
public class JdbcDataSource implements com.ruleengine.datasource.DataSource {

    private static final Logger logger = LoggerFactory.getLogger(JdbcDataSource.class);

    private final DataSource dataSource;
    private final String name;

    public JdbcDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.name = "jdbc-datasource";
    }

    @Override
    public List<Map<String, Object>> query(DataQuery query) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn, query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            logger.debug("Query executed: {} rows returned", results.size());

        } catch (SQLException e) {
            logger.error("Error executing query", e);
            throw new DataSourceException("Query execution failed", e);
        }

        return results;
    }

    @Override
    public Map<String, Object> fetchById(String id) {
        // Generic fetch by id - assumes 'id' column
        DataQuery query = DataQuery.sql(
                "SELECT * FROM data WHERE id = ?",
                Map.of("id", id)
        );
        List<Map<String, Object>> results = query(query);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean isConnected() {
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void close() {
        // DataSource managed by Spring, no explicit close needed
        logger.info("JDBC DataSource closed");
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.JDBC_DATABASE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }

    /**
     * Prepare statement with parameters
     */
    private PreparedStatement prepareStatement(Connection conn, DataQuery query) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(query.getQuery());

        // Set parameters
        if (query.getParameters() != null && !query.getParameters().isEmpty()) {
            int index = 1;
            for (Object value : query.getParameters().values()) {
                stmt.setObject(index++, value);
            }
        }

        // Set timeout if specified
        if (query.getTimeoutMs() != null) {
            stmt.setQueryTimeout((int) (query.getTimeoutMs() / 1000));
        }

        // Set limit if specified
        if (query.getLimit() != null) {
            stmt.setMaxRows(query.getLimit());
        }

        return stmt;
    }

    /**
     * Execute update/insert/delete
     */
    public int executeUpdate(DataQuery query) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStatement(conn, query)) {

            int rows = stmt.executeUpdate();
            logger.debug("Update executed: {} rows affected", rows);
            return rows;

        } catch (SQLException e) {
            logger.error("Error executing update", e);
            throw new DataSourceException("Update execution failed", e);
        }
    }

    /**
     * Execute batch operations
     */
    public int[] executeBatch(List<DataQuery> queries) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                for (DataQuery query : queries) {
                    stmt.addBatch(query.getQuery());
                }

                int[] results = stmt.executeBatch();
                conn.commit();

                logger.debug("Batch executed: {} queries", queries.size());
                return results;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            logger.error("Error executing batch", e);
            throw new DataSourceException("Batch execution failed", e);
        }
    }
}
