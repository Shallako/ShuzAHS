package com.komatsu.ahs.telemetry.integration;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.function.TelemetryDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kafka Integration Tests
 * 
 * Tests Kafka connectivity and message consumption using Flink's Kafka connector.
 * 
 * NOTE: These tests require a running Kafka instance.
 * To run: docker-compose up -d kafka
 * 
 * These tests are disabled by default. Enable by setting:
 * -Dtest.kafka.enabled=true
 */
@DisplayName("Kafka Integration Tests")
class KafkaIntegrationTest {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TEST_TOPIC = "test-telemetry";
    private static final boolean KAFKA_TESTS_ENABLED = 
        Boolean.parseBoolean(System.getProperty("test.kafka.enabled", "false"));
    
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create Kafka source for telemetry")
    void testKafkaSourceCreation() {
        if (!KAFKA_TESTS_ENABLED) {
            System.out.println("Kafka tests disabled. Set -Dtest.kafka.enabled=true to enable");
            return;
        }

        assertDoesNotThrow(() -> {
            KafkaSource<VehicleTelemetry> source = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(TEST_TOPIC)
                .setGroupId("test-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializer())
                .build();
            
            assertNotNull(source);
        });
    }

    @Test
    @DisplayName("Should configure watermark strategy")
    void testWatermarkStrategy() {
        WatermarkStrategy<VehicleTelemetry> watermarkStrategy = WatermarkStrategy
            .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
            .withTimestampAssigner((telemetry, timestamp) -> 
                telemetry.getTimestamp().toEpochMilli());
        
        assertNotNull(watermarkStrategy);
    }

    @Test
    @DisplayName("Should deserialize telemetry from JSON")
    void testTelemetryDeserialization() throws Exception {
        TelemetryDeserializer deserializer = new TelemetryDeserializer();
        // Initialize the deserializer (calls open() to set up ObjectMapper)
        deserializer.open(null);
        
        VehicleTelemetry testTelemetry = createTestTelemetry();
        String json = objectMapper.writeValueAsString(testTelemetry);
        
        VehicleTelemetry deserialized = deserializer.deserialize(json.getBytes());
        
        assertNotNull(deserialized);
        assertEquals(testTelemetry.getVehicleId(), deserialized.getVehicleId());
    }

    @Test
    @DisplayName("Should handle Kafka consumer configuration")
    void testKafkaConsumerProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS);
        props.put("group.id", "test-consumer-group");
        props.put("auto.offset.reset", "latest");
        props.put("enable.auto.commit", "true");
        
        assertNotNull(props);
        assertEquals(KAFKA_BOOTSTRAP_SERVERS, props.getProperty("bootstrap.servers"));
    }

    @Test
    @DisplayName("Should create Flink stream from Kafka source")
    void testFlinkKafkaIntegration() {
        if (!KAFKA_TESTS_ENABLED) {
            System.out.println("Kafka tests disabled. Set -Dtest.kafka.enabled=true to enable");
            return;
        }

        assertDoesNotThrow(() -> {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
            env.setParallelism(1);

            KafkaSource<VehicleTelemetry> source = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics(TEST_TOPIC)
                .setGroupId("test-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new TelemetryDeserializer())
                .build();

            WatermarkStrategy<VehicleTelemetry> watermarkStrategy = WatermarkStrategy
                .<VehicleTelemetry>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((telemetry, timestamp) -> 
                    telemetry.getTimestamp().toEpochMilli());

            DataStream<VehicleTelemetry> stream = env
                .fromSource(source, watermarkStrategy, "Test Kafka Source");
            
            assertNotNull(stream);
        });
    }

    @Test
    @DisplayName("Should handle multiple Kafka topics")
    void testMultipleTopics() {
        if (!KAFKA_TESTS_ENABLED) {
            System.out.println("Kafka tests disabled. Set -Dtest.kafka.enabled=true to enable");
            return;
        }

        assertDoesNotThrow(() -> {
            KafkaSource<VehicleTelemetry> source = KafkaSource.<VehicleTelemetry>builder()
                .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
                .setTopics("vehicle-telemetry", "test-telemetry", "backup-telemetry")
                .setGroupId("multi-topic-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new TelemetryDeserializer())
                .build();
            
            assertNotNull(source);
        });
    }

    @Test
    @DisplayName("Should configure checkpoint interval")
    void testCheckpointConfiguration() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        env.enableCheckpointing(60000); // 60 seconds
        env.getCheckpointConfig().setCheckpointTimeout(120000); // 120 seconds
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30000); // 30 seconds
        
        assertTrue(env.getCheckpointConfig().isCheckpointingEnabled());
        assertEquals(60000, env.getCheckpointConfig().getCheckpointInterval());
    }

    @Test
    @DisplayName("Should handle different offset initialization strategies")
    void testOffsetStrategies() {
        assertDoesNotThrow(() -> {
            OffsetsInitializer earliest = OffsetsInitializer.earliest();
            OffsetsInitializer latest = OffsetsInitializer.latest();
            OffsetsInitializer committed = OffsetsInitializer.committedOffsets();
            
            assertNotNull(earliest);
            assertNotNull(latest);
            assertNotNull(committed);
        });
    }

    // Helper methods

    private VehicleTelemetry createTestTelemetry() {
        return VehicleTelemetry.builder()
            .vehicleId("KOMATSU-930E-001")
            .timestamp(Instant.now())
            .speedKph(35.5)
            .fuelLevelPercent(75.0)
            .batteryLevelPercent(95.0)
            .engineTemperatureCelsius(88.0)
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
}
