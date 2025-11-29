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
 * Exposes fleet metrics to Prometheus for visualization in Grafana
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
    
    // Alert counters
    private Counter criticalAlerts;
    private Counter warningAlerts;
    private Counter errorAlerts;
    
    public FleetMetricsExporter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void initMetrics() {
        // Register vehicle count gauges
        Gauge.builder("ahs.fleet.vehicles.total", totalVehicles, AtomicInteger::get)
            .description("Total number of vehicles in fleet")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.active", activeVehicles, AtomicInteger::get)
            .description("Number of active vehicles")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.idle", idleVehicles, AtomicInteger::get)
            .description("Number of idle vehicles")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.routing", routingVehicles, AtomicInteger::get)
            .description("Number of vehicles routing")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.loading", loadingVehicles, AtomicInteger::get)
            .description("Number of vehicles loading")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.hauling", haulingVehicles, AtomicInteger::get)
            .description("Number of vehicles hauling")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.dumping", dumpingVehicles, AtomicInteger::get)
            .description("Number of vehicles dumping")
            .register(meterRegistry);
            
        Gauge.builder("ahs.fleet.vehicles.emergency", emergencyStopVehicles, AtomicInteger::get)
            .description("Number of vehicles in emergency stop")
            .register(meterRegistry);
        
        // Register alert counters
        criticalAlerts = Counter.builder("ahs.fleet.alerts.critical")
            .description("Count of critical alerts")
            .register(meterRegistry);
            
        warningAlerts = Counter.builder("ahs.fleet.alerts.warning")
            .description("Count of warning alerts")
            .register(meterRegistry);
            
        errorAlerts = Counter.builder("ahs.fleet.alerts.error")
            .description("Count of error alerts")
            .register(meterRegistry);
            
        log.info("Prometheus metrics initialized for fleet management");
    }
    
    // Update methods
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
    
    public void incrementCriticalAlert() {
        criticalAlerts.increment();
    }
    
    public void incrementWarningAlert() {
        warningAlerts.increment();
    }
    
    public void incrementErrorAlert() {
        errorAlerts.increment();
    }
}
