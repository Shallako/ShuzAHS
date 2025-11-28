package com.komatsu.ahs.stream;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Advanced Flink job with windowing and aggregations
 * Computes vehicle metrics over 1-minute tumbling windows
 */
public class VehicleMetricsStreamingJob {
    
    private static final Logger LOG = LoggerFactory.getLogger(VehicleMetricsStreamingJob.class);
    
    private static final String KAFKA_BROKERS = "localhost:9092";
    private static final String TELEMETRY_TOPIC = "ahs.telemetry";
    private static final String CONSUMER_GROUP = "ahs-metrics-processor";
    
    public static void main(String[] args) throws Exception {
        
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        env.enableCheckpointing(10000);
        env.setParallelism(4);
        
        // Create Kafka source
        KafkaSource<VehicleTelemetry> telemetrySource = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BROKERS)
                .setTopics(TELEMETRY_TOPIC)
                .setGroupId(CONSUMER_GROUP)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializationSchema())
                .build();
        
        WatermarkStrategy<VehicleTelemetry> watermarkStrategy = WatermarkStrategy
                .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((telemetry, timestamp) -> 
                    telemetry.getTimestamp().toEpochMilli());
        
        DataStream<VehicleTelemetry> telemetryStream = env
                .fromSource(telemetrySource, watermarkStrategy, "Kafka Telemetry Source");
        
        // Compute metrics over 1-minute tumbling windows
        DataStream<VehicleMetrics> metricsStream = telemetryStream
                .keyBy(VehicleTelemetry::getVehicleId)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .aggregate(new VehicleMetricsAggregator())
                .name("Vehicle Metrics Aggregator");
        
        // Print metrics to console (in production, write to Kafka or database)
        metricsStream.print();
        
        // Filter for critical alerts (high temperature or low fuel)
        metricsStream
                .filter(metrics -> 
                    metrics.getMaxEngineTemperature() > 95.0 || 
                    metrics.getAvgFuelLevelPercent() < 20.0 ||
                    metrics.getCriticalAlertCount() > 0)
                .map(metrics -> {
                    LOG.warn("CRITICAL ALERT for vehicle {}: maxTemp={}, avgFuel={}, criticalAlerts={}",
                            metrics.getVehicleId(),
                            metrics.getMaxEngineTemperature(),
                            metrics.getAvgFuelLevelPercent(),
                            metrics.getCriticalAlertCount());
                    return metrics;
                })
                .name("Critical Alert Filter");
        
        env.execute("AHS Vehicle Metrics Streaming Job");
    }
}
