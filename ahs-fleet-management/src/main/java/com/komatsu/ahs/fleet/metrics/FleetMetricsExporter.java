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
 * Includes CEP (Complex Event Processing) alert metrics from Flink.
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
    
    // CEP Pattern-specific alert counters
    private Counter rapidDecelerationAlerts;
    private Counter lowFuelAlerts;
    private Counter overheatingAlerts;
    private Counter highTemperatureAlerts;
    private Counter tirePressureAlerts;
    
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
        
        // Register alert severity counters
        criticalAlerts = Counter.builder("ahs_fleet_alerts_critical_total")
            .description("Total count of critical severity alerts")
            .register(meterRegistry);
            
        warningAlerts = Counter.builder("ahs_fleet_alerts_warning_total")
            .description("Total count of warning severity alerts")
            .register(meterRegistry);
            
        errorAlerts = Counter.builder("ahs_fleet_alerts_error_total")
            .description("Total count of error severity alerts")
            .register(meterRegistry);

        // Register CEP pattern-specific alert counters
        rapidDecelerationAlerts = Counter.builder("ahs_cep_alerts_rapid_deceleration_total")
            .description("CEP: Rapid deceleration events (>50 to <10 kph in 5s)")
            .register(meterRegistry);
            
        lowFuelAlerts = Counter.builder("ahs_cep_alerts_low_fuel_total")
            .description("CEP: Low fuel alerts (<15%)")
            .register(meterRegistry);
            
        overheatingAlerts = Counter.builder("ahs_cep_alerts_overheating_total")
            .description("CEP: Engine overheating (>95°C sustained)")
            .register(meterRegistry);
            
        highTemperatureAlerts = Counter.builder("ahs_cep_alerts_high_temperature_total")
            .description("CEP: High engine temperature threshold alerts")
            .register(meterRegistry);
            
        tirePressureAlerts = Counter.builder("ahs_cep_alerts_tire_pressure_total")
            .description("CEP: Low tire pressure alerts")
            .register(meterRegistry);
            
        log.info("Prometheus metrics initialized for fleet management (including CEP alerts)");
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
    public void incrementCriticalAlert() {
        criticalAlerts.increment();
    }
    
    public void incrementWarningAlert() {
        warningAlerts.increment();
    }
    
    public void incrementErrorAlert() {
        errorAlerts.increment();
    }
    
    // CEP alert type increment methods
    public void incrementRapidDecelerationAlert() {
        rapidDecelerationAlerts.increment();
    }
    
    public void incrementLowFuelAlert() {
        lowFuelAlerts.increment();
    }
    
    public void incrementOverheatingAlert() {
        overheatingAlerts.increment();
    }
    
    public void incrementHighTemperatureAlert() {
        highTemperatureAlerts.increment();
    }
    
    public void incrementTirePressureAlert() {
        tirePressureAlerts.increment();
    }
    
    /**
     * Increment CEP alert counter by alert type string
     */
    public void incrementCepAlertByType(String alertType) {
        if (alertType == null) return;
        
        switch (alertType.toUpperCase()) {
            case "RAPID_DECELERATION":
                incrementRapidDecelerationAlert();
                break;
            case "LOW_FUEL":
                incrementLowFuelAlert();
                break;
            case "OVERHEATING":
                incrementOverheatingAlert();
                break;
            case "HIGH_TEMPERATURE":
                incrementHighTemperatureAlert();
                break;
            case "TIRE_PRESSURE_LOW":
            case "LOW_TIRE_PRESSURE":
                incrementTirePressureAlert();
                break;
            default:
                log.debug("Unknown CEP alert type: {}", alertType);
        }
    }
}
