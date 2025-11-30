package com.komatsu.ahs.telemetry.function;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Telemetry Deserializer
 * 
 * Deserializes JSON telemetry messages from Kafka into VehicleTelemetry objects.
 * Handles both raw VehicleTelemetry and wrapped VehicleTelemetryEvent formats.
 */
public class TelemetryDeserializer implements DeserializationSchema<VehicleTelemetry> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryDeserializer.class);
    
    private transient ObjectMapper objectMapper;
    
    @Override
    public void open(InitializationContext context) throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @Override
    public VehicleTelemetry deserialize(byte[] message) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
        
        try {
            // First, parse as a generic JSON tree to determine the format
            JsonNode rootNode = objectMapper.readTree(message);
            
            // Check if this is a VehicleTelemetryEvent (has "telemetry" field)
            if (rootNode.has("telemetry") && rootNode.get("telemetry").isObject()) {
                // Extract the nested telemetry object
                JsonNode telemetryNode = rootNode.get("telemetry");
                VehicleTelemetry telemetry = objectMapper.treeToValue(telemetryNode, VehicleTelemetry.class);
                
                // If vehicleId is not set in telemetry, get it from the event wrapper
                if (telemetry.getVehicleId() == null && rootNode.has("vehicleId")) {
                    telemetry.setVehicleId(rootNode.get("vehicleId").asText());
                }
                
                LOG.debug("Deserialized VehicleTelemetryEvent for vehicle: {}", telemetry.getVehicleId());
                return telemetry;
            } else {
                // Direct VehicleTelemetry format
                VehicleTelemetry telemetry = objectMapper.treeToValue(rootNode, VehicleTelemetry.class);
                LOG.debug("Deserialized direct VehicleTelemetry for vehicle: {}", telemetry.getVehicleId());
                return telemetry;
            }
        } catch (Exception e) {
            LOG.error("Failed to deserialize telemetry message: {}", new String(message), e);
            return null;
        }
    }
    
    @Override
    public boolean isEndOfStream(VehicleTelemetry nextElement) {
        return false;
    }
    
    @Override
    public TypeInformation<VehicleTelemetry> getProducedType() {
        return TypeInformation.of(VehicleTelemetry.class);
    }
}
