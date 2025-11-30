package com.komatsu.ahs.stream.cep;

import com.komatsu.ahs.domain.events.VehicleAlertEvent.AlertType;
import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static com.komatsu.ahs.stream.cep.AlertThresholds.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive simulation tests for all Flink CEP alert patterns.
 * Tests each of the 16 alert types defined in VehicleAlertEvent.AlertType.
 */
class CepAlertSimulationTest {
    
    private StreamExecutionEnvironment env;
    private static final String TEST_VEHICLE_ID = "TRUCK-930E-001";
    
    @BeforeEach
    void setUp() {
        env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1); // For deterministic testing
    }

    // ==================== SAFETY ALERT TESTS ====================
    
    @Test
    @DisplayName("COLLISION_WARNING: Simulate rapid deceleration event")
    void testCollisionWarningPattern() throws Exception {
        // Simulate rapid deceleration from 45 kph to 5 kph
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(45.0).build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 1000).toBuilder()
                        .speedKph(30.0).build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 2000).toBuilder()
                        .speedKph(5.0).build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createCollisionWarningPattern()
        );
        
        List<AlertType> results = new ArrayList<>();
        patternStream.select(map -> {
            results.add(AlertType.COLLISION_WARNING);
            return AlertType.COLLISION_WARNING;
        }).print();
        
        env.execute("Collision Warning Test");
    }
    
    @Test
    @DisplayName("OBSTACLE_DETECTED: Simulate sudden stop from speed")
    void testObstacleDetectedPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(25.0).build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 2000).toBuilder()
                        .speedKph(0.2).build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createObstacleDetectedPattern()
        );
        
        env.execute("Obstacle Detected Test");
    }
    
    @Test
    @DisplayName("SAFETY_ENVELOPE_BREACH: Loaded truck exceeding speed limit")
    void testSafetyEnvelopeBreachPattern() throws Exception {
        // Loaded truck at 50 kph (> MAX_SPEED_LOADED_KPH of 45)
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(50.0)
                        .isLoaded(true)
                        .payloadTons(250.0)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createSafetyEnvelopeBreachPattern()
        );
        
        env.execute("Safety Envelope Breach Test");
    }

    // ==================== OPERATIONAL ALERT TESTS ====================
    
    @Test
    @DisplayName("LOW_FUEL: Fuel level below 20%")
    void testLowFuelPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .fuelLevelPercent(15.0) // Below LOW_FUEL_WARNING_PERCENT (20%)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createLowFuelPattern()
        );
        
        env.execute("Low Fuel Test");
    }
    
    @Test
    @DisplayName("ENGINE_OVERHEATING: Rising engine temperature")
    void testEngineOverheatingPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .engineTemperatureCelsius(92.0) // Near warning
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 30000).toBuilder()
                        .engineTemperatureCelsius(98.0) // Above ENGINE_TEMP_WARNING_CELSIUS (95)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createEngineOverheatingPattern()
        );
        
        env.execute("Engine Overheating Test");
    }
    
    @Test
    @DisplayName("LOW_TIRE_PRESSURE: Tire below 95 PSI threshold")
    void testLowTirePressurePattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .tirePressureFrontLeftPsi(90.0) // Below TIRE_PRESSURE_MIN_WARNING_PSI (95)
                        .tirePressureFrontRightPsi(105.0)
                        .tirePressureRearLeftPsi(105.0)
                        .tirePressureRearRightPsi(105.0)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createLowTirePressurePattern()
        );
        
        env.execute("Low Tire Pressure Test");
    }
    
    @Test
    @DisplayName("EXCESSIVE_SPEED: Speed above 60 kph critical threshold")
    void testExcessiveSpeedPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(65.0) // Above CRITICAL_SPEED_KPH (60)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createExcessiveSpeedPattern()
        );
        
        env.execute("Excessive Speed Test");
    }
    
    @Test
    @DisplayName("BRAKE_PRESSURE_LOW: Brake pressure below 90 PSI")
    void testBrakePressureLowPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .brakePressurePsi(80.0) // Below BRAKE_PRESSURE_MIN_WARNING_PSI (90)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createBrakePressureLowPattern()
        );
        
        env.execute("Brake Pressure Low Test");
    }
    
    @Test
    @DisplayName("HYDRAULIC_PRESSURE_LOW: Hydraulic pressure below 2500 PSI")
    void testHydraulicPressureLowPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .hydraulicPressurePsi(2200.0) // Below HYDRAULIC_PRESSURE_MIN_WARNING_PSI (2500)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createHydraulicPressureLowPattern()
        );
        
        env.execute("Hydraulic Pressure Low Test");
    }

    // ==================== SYSTEM ALERT TESTS ====================
    
    @Test
    @DisplayName("SYSTEM_FAULT: Multiple diagnostic codes")
    void testSystemFaultPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .diagnosticCodeCount(5) // Above MAX_DIAGNOSTIC_CODES_WARNING (3)
                        .checkEngineLight(true)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createSystemFaultPattern()
        );
        
        env.execute("System Fault Test");
    }
    
    @Test
    @DisplayName("LOW_BATTERY: Battery below 30%")
    void testLowBatteryPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .batteryLevelPercent(25.0) // Below LOW_BATTERY_WARNING_PERCENT (30)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createLowBatteryPattern()
        );
        
        env.execute("Low Battery Test");
    }
    
    @Test
    @DisplayName("MAINTENANCE_REQUIRED: Operating hours at service interval")
    void testMaintenanceRequiredPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .operatingHours(505L) // Near MAINTENANCE_INTERVAL_HOURS (500)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createMaintenanceRequiredPattern()
        );
        
        env.execute("Maintenance Required Test");
    }
    
    @Test
    @DisplayName("COMMUNICATION_LOSS: Warning light triggered")
    void testCommunicationLossPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .warningLight(true)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createCommunicationLossPattern()
        );
        
        env.execute("Communication Loss Test");
    }

    // ==================== NAVIGATION ALERT TESTS ====================
    
    @Test
    @DisplayName("ROUTE_DEVIATION: Significant heading change while moving")
    void testRouteDeviationPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(25.0)
                        .headingDegrees(90.0)
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 5000).toBuilder()
                        .speedKph(20.0)
                        .headingDegrees(180.0) // 90 degree change
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createRouteDeviationPattern()
        );
        
        env.execute("Route Deviation Test");
    }
    
    @Test
    @DisplayName("STUCK_DETECTION: Not moving but engine running")
    void testStuckDetectionPattern() throws Exception {
        // 3+ consecutive readings with speed near 0 but engine RPM > idle
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(0.2)
                        .engineRpm(1200.0) // Above ENGINE_MIN_IDLE_RPM (650)
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 5000).toBuilder()
                        .speedKph(0.3)
                        .engineRpm(1100.0)
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 10000).toBuilder()
                        .speedKph(0.1)
                        .engineRpm(1150.0)
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 15000).toBuilder()
                        .speedKph(0.2)
                        .engineRpm(1180.0)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createStuckDetectionPattern()
        );
        
        env.execute("Stuck Detection Test");
    }
    
    @Test
    @DisplayName("POSITIONING_ERROR: GPS coordinate jump at low speed")
    void testPositioningErrorPattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .speedKph(10.0)
                        .location(new GpsCoordinate(-22.500, 118.300, 450.0))
                        .build(),
                createBaseTelemetry(TEST_VEHICLE_ID, 2000).toBuilder()
                        .speedKph(10.0)
                        .location(new GpsCoordinate(-22.510, 118.310, 450.0)) // Large jump
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createPositioningErrorPattern()
        );
        
        env.execute("Positioning Error Test");
    }

    // ==================== CRITICAL PATTERN TESTS ====================
    
    @Test
    @DisplayName("CRITICAL_ENGINE: Temperature above 105°C")
    void testCriticalEnginePattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .engineTemperatureCelsius(110.0) // Above ENGINE_TEMP_CRITICAL_CELSIUS (105)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createCriticalEnginePattern()
        );
        
        env.execute("Critical Engine Test");
    }
    
    @Test
    @DisplayName("CRITICAL_BRAKE_FAILURE: Brake pressure below 70 PSI")
    void testCriticalBrakeFailurePattern() throws Exception {
        List<VehicleTelemetry> events = Arrays.asList(
                createBaseTelemetry(TEST_VEHICLE_ID, 0).toBuilder()
                        .brakePressurePsi(60.0) // Below BRAKE_PRESSURE_CRITICAL_PSI (70)
                        .build()
        );
        
        DataStream<VehicleTelemetry> stream = env.fromCollection(events);
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                stream.keyBy(VehicleTelemetry::getVehicleId),
                CepPatternFactory.createCriticalBrakeFailurePattern()
        );
        
        env.execute("Critical Brake Failure Test");
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Creates a base VehicleTelemetry with normal operating values
     */
    private VehicleTelemetry createBaseTelemetry(String vehicleId, long offsetMillis) {
        return VehicleTelemetry.builder()
                .vehicleId(vehicleId)
                .timestamp(Instant.now().plusMillis(offsetMillis))
                .location(new GpsCoordinate(-22.500, 118.300, 450.0))
                // Navigation
                .speedKph(25.0)
                .headingDegrees(90.0)
                // Powertrain
                .engineRpm(1200.0)
                .fuelLevelPercent(75.0)
                .batteryLevelPercent(85.0)
                .engineTemperatureCelsius(85.0)
                // Payload
                .payloadTons(100.0)
                .isLoaded(false)
                // Brakes and tires
                .brakePressurePsi(120.0)
                .tirePressureFrontLeftPsi(105.0)
                .tirePressureFrontRightPsi(105.0)
                .tirePressureRearLeftPsi(105.0)
                .tirePressureRearRightPsi(105.0)
                .tireTemperatureAvgCelsius(45.0)
                // Hydraulics
                .hydraulicPressurePsi(3000.0)
                .hydraulicFluidTempCelsius(65.0)
                // Diagnostics
                .diagnosticCodeCount(0)
                .warningLight(false)
                .checkEngineLight(false)
                // Operational
                .totalDistanceMeters(50000L)
                .operatingHours(250L)
                .tripCount(100)
                .build();
    }
}
