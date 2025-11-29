package com.komatsu.ahs.fleet.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.domain.events.VehicleCommandEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Vehicle Command Producer
 * 
 * Publishes vehicle command events to Kafka for processing by autonomous vehicles
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.vehicle-commands}")
    private String commandTopic;
    
    /**
     * Send vehicle command event
     */
    public void sendCommand(VehicleCommandEvent command) {
        try {
            String message = objectMapper.writeValueAsString(command);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(commandTopic, command.getVehicleId(), message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent command {} to vehicle {}: offset={}", 
                        command.getCommandType(), 
                        command.getVehicleId(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send command to vehicle {}", 
                        command.getVehicleId(), ex);
                }
            });
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize command event", e);
        }
    }

    /**
     * Send route assignment command
     */
    public void sendRouteAssignment(String vehicleId, String routePayload) {
        VehicleCommandEvent command = new VehicleCommandEvent(
            vehicleId,
            VehicleCommandEvent.CommandType.ROUTE_ASSIGNMENT,
            routePayload
        );
        command.setPriority(VehicleCommandEvent.Priority.NORMAL);
        sendCommand(command);
    }
    
    /**
     * Send emergency stop command
     */
    public void sendEmergencyStop(String vehicleId) {
        VehicleCommandEvent command = new VehicleCommandEvent(
            vehicleId,
            VehicleCommandEvent.CommandType.EMERGENCY_STOP,
            "IMMEDIATE_STOP"
        );
        command.setPriority(VehicleCommandEvent.Priority.CRITICAL);
        sendCommand(command);
    }
}
