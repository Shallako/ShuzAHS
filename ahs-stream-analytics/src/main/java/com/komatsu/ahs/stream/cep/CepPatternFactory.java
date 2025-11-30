package com.komatsu.ahs.stream.cep;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.cep.pattern.conditions.IterativeCondition;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.List;
import java.util.Map;

import static com.komatsu.ahs.stream.cep.AlertThresholds.*;

/**
 * Factory for creating Flink CEP patterns for vehicle alert detection.
 * Implements all 16 alert types defined in VehicleAlertEvent.AlertType.
 */
public final class CepPatternFactory {
    
    private CepPatternFactory() {} // Utility class
    
    // ==================== SAFETY ALERTS ====================
    
    /**
     * COLLISION_WARNING: Detects rapid deceleration events indicating possible collision avoidance
     * Pattern: Speed drops rapidly (>5 km/h/s) across consecutive readings
     */
    public static Pattern<VehicleTelemetry, ?> createCollisionWarningPattern() {
        return Pattern.<VehicleTelemetry>begin("first")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() > MIN_MOVING_SPEED_KPH;
                    }
                })
                .next("second")
                .where(new IterativeCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry current, Context<VehicleTelemetry> ctx) throws Exception {
                        for (VehicleTelemetry prev : ctx.getEventsForPattern("first")) {
                            double speedDrop = prev.getSpeedKph() - current.getSpeedKph();
                            // Rapid deceleration detection
                            return speedDrop > RAPID_DECEL_THRESHOLD * 2;
                        }
                        return false;
                    }
                })
                .within(Time.seconds(3));
    }
    
    /**
     * OBSTACLE_DETECTED: Detects sudden stop from significant speed
     * Pattern: Vehicle moving > 10 km/h suddenly stops
     */
    public static Pattern<VehicleTelemetry, ?> createObstacleDetectedPattern() {
        return Pattern.<VehicleTelemetry>begin("moving")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() > 10.0;
                    }
                })
                .next("stopped")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() < MIN_MOVING_SPEED_KPH;
                    }
                })
                .within(Time.seconds(5));
    }

    /**
     * SAFETY_ENVELOPE_BREACH: Speed exceeds safe threshold for load state
     * Pattern: Loaded vehicle > 45 km/h or empty vehicle > 55 km/h
     */
    public static Pattern<VehicleTelemetry, ?> createSafetyEnvelopeBreachPattern() {
        return Pattern.<VehicleTelemetry>begin("breach")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        if (t.isLoaded()) {
                            return t.getSpeedKph() > MAX_SPEED_LOADED_KPH;
                        } else {
                            return t.getSpeedKph() > MAX_SPEED_EMPTY_KPH;
                        }
                    }
                });
    }
    
    // ==================== OPERATIONAL ALERTS ====================
    
    /**
     * LOW_FUEL: Fuel level drops below warning threshold
     * Pattern: Single event with fuel < 20%
     */
    public static Pattern<VehicleTelemetry, ?> createLowFuelPattern() {
        return Pattern.<VehicleTelemetry>begin("lowFuel")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getFuelLevelPercent() < LOW_FUEL_WARNING_PERCENT &&
                               t.getFuelLevelPercent() > 0;
                    }
                });
    }
    
    /**
     * ENGINE_OVERHEATING: Engine temperature rising trend followed by critical temp
     * Pattern: Two consecutive readings showing temperature increase to warning level
     */
    public static Pattern<VehicleTelemetry, ?> createEngineOverheatingPattern() {
        return Pattern.<VehicleTelemetry>begin("warming")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getEngineTemperatureCelsius() > ENGINE_TEMP_WARNING_CELSIUS - 5;
                    }
                })
                .followedBy("overheating")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getEngineTemperatureCelsius() > ENGINE_TEMP_WARNING_CELSIUS;
                    }
                })
                .within(Time.seconds(60));
    }
    
    /**
     * LOW_TIRE_PRESSURE: Any tire pressure below warning threshold
     * Pattern: Single event with any tire < 95 PSI
     */
    public static Pattern<VehicleTelemetry, ?> createLowTirePressurePattern() {
        return Pattern.<VehicleTelemetry>begin("lowTire")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getTirePressureFrontLeftPsi() < TIRE_PRESSURE_MIN_WARNING_PSI ||
                               t.getTirePressureFrontRightPsi() < TIRE_PRESSURE_MIN_WARNING_PSI ||
                               t.getTirePressureRearLeftPsi() < TIRE_PRESSURE_MIN_WARNING_PSI ||
                               t.getTirePressureRearRightPsi() < TIRE_PRESSURE_MIN_WARNING_PSI;
                    }
                });
    }
    
    /**
     * EXCESSIVE_SPEED: Speed exceeds critical threshold
     * Pattern: Speed > 60 km/h (absolute maximum for safety)
     */
    public static Pattern<VehicleTelemetry, ?> createExcessiveSpeedPattern() {
        return Pattern.<VehicleTelemetry>begin("overspeed")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() > CRITICAL_SPEED_KPH;
                    }
                });
    }

    /**
     * BRAKE_PRESSURE_LOW: Brake system pressure below safe operating level
     * Pattern: Brake pressure < 90 PSI
     */
    public static Pattern<VehicleTelemetry, ?> createBrakePressureLowPattern() {
        return Pattern.<VehicleTelemetry>begin("lowBrake")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getBrakePressurePsi() < BRAKE_PRESSURE_MIN_WARNING_PSI &&
                               t.getBrakePressurePsi() > 0;
                    }
                });
    }
    
    /**
     * HYDRAULIC_PRESSURE_LOW: Hydraulic system pressure below operational level
     * Pattern: Hydraulic pressure < 2500 PSI
     */
    public static Pattern<VehicleTelemetry, ?> createHydraulicPressureLowPattern() {
        return Pattern.<VehicleTelemetry>begin("lowHydraulic")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getHydraulicPressurePsi() < HYDRAULIC_PRESSURE_MIN_WARNING_PSI &&
                               t.getHydraulicPressurePsi() > 0;
                    }
                });
    }
    
    // ==================== SYSTEM ALERTS ====================
    
    /**
     * SYSTEM_FAULT: Multiple diagnostic codes present
     * Pattern: Diagnostic code count exceeds warning threshold
     */
    public static Pattern<VehicleTelemetry, ?> createSystemFaultPattern() {
        return Pattern.<VehicleTelemetry>begin("fault")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getDiagnosticCodeCount() > MAX_DIAGNOSTIC_CODES_WARNING ||
                               t.isCheckEngineLight();
                    }
                });
    }
    
    /**
     * LOW_BATTERY: Battery level below safe threshold
     * Pattern: Battery < 30%
     */
    public static Pattern<VehicleTelemetry, ?> createLowBatteryPattern() {
        return Pattern.<VehicleTelemetry>begin("lowBattery")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getBatteryLevelPercent() < LOW_BATTERY_WARNING_PERCENT &&
                               t.getBatteryLevelPercent() > 0;
                    }
                });
    }
    
    /**
     * MAINTENANCE_REQUIRED: Operating hours exceed service interval
     * Pattern: Hours > 500 or multiple diagnostic codes
     */
    public static Pattern<VehicleTelemetry, ?> createMaintenanceRequiredPattern() {
        return Pattern.<VehicleTelemetry>begin("maintenance")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return (t.getOperatingHours() > 0 && 
                                t.getOperatingHours() % MAINTENANCE_INTERVAL_HOURS < 10) ||
                               t.getDiagnosticCodeCount() >= MAX_DIAGNOSTIC_CODES_WARNING;
                    }
                });
    }
    
    /**
     * COMMUNICATION_LOSS: Detected via missing telemetry - use timeout pattern
     * Pattern: No events received within 30 second window (handled separately)
     */
    public static Pattern<VehicleTelemetry, ?> createCommunicationLossPattern() {
        // Communication loss is detected via timeout - this pattern checks for warning light
        return Pattern.<VehicleTelemetry>begin("commWarning")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.isWarningLight();
                    }
                });
    }

    // ==================== NAVIGATION ALERTS ====================
    
    /**
     * ROUTE_DEVIATION: Vehicle strays from planned route
     * Pattern: Location significantly different from expected (simulated via large heading change)
     */
    public static Pattern<VehicleTelemetry, ?> createRouteDeviationPattern() {
        return Pattern.<VehicleTelemetry>begin("normalHeading")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() > MIN_MOVING_SPEED_KPH;
                    }
                })
                .followedBy("deviatedHeading")
                .where(new IterativeCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry current, Context<VehicleTelemetry> ctx) throws Exception {
                        for (VehicleTelemetry prev : ctx.getEventsForPattern("normalHeading")) {
                            double headingChange = Math.abs(current.getHeadingDegrees() - prev.getHeadingDegrees());
                            // Account for wraparound at 360 degrees
                            if (headingChange > 180) {
                                headingChange = 360 - headingChange;
                            }
                            // Significant heading change indicates possible route deviation
                            return headingChange > 45 && current.getSpeedKph() > MIN_MOVING_SPEED_KPH;
                        }
                        return false;
                    }
                })
                .within(Time.seconds(10));
    }
    
    /**
     * STUCK_DETECTION: Vehicle not moving despite engine running
     * Pattern: Multiple consecutive readings with speed near 0 but engine RPM > idle
     */
    public static Pattern<VehicleTelemetry, ?> createStuckDetectionPattern() {
        return Pattern.<VehicleTelemetry>begin("notMoving")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getSpeedKph() < MIN_MOVING_SPEED_KPH && 
                               t.getEngineRpm() > ENGINE_MIN_IDLE_RPM;
                    }
                })
                .timesOrMore(3)
                .consecutive()
                .within(Time.seconds(30));
    }
    
    /**
     * POSITIONING_ERROR: GPS accuracy degraded or location invalid
     * Pattern: Detected via unusual coordinate jumps or invalid readings
     */
    public static Pattern<VehicleTelemetry, ?> createPositioningErrorPattern() {
        return Pattern.<VehicleTelemetry>begin("position1")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getLocation() != null && t.getSpeedKph() < 20;
                    }
                })
                .next("position2")
                .where(new IterativeCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry current, Context<VehicleTelemetry> ctx) throws Exception {
                        if (current.getLocation() == null) return true;
                        for (VehicleTelemetry prev : ctx.getEventsForPattern("position1")) {
                            if (prev.getLocation() == null) return true;
                            // Calculate approximate distance (simplified)
                            double latDiff = Math.abs(current.getLocation().getLatitude() - 
                                                     prev.getLocation().getLatitude());
                            double lonDiff = Math.abs(current.getLocation().getLongitude() - 
                                                     prev.getLocation().getLongitude());
                            // Significant jump at low speed indicates GPS error
                            double degreesPerSecond = (latDiff + lonDiff);
                            return degreesPerSecond > 0.001; // ~100m jump at low speed
                        }
                        return false;
                    }
                })
                .within(Time.seconds(5));
    }
    
    // ==================== CRITICAL COMBINED PATTERNS ====================
    
    /**
     * CRITICAL_ENGINE: Engine temp critical AND low oil pressure pattern
     * Pattern: Engine temp > 105°C
     */
    public static Pattern<VehicleTelemetry, ?> createCriticalEnginePattern() {
        return Pattern.<VehicleTelemetry>begin("criticalEngine")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getEngineTemperatureCelsius() > ENGINE_TEMP_CRITICAL_CELSIUS;
                    }
                });
    }
    
    /**
     * CRITICAL_BRAKE_FAILURE: Brake pressure critically low
     * Pattern: Brake pressure < 70 PSI
     */
    public static Pattern<VehicleTelemetry, ?> createCriticalBrakeFailurePattern() {
        return Pattern.<VehicleTelemetry>begin("criticalBrake")
                .where(new SimpleCondition<VehicleTelemetry>() {
                    @Override
                    public boolean filter(VehicleTelemetry t) {
                        return t.getBrakePressurePsi() < BRAKE_PRESSURE_CRITICAL_PSI &&
                               t.getBrakePressurePsi() > 0;
                    }
                });
    }
}
