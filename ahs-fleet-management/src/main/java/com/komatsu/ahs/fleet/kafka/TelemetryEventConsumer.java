package com.komatsu.ahs.fleet.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.komatsu.ahs.domain.events.VehicleTelemetryEvent;
import com.komatsu.ahs.fleet.service.FleetManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Telemetry Event Consumer
 * 
 * Consumes processed telemetry events from Kafka and updates
 * fleet management state accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryEventConsumer {

    private final FleetManagementService fleetService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
        topics = "${kafka.topics.vehicle-telemetry}",
        groupId = "${kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTelemetryEvent(String message) {
        try {
            VehicleTelemetryEvent event = objectMapper.readValue(
                message, VehicleTelemetryEvent.class);
            
            log.info("Received telemetry for vehicle: {}", event.getVehicleId());
            
            // Update vehicle telemetry in fleet management
            if (event.getTelemetry() != null) {
                fleetService.updateVehicleTelemetry(
                    event.getVehicleId(), 
                    event.getTelemetry()
                );
            }
            
            // Update vehicle status if present
            if (event.getVehicleStatus() != null) {
                fleetService.updateVehicleStatus(
                    event.getVehicleId(),
                    event.getVehicleStatus()
                );
            }
            
        } catch (Exception e) {
            log.error("Error processing telemetry event", e);
        }
    }
}
