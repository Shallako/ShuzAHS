package com.komatsu.ahs.fleet.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.domain.events.FleetMetricsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Metrics Producer
 * 
 * Publishes fleet-wide aggregated metrics to Kafka for monitoring and analytics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.fleet-metrics}")
    private String metricsTopic;

    /**
     * Publish fleet metrics event
     */
    public void publishMetrics(FleetMetricsEvent metrics) {
        try {
            String message = objectMapper.writeValueAsString(metrics);
            kafkaTemplate.send(metricsTopic, "fleet", message);
            
            log.info("Published fleet metrics: {} total vehicles, {} active, avg speed: {:.1f} km/h", 
                metrics.getTotalVehicles(),
                metrics.getActiveVehicles(), 
                metrics.getAverageSpeed());
                
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize fleet metrics event", e);
        }
    }
}
