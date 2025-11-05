package com.ruleengine.batch;

import com.ruleengine.datasource.DataQuery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch Process Job Configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessJob {

    private String name;

    // Data source
    private String dataSourceName;
    private DataQuery query;
    private String factKey; // Key to use for the fact in context

    // Rules to execute
    private String ruleId;
    private String ruleGroup;

    // Batch configuration
    @Builder.Default
    private int batchSize = 1000;

    @Builder.Default
    private boolean parallel = true;

    /**
     * Create simple batch job
     */
    public static BatchProcessJob simple(
            String name,
            String dataSourceName,
            DataQuery query,
            String ruleGroup) {

        return BatchProcessJob.builder()
                .name(name)
                .dataSourceName(dataSourceName)
                .query(query)
                .ruleGroup(ruleGroup)
                .factKey("data")
                .build();
    }
}
