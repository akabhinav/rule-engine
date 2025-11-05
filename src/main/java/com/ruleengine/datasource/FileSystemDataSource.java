package com.ruleengine.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * File System Data Source Connector
 *
 * Supports:
 * - JSON files
 * - CSV files
 * - XML files
 * - Text files
 * - Directory scanning
 * - Pattern matching
 */
@Component
public class FileSystemDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemDataSource.class);

    private final ObjectMapper objectMapper;
    private final String basePath;

    public FileSystemDataSource() {
        this.objectMapper = new ObjectMapper();
        this.basePath = System.getProperty("user.dir");
    }

    public FileSystemDataSource(String basePath) {
        this.objectMapper = new ObjectMapper();
        this.basePath = basePath;
    }

    @Override
    public List<Map<String, Object>> query(DataQuery query) {
        try {
            String filePath = query.getFilePath() != null
                    ? query.getFilePath()
                    : basePath;

            File file = new File(filePath);

            if (file.isDirectory()) {
                return queryDirectory(file, query);
            } else {
                return queryFile(file);
            }

        } catch (Exception e) {
            logger.error("File system query failed", e);
            throw new DataSourceException("File system query failed", e);
        }
    }

    @Override
    public Map<String, Object> fetchById(String id) {
        // For file system, id is the file path
        DataQuery query = DataQuery.builder().filePath(id).build();
        List<Map<String, Object>> results = query(query);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean isConnected() {
        return Files.exists(Paths.get(basePath));
    }

    @Override
    public void close() {
        logger.info("FileSystem DataSource closed");
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.FILE_SYSTEM;
    }

    @Override
    public String getName() {
        return "filesystem-datasource";
    }

    @Override
    public boolean testConnection() {
        return isConnected();
    }

    /**
     * Query a directory
     */
    private List<Map<String, Object>> queryDirectory(File directory, DataQuery query) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        String pattern = query.getFilePattern();

        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> pattern == null || p.toString().matches(pattern))
                    .limit(query.getLimit() != null ? query.getLimit() : Long.MAX_VALUE)
                    .forEach(path -> {
                        try {
                            results.addAll(queryFile(path.toFile()));
                        } catch (Exception e) {
                            logger.warn("Error reading file: {}", path, e);
                        }
                    });
        }

        return results;
    }

    /**
     * Query a single file
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryFile(File file) {
        try {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".json")) {
                return readJsonFile(file);
            } else if (fileName.endsWith(".csv")) {
                return readCsvFile(file);
            } else if (fileName.endsWith(".txt")) {
                return readTextFile(file);
            } else {
                logger.warn("Unsupported file type: {}", fileName);
                return List.of();
            }

        } catch (Exception e) {
            logger.error("Error reading file: {}", file.getPath(), e);
            return List.of();
        }
    }

    /**
     * Read JSON file
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readJsonFile(File file) throws IOException {
        Object data = objectMapper.readValue(file, Object.class);

        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        } else if (data instanceof Map) {
            return List.of((Map<String, Object>) data);
        }

        return List.of();
    }

    /**
     * Read CSV file
     */
    private List<Map<String, Object>> readCsvFile(File file) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        if (lines.isEmpty()) {
            return results;
        }

        // First line is header
        String[] headers = lines.get(0).split(",");

        // Read data rows
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            Map<String, Object> row = new HashMap<>();

            for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                row.put(headers[j].trim(), values[j].trim());
            }

            results.add(row);
        }

        return results;
    }

    /**
     * Read text file
     */
    private List<Map<String, Object>> readTextFile(File file) throws IOException {
        String content = Files.readString(file.toPath());
        Map<String, Object> data = Map.of(
                "fileName", file.getName(),
                "content", content,
                "size", file.length(),
                "lastModified", file.lastModified()
        );
        return List.of(data);
    }
}
