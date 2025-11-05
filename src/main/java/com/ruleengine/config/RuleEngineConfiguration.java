package com.ruleengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Rule Engine Configuration
 */
@Configuration
@ConfigurationProperties(prefix = "rule-engine")
public class RuleEngineConfiguration {

    private ExecutionSettings execution = new ExecutionSettings();
    private CacheSettings cache = new CacheSettings();
    private OptimizationSettings optimization = new OptimizationSettings();
    private MonitoringSettings monitoring = new MonitoringSettings();

    /**
     * Thread pool for parallel rule execution
     */
    @Bean(name = "ruleExecutor")
    public Executor ruleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(execution.threadPoolSize);
        executor.setMaxPoolSize(execution.threadPoolSize * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("rule-exec-");
        executor.initialize();
        return executor;
    }

    // Getters and setters
    public ExecutionSettings getExecution() {
        return execution;
    }

    public void setExecution(ExecutionSettings execution) {
        this.execution = execution;
    }

    public CacheSettings getCache() {
        return cache;
    }

    public void setCache(CacheSettings cache) {
        this.cache = cache;
    }

    public OptimizationSettings getOptimization() {
        return optimization;
    }

    public void setOptimization(OptimizationSettings optimization) {
        this.optimization = optimization;
    }

    public MonitoringSettings getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringSettings monitoring) {
        this.monitoring = monitoring;
    }

    public static class ExecutionSettings {
        private int threadPoolSize = 20;
        private long maxRuleExecutionTimeMs = 1000;
        private boolean enableParallelExecution = true;

        // Getters and setters
        public int getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }

        public long getMaxRuleExecutionTimeMs() {
            return maxRuleExecutionTimeMs;
        }

        public void setMaxRuleExecutionTimeMs(long maxRuleExecutionTimeMs) {
            this.maxRuleExecutionTimeMs = maxRuleExecutionTimeMs;
        }

        public boolean isEnableParallelExecution() {
            return enableParallelExecution;
        }

        public void setEnableParallelExecution(boolean enableParallelExecution) {
            this.enableParallelExecution = enableParallelExecution;
        }
    }

    public static class CacheSettings {
        private int hotRulesSize = 1000;
        private int factCacheSize = 5000;
        private int ttlMinutes = 60;

        // Getters and setters
        public int getHotRulesSize() {
            return hotRulesSize;
        }

        public void setHotRulesSize(int hotRulesSize) {
            this.hotRulesSize = hotRulesSize;
        }

        public int getFactCacheSize() {
            return factCacheSize;
        }

        public void setFactCacheSize(int factCacheSize) {
            this.factCacheSize = factCacheSize;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    public static class OptimizationSettings {
        private boolean enableJitCompilation = true;
        private int jitThreshold = 100;
        private boolean enableRuleIndexing = true;

        // Getters and setters
        public boolean isEnableJitCompilation() {
            return enableJitCompilation;
        }

        public void setEnableJitCompilation(boolean enableJitCompilation) {
            this.enableJitCompilation = enableJitCompilation;
        }

        public int getJitThreshold() {
            return jitThreshold;
        }

        public void setJitThreshold(int jitThreshold) {
            this.jitThreshold = jitThreshold;
        }

        public boolean isEnableRuleIndexing() {
            return enableRuleIndexing;
        }

        public void setEnableRuleIndexing(boolean enableRuleIndexing) {
            this.enableRuleIndexing = enableRuleIndexing;
        }
    }

    public static class MonitoringSettings {
        private boolean enableAuditLog = true;
        private boolean enableMetrics = true;
        private boolean traceExecution = true;

        // Getters and setters
        public boolean isEnableAuditLog() {
            return enableAuditLog;
        }

        public void setEnableAuditLog(boolean enableAuditLog) {
            this.enableAuditLog = enableAuditLog;
        }

        public boolean isEnableMetrics() {
            return enableMetrics;
        }

        public void setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }

        public boolean isTraceExecution() {
            return traceExecution;
        }

        public void setTraceExecution(boolean traceExecution) {
            this.traceExecution = traceExecution;
        }
    }
}
