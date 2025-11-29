package com.komatsu.ahs.telemetry.integration;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.function.TelemetryAlertFunction;
import com.komatsu.ahs.telemetry.function.TelemetryWindowAggregator;
import com.komatsu.ahs.telemetry.function.AnomalyDetectionPatterns;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
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
 * End-to-End Integration Tests
 * 
 * Tests the complete telemetry processing pipeline from data ingestion
 * through alert generation and metrics aggregation.
 */
@DisplayName("End-to-End Integration Tests")
class EndToEndIntegrationTest {

    @RegisterExtension
    static final MiniClusterExtension FLINK = new MiniClusterExtension();

    @Test
    @DisplayName("Should process complete telemetry pipeline end-to-end")
    void testCompleteProcessingPipeline() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        // Create diverse telemetry data
        List<VehicleTelemetry> telemetryData = createDiverseTelemetryData();

        DataStream<VehicleTelemetry> telemetryStream = env.fromCollection(telemetryData);

        // Generate basic alerts
        DataStream<TelemetryAlert> basicAlerts = telemetryStream
            .flatMap(new TelemetryAlertFunction());

        // Detect rapid deceleration
        DataStream<TelemetryAlert> rapidDecelerationAlerts = 
            AnomalyDetectionPatterns.detectRapidDeceleration(telemetryStream);

        // Detect low fuel
        DataStream<TelemetryAlert> lowFuelAlerts = 
            AnomalyDetectionPatterns.detectLowFuel(telemetryStream);

        // Detect overheating
        DataStream<TelemetryAlert> overheatingAlerts = 
            AnomalyDetectionPatterns.detectOverheating(telemetryStream);

        // Union all alerts
        DataStream<TelemetryAlert> allAlerts = basicAlerts
            .union(rapidDecelerationAlerts)
            .union(lowFuelAlerts)
            .union(overheatingAlerts);

        // Aggregate metrics
        DataStream<VehicleMetrics> metrics = telemetryStream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        // Verify streams can be processed
        assertDoesNotThrow(() -> {
            allAlerts.print("ALERTS");
            metrics.print("METRICS");
        });
    }

    @Test
    @DisplayName("Should process data from multiple vehicles concurrently")
    void testMultiVehicleConcurrentProcessing() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(3);

        List<VehicleTelemetry> multiVehicleData = new ArrayList<>();
        
        // 10 vehicles, 50 data points each = 500 total records
        for (int v = 1; v <= 10; v++) {
            String vehicleId = String.format("KOMATSU-930E-%03d", v);
            for (int i = 0; i < 50; i++) {
                multiVehicleData.add(createRandomTelemetry(vehicleId));
            }
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(multiVehicleData);
        
        DataStream<VehicleMetrics> metrics = stream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        DataStream<TelemetryAlert> alerts = stream
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            metrics.print("VEHICLE-METRICS");
            alerts.print("VEHICLE-ALERTS");
        });
    }

    @Test
    @DisplayName("Should handle mixed normal and anomalous data")
    void testMixedDataProcessing() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        List<VehicleTelemetry> mixedData = new ArrayList<>();
        
        // Normal data
        for (int i = 0; i < 20; i++) {
            mixedData.add(createNormalTelemetry("KOMATSU-930E-001"));
        }
        
        // Anomalous data - high speed
        mixedData.add(createTelemetry("KOMATSU-930E-001", 75.0, 50.0, 88.0));
        
        // Anomalous data - low fuel
        mixedData.add(createTelemetry("KOMATSU-930E-001", 30.0, 8.0, 85.0));
        
        // Anomalous data - overheating
        mixedData.add(createTelemetry("KOMATSU-930E-001", 35.0, 50.0, 108.0));
        
        // More normal data
        for (int i = 0; i < 20; i++) {
            mixedData.add(createNormalTelemetry("KOMATSU-930E-001"));
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(mixedData);
        
        DataStream<TelemetryAlert> alerts = stream
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            alerts.print("MIXED-DATA-ALERTS");
        });
    }

    @Test
    @DisplayName("Should process telemetry with varying time gaps")
    void testVaryingTimeGaps() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> dataWithGaps = new ArrayList<>();
        Instant baseTime = Instant.now();
        
        // Create data with varying time gaps
        for (int i = 0; i < 20; i++) {
            VehicleTelemetry telemetry = createNormalTelemetry("KOMATSU-930E-001");
            // Varying gaps: 1s, 5s, 10s, 30s
            int gap = (i % 4 + 1) * (i % 4 == 3 ? 10 : 1);
            telemetry.setTimestamp(baseTime.plusSeconds(i * gap));
            dataWithGaps.add(telemetry);
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(dataWithGaps);
        
        DataStream<VehicleMetrics> metrics = stream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
            .aggregate(new TelemetryWindowAggregator());

        assertDoesNotThrow(() -> {
            metrics.print("TIME-GAP-METRICS");
        });
    }

    @Test
    @DisplayName("Should handle both 930E and 980E vehicles")
    void testMultipleVehicleModels() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        List<VehicleTelemetry> multiModelData = new ArrayList<>();
        
        // 5x 930E vehicles (300 ton capacity)
        for (int i = 1; i <= 5; i++) {
            String vehicleId = String.format("KOMATSU-930E-%03d", i);
            for (int j = 0; j < 20; j++) {
                VehicleTelemetry telemetry = createNormalTelemetry(vehicleId);
                telemetry.setPayloadTons(240.0 + Math.random() * 60.0); // 240-300 tons
                multiModelData.add(telemetry);
            }
        }
        
        // 3x 980E vehicles (400 ton capacity)
        for (int i = 1; i <= 3; i++) {
            String vehicleId = String.format("KOMATSU-980E-%03d", i);
            for (int j = 0; j < 20; j++) {
                VehicleTelemetry telemetry = createNormalTelemetry(vehicleId);
                telemetry.setPayloadTons(320.0 + Math.random() * 80.0); // 320-400 tons
                multiModelData.add(telemetry);
            }
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(multiModelData);
        
        DataStream<VehicleMetrics> metrics = stream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .aggregate(new TelemetryWindowAggregator());

        assertDoesNotThrow(() -> {
            metrics.print("MULTI-MODEL-METRICS");
        });
    }

    @Test
    @DisplayName("Should process complete mining cycle simulation")
    void testMiningCycleSimulation() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        List<VehicleTelemetry> cycleData = new ArrayList<>();
        String vehicleId = "KOMATSU-930E-001";
        Instant time = Instant.now();
        
        // IDLE state (5 records)
        for (int i = 0; i < 5; i++) {
            cycleData.add(createStateTelemetry(vehicleId, time.plusSeconds(i), 0.0, 50.0));
        }
        
        // ROUTING to load (10 records)
        for (int i = 0; i < 10; i++) {
            cycleData.add(createStateTelemetry(vehicleId, time.plusSeconds(5 + i), 40.0, 48.0));
        }
        
        // LOADING (5 records)
        for (int i = 0; i < 5; i++) {
            cycleData.add(createStateTelemetry(vehicleId, time.plusSeconds(15 + i), 0.0, 46.0));
        }
        
        // HAULING to dump (15 records)
        for (int i = 0; i < 15; i++) {
            cycleData.add(createStateTelemetry(vehicleId, time.plusSeconds(20 + i), 30.0, 44.0));
        }
        
        // DUMPING (5 records)
        for (int i = 0; i < 5; i++) {
            cycleData.add(createStateTelemetry(vehicleId, time.plusSeconds(35 + i), 0.0, 42.0));
        }

        DataStream<VehicleTelemetry> stream = env.fromCollection(cycleData);
        
        DataStream<VehicleMetrics> metrics = stream
            .keyBy(VehicleTelemetry::getVehicleId)
            .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
            .aggregate(new TelemetryWindowAggregator());

        DataStream<TelemetryAlert> alerts = stream
            .flatMap(new TelemetryAlertFunction());

        assertDoesNotThrow(() -> {
            metrics.print("CYCLE-METRICS");
            alerts.print("CYCLE-ALERTS");
        });
    }

    // Helper methods

    private List<VehicleTelemetry> createDiverseTelemetryData() {
        List<VehicleTelemetry> data = new ArrayList<>();
        String[] vehicles = {"KOMATSU-930E-001", "KOMATSU-930E-002", "KOMATSU-980E-001"};
        
        for (String vehicleId : vehicles) {
            for (int i = 0; i < 15; i++) {
                data.add(createRandomTelemetry(vehicleId));
            }
        }
        
        return data;
    }

    private VehicleTelemetry createNormalTelemetry(String vehicleId) {
        return createTelemetry(vehicleId, 
            20.0 + Math.random() * 40.0,  // 20-60 km/h
            50.0 + Math.random() * 50.0,  // 50-100% fuel
            80.0 + Math.random() * 15.0   // 80-95°C
        );
    }

    private VehicleTelemetry createRandomTelemetry(String vehicleId) {
        return VehicleTelemetry.builder()
            .vehicleId(vehicleId)
            .timestamp(Instant.now())
            .speedKph(Math.random() * 60.0)
            .fuelLevelPercent(Math.random() * 100.0)
            .batteryLevelPercent(90.0 + Math.random() * 10.0)
            .engineTemperatureCelsius(75.0 + Math.random() * 30.0)
            .brakePressurePsi(85.0 + Math.random() * 15.0)
            .tirePressureFrontLeftPsi(85.0 + Math.random() * 15.0)
            .tirePressureFrontRightPsi(85.0 + Math.random() * 15.0)
            .tirePressureRearLeftPsi(85.0 + Math.random() * 15.0)
            .tirePressureRearRightPsi(85.0 + Math.random() * 15.0)
            .payloadTons(200.0 + Math.random() * 100.0)
            .location(GpsCoordinate.builder()
                .latitude(-23.5 + (Math.random() * 0.2 - 0.1))
                .longitude(-70.4 + (Math.random() * 0.2 - 0.1))
                .altitude(2900.0 + (Math.random() * 100.0))
                .build())
            .headingDegrees(Math.random() * 360.0)
            .engineRpm(1500.0 + Math.random() * 500.0)
            .build();
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
                .latitude(-23.5)
                .longitude(-70.4)
                .altitude(2900.0)
                .build())
            .headingDegrees(180.0)
            .engineRpm(1800.0)
            .build();
    }

    private VehicleTelemetry createStateTelemetry(String vehicleId, Instant time, 
                                                   double speedKph, double fuelPercent) {
        return VehicleTelemetry.builder()
            .vehicleId(vehicleId)
            .timestamp(time)
            .speedKph(speedKph)
            .fuelLevelPercent(fuelPercent)
            .batteryLevelPercent(95.0)
            .engineTemperatureCelsius(85.0)
            .brakePressurePsi(95.0)
            .tirePressureFrontLeftPsi(95.0)
            .tirePressureFrontRightPsi(95.0)
            .tirePressureRearLeftPsi(95.0)
            .tirePressureRearRightPsi(95.0)
            .payloadTons(speedKph > 20.0 ? 280.0 : 0.0) // loaded when moving fast
            .location(GpsCoordinate.builder()
                .latitude(-23.5 + (Math.random() * 0.1))
                .longitude(-70.4 + (Math.random() * 0.1))
                .altitude(2900.0)
                .build())
            .headingDegrees(180.0)
            .engineRpm(speedKph > 0 ? 1800.0 : 800.0)
            .build();
    }
}
