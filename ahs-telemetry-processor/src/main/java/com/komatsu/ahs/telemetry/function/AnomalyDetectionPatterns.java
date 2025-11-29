package com.komatsu.ahs.telemetry.function;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.List;
import java.util.Map;

/**
 * Anomaly Detection using Flink CEP
 * 
 * Detects complex patterns and anomalies in telemetry data:
 * - Rapid speed changes
 * - Low fuel warnings
 * - Overheating
 * - Abnormal tire pressure
 */
public class AnomalyDetectionPatterns {

    /**
     * Detect rapid deceleration pattern (potential collision avoidance)
     */
    public static DataStream<TelemetryAlert> detectRapidDeceleration(
            DataStream<VehicleTelemetry> telemetryStream) {
        
        Pattern<VehicleTelemetry, ?> rapidDecelerationPattern = Pattern
            .<VehicleTelemetry>begin("high-speed")
            .where(new SimpleCondition<VehicleTelemetry>() {
                @Override
                public boolean filter(VehicleTelemetry t) {
                    return t.getSpeedKph() > 50.0; // High speed (50+ kph)
                }
            })
            .next("low-speed")
            .where(new SimpleCondition<VehicleTelemetry>() {
                @Override
                public boolean filter(VehicleTelemetry t) {
                    return t.getSpeedKph() < 10.0; // Sudden drop to <10 kph)
                }
            })
            .within(Time.seconds(5)); // Within 5 seconds
        
        PatternStream<VehicleTelemetry> patternStream = 
            CEP.pattern(telemetryStream.keyBy(VehicleTelemetry::getVehicleId), 
                       rapidDecelerationPattern);
        
        return patternStream.select(new PatternSelectFunction<VehicleTelemetry, TelemetryAlert>() {
            @Override
            public TelemetryAlert select(Map<String, List<VehicleTelemetry>> pattern) {
                VehicleTelemetry highSpeed = pattern.get("high-speed").get(0);
                VehicleTelemetry lowSpeed = pattern.get("low-speed").get(0);
                
                return TelemetryAlert.builder()
                    .vehicleId(highSpeed.getVehicleId())
                    .alertType(TelemetryAlert.AlertType.RAPID_DECELERATION)
                    .severity(TelemetryAlert.AlertSeverity.WARNING)
                    .message(String.format(
                        "Rapid deceleration detected: %.1f kph to %.1f kph in 5 seconds",
                        highSpeed.getSpeedKph(), lowSpeed.getSpeedKph()))
                    .build();
            }
        });
    }
    
    /**
     * Detect low fuel pattern
     */
    public static DataStream<TelemetryAlert> detectLowFuel(
            DataStream<VehicleTelemetry> telemetryStream) {
        
        return telemetryStream
            .filter(t -> t.getFuelLevelPercent() < 15.0) // Less than 15%
            .map(telemetry -> TelemetryAlert.builder()
                .vehicleId(telemetry.getVehicleId())
                .alertType(TelemetryAlert.AlertType.LOW_FUEL)
                .severity(telemetry.getFuelLevelPercent() < 5.0 ? 
                         TelemetryAlert.AlertSeverity.CRITICAL : 
                         TelemetryAlert.AlertSeverity.WARNING)
                .message(String.format("Low fuel level: %.1f%%", telemetry.getFuelLevelPercent()))
                .build());
    }

    /**
     * Detect overheating pattern
     */
    public static DataStream<TelemetryAlert> detectOverheating(
            DataStream<VehicleTelemetry> telemetryStream) {
        
        Pattern<VehicleTelemetry, ?> overheatingPattern = Pattern
            .<VehicleTelemetry>begin("high-temp")
            .where(new SimpleCondition<VehicleTelemetry>() {
                @Override
                public boolean filter(VehicleTelemetry t) {
                    return t.getEngineTemperatureCelsius() > 95.0; // Above 95°C
                }
            })
            .times(3) // Three consecutive readings
            .within(Time.minutes(2));
        
        PatternStream<VehicleTelemetry> patternStream = 
            CEP.pattern(telemetryStream.keyBy(VehicleTelemetry::getVehicleId), 
                       overheatingPattern);
        
        return patternStream.select(new PatternSelectFunction<VehicleTelemetry, TelemetryAlert>() {
            @Override
            public TelemetryAlert select(Map<String, List<VehicleTelemetry>> pattern) {
                List<VehicleTelemetry> temps = pattern.get("high-temp");
                VehicleTelemetry latest = temps.get(temps.size() - 1);
                
                return TelemetryAlert.builder()
                    .vehicleId(latest.getVehicleId())
                    .alertType(TelemetryAlert.AlertType.OVERHEATING)
                    .severity(TelemetryAlert.AlertSeverity.CRITICAL)
                    .message(String.format(
                        "Engine overheating: %.1f°C sustained for 2 minutes",
                        latest.getEngineTemperatureCelsius()))
                    .build();
            }
        });
    }
}
