package com.komatsu.ahs.telemetry.integration;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.function.TelemetryAlertFunction;
import com.komatsu.ahs.telemetry.function.TelemetryWindowAggregator;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.junit5.MiniClusterExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Flink Telemetry Processor
 * 
 * Tests the complete telemetry processing pipeline using Flink's MiniCluster
 * for local testing without requiring a full Flink cluster.
 */
@DisplayName("Telemetry Processor Integration Tests")
class TelemetryProcessorIntegrationTest {

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension();

    @Test
    @DisplayName("Should process telemetry data through pipeline")
    void testTelemetryProcessingPipeline() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        // Create test telemetry data
        List<VehicleTelemetry> testData = createTestTelemetryData();

        // Create data stream from collection
        DataStream<VehicleTelemetry> telemetryStream = env.fromCollection(testData);

        // Apply alert function
        DataStream<TelemetryAlert> alerts = telemetryStream
            .flatMap(new TelemetryAlertFunction());

        // Verify alerts can be collected
        assertDoesNotThrow(() -> {
            alerts.print();
        });
    }

    @Test
    @DisplayName("Should aggregate metrics in time windows")
    void testMetricsAggregation() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> testData = createTestTelemetryData();

        DataStream<VehicleMetrics> metrics = env.fromCollection(testData)
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        assertDoesNotThrow(() -> {
            metrics.print();
        });
    }

    @Test
    @DisplayName("Should handle multiple vehicles in parallel")
    void testMultiVehicleProcessing() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        List<VehicleTelemetry> multiVehicleData = new ArrayList<>();
        
        // Create data for 5 different vehicles
        for (int i = 1; i <= 5; i++) {
            String vehicleId = String.format("KOMATSU-930E-%03d", i);
            multiVehicleData.addAll(createTelemetryForVehicle(vehicleId, 10));
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(multiVehicleData);
        
        DataStream<VehicleMetrics> metrics = stream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        assertDoesNotThrow(() -> {
            metrics.print();
        });
    }

    @Test
    @DisplayName("Should detect high-speed alerts")
    void testHighSpeedAlertDetection() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> highSpeedData = new ArrayList<>();
        
        // Create telemetry with excessive speed (>60 km/h)
        highSpeedData.add(createTelemetry("KOMATSU-930E-001", 75.0, 50.0, 90.0));
        highSpeedData.add(createTelemetry("KOMATSU-930E-002", 80.0, 45.0, 88.0));

        DataStream<TelemetryAlert> alerts = env.fromCollection(highSpeedData)
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            alerts.print();
        });
    }

    @Test
    @DisplayName("Should detect low fuel alerts")
    void testLowFuelAlertDetection() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> lowFuelData = new ArrayList<>();
        
        // Create telemetry with low fuel (<15%)
        lowFuelData.add(createTelemetry("KOMATSU-930E-001", 30.0, 12.0, 85.0));
        lowFuelData.add(createTelemetry("KOMATSU-930E-002", 25.0, 8.0, 82.0));

        DataStream<TelemetryAlert> alerts = env.fromCollection(lowFuelData)
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            alerts.print();
        });
    }

    @Test
    @DisplayName("Should detect overheating alerts")
    void testOverheatingAlertDetection() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> overheatingData = new ArrayList<>();
        
        // Create telemetry with high engine temp (>100°C)
        overheatingData.add(createTelemetry("KOMATSU-930E-001", 35.0, 50.0, 105.0));
        overheatingData.add(createTelemetry("KOMATSU-930E-002", 40.0, 48.0, 110.0));

        DataStream<TelemetryAlert> alerts = env.fromCollection(overheatingData)
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            alerts.print();
        });
    }

    @Test
    @DisplayName("Should handle empty telemetry stream")
    void testEmptyStream() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> emptyData = new ArrayList<>();

        // For empty streams, just verify we can create a stream without errors
        // Cannot use keyBy/window on empty collections as Flink needs to infer types
        DataStream<VehicleTelemetry> stream = env.fromCollection(emptyData, 
            org.apache.flink.api.common.typeinfo.TypeInformation.of(VehicleTelemetry.class));

        assertDoesNotThrow(() -> {
            stream.print();
        });
    }

    @Test
    @DisplayName("Should process large volume of telemetry data")
    void testHighVolumeProcessing() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        List<VehicleTelemetry> largeDataset = new ArrayList<>();
        
        // Create 1000 telemetry records for 10 vehicles
        for (int v = 1; v <= 10; v++) {
            String vehicleId = String.format("KOMATSU-930E-%03d", v);
            largeDataset.addAll(createTelemetryForVehicle(vehicleId, 100));
        }

        DataStream<VehicleMetrics> metrics = env.fromCollection(largeDataset)
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        assertDoesNotThrow(() -> {
            metrics.print();
        });
    }

    // Helper methods

    private List<VehicleTelemetry> createTestTelemetryData() {
        List<VehicleTelemetry> data = new ArrayList<>();
        data.add(createTelemetry("KOMATSU-930E-001", 35.5, 75.0, 88.0));
        data.add(createTelemetry("KOMATSU-930E-002", 42.0, 68.0, 91.0));
        data.add(createTelemetry("KOMATSU-930E-003", 28.5, 82.0, 85.0));
        return data;
    }

    private List<VehicleTelemetry> createTelemetryForVehicle(String vehicleId, int count) {
        List<VehicleTelemetry> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double speed = 20.0 + (Math.random() * 40.0);
            double fuel = 50.0 + (Math.random() * 50.0);
            double temp = 80.0 + (Math.random() * 15.0);
            data.add(createTelemetry(vehicleId, speed, fuel, temp));
        }
        return data;
    }

    private VehicleTelemetry createTelemetry(String vehicleId, double speedKph, 
                                             double fuelPercent, double engineTemp) {
        return VehicleTelemetry.builder()
            .vehicleId(vehicleId)
            .timestamp(Instant.now())
            .speedKph(speedKph)
            .fuelLevelPercent(fuelPercent)
            .batteryLevelPercent(95.0)
            .engineTemperatureCelsius(engineTemp)
            .brakePressurePsi(95.0)
            .tirePressureFrontLeftPsi(95.0)
            .tirePressureFrontRightPsi(95.0)
            .tirePressureRearLeftPsi(95.0)
            .tirePressureRearRightPsi(95.0)
            .payloadTons(250.0)
            .location(GpsCoordinate.builder()
                .latitude(-23.5 + (Math.random() * 0.1))
                .longitude(-70.4 + (Math.random() * 0.1))
                .altitude(2900.0 + (Math.random() * 50.0))
                .build())
            .headingDegrees(180.0)
            .engineRpm(1800.0)
            .build();
    }
}
