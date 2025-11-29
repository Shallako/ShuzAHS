package com.komatsu.ahs.telemetry.function;

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
 * Deserializes JSON telemetry messages from Kafka into VehicleTelemetry objects
 */
public class TelemetryDeserializer implements DeserializationSchema<VehicleTelemetry> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryDeserializer.class);
    
    private transient ObjectMapper objectMapper;
    
    @Override
    public void open(InitializationContext context) throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public VehicleTelemetry deserialize(byte[] message) throws IOException {
        try {
            return objectMapper.readValue(message, VehicleTelemetry.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize telemetry message", e);
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
