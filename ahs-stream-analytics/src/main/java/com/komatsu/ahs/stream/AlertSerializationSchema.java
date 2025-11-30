package com.komatsu.ahs.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.events.VehicleAlertEvent;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka serializer for VehicleAlertEvent JSON messages.
 * Used to publish CEP-generated alerts to the ahs.vehicle.alerts topic
 * for consumption by the Fleet Management service and Grafana visualization.
 */
public class AlertSerializationSchema implements SerializationSchema<VehicleAlertEvent> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AlertSerializationSchema.class);
    private transient ObjectMapper objectMapper;
    
    @Override
    public void open(InitializationContext context) {
        initializeObjectMapper();
    }
    
    private void initializeObjectMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public byte[] serialize(VehicleAlertEvent alert) {
        if (objectMapper == null) {
            initializeObjectMapper();
        }
        try {
            return objectMapper.writeValueAsBytes(alert);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to serialize VehicleAlertEvent: {}", alert, e);
            return new byte[0];
        }
    }
}
