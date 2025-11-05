package com.ruleengine.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a demo use case with pre-configured rules and scenarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoUseCase {
    private String id;
    private String name;
    private String description;
    private String category; // "complex" or "medium"
    private String icon;
    private String color;
    private String group; // Rule group for this use case
    private List<DemoScenario> scenarios;
    private Map<String, String> fieldDescriptions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemoScenario {
        private String name;
        private String description;
        private Map<String, Map<String, Object>> facts;
        private String expectedOutcome;
    }
}
