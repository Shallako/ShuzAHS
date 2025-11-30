package com.komatsu.ahs.telemetry;

import com.komatsu.ahs.telemetry.function.TelemetryDeserializer;
import com.komatsu.ahs.telemetry.function.TelemetryAlertFunction;
import com.komatsu.ahs.telemetry.function.TelemetryWindowAggregator;
import com.komatsu.ahs.telemetry.function.AnomalyDetectionPatterns;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


/**
 * Flink Telemetry Processor Job
 * 
 * Main Flink streaming job that processes vehicle telemetry data in real-time.
 * 
 * Features:
 * - Consumes telemetry data from Kafka
 * - Performs real-time analytics and aggregations
 * - Detects anomalies and generates alerts
 * - Outputs processed metrics back to Kafka
 * 
 * @author Komatsu MTS Team
 */
public class TelemetryProcessorJob {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryProcessorJob.class);
    
    // Kafka configuration (env-overridable for Docker and local runs)
    private static final String KAFKA_BOOTSTRAP_SERVERS =
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:29092");
    private static final String INPUT_TOPIC =
        System.getenv().getOrDefault("INPUT_TOPIC", "ahs.telemetry.processed");
    private static final String OUTPUT_METRICS_TOPIC =
        System.getenv().getOrDefault("OUTPUT_METRICS_TOPIC", "ahs.fleet.metrics");
    private static final String OUTPUT_ALERTS_TOPIC =
        System.getenv().getOrDefault("OUTPUT_ALERTS_TOPIC", "ahs.vehicle.alerts");
    private static final String CONSUMER_GROUP =
        System.getenv().getOrDefault("CONSUMER_GROUP", "telemetry-processor-group");
    
    public static void main(String[] args) throws Exception {
        LOG.info("Starting AHS Telemetry Processor Job");
        
        // Set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Configure checkpointing for fault tolerance
        env.enableCheckpointing(60000); // checkpoint every 60 seconds
        env.getCheckpointConfig().setCheckpointTimeout(120000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30000);
        
        // Configure Kafka source
        KafkaSource<VehicleTelemetry> source = KafkaSource.<VehicleTelemetry>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setTopics(INPUT_TOPIC)
            .setGroupId(CONSUMER_GROUP)
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new TelemetryDeserializer())
            .build();
        
        // Create telemetry stream
        DataStream<VehicleTelemetry> telemetryStream = env
            .fromSource(source, WatermarkStrategy
                .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((telemetry, timestamp) -> 
                    telemetry.getTimestamp().toEpochMilli()),
                "Kafka Vehicle Telemetry Source");
        
        // Process telemetry and generate alerts
        DataStream<TelemetryAlert> basicAlerts = telemetryStream
            .flatMap(new TelemetryAlertFunction());
        
        // Advanced anomaly detection using CEP
        DataStream<TelemetryAlert> rapidDecelerationAlerts = 
            AnomalyDetectionPatterns.detectRapidDeceleration(telemetryStream);
        
        DataStream<TelemetryAlert> lowFuelAlerts = 
            AnomalyDetectionPatterns.detectLowFuel(telemetryStream);
        
        DataStream<TelemetryAlert> overheatingAlerts = 
            AnomalyDetectionPatterns.detectOverheating(telemetryStream);
        
        // Union all alert streams
        DataStream<TelemetryAlert> alerts = basicAlerts
            .union(rapidDecelerationAlerts)
            .union(lowFuelAlerts)
            .union(overheatingAlerts);
        
        // Aggregate metrics per vehicle in 1-minute windows
        DataStream<VehicleMetrics> metrics = telemetryStream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
            .aggregate(new TelemetryWindowAggregator())
            .name("Vehicle Metrics Aggregation");
        
        // Configure Kafka sinks
        KafkaSink<String> alertsSink = KafkaSink.<String>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(OUTPUT_ALERTS_TOPIC)
                .setValueSerializationSchema(new SimpleStringSchema())
                .build())
            .build();
        
        KafkaSink<String> metricsSink = KafkaSink.<String>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                .setTopic(OUTPUT_METRICS_TOPIC)
                .setValueSerializationSchema(new SimpleStringSchema())
                .build())
            .build();
        
        // Write alerts and metrics to Kafka
        alerts
            .map(TelemetryAlert::toJson)
            .sinkTo(alertsSink)
            .name("Alerts Kafka Sink");
        
        metrics
            .map(VehicleMetrics::toJson)
            .sinkTo(metricsSink)
            .name("Metrics Kafka Sink");
        
        // Execute the job
        LOG.info("Executing Telemetry Processor Job with parallelism: {}", 
                env.getParallelism());
        env.execute("AHS Telemetry Processor");
    }
}
