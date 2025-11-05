package com.ruleengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Next-Gen Rule Engine - Main Application
 *
 * A runtime-driven, in-memory, self-optimizing rule engine
 * with zero compilation time and sub-millisecond latency.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class RuleEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleEngineApplication.class, args);
    }
}
