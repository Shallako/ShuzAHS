package com.komatsu.ahs.telemetry.function;

import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import com.komatsu.ahs.telemetry.model.TelemetryAlert.AlertSeverity;
import com.komatsu.ahs.telemetry.model.TelemetryAlert.AlertType;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Telemetry Alert Function
 * 
 * Analyzes vehicle telemetry and generates alerts when thresholds are exceeded
 */
public class TelemetryAlertFunction implements FlatMapFunction<VehicleTelemetry, TelemetryAlert> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryAlertFunction.class);
    
    // Threshold constants
    private static final double MAX_ENGINE_TEMP_CELSIUS = 110.0;
    private static final double MIN_FUEL_PERCENT = 15.0;
    private static final double MIN_BATTERY_PERCENT = 20.0;
    private static final double MIN_TIRE_PRESSURE_PSI = 85.0;
    private static final double MAX_TIRE_TEMP_CELSIUS = 90.0;
    private static final double MIN_BRAKE_PRESSURE_PSI = 100.0;
    private static final double MAX_PAYLOAD_TONS = 400.0;
    
    @Override
    public void flatMap(VehicleTelemetry telemetry, Collector<TelemetryAlert> out) throws Exception {
        // Check engine temperature
        if (telemetry.getEngineTemperatureCelsius() > MAX_ENGINE_TEMP_CELSIUS) {
            out.collect(createAlert(
                telemetry,
                AlertType.HIGH_TEMPERATURE,
                AlertSeverity.CRITICAL,
                "Engine temperature exceeded safe operating limit",
                telemetry.getEngineTemperatureCelsius(),
                MAX_ENGINE_TEMP_CELSIUS
            ));
        }
        
        // Check fuel level
        if (telemetry.getFuelLevelPercent() < MIN_FUEL_PERCENT && 
            telemetry.getFuelLevelPercent() > 0) {
            out.collect(createAlert(
                telemetry,
                AlertType.LOW_FUEL,
                AlertSeverity.WARNING,
                "Fuel level is low",
                telemetry.getFuelLevelPercent(),
                MIN_FUEL_PERCENT
            ));
        }
        
        // Check battery level
        if (telemetry.getBatteryLevelPercent() < MIN_BATTERY_PERCENT && 
            telemetry.getBatteryLevelPercent() > 0) {
            out.collect(createAlert(
                telemetry,
                AlertType.LOW_BATTERY,
                AlertSeverity.WARNING,
                "Battery level is low",
                telemetry.getBatteryLevelPercent(),
                MIN_BATTERY_PERCENT
            ));
        }
        
        // Check tire pressure (average of all tires)
        double avgTirePressure = (telemetry.getTirePressureFrontLeftPsi() +
                                  telemetry.getTirePressureFrontRightPsi() +
                                  telemetry.getTirePressureRearLeftPsi() +
                                  telemetry.getTirePressureRearRightPsi()) / 4.0;
        
        if (avgTirePressure < MIN_TIRE_PRESSURE_PSI && avgTirePressure > 0) {
            out.collect(createAlert(
                telemetry,
                AlertType.TIRE_PRESSURE_LOW,
                AlertSeverity.CRITICAL,
                "Average tire pressure is below safe threshold",
                avgTirePressure,
                MIN_TIRE_PRESSURE_PSI
            ));
        }
        
        // Check tire temperature
        if (telemetry.getTireTemperatureAvgCelsius() > MAX_TIRE_TEMP_CELSIUS) {
            out.collect(createAlert(
                telemetry,
                AlertType.TIRE_TEMPERATURE_HIGH,
                AlertSeverity.WARNING,
                "Tire temperature is elevated",
                telemetry.getTireTemperatureAvgCelsius(),
                MAX_TIRE_TEMP_CELSIUS
            ));
        }
        
        // Check brake pressure
        if (telemetry.getBrakePressurePsi() < MIN_BRAKE_PRESSURE_PSI) {
            out.collect(createAlert(
                telemetry,
                AlertType.BRAKE_PRESSURE_LOW,
                AlertSeverity.CRITICAL,
                "Brake pressure is critically low",
                telemetry.getBrakePressurePsi(),
                MIN_BRAKE_PRESSURE_PSI
            ));
        }
        
        // Check payload overload
        if (telemetry.getPayloadTons() > MAX_PAYLOAD_TONS) {
            out.collect(createAlert(
                telemetry,
                AlertType.OVERLOAD,
                AlertSeverity.CRITICAL,
                "Vehicle is overloaded beyond safe capacity",
                telemetry.getPayloadTons(),
                MAX_PAYLOAD_TONS
            ));
        }
        
        // Check warning lights
        if (telemetry.isWarningLight()) {
            out.collect(createAlert(
                telemetry,
                AlertType.ENGINE_WARNING,
                AlertSeverity.WARNING,
                "Warning light is active",
                1.0,
                0.0
            ));
        }
        
        // Check diagnostic codes
        if (telemetry.getDiagnosticCodeCount() > 0) {
            out.collect(createAlert(
                telemetry,
                AlertType.DIAGNOSTIC_ERROR,
                AlertSeverity.WARNING,
                String.format("%d diagnostic code(s) detected", 
                    telemetry.getDiagnosticCodeCount()),
                telemetry.getDiagnosticCodeCount(),
                0.0
            ));
        }
    }
    
    /**
     * Helper method to create an alert
     */
    private TelemetryAlert createAlert(
            VehicleTelemetry telemetry,
            AlertType alertType,
            AlertSeverity severity,
            String message,
            double metricValue,
            double thresholdValue) {
        
        return TelemetryAlert.builder()
            .alertId(UUID.randomUUID().toString())
            .vehicleId(telemetry.getVehicleId())
            .timestamp(telemetry.getTimestamp())
            .alertType(alertType)
            .severity(severity)
            .message(message)
            .metricValue(metricValue)
            .thresholdValue(thresholdValue)
            .build();
    }
}
