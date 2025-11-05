package com.ruleengine.parser;

import com.ruleengine.model.Rule;

import java.util.List;

/**
 * Rule Parser Interface
 *
 * Converts external formats (JSON, YAML, XML) to Rule objects
 */
public interface RuleParser {

    /**
     * Parse a single rule from string
     */
    Rule parse(String ruleDefinition);

    /**
     * Parse multiple rules from string
     */
    List<Rule> parseMultiple(String rulesDefinition);

    /**
     * Parse rule from file
     */
    Rule parseFromFile(String filePath);

    /**
     * Get supported format
     */
    RuleFormat getFormat();

    /**
     * Supported formats
     */
    enum RuleFormat {
        JSON,
        YAML,
        XML
    }
}
