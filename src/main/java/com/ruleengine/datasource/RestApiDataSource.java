package com.ruleengine.datasource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Data Source Connector
 *
 * Supports:
 * - GET, POST, PUT, DELETE requests
 * - JSON responses
 * - Header management
 * - Authentication (Basic, Bearer)
 * - Query parameters
 */
@Component
public class RestApiDataSource implements DataSource {

    private static final Logger logger = LoggerFactory.getLogger(RestApiDataSource.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Map<String, String> defaultHeaders;

    public RestApiDataSource() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = "";
        this.defaultHeaders = new HashMap<>();
    }

    public RestApiDataSource(String baseUrl, Map<String, String> headers) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
        this.defaultHeaders = headers != null ? headers : new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> query(DataQuery query) {
        try {
            String url = buildUrl(query);
            HttpMethod method = HttpMethod.valueOf(query.getMethod() != null ? query.getMethod() : "GET");
            HttpEntity<?> entity = buildHttpEntity(query);

            logger.debug("REST API Request: {} {}", method, url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    method,
                    entity,
                    String.class
            );

            return parseResponse(response.getBody());

        } catch (Exception e) {
            logger.error("REST API call failed", e);
            throw new DataSourceException("REST API call failed", e);
        }
    }

    @Override
    public Map<String, Object> fetchById(String id) {
        DataQuery query = DataQuery.rest(baseUrl + "/" + id, "GET");
        List<Map<String, Object>> results = query(query);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public boolean isConnected() {
        return testConnection();
    }

    @Override
    public void close() {
        logger.info("REST API DataSource closed");
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.REST_API;
    }

    @Override
    public String getName() {
        return "rest-api-datasource";
    }

    @Override
    public boolean testConnection() {
        try {
            if (baseUrl.isEmpty()) {
                return false;
            }
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl,
                    HttpMethod.GET,
                    buildHttpEntity(null),
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build full URL with query parameters
     */
    private String buildUrl(DataQuery query) {
        StringBuilder url = new StringBuilder();

        if (query.getEndpoint() != null) {
            url.append(query.getEndpoint().startsWith("http")
                    ? query.getEndpoint()
                    : baseUrl + query.getEndpoint());
        } else {
            url.append(baseUrl);
        }

        // Add query parameters
        if (query.getParameters() != null && !query.getParameters().isEmpty()) {
            url.append("?");
            query.getParameters().forEach((key, value) ->
                    url.append(key).append("=").append(value).append("&"));
            url.deleteCharAt(url.length() - 1); // Remove last &
        }

        return url.toString();
    }

    /**
     * Build HTTP entity with headers and body
     */
    private HttpEntity<?> buildHttpEntity(DataQuery query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add default headers
        defaultHeaders.forEach(headers::set);

        Object body = query != null ? query.getFilters() : null;
        return new HttpEntity<>(body, headers);
    }

    /**
     * Parse JSON response to List of Maps
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String jsonResponse) {
        try {
            Object parsed = objectMapper.readValue(jsonResponse, Object.class);

            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            } else if (parsed instanceof Map) {
                List<Map<String, Object>> result = new ArrayList<>();
                result.add((Map<String, Object>) parsed);
                return result;
            }

            return List.of();

        } catch (Exception e) {
            logger.error("Failed to parse JSON response", e);
            throw new DataSourceException("Failed to parse JSON response", e);
        }
    }

    /**
     * Set authentication header
     */
    public void setBearerToken(String token) {
        defaultHeaders.put("Authorization", "Bearer " + token);
    }

    /**
     * Set basic authentication
     */
    public void setBasicAuth(String username, String password) {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        defaultHeaders.put("Authorization", "Basic " + encodedAuth);
    }
}
