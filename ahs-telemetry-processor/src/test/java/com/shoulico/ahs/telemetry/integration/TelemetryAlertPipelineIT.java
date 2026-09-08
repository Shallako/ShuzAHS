package com.shoulico.ahs.telemetry.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.shoulico.ahs.domain.events.VehicleTelemetryEvent;
import com.shoulico.ahs.domain.model.VehicleStatus;
import com.shoulico.ahs.domain.model.VehicleTelemetry;
import com.shoulico.ahs.telemetry.JetTelemetryProcessorJob;
import com.shoulico.ahs.telemetry.model.TelemetryAlert;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration test verifying the Hazelcast Jet telemetry stream processing pipeline
 * against an ephemeral Kafka broker spun up via Testcontainers.
 */
@Testcontainers
class TelemetryAlertPipelineIT {

    private static final String TELEMETRY_TOPIC = "vehicle-telemetry";
    private static final String ALERTS_TOPIC = "telemetry-alerts";
    private static final String METRICS_TOPIC = "vehicle-metrics";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    private static HazelcastInstance hazelcastInstance;
    private static Job jetJob;
    private static KafkaProducer<String, String> producer;
    private static KafkaConsumer<String, String> consumer;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Provision required topics on the broker
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(
                    new NewTopic(TELEMETRY_TOPIC, 1, (short) 1),
                    new NewTopic(ALERTS_TOPIC, 1, (short) 1),
                    new NewTopic(METRICS_TOPIC, 1, (short) 1)
            )).all().get(30, TimeUnit.SECONDS);
        }

        // Spin up embedded Hazelcast instance and start Jet streaming job
        hazelcastInstance = Hazelcast.newHazelcastInstance(JetTelemetryProcessorJob.createHazelcastConfig());
        jetJob = JetTelemetryProcessorJob.startJob(
                hazelcastInstance,
                KAFKA.getBootstrapServers(),
                TELEMETRY_TOPIC,
                ALERTS_TOPIC,
                METRICS_TOPIC
        );

        // Initialize Kafka Producer for publishing test telemetry
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "1");
        producer = new KafkaProducer<>(producerProps);

        // Initialize Kafka Consumer for intercepting generated alerts
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "alert-it-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(ALERTS_TOPIC));
    }

    @AfterAll
    static void tearDown() {
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception ignored) {}
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception ignored) {}
        }
        if (jetJob != null) {
            try {
                jetJob.cancel();
            } catch (Exception ignored) {}
        }
        if (hazelcastInstance != null) {
            try {
                hazelcastInstance.shutdown();
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Should detect LOW_FUEL anomaly and publish TelemetryAlert to telemetry-alerts topic")
    void shouldDetectLowFuelAndPublishAlert() throws Exception {
        String vehicleId = "TITAN-300-001";

        // Nominal telemetry payload with fuelLevelPercent below 15.0 threshold
        VehicleTelemetry telemetry = VehicleTelemetry.builder()
                .vehicleId(vehicleId)
                .timestamp(Instant.now())
                .speedKph(28.5)
                .fuelLevelPercent(10.0) // Low fuel condition (< 15.0)
                .batteryLevelPercent(92.0)
                .engineTemperatureCelsius(86.0)
                .payloadTons(275.0)
                .brakePressurePsi(125.0)
                .tirePressureFrontLeftPsi(102.0)
                .tirePressureFrontRightPsi(101.5)
                .tirePressureRearLeftPsi(103.0)
                .tirePressureRearRightPsi(102.5)
                .tireTemperatureAvgCelsius(68.0)
                .warningLight(false)
                .diagnosticCodeCount(0)
                .build();

        VehicleTelemetryEvent event = new VehicleTelemetryEvent(vehicleId, telemetry);
        event.setSource("ahs-data-generator");
        event.setVehicleStatus(VehicleStatus.HAULING);

        String payloadJson = objectMapper.writeValueAsString(event);

        // Publish to Kafka input topic
        producer.send(new ProducerRecord<>(TELEMETRY_TOPIC, vehicleId, payloadJson)).get(10, TimeUnit.SECONDS);
        producer.flush();

        // Await alert delivery to telemetry-alerts topic
        List<TelemetryAlert> receivedAlerts = new CopyOnWriteArrayList<>();

        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, String> record : records) {
                        TelemetryAlert alert = objectMapper.readValue(record.value(), TelemetryAlert.class);
                        receivedAlerts.add(alert);
                    }

                    assertThat(receivedAlerts)
                            .withFailMessage("Expected to receive LOW_FUEL alert for %s but got: %s", vehicleId, receivedAlerts)
                            .anySatisfy(alert -> {
                                assertThat(alert.getAlertType()).isEqualTo(TelemetryAlert.AlertType.LOW_FUEL);
                                assertThat(alert.getVehicleId()).isEqualTo("TITAN-300-001");
                                assertThat(alert.getSeverity()).isEqualTo(TelemetryAlert.AlertSeverity.WARNING);
                                assertThat(alert.getMetricValue()).isEqualTo(10.0);
                                assertThat(alert.getThresholdValue()).isEqualTo(15.0);
                            });
                });
    }
}
