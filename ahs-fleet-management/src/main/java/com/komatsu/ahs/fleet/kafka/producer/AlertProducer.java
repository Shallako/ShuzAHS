package com.komatsu.ahs.fleet.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.domain.events.VehicleAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Alert Producer
 * 
 * Publishes vehicle alert events to Kafka for monitoring and response
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.vehicle-alerts}")
    private String alertTopic;

    /**
     * Publish alert event
     */
    public void publishAlert(VehicleAlertEvent alert) {
        try {
            String message = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(alertTopic, alert.getVehicleId(), message);
            
            log.info("Published {} alert for vehicle {}: {}", 
                alert.getSeverity(), 
                alert.getVehicleId(), 
                alert.getMessage());
                
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize alert event", e);
        }
    }
    
    /**
     * Publish critical alert
     */
    public void publishCriticalAlert(String vehicleId, 
                                     VehicleAlertEvent.AlertType alertType, 
                                     String message) {
        VehicleAlertEvent alert = new VehicleAlertEvent(
            vehicleId, 
            alertType, 
            VehicleAlertEvent.Severity.CRITICAL, 
            message
        );
        publishAlert(alert);
    }
}
