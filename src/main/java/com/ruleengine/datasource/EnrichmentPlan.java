package com.ruleengine.datasource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Enrichment Plan
 *
 * Defines how to enrich a RuleContext with data from multiple sources
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentPlan {

    private String name;

    @Builder.Default
    private List<EnrichmentStep> steps = new ArrayList<>();

    /**
     * Add enrichment step
     */
    public EnrichmentPlan addStep(EnrichmentStep step) {
        steps.add(step);
        return this;
    }

    /**
     * Create a simple plan with one step
     */
    public static EnrichmentPlan simple(String dataSourceName, String factKey, DataQuery query) {
        return EnrichmentPlan.builder()
                .name("Simple Enrichment")
                .steps(List.of(
                        EnrichmentStep.builder()
                                .name("Fetch " + factKey)
                                .dataSourceName(dataSourceName)
                                .factKey(factKey)
                                .query(query)
                                .build()
                ))
                .build();
    }
}

/**
 * Enrichment Step
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class EnrichmentStep {

    private String name;
    private String dataSourceName;
    private String factKey;
    private DataQuery query;

    @Builder.Default
    private boolean cacheEnabled = true;

    // Optional transformation function
    private Function<List<Map<String, Object>>, Object> transform;
}
