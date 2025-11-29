package com.komatsu.ahs.telemetry;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.VehicleTelemetry;
import com.komatsu.ahs.telemetry.function.TelemetryAlertFunction;
import com.komatsu.ahs.telemetry.model.TelemetryAlert;
import org.apache.flink.util.Collector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test class for TelemetryAlertFunction
 */
class TelemetryAlertFunctionTest {

    private TelemetryAlertFunction alertFunction;
    private Collector<TelemetryAlert> collector;
    
    @BeforeEach
    void setUp() {
        alertFunction = new TelemetryAlertFunction();
        collector = mock(Collector.class);
    }
    
    @Test
    void shouldGenerateHighTemperatureAlert() throws Exception {
        // Given
        VehicleTelemetry telemetry = createBasicTelemetry();
        telemetry.setEngineTemperatureCelsius(115.0); // Above threshold of 110
        
        // When
        alertFunction.flatMap(telemetry, collector);
        
        // Then
        ArgumentCaptor<TelemetryAlert> captor = ArgumentCaptor.forClass(TelemetryAlert.class);
        verify(collector, atLeastOnce()).collect(captor.capture());
        
        List<TelemetryAlert> alerts = captor.getAllValues();
        assertThat(alerts).isNotEmpty();
        assertThat(alerts).anyMatch(alert -> 
            alert.getAlertType() == TelemetryAlert.AlertType.HIGH_TEMPERATURE);
    }
    
    @Test
    void shouldGenerateLowFuelAlert() throws Exception {
        // Given
        VehicleTelemetry telemetry = createBasicTelemetry();
        telemetry.setFuelLevelPercent(10.0); // Below threshold of 15
        
        // When
        alertFunction.flatMap(telemetry, collector);
        
        // Then
        ArgumentCaptor<TelemetryAlert> captor = ArgumentCaptor.forClass(TelemetryAlert.class);
        verify(collector, atLeastOnce()).collect(captor.capture());
        
        List<TelemetryAlert> alerts = captor.getAllValues();
        assertThat(alerts).anyMatch(alert -> 
            alert.getAlertType() == TelemetryAlert.AlertType.LOW_FUEL);
    }
    
    @Test
    void shouldGenerateTirePressureAlert() throws Exception {
        // Given
        VehicleTelemetry telemetry = createBasicTelemetry();
        telemetry.setTirePressureFrontLeftPsi(80.0);
        telemetry.setTirePressureFrontRightPsi(80.0);
        telemetry.setTirePressureRearLeftPsi(80.0);
        telemetry.setTirePressureRearRightPsi(80.0);
        
        // When
        alertFunction.flatMap(telemetry, collector);
        
        // Then
        ArgumentCaptor<TelemetryAlert> captor = ArgumentCaptor.forClass(TelemetryAlert.class);
        verify(collector, atLeastOnce()).collect(captor.capture());
        
        List<TelemetryAlert> alerts = captor.getAllValues();
        assertThat(alerts).anyMatch(alert -> 
            alert.getAlertType() == TelemetryAlert.AlertType.TIRE_PRESSURE_LOW);
    }
    
    @Test
    void shouldGenerateOverloadAlert() throws Exception {
        // Given
        VehicleTelemetry telemetry = createBasicTelemetry();
        telemetry.setPayloadTons(450.0); // Above threshold of 400
        
        // When
        alertFunction.flatMap(telemetry, collector);
        
        // Then
        ArgumentCaptor<TelemetryAlert> captor = ArgumentCaptor.forClass(TelemetryAlert.class);
        verify(collector, atLeastOnce()).collect(captor.capture());
        
        List<TelemetryAlert> alerts = captor.getAllValues();
        assertThat(alerts).anyMatch(alert -> 
            alert.getAlertType() == TelemetryAlert.AlertType.OVERLOAD);
    }
    
    @Test
    void shouldNotGenerateAlertsForNormalTelemetry() throws Exception {
        // Given
        VehicleTelemetry telemetry = createBasicTelemetry();
        
        // When
        alertFunction.flatMap(telemetry, collector);
        
        // Then
        verify(collector, never()).collect(any());
    }
    
    /**
     * Create a basic telemetry object with normal values
     */
    private VehicleTelemetry createBasicTelemetry() {
        return VehicleTelemetry.builder()
            .vehicleId("TEST-001")
            .timestamp(Instant.now())
            .location(GpsCoordinate.builder()
                .latitude(40.7128)
                .longitude(-74.0060)
                .build())
            .speedKph(30.0)
            .headingDegrees(90.0)
            .engineRpm(1500.0)
            .fuelLevelPercent(75.0)
            .batteryLevelPercent(85.0)
            .engineTemperatureCelsius(85.0)
            .payloadTons(250.0)
            .isLoaded(true)
            .brakePressurePsi(120.0)
            .tirePressureFrontLeftPsi(100.0)
            .tirePressureFrontRightPsi(100.0)
            .tirePressureRearLeftPsi(100.0)
            .tirePressureRearRightPsi(100.0)
            .tireTemperatureAvgCelsius(65.0)
            .hydraulicPressurePsi(2500.0)
            .hydraulicFluidTempCelsius(60.0)
            .diagnosticCodeCount(0)
            .warningLight(false)
            .checkEngineLight(false)
            .totalDistanceMeters(1000000L)
            .operatingHours(500L)
            .tripCount(100)
            .build();
    }
}
