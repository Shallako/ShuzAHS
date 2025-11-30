package com.komatsu.ahs.stream.cep;

import com.komatsu.ahs.domain.events.VehicleAlertEvent;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.stream.AlertSerializationSchema;
import com.komatsu.ahs.stream.TelemetryDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Flink streaming job for CEP-based alert processing.
 * Consumes telemetry from Kafka, applies CEP patterns to detect alerts,
 * and publishes alerts to Kafka for Fleet Management service consumption.
 * 
 * Alert Flow:
 * 1. Telemetry events consumed from ahs.telemetry.processed
 * 2. CEP patterns applied to detect anomalies
 * 3. VehicleAlertEvent published to ahs.vehicle.alerts
 * 4. Fleet Management consumes alerts and updates Prometheus metrics
 * 5. Grafana visualizes metrics
 */
public class CepAlertStreamingJob {
    
    private static final Logger LOG = LoggerFactory.getLogger(CepAlertStreamingJob.class);
    
    // Configuration - can be externalized
    private static final String KAFKA_BROKERS = System.getenv().getOrDefault(
            "KAFKA_BROKERS", "kafka:29092");
    private static final String TELEMETRY_TOPIC = System.getenv().getOrDefault(
            "TELEMETRY_TOPIC", "ahs.telemetry.processed");
    private static final String ALERTS_TOPIC = System.getenv().getOrDefault(
            "ALERTS_TOPIC", "ahs.vehicle.alerts");
    private static final String CONSUMER_GROUP = "ahs-cep-processor";
    
    public static void main(String[] args) throws Exception {
        LOG.info("Starting CEP Alert Streaming Job");
        LOG.info("Kafka Brokers: {}", KAFKA_BROKERS);
        LOG.info("Telemetry Topic: {}", TELEMETRY_TOPIC);
        LOG.info("Alerts Topic: {}", ALERTS_TOPIC);
        
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Enable checkpointing for fault tolerance
        env.enableCheckpointing(30000); // checkpoint every 30 seconds
        
        // Create Kafka source
        KafkaSource<VehicleTelemetry> telemetrySource = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BROKERS)
                .setTopics(TELEMETRY_TOPIC)
                .setGroupId(CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializationSchema())
                .build();
        
        // Create Kafka sink for alerts
        KafkaSink<VehicleAlertEvent> alertSink = KafkaSink.<VehicleAlertEvent>builder()
                .setBootstrapServers(KAFKA_BROKERS)
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(ALERTS_TOPIC)
                        .setValueSerializationSchema(new AlertSerializationSchema())
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
        
        // Watermark strategy for event time processing
        WatermarkStrategy<VehicleTelemetry> watermarkStrategy = WatermarkStrategy
                .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((telemetry, timestamp) -> 
                        telemetry.getTimestamp().toEpochMilli());
        
        // Create telemetry stream
        DataStream<VehicleTelemetry> telemetryStream = env
                .fromSource(telemetrySource, watermarkStrategy, "Kafka Telemetry Source");
        
        // Apply CEP alert processing
        CepAlertProcessor alertProcessor = new CepAlertProcessor();
        DataStream<VehicleAlertEvent> alertStream = alertProcessor.processAlerts(telemetryStream);
        
        // Log alerts for debugging/monitoring
        alertStream
                .map(alert -> {
                    LOG.warn("ALERT: [{}] {} - Vehicle: {} - {}", 
                            alert.getSeverity(),
                            alert.getAlertType(),
                            alert.getVehicleId(),
                            alert.getMessage());
                    return alert;
                })
                .name("Alert Logging");
        
        // Sink alerts to Kafka for Fleet Management consumption
        alertStream
                .sinkTo(alertSink)
                .name("Kafka Alert Sink");
        
        LOG.info("CEP Alert Pipeline configured: {} -> CEP Patterns -> {}", 
                TELEMETRY_TOPIC, ALERTS_TOPIC);
        
        // Execute the job
        env.execute("AHS CEP Alert Processing Job");
    }
}
