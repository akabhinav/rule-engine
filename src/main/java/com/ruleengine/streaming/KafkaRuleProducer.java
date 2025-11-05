package com.ruleengine.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruleengine.model.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Rule Producer
 *
 * Publishes rule results and events to Kafka
 */
@Component
public class KafkaRuleProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaRuleProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.results:rule-results}")
    private String resultsTopic;

    public KafkaRuleProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send rule result to Kafka
     */
    public void sendRuleResult(RuleResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            kafkaTemplate.send(resultsTopic, result.getRuleId(), json);
            logger.debug("Sent rule result to Kafka: {}", result.getRuleId());
        } catch (Exception e) {
            logger.error("Error sending rule result to Kafka", e);
        }
    }

    /**
     * Send custom event to Kafka
     */
    public void sendEvent(String topic, String key, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, json);
            logger.debug("Sent event to Kafka topic {}: {}", topic, key);
        } catch (Exception e) {
            logger.error("Error sending event to Kafka", e);
        }
    }
}
