package com.komatsu.ahs.stream;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Flink streaming job for real-time telemetry processing
 * Consumes telemetry data from Kafka and performs analytics
 */
public class TelemetryStreamingJob {
    
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryStreamingJob.class);
    
    private static final String KAFKA_BROKERS = "kafka:29092";
    private static final String TELEMETRY_TOPIC = "ahs.telemetry.processed";
    private static final String CONSUMER_GROUP = "ahs-telemetry-processor";
    
    public static void main(String[] args) throws Exception {
        
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Disable checkpointing for now (enable later with proper configuration)
        // env.enableCheckpointing(10000); // checkpoint every 10 seconds
        
        // Create Kafka source for telemetry data
        KafkaSource<VehicleTelemetry> telemetrySource = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BROKERS)
                .setTopics(TELEMETRY_TOPIC)
                .setGroupId(CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializationSchema())
                .build();
        
        // Create watermark strategy for event time processing
        WatermarkStrategy<VehicleTelemetry> watermarkStrategy = WatermarkStrategy
                .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((telemetry, timestamp) -> 
                    telemetry.getTimestamp().toEpochMilli());
        
        // Create telemetry stream
        DataStream<VehicleTelemetry> telemetryStream = env
                .fromSource(telemetrySource, watermarkStrategy, "Kafka Telemetry Source");
        
        // Process telemetry data
        telemetryStream
                .keyBy(VehicleTelemetry::getVehicleId)
                .map(telemetry -> {
                    LOG.info("Processing telemetry for vehicle: {} at speed: {} km/h",
                            telemetry.getVehicleId(),
                            telemetry.getSpeedKph());
                    return telemetry;
                })
                .name("Telemetry Processor");
        
        // Execute the streaming job
        env.execute("AHS Telemetry Streaming Job");
    }
}
