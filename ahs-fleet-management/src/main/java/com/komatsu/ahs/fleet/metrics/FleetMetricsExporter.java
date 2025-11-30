package com.komatsu.ahs.fleet.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus Metrics Exporter for Fleet Management
 * 
 * Exposes fleet metrics to Prometheus for visualization in Grafana.
 * Includes all VehicleAlertEvent types from Flink CEP processing.
 */
@Component
@Slf4j
public class FleetMetricsExporter {

    private final MeterRegistry meterRegistry;
    
    // Vehicle counts (gauges)
    private final AtomicInteger totalVehicles = new AtomicInteger(0);
    private final AtomicInteger activeVehicles = new AtomicInteger(0);
    private final AtomicInteger idleVehicles = new AtomicInteger(0);
    private final AtomicInteger routingVehicles = new AtomicInteger(0);
    private final AtomicInteger loadingVehicles = new AtomicInteger(0);
    private final AtomicInteger haulingVehicles = new AtomicInteger(0);
    private final AtomicInteger dumpingVehicles = new AtomicInteger(0);
    private final AtomicInteger emergencyStopVehicles = new AtomicInteger(0);
    
    // Alert severity counters
    private Counter criticalAlerts;
    private Counter warningAlerts;
    private Counter errorAlerts;
    private Counter infoAlerts;
    
    // Safety Alert counters
    private Counter collisionWarningAlerts;
    private Counter obstacleDetectedAlerts;
    private Counter safetyEnvelopeBreachAlerts;
    
    // Operational Alert counters
    private Counter lowFuelAlerts;
    private Counter engineOverheatingAlerts;
    private Counter lowTirePressureAlerts;
    private Counter excessiveSpeedAlerts;
    private Counter brakePressureLowAlerts;
    private Counter hydraulicPressureLowAlerts;
    
    // System Alert counters
    private Counter systemFaultAlerts;
    private Counter lowBatteryAlerts;
    private Counter maintenanceRequiredAlerts;
    private Counter communicationLossAlerts;
    
    // Navigation Alert counters
    private Counter routeDeviationAlerts;
    private Counter stuckDetectionAlerts;
    private Counter positioningErrorAlerts;
    
    // Legacy CEP counters (for backward compatibility with existing dashboards)
    private Counter rapidDecelerationAlerts;
    private Counter overheatingAlerts;
    private Counter highTemperatureAlerts;
    private Counter tirePressureAlerts;
    private Counter legacyLowFuelAlerts;
    
    public FleetMetricsExporter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void initMetrics() {
        // Register vehicle count gauges
        Gauge.builder("ahs_fleet_vehicles_total", totalVehicles, AtomicInteger::get)
            .description("Total number of vehicles in fleet")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_active", activeVehicles, AtomicInteger::get)
            .description("Number of active vehicles")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_idle", idleVehicles, AtomicInteger::get)
            .description("Number of idle vehicles")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_routing", routingVehicles, AtomicInteger::get)
            .description("Number of vehicles routing")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_loading", loadingVehicles, AtomicInteger::get)
            .description("Number of vehicles loading")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_hauling", haulingVehicles, AtomicInteger::get)
            .description("Number of vehicles hauling")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_dumping", dumpingVehicles, AtomicInteger::get)
            .description("Number of vehicles dumping")
            .register(meterRegistry);
            
        Gauge.builder("ahs_fleet_vehicles_emergency", emergencyStopVehicles, AtomicInteger::get)
            .description("Number of vehicles in emergency stop")
            .register(meterRegistry);
        
        // Alert severity counters
        criticalAlerts = Counter.builder("ahs_fleet_alerts_critical_total")
            .description("Total count of critical severity alerts")
            .register(meterRegistry);
            
        warningAlerts = Counter.builder("ahs_fleet_alerts_warning_total")
            .description("Total count of warning severity alerts")
            .register(meterRegistry);
            
        errorAlerts = Counter.builder("ahs_fleet_alerts_error_total")
            .description("Total count of error severity alerts")
            .register(meterRegistry);
            
        infoAlerts = Counter.builder("ahs_fleet_alerts_info_total")
            .description("Total count of info severity alerts")
            .register(meterRegistry);

        // Safety Alert counters
        collisionWarningAlerts = Counter.builder("ahs_alert_collision_warning_total")
            .description("Collision warning alerts")
            .register(meterRegistry);
            
        obstacleDetectedAlerts = Counter.builder("ahs_alert_obstacle_detected_total")
            .description("Obstacle detected alerts")
            .register(meterRegistry);
            
        safetyEnvelopeBreachAlerts = Counter.builder("ahs_alert_safety_envelope_breach_total")
            .description("Safety envelope breach alerts")
            .register(meterRegistry);

        // Operational Alert counters
        lowFuelAlerts = Counter.builder("ahs_alert_low_fuel_total")
            .description("Low fuel alerts")
            .register(meterRegistry);
            
        engineOverheatingAlerts = Counter.builder("ahs_alert_engine_overheating_total")
            .description("Engine overheating alerts")
            .register(meterRegistry);
            
        lowTirePressureAlerts = Counter.builder("ahs_alert_low_tire_pressure_total")
            .description("Low tire pressure alerts")
            .register(meterRegistry);
            
        excessiveSpeedAlerts = Counter.builder("ahs_alert_excessive_speed_total")
            .description("Excessive speed alerts")
            .register(meterRegistry);
            
        brakePressureLowAlerts = Counter.builder("ahs_alert_brake_pressure_low_total")
            .description("Brake pressure low alerts")
            .register(meterRegistry);
            
        hydraulicPressureLowAlerts = Counter.builder("ahs_alert_hydraulic_pressure_low_total")
            .description("Hydraulic pressure low alerts")
            .register(meterRegistry);

        // System Alert counters
        systemFaultAlerts = Counter.builder("ahs_alert_system_fault_total")
            .description("System fault alerts")
            .register(meterRegistry);
            
        lowBatteryAlerts = Counter.builder("ahs_alert_low_battery_total")
            .description("Low battery alerts")
            .register(meterRegistry);
            
        maintenanceRequiredAlerts = Counter.builder("ahs_alert_maintenance_required_total")
            .description("Maintenance required alerts")
            .register(meterRegistry);
            
        communicationLossAlerts = Counter.builder("ahs_alert_communication_loss_total")
            .description("Communication loss alerts")
            .register(meterRegistry);

        // Navigation Alert counters
        routeDeviationAlerts = Counter.builder("ahs_alert_route_deviation_total")
            .description("Route deviation alerts")
            .register(meterRegistry);
            
        stuckDetectionAlerts = Counter.builder("ahs_alert_stuck_detection_total")
            .description("Stuck detection alerts")
            .register(meterRegistry);
            
        positioningErrorAlerts = Counter.builder("ahs_alert_positioning_error_total")
            .description("Positioning error alerts")
            .register(meterRegistry);

        // Legacy CEP counters (backward compatibility)
        rapidDecelerationAlerts = Counter.builder("ahs_cep_alerts_rapid_deceleration_total")
            .description("CEP: Rapid deceleration events")
            .register(meterRegistry);
            
        overheatingAlerts = Counter.builder("ahs_cep_alerts_overheating_total")
            .description("CEP: Engine overheating")
            .register(meterRegistry);
            
        highTemperatureAlerts = Counter.builder("ahs_cep_alerts_high_temperature_total")
            .description("CEP: High engine temperature")
            .register(meterRegistry);
            
        tirePressureAlerts = Counter.builder("ahs_cep_alerts_tire_pressure_total")
            .description("CEP: Low tire pressure")
            .register(meterRegistry);
            
        // Keep low fuel for legacy compatibility
        legacyLowFuelAlerts = Counter.builder("ahs_cep_alerts_low_fuel_total")
            .description("CEP: Low fuel alerts")
            .register(meterRegistry);
            
        log.info("Prometheus metrics initialized for fleet management (including all VehicleAlertEvent types)");
    }
    
    // Vehicle count update methods
    public void updateVehicleCounts(int total, int active, int idle, int routing, 
                                     int loading, int hauling, int dumping, int emergency) {
        totalVehicles.set(total);
        activeVehicles.set(active);
        idleVehicles.set(idle);
        routingVehicles.set(routing);
        loadingVehicles.set(loading);
        haulingVehicles.set(hauling);
        dumpingVehicles.set(dumping);
        emergencyStopVehicles.set(emergency);
    }

    // Alert severity increment methods
    public void incrementCriticalAlert() { criticalAlerts.increment(); }
    public void incrementWarningAlert() { warningAlerts.increment(); }
    public void incrementErrorAlert() { errorAlerts.increment(); }
    public void incrementInfoAlert() { infoAlerts.increment(); }
    
    // Legacy CEP methods for backward compatibility
    public void incrementRapidDecelerationAlert() { rapidDecelerationAlerts.increment(); }
    public void incrementLowFuelAlert() { 
        lowFuelAlerts.increment(); 
        legacyLowFuelAlerts.increment();
    }
    public void incrementOverheatingAlert() { overheatingAlerts.increment(); }
    public void incrementHighTemperatureAlert() { highTemperatureAlerts.increment(); }
    public void incrementTirePressureAlert() { tirePressureAlerts.increment(); }
    
    /**
     * Increment CEP alert counter by alert type string.
     * Handles all VehicleAlertEvent.AlertType values.
     */
    public void incrementCepAlertByType(String alertType) {
        if (alertType == null) return;
        
        switch (alertType.toUpperCase()) {
            // Safety Alerts
            case "COLLISION_WARNING":
                collisionWarningAlerts.increment();
                rapidDecelerationAlerts.increment(); // backward compatibility
                break;
            case "OBSTACLE_DETECTED":
                obstacleDetectedAlerts.increment();
                break;
            case "SAFETY_ENVELOPE_BREACH":
                safetyEnvelopeBreachAlerts.increment();
                break;
                
            // Operational Alerts
            case "LOW_FUEL":
                lowFuelAlerts.increment();
                legacyLowFuelAlerts.increment(); // backward compatibility
                break;
            case "ENGINE_OVERHEATING":
                engineOverheatingAlerts.increment();
                overheatingAlerts.increment(); // backward compatibility
                break;
            case "LOW_TIRE_PRESSURE":
            case "TIRE_PRESSURE_LOW":
                lowTirePressureAlerts.increment();
                tirePressureAlerts.increment(); // backward compatibility
                break;
            case "EXCESSIVE_SPEED":
                excessiveSpeedAlerts.increment();
                break;
            case "BRAKE_PRESSURE_LOW":
                brakePressureLowAlerts.increment();
                break;
            case "HYDRAULIC_PRESSURE_LOW":
                hydraulicPressureLowAlerts.increment();
                break;
                
            // System Alerts
            case "SYSTEM_FAULT":
            case "DIAGNOSTIC_ERROR":  // Alias from stream analytics
                systemFaultAlerts.increment();
                break;
            case "LOW_BATTERY":
                lowBatteryAlerts.increment();
                break;
            case "MAINTENANCE_REQUIRED":
                maintenanceRequiredAlerts.increment();
                break;
            case "COMMUNICATION_LOSS":
                communicationLossAlerts.increment();
                break;
                
            // Navigation Alerts
            case "ROUTE_DEVIATION":
                routeDeviationAlerts.increment();
                break;
            case "STUCK_DETECTION":
                stuckDetectionAlerts.increment();
                break;
            case "POSITIONING_ERROR":
                positioningErrorAlerts.increment();
                break;
                
            // Legacy mappings
            case "RAPID_DECELERATION":
                rapidDecelerationAlerts.increment();
                collisionWarningAlerts.increment();
                break;
            case "OVERHEATING":
                overheatingAlerts.increment();
                engineOverheatingAlerts.increment();
                break;
            case "HIGH_TEMPERATURE":
                highTemperatureAlerts.increment();
                break;
                
            default:
                log.debug("Unknown CEP alert type: {}", alertType);
        }
    }
}
