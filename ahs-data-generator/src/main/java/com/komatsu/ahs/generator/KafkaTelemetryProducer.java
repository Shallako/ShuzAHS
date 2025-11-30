package com.komatsu.ahs.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.komatsu.ahs.domain.events.VehicleTelemetryEvent;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kafka Telemetry Producer
 * 
 * Publishes generated telemetry data to Kafka topics for testing
 * the streaming pipeline.
 */
public class KafkaTelemetryProducer implements AutoCloseable {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTelemetryProducer.class);
    
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final TelemetryDataGenerator dataGenerator;
    
    public KafkaTelemetryProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        
        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.dataGenerator = new TelemetryDataGenerator();
        
        LOG.info("Kafka telemetry producer initialized with bootstrap servers: {}", bootstrapServers);
    }
    
    /**
     * Publish telemetry event to Kafka
     */
    public void publishTelemetry(String topic, String vehicleId, VehicleStatus status) {
        try {
            VehicleTelemetry telemetry = dataGenerator.generateTelemetry(vehicleId, status);
            VehicleTelemetryEvent event = new VehicleTelemetryEvent(vehicleId, telemetry);
            event.setSource("data-generator");
            event.setVehicleStatus(status);  // Include vehicle status in the event
            
            String json = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, vehicleId, json);
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOG.error("Failed to send telemetry for vehicle {}: {}", vehicleId, exception.getMessage());
                } else {
                    LOG.debug("Sent telemetry for vehicle {} to partition {}", 
                            vehicleId, metadata.partition());
                }
            });
            
        } catch (Exception e) {
            LOG.error("Error publishing telemetry for vehicle {}", vehicleId, e);
        }
    }
    
    /**
     * Flush pending messages
     */
    public void flush() {
        producer.flush();
        LOG.debug("Flushed pending messages");
    }
    
    @Override
    public void close() {
        producer.close();
        LOG.info("Kafka producer closed");
    }
}
