package com.ruleengine.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruleengine.model.Rule;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * JSON Rule Parser
 *
 * Parses rules from JSON format
 */
@Component
public class JsonRuleParser implements RuleParser {

    private final ObjectMapper objectMapper;

    public JsonRuleParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Rule parse(String ruleDefinition) {
        try {
            return objectMapper.readValue(ruleDefinition, Rule.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse rule from JSON", e);
        }
    }

    @Override
    public List<Rule> parseMultiple(String rulesDefinition) {
        try {
            return objectMapper.readValue(
                    rulesDefinition,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Rule.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse rules from JSON", e);
        }
    }

    @Override
    public Rule parseFromFile(String filePath) {
        try {
            return objectMapper.readValue(new File(filePath), Rule.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse rule from file: " + filePath, e);
        }
    }

    @Override
    public RuleFormat getFormat() {
        return RuleFormat.JSON;
    }

    /**
     * Serialize rule to JSON
     */
    public String toJson(Rule rule) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rule);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize rule to JSON", e);
        }
    }
}
