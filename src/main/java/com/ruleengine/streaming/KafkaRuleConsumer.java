package com.ruleengine.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruleengine.model.Fact;
import com.ruleengine.model.RuleContext;
import com.ruleengine.model.RuleResult;
import com.ruleengine.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Kafka Rule Consumer
 *
 * Consumes events from Kafka and evaluates rules in real-time
 */
@Component
public class KafkaRuleConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaRuleConsumer.class);

    private final RuleService ruleService;
    private final ObjectMapper objectMapper;
    private final KafkaRuleProducer producer;

    public KafkaRuleConsumer(
            RuleService ruleService,
            ObjectMapper objectMapper,
            KafkaRuleProducer producer) {
        this.ruleService = ruleService;
        this.objectMapper = objectMapper;
        this.producer = producer;
    }

    /**
     * Listen for events and execute rules
     */
    @KafkaListener(topics = "${kafka.topics.events:rule-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEvent(String message) {
        try {
            logger.debug("Received event: {}", message);

            // Parse event
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);

            // Extract rule group and facts
            String ruleGroup = (String) eventData.get("ruleGroup");
            String tenant = (String) eventData.get("tenant");

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> facts = (Map<String, Map<String, Object>>) eventData.get("facts");

            // Create context
            RuleContext context = RuleContext.withFacts(facts);
            if (tenant != null) {
                context.setTenant(tenant);
            }

            // Execute rules
            List<RuleResult> results;
            if (ruleGroup != null) {
                results = ruleService.executeRuleGroup(ruleGroup, context);
            } else if (tenant != null) {
                results = ruleService.executeForTenant(tenant, context);
            } else {
                logger.warn("Event missing ruleGroup or tenant");
                return;
            }

            // Send results to output topic
            for (RuleResult result : results) {
                if (result.isMatched()) {
                    producer.sendRuleResult(result);
                }
            }

            logger.info("Processed event with {} rule matches",
                    results.stream().filter(RuleResult::isMatched).count());

        } catch (Exception e) {
            logger.error("Error processing event", e);
        }
    }

    /**
     * Listen for rule updates
     */
    @KafkaListener(topics = "${kafka.topics.rule-updates:rule-updates}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRuleUpdate(String message) {
        try {
            logger.info("Received rule update: {}", message);

            // Parse rule update
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = objectMapper.readValue(message, Map.class);

            String action = (String) updateData.get("action");
            String ruleJson = (String) updateData.get("rule");

            switch (action) {
                case "CREATE", "UPDATE" -> {
                    ruleService.createRuleFromJson(ruleJson);
                    logger.info("Processed rule {} via Kafka", action);
                }
                case "DELETE" -> {
                    String ruleId = (String) updateData.get("ruleId");
                    ruleService.deleteRule(ruleId);
                    logger.info("Deleted rule {} via Kafka", ruleId);
                }
                default -> logger.warn("Unknown rule update action: {}", action);
            }

        } catch (Exception e) {
            logger.error("Error processing rule update", e);
        }
    }
}
