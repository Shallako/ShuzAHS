package com.komatsu.ahs.stream.cep;

import com.komatsu.ahs.domain.events.VehicleAlertEvent;
import com.komatsu.ahs.domain.events.VehicleAlertEvent.AlertType;
import com.komatsu.ahs.domain.events.VehicleAlertEvent.Severity;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Applies all CEP patterns to telemetry stream and generates alerts.
 * Central orchestrator for Complex Event Processing in the AHS system.
 */
public class CepAlertProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(CepAlertProcessor.class);
    
    /**
     * Process telemetry stream and generate all alert types
     * @param telemetryStream Keyed telemetry stream by vehicle ID
     * @return Combined stream of all alert events
     */
    public DataStream<VehicleAlertEvent> processAlerts(DataStream<VehicleTelemetry> telemetryStream) {
        
        // Safety Alerts
        DataStream<VehicleAlertEvent> collisionAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createCollisionWarningPattern(),
                AlertType.COLLISION_WARNING,
                Severity.CRITICAL,
                "Collision warning - rapid deceleration detected"
        );
        
        DataStream<VehicleAlertEvent> obstacleAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createObstacleDetectedPattern(),
                AlertType.OBSTACLE_DETECTED,
                Severity.WARNING,
                "Obstacle detected - sudden stop from speed"
        );
        
        DataStream<VehicleAlertEvent> safetyEnvelopeAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createSafetyEnvelopeBreachPattern(),
                AlertType.SAFETY_ENVELOPE_BREACH,
                Severity.ERROR,
                "Safety envelope breach - speed exceeds safe limit for load state"
        );

        // Operational Alerts
        DataStream<VehicleAlertEvent> lowFuelAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createLowFuelPattern(),
                AlertType.LOW_FUEL,
                Severity.WARNING,
                "Low fuel level detected"
        );
        
        DataStream<VehicleAlertEvent> engineOverheatAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createEngineOverheatingPattern(),
                AlertType.ENGINE_OVERHEATING,
                Severity.ERROR,
                "Engine temperature rising - overheating detected"
        );
        
        DataStream<VehicleAlertEvent> lowTireAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createLowTirePressurePattern(),
                AlertType.LOW_TIRE_PRESSURE,
                Severity.WARNING,
                "Low tire pressure detected"
        );
        
        DataStream<VehicleAlertEvent> excessiveSpeedAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createExcessiveSpeedPattern(),
                AlertType.EXCESSIVE_SPEED,
                Severity.CRITICAL,
                "Excessive speed detected - above critical threshold"
        );
        
        DataStream<VehicleAlertEvent> lowBrakeAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createBrakePressureLowPattern(),
                AlertType.BRAKE_PRESSURE_LOW,
                Severity.ERROR,
                "Low brake pressure detected"
        );
        
        DataStream<VehicleAlertEvent> lowHydraulicAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createHydraulicPressureLowPattern(),
                AlertType.HYDRAULIC_PRESSURE_LOW,
                Severity.WARNING,
                "Low hydraulic pressure detected"
        );
        
        // System Alerts
        DataStream<VehicleAlertEvent> systemFaultAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createSystemFaultPattern(),
                AlertType.SYSTEM_FAULT,
                Severity.ERROR,
                "System fault detected - diagnostic codes present"
        );
        
        DataStream<VehicleAlertEvent> lowBatteryAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createLowBatteryPattern(),
                AlertType.LOW_BATTERY,
                Severity.WARNING,
                "Low battery level detected"
        );
        
        DataStream<VehicleAlertEvent> maintenanceAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createMaintenanceRequiredPattern(),
                AlertType.MAINTENANCE_REQUIRED,
                Severity.INFO,
                "Maintenance required - service interval reached"
        );
        
        DataStream<VehicleAlertEvent> commLossAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createCommunicationLossPattern(),
                AlertType.COMMUNICATION_LOSS,
                Severity.ERROR,
                "Communication warning indicator active"
        );

        // Navigation Alerts
        DataStream<VehicleAlertEvent> routeDeviationAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createRouteDeviationPattern(),
                AlertType.ROUTE_DEVIATION,
                Severity.WARNING,
                "Route deviation detected - significant heading change"
        );
        
        DataStream<VehicleAlertEvent> stuckAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createStuckDetectionPattern(),
                AlertType.STUCK_DETECTION,
                Severity.WARNING,
                "Vehicle appears stuck - not moving with engine running"
        );
        
        DataStream<VehicleAlertEvent> positioningAlerts = applyPattern(
                telemetryStream,
                CepPatternFactory.createPositioningErrorPattern(),
                AlertType.POSITIONING_ERROR,
                Severity.ERROR,
                "GPS positioning error detected"
        );
        
        // Union all alert streams
        return collisionAlerts
                .union(obstacleAlerts)
                .union(safetyEnvelopeAlerts)
                .union(lowFuelAlerts)
                .union(engineOverheatAlerts)
                .union(lowTireAlerts)
                .union(excessiveSpeedAlerts)
                .union(lowBrakeAlerts)
                .union(lowHydraulicAlerts)
                .union(systemFaultAlerts)
                .union(lowBatteryAlerts)
                .union(maintenanceAlerts)
                .union(commLossAlerts)
                .union(routeDeviationAlerts)
                .union(stuckAlerts)
                .union(positioningAlerts);
    }
    
    /**
     * Apply a single pattern and create alerts
     */
    private DataStream<VehicleAlertEvent> applyPattern(
            DataStream<VehicleTelemetry> telemetryStream,
            Pattern<VehicleTelemetry, ?> pattern,
            AlertType alertType,
            Severity severity,
            String message) {
        
        PatternStream<VehicleTelemetry> patternStream = CEP.pattern(
                telemetryStream.keyBy(VehicleTelemetry::getVehicleId),
                pattern
        );
        
        return patternStream.select(new PatternSelectFunction<VehicleTelemetry, VehicleAlertEvent>() {
            @Override
            public VehicleAlertEvent select(Map<String, List<VehicleTelemetry>> matches) {
                // Get the last telemetry event from the pattern match
                VehicleTelemetry lastTelemetry = null;
                for (List<VehicleTelemetry> events : matches.values()) {
                    if (!events.isEmpty()) {
                        lastTelemetry = events.get(events.size() - 1);
                    }
                }
                
                if (lastTelemetry == null) {
                    return null;
                }
                
                VehicleAlertEvent alert = new VehicleAlertEvent(
                        lastTelemetry.getVehicleId(),
                        alertType,
                        severity,
                        message
                );
                
                // Add telemetry context to metadata
                alert.addMetadata("speed_kph", lastTelemetry.getSpeedKph());
                alert.addMetadata("fuel_percent", lastTelemetry.getFuelLevelPercent());
                alert.addMetadata("engine_temp", lastTelemetry.getEngineTemperatureCelsius());
                alert.addMetadata("brake_pressure", lastTelemetry.getBrakePressurePsi());
                
                LOG.info("Alert generated: {} for vehicle {} - {}", 
                        alertType, lastTelemetry.getVehicleId(), message);
                
                return alert;
            }
        }).name("CEP-" + alertType.name());
    }
}
