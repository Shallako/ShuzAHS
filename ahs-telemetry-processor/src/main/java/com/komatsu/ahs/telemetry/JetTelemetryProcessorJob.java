package com.komatsu.ahs.telemetry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.kafka.KafkaSinks;
import com.hazelcast.jet.kafka.KafkaSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import com.komatsu.ahs.telemetry.model.VehicleMetrics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import static com.hazelcast.jet.Traversers.traverseIterable;
import static com.hazelcast.jet.aggregate.AggregateOperations.toList;

/**
 * Hazelcast Jet Telemetry Processor Job (embedded mode)
 *
 * Replaces the Flink job with a Jet Pipeline that:
 * - Consumes telemetry from Kafka (JSON VehicleTelemetryEvent or VehicleTelemetry)
 * - Generates alerts based on threshold checks
 * - Aggregates basic per-vehicle metrics in 1-minute tumbling windows
 * - Publishes alerts and metrics as JSON to Kafka
 */
public class JetTelemetryProcessorJob {

    private static final Logger LOG = LoggerFactory.getLogger(JetTelemetryProcessorJob.class);

    // Kafka configuration (env-overridable)
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

    // Use a single static ObjectMapper so pipeline lambdas don't capture a non-serializable instance
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static void main(String[] args) {
        LOG.info("Starting AHS Telemetry Processor (Hazelcast Jet, embedded)");

        // Start embedded Hazelcast member
        Config config = new Config();
        // Enable Jet engine explicitly for embedded member
        config.getJetConfig().setEnabled(true);
        // Reduce noisy phone-home warnings during shutdown in dev/local runs
        config.setProperty("hazelcast.phone.home.enabled", "false");

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);
        JetService jet = hz.getJet();

        // Prepare a named job so restarts don't spawn duplicates
        JobConfig jobConfig = new JobConfig().setName("ahs-telemetry-processor");

        Job job = null;
        int attempt = 0;
        int maxAttempts = 5; // basic startup retry if Kafka isn't ready yet
        while (job == null) {
            try {
                Pipeline p = buildPipeline();
                job = jet.newJobIfAbsent(p, jobConfig);
                LOG.info("Hazelcast Jet job submitted: {} (status={})", job.getName(), job.getStatus());
            } catch (Exception e) {
                attempt++;
                long backoffMs = Math.min(30_000L, 2_000L * attempt);
                LOG.warn("Failed to start Jet job (attempt {}/{}). Retrying in {} ms...", attempt, maxAttempts, backoffMs, e);
                if (attempt >= maxAttempts) {
                    LOG.error("Exceeded maximum startup attempts ({}). Shutting down.", maxAttempts);
                    hz.shutdown();
                    return;
                }
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Keep JVM alive until it is terminated; on SIGTERM, cancel job and shutdown member
        final Job runningJob = job;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOG.info("Shutdown hook triggered. Cancelling Jet job '{}'...", runningJob.getName());
                try {
                    runningJob.cancel();
                } catch (Exception e) {
                    LOG.warn("Job cancel threw (ignored): {}", e.toString());
                }
            } finally {
                LOG.info("Shutting down Hazelcast instance...");
                try {
                    hz.shutdown();
                } finally {
                    latch.countDown();
                }
            }
        }, "telemetry-processor-shutdown"));

        try {
            // Periodically log job status to aid troubleshooting
            while (true) {
                try {
                    LOG.debug("Jet job status: {}", runningJob.getStatus());
                } catch (Exception ignored) {
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                Thread.sleep(30_000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Pipeline buildPipeline() {
        Pipeline p = Pipeline.create();

        // Kafka consumer properties
        Properties consumerProps = new Properties();
        consumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        consumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        // Kafka producer properties
        Properties producerProps = new Properties();
        producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        producerProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 1) Ingest telemetry from Kafka and parse JSON
        StreamStage<VehicleTelemetry> telemetry = p.readFrom(
                        KafkaSources.kafka(consumerProps, rec -> rec.value().toString(), INPUT_TOPIC))
                .withoutTimestamps()
                .map(JetTelemetryProcessorJob::parseTelemetry)
                .filter(t -> t != null);

        // Assign event-time timestamps required for windowed aggregations
        // Use the telemetry timestamp; if missing, fall back to current time. Allow 5s out-of-orderness.
        StreamStage<VehicleTelemetry> telemetryWithTs = telemetry.addTimestamps(t -> {
            Instant ts = t.getTimestamp();
            return ts != null ? ts.toEpochMilli() : System.currentTimeMillis();
        }, 5_000);

        // 2) Generate alerts (threshold checks)
        StreamStage<java.util.Map.Entry<String, String>> alertsRecords = telemetryWithTs
                .flatMap(t -> traverseIterable(AlertGenerator.generate(t)))
                .map(alert -> Map.entry(alert.getVehicleId(), alert.toJson()));

        // 3) Aggregate simple metrics in 1-minute tumbling windows per vehicle
        StreamStage<java.util.Map.Entry<String, String>> metricsRecords = telemetryWithTs
                .groupingKey(VehicleTelemetry::getVehicleId)
                .window(com.hazelcast.jet.pipeline.WindowDefinition.tumbling(60_000))
                .aggregate(toList())
                .map((KeyedWindowResult<String, List<VehicleTelemetry>> res) -> {
                    String vehicleId = res.getKey();
                    List<VehicleTelemetry> list = res.getValue();
                    VehicleMetrics vm = computeMetrics(vehicleId, list);
                    return Map.entry(vehicleId, vm.toJson());
                });

        // 4) Write to Kafka
        alertsRecords.writeTo(KafkaSinks.kafka(producerProps, OUTPUT_ALERTS_TOPIC));
        metricsRecords.writeTo(KafkaSinks.kafka(producerProps, OUTPUT_METRICS_TOPIC));

        return p;
    }

    private static VehicleTelemetry parseTelemetry(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.has("telemetry") && root.get("telemetry").isObject()) {
                VehicleTelemetry t = MAPPER.treeToValue(root.get("telemetry"), VehicleTelemetry.class);
                if (t.getVehicleId() == null && root.has("vehicleId")) {
                    t.setVehicleId(root.get("vehicleId").asText());
                }
                return t;
            } else {
                return MAPPER.treeToValue(root, VehicleTelemetry.class);
            }
        } catch (Exception e) {
            LOG.error("Failed to parse telemetry JSON: {}", json, e);
            return null;
        }
    }

    private static VehicleMetrics computeMetrics(String vehicleId, List<VehicleTelemetry> list) {
        VehicleMetrics vm = new VehicleMetrics();
        vm.setVehicleId(vehicleId);
        if (list == null || list.isEmpty()) {
            Instant now = Instant.now();
            vm.setWindowStart(now);
            vm.setWindowEnd(now);
            vm.setDataPointCount(0);
            vm.setAverageSpeed(0.0);
            vm.setMinSpeed(0.0);
            vm.setMaxSpeed(0.0);
            vm.setAverageFuelLevel(0.0);
            return vm;
        }
        double totalSpeed = 0.0;
        Double minSpeed = null;
        Double maxSpeed = null;
        double totalFuel = 0.0;
        Instant first = null;
        Instant last = null;
        for (VehicleTelemetry t : list) {
            totalSpeed += t.getSpeedKph();
            minSpeed = (minSpeed == null) ? t.getSpeedKph() : Math.min(minSpeed, t.getSpeedKph());
            maxSpeed = (maxSpeed == null) ? t.getSpeedKph() : Math.max(maxSpeed, t.getSpeedKph());
            totalFuel += t.getFuelLevelPercent();
            if (first == null || t.getTimestamp().isBefore(first)) first = t.getTimestamp();
            if (last == null || t.getTimestamp().isAfter(last)) last = t.getTimestamp();
        }
        long count = list.size();
        vm.setWindowStart(first != null ? first : Instant.now());
        vm.setWindowEnd(last != null ? last : vm.getWindowStart());
        vm.setDataPointCount(count);
        vm.setAverageSpeed(count > 0 ? totalSpeed / count : 0.0);
        vm.setMinSpeed(minSpeed);
        vm.setMaxSpeed(maxSpeed);
        vm.setAverageFuelLevel(count > 0 ? totalFuel / count : 0.0);
        return vm;
    }

    // Simple alert generator mirroring the thresholds used in Flink function
    private static class AlertGenerator {
        private static final double MAX_ENGINE_TEMP_CELSIUS = 110.0;
        private static final double MIN_FUEL_PERCENT = 15.0;
        private static final double MIN_BATTERY_PERCENT = 20.0;
        private static final double MIN_TIRE_PRESSURE_PSI = 85.0;
        private static final double MAX_TIRE_TEMP_CELSIUS = 90.0;
        private static final double MIN_BRAKE_PRESSURE_PSI = 100.0;
        private static final double MAX_PAYLOAD_TONS = 400.0;

        static java.util.List<TelemetryAlert> generate(VehicleTelemetry t) {
            java.util.ArrayList<TelemetryAlert> alerts = new java.util.ArrayList<>();

            if (t.getEngineTemperatureCelsius() > MAX_ENGINE_TEMP_CELSIUS) {
                alerts.add(alert(t, TelemetryAlert.AlertType.HIGH_TEMPERATURE, TelemetryAlert.AlertSeverity.CRITICAL,
                        "Engine temperature exceeded safe operating limit",
                        t.getEngineTemperatureCelsius(), MAX_ENGINE_TEMP_CELSIUS));
            }

            if (t.getFuelLevelPercent() < MIN_FUEL_PERCENT && t.getFuelLevelPercent() > 0) {
                alerts.add(alert(t, TelemetryAlert.AlertType.LOW_FUEL, TelemetryAlert.AlertSeverity.WARNING,
                        "Fuel level is low", t.getFuelLevelPercent(), MIN_FUEL_PERCENT));
            }

            if (t.getBatteryLevelPercent() < MIN_BATTERY_PERCENT && t.getBatteryLevelPercent() > 0) {
                alerts.add(alert(t, TelemetryAlert.AlertType.LOW_BATTERY, TelemetryAlert.AlertSeverity.WARNING,
                        "Battery level is low", t.getBatteryLevelPercent(), MIN_BATTERY_PERCENT));
            }

            double avgTirePressure = (t.getTirePressureFrontLeftPsi() + t.getTirePressureFrontRightPsi()
                    + t.getTirePressureRearLeftPsi() + t.getTirePressureRearRightPsi()) / 4.0;
            if (avgTirePressure < MIN_TIRE_PRESSURE_PSI && avgTirePressure > 0) {
                alerts.add(alert(t, TelemetryAlert.AlertType.TIRE_PRESSURE_LOW, TelemetryAlert.AlertSeverity.CRITICAL,
                        "Average tire pressure is below safe threshold", avgTirePressure, MIN_TIRE_PRESSURE_PSI));
            }

            if (t.getTireTemperatureAvgCelsius() > MAX_TIRE_TEMP_CELSIUS) {
                alerts.add(alert(t, TelemetryAlert.AlertType.TIRE_TEMPERATURE_HIGH, TelemetryAlert.AlertSeverity.WARNING,
                        "Tire temperature is elevated", t.getTireTemperatureAvgCelsius(), MAX_TIRE_TEMP_CELSIUS));
            }

            if (t.getBrakePressurePsi() < MIN_BRAKE_PRESSURE_PSI) {
                alerts.add(alert(t, TelemetryAlert.AlertType.BRAKE_PRESSURE_LOW, TelemetryAlert.AlertSeverity.CRITICAL,
                        "Brake pressure is critically low", t.getBrakePressurePsi(), MIN_BRAKE_PRESSURE_PSI));
            }

            if (t.getPayloadTons() > MAX_PAYLOAD_TONS) {
                alerts.add(alert(t, TelemetryAlert.AlertType.OVERLOAD, TelemetryAlert.AlertSeverity.CRITICAL,
                        "Vehicle is overloaded beyond safe capacity", t.getPayloadTons(), MAX_PAYLOAD_TONS));
            }

            if (t.isWarningLight()) {
                alerts.add(alert(t, TelemetryAlert.AlertType.ENGINE_WARNING, TelemetryAlert.AlertSeverity.WARNING,
                        "Warning light is active", 1.0, 0.0));
            }

            if (t.getDiagnosticCodeCount() > 0) {
                alerts.add(alert(t, TelemetryAlert.AlertType.DIAGNOSTIC_ERROR, TelemetryAlert.AlertSeverity.WARNING,
                        String.format("%d diagnostic code(s) detected", t.getDiagnosticCodeCount()),
                        t.getDiagnosticCodeCount(), 0.0));
            }

            return alerts;
        }

        private static TelemetryAlert alert(
                VehicleTelemetry t,
                TelemetryAlert.AlertType type,
                TelemetryAlert.AlertSeverity severity,
                String message,
                double metric,
                double threshold) {
            return TelemetryAlert.builder()
                    .alertId(UUID.randomUUID().toString())
                    .vehicleId(t.getVehicleId())
                    .timestamp(t.getTimestamp())
                    .alertType(type)
                    .severity(severity)
                    .message(message)
                    .metricValue(metric)
                    .thresholdValue(threshold)
                    .build();
        }
    }
}
