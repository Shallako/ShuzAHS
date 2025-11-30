package com.komatsu.ahs.generator;

import com.komatsu.ahs.domain.model.GpsCoordinate;
import com.komatsu.ahs.domain.model.Location;
import com.komatsu.ahs.domain.model.VehicleStatus;
import com.komatsu.ahs.domain.model.VehicleTelemetry;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Telemetry Data Generator
 *
 * <p>Generates realistic telemetry data for autonomous haul trucks simulating various operational
 * scenarios in a mine environment. Includes anomaly injection for CEP pattern testing.
 * 
 * <p>Supports all 16 VehicleAlertEvent types:
 * <ul>
 *   <li>Safety: COLLISION_WARNING, OBSTACLE_DETECTED, SAFETY_ENVELOPE_BREACH</li>
 *   <li>Operational: LOW_FUEL, ENGINE_OVERHEATING, LOW_TIRE_PRESSURE, EXCESSIVE_SPEED, 
 *       BRAKE_PRESSURE_LOW, HYDRAULIC_PRESSURE_LOW</li>
 *   <li>System: SYSTEM_FAULT, LOW_BATTERY, MAINTENANCE_REQUIRED, COMMUNICATION_LOSS</li>
 *   <li>Navigation: ROUTE_DEVIATION, STUCK_DETECTION, POSITIONING_ERROR</li>
 * </ul>
 */
public class TelemetryDataGenerator {

  private final ThreadLocalRandom random = ThreadLocalRandom.current();
  
  // Track previous values for pattern detection
  private double previousHeading = 0.0;
  private GpsCoordinate previousLocation = null;
  private long operatingHoursBase = 480; // Start near maintenance threshold

  // Mine site boundaries (simulated coordinates for a large open-pit mine)
  private static final double MIN_LATITUDE = -23.5;
  private static final double MAX_LATITUDE = -23.3;
  private static final double MIN_LONGITUDE = -70.5;
  private static final double MAX_LONGITUDE = -70.3;
  private static final double MIN_ALTITUDE = 2800.0;
  private static final double MAX_ALTITUDE = 3200.0;

  // Operational parameters for Komatsu 930E/980E trucks
  private static final double MIN_SPEED = 0.0;
  private static final double MAX_SPEED_LOADED = 40.0; // km/h loaded
  private static final double MAX_SPEED_EMPTY = 60.0; // km/h empty
  private static final double MIN_FUEL = 5.0;
  private static final double MAX_FUEL = 100.0;
  private static final double MIN_LOAD = 0.0;
  private static final double MAX_LOAD_930E = 300.0; // tons
  private static final double MAX_LOAD_980E = 400.0; // tons
  
  // ==================== ANOMALY INJECTION RATES ====================
  // Safety Alerts (higher rates for safety-critical patterns)
  private static final double COLLISION_WARNING_RATE = 0.02;      // 2% - rapid deceleration
  private static final double OBSTACLE_DETECTED_RATE = 0.015;     // 1.5% - sudden stop
  private static final double SAFETY_ENVELOPE_BREACH_RATE = 0.02; // 2% - overspeed for load state
  
  // Operational Alerts
  private static final double LOW_FUEL_RATE = 0.02;               // 2% - fuel < 20%
  private static final double ENGINE_OVERHEATING_RATE = 0.015;    // 1.5% - temp > 95°C
  private static final double LOW_TIRE_PRESSURE_RATE = 0.02;      // 2% - tire < 95 PSI
  private static final double EXCESSIVE_SPEED_RATE = 0.02;        // 2% - speed > 60 kph
  private static final double BRAKE_PRESSURE_LOW_RATE = 0.015;    // 1.5% - brake < 90 PSI
  private static final double HYDRAULIC_PRESSURE_LOW_RATE = 0.015;// 1.5% - hydraulic < 2500 PSI
  
  // System Alerts
  private static final double SYSTEM_FAULT_RATE = 0.01;           // 1% - diagnostic codes
  private static final double LOW_BATTERY_RATE = 0.015;           // 1.5% - battery < 30%
  private static final double MAINTENANCE_REQUIRED_RATE = 0.01;   // 1% - hours near 500
  private static final double COMMUNICATION_LOSS_RATE = 0.01;     // 1% - warning light
  
  // Navigation Alerts
  private static final double ROUTE_DEVIATION_RATE = 0.02;        // 2% - heading change > 45°
  private static final double STUCK_DETECTION_RATE = 0.01;        // 1% - no movement + high RPM
  private static final double POSITIONING_ERROR_RATE = 0.015;     // 1.5% - GPS jump

  /** Generate realistic telemetry for a vehicle based on its current state */
  public VehicleTelemetry generateTelemetry(String vehicleId, VehicleStatus status) {
    VehicleTelemetry telemetry = new VehicleTelemetry();
    telemetry.setVehicleId(vehicleId);
    telemetry.setTimestamp(Instant.now());

    // ==================== ANOMALY INJECTION FLAGS ====================
    // Safety Alerts
    boolean injectCollisionWarning = random.nextDouble() < COLLISION_WARNING_RATE;
    boolean injectObstacleDetected = random.nextDouble() < OBSTACLE_DETECTED_RATE;
    boolean injectSafetyEnvelopeBreach = random.nextDouble() < SAFETY_ENVELOPE_BREACH_RATE;
    
    // Operational Alerts
    boolean injectLowFuel = random.nextDouble() < LOW_FUEL_RATE;
    boolean injectOverheating = random.nextDouble() < ENGINE_OVERHEATING_RATE;
    boolean injectLowTirePressure = random.nextDouble() < LOW_TIRE_PRESSURE_RATE;
    boolean injectExcessiveSpeed = random.nextDouble() < EXCESSIVE_SPEED_RATE;
    boolean injectLowBrakePressure = random.nextDouble() < BRAKE_PRESSURE_LOW_RATE;
    boolean injectLowHydraulicPressure = random.nextDouble() < HYDRAULIC_PRESSURE_LOW_RATE;
    
    // System Alerts
    boolean injectSystemFault = random.nextDouble() < SYSTEM_FAULT_RATE;
    boolean injectLowBattery = random.nextDouble() < LOW_BATTERY_RATE;
    boolean injectMaintenanceRequired = random.nextDouble() < MAINTENANCE_REQUIRED_RATE;
    boolean injectCommunicationLoss = random.nextDouble() < COMMUNICATION_LOSS_RATE;
    
    // Navigation Alerts
    boolean injectRouteDeviation = random.nextDouble() < ROUTE_DEVIATION_RATE;
    boolean injectStuckDetection = random.nextDouble() < STUCK_DETECTION_RATE;
    boolean injectPositioningError = random.nextDouble() < POSITIONING_ERROR_RATE;

    // ==================== GPS & LOCATION ====================
    GpsCoordinate gps;
    if (injectPositioningError && previousLocation != null) {
      // Create a GPS jump (positioning error) - sudden location shift > 100m at low speed
      gps = GpsCoordinate.builder()
          .latitude(previousLocation.getLatitude() + randomDouble(0.002, 0.005))
          .longitude(previousLocation.getLongitude() + randomDouble(0.002, 0.005))
          .altitude(randomDouble(MIN_ALTITUDE, MAX_ALTITUDE))
          .accuracy(randomDouble(5.0, 15.0)) // Poor accuracy
          .heading(randomDouble(0, 359))
          .speed(randomDouble(0.0, 15.0)) // Low speed during GPS error
          .build();
    } else {
      gps = generateGpsCoordinate();
    }
    telemetry.setLocation(gps);
    previousLocation = gps;

    // ==================== SPEED & MOVEMENT ====================
    double speed;
    switch (status) {
      case HAULING:
        if (injectExcessiveSpeed || injectSafetyEnvelopeBreach) {
          speed = randomDouble(55.0, 68.0); // Above critical speed
        } else if (injectCollisionWarning) {
          speed = randomDouble(50.0, 60.0); // High speed before rapid decel
        } else {
          speed = randomDouble(20.0, MAX_SPEED_LOADED);
        }
        double maxLoad = vehicleId.contains("980E") ? MAX_LOAD_980E : MAX_LOAD_930E;
        telemetry.setPayloadTons(randomDouble(maxLoad * 0.8, maxLoad));
        telemetry.setLoaded(true);
        break;

      case LOADING:
        speed = injectStuckDetection ? 0.0 : 0.0; // Always 0 when loading
        telemetry.setPayloadTons(randomDouble(50.0, 250.0));
        telemetry.setLoaded(false);
        break;

      case DUMPING:
        speed = 0.0;
        telemetry.setPayloadTons(randomDouble(0.0, 50.0));
        telemetry.setLoaded(false);
        break;

      case IDLE:
        speed = injectStuckDetection ? 0.0 : 0.0;
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
        break;

      case ROUTING:
        if (injectExcessiveSpeed || injectSafetyEnvelopeBreach) {
          speed = randomDouble(58.0, 68.0); // Above empty threshold
        } else if (injectObstacleDetected) {
          speed = randomDouble(15.0, 25.0); // Moving before sudden stop
        } else {
          speed = randomDouble(30.0, MAX_SPEED_EMPTY);
        }
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
        break;

      default:
        speed = 0.0;
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
    }
    telemetry.setSpeedKph(speed);

    // ==================== FUEL ====================
    if (injectLowFuel) {
      telemetry.setFuelLevelPercent(randomDouble(3.0, 18.0)); // Below 20% threshold
    } else {
      telemetry.setFuelLevelPercent(randomDouble(35.0, MAX_FUEL));
    }

    // ==================== ENGINE TEMPERATURE ====================
    if (injectOverheating) {
      telemetry.setEngineTemperatureCelsius(randomDouble(96.0, 108.0)); // Above 95°C threshold
    } else {
      telemetry.setEngineTemperatureCelsius(randomDouble(78.0, 92.0));
    }
    
    // ==================== ENGINE RPM ====================
    if (injectStuckDetection && speed < 0.5) {
      // High RPM but not moving = stuck
      telemetry.setEngineRpm(randomDouble(1000.0, 1600.0));
    } else {
      telemetry.setEngineRpm(speed > 0.5 ? randomDouble(1200.0, 1800.0) : randomDouble(600.0, 800.0));
    }

    // ==================== HEADING (for route deviation) ====================
    double newHeading;
    if (injectRouteDeviation && speed > 5.0) {
      // Large heading change > 45° to trigger route deviation
      newHeading = (previousHeading + randomDouble(50.0, 120.0)) % 360;
    } else {
      // Normal heading with small variations
      newHeading = (previousHeading + randomDouble(-10.0, 10.0) + 360) % 360;
    }
    telemetry.setHeadingDegrees(newHeading);
    previousHeading = newHeading;

    // ==================== TIRE PRESSURE ====================
    if (injectLowTirePressure) {
      // At least one tire below 95 PSI threshold
      telemetry.setTirePressureFrontLeftPsi(randomDouble(82.0, 93.0));
      telemetry.setTirePressureFrontRightPsi(randomDouble(96.0, 108.0));
      telemetry.setTirePressureRearLeftPsi(randomDouble(96.0, 108.0));
      telemetry.setTirePressureRearRightPsi(randomDouble(96.0, 108.0));
    } else {
      telemetry.setTirePressureFrontLeftPsi(randomDouble(98.0, 115.0));
      telemetry.setTirePressureFrontRightPsi(randomDouble(98.0, 115.0));
      telemetry.setTirePressureRearLeftPsi(randomDouble(98.0, 115.0));
      telemetry.setTirePressureRearRightPsi(randomDouble(98.0, 115.0));
    }
    telemetry.setTireTemperatureAvgCelsius(randomDouble(40.0, 75.0));

    // ==================== BRAKE PRESSURE ====================
    if (injectLowBrakePressure) {
      telemetry.setBrakePressurePsi(randomDouble(72.0, 88.0)); // Below 90 PSI threshold
    } else {
      telemetry.setBrakePressurePsi(randomDouble(95.0, 125.0));
    }

    // ==================== HYDRAULIC PRESSURE ====================
    if (injectLowHydraulicPressure) {
      telemetry.setHydraulicPressurePsi(randomDouble(2100.0, 2450.0)); // Below 2500 PSI threshold
    } else {
      telemetry.setHydraulicPressurePsi(randomDouble(2600.0, 3200.0));
    }
    telemetry.setHydraulicFluidTempCelsius(randomDouble(50.0, 80.0));

    // ==================== BATTERY ====================
    if (injectLowBattery) {
      telemetry.setBatteryLevelPercent(randomDouble(12.0, 28.0)); // Below 30% threshold
    } else {
      telemetry.setBatteryLevelPercent(randomDouble(70.0, 100.0));
    }

    // ==================== DIAGNOSTICS (System Fault & Maintenance) ====================
    if (injectSystemFault) {
      telemetry.setDiagnosticCodeCount(random.nextInt(4, 8)); // Above 3 threshold
      telemetry.setCheckEngineLight(true);
    } else {
      telemetry.setDiagnosticCodeCount(random.nextInt(0, 2));
      telemetry.setCheckEngineLight(false);
    }

    // ==================== WARNING LIGHT (Communication Loss) ====================
    telemetry.setWarningLight(injectCommunicationLoss);

    // ==================== OPERATING HOURS (Maintenance Required) ====================
    if (injectMaintenanceRequired) {
      // Near or past 500-hour maintenance interval
      telemetry.setOperatingHours(500 + random.nextLong(-5, 15));
    } else {
      // Normal operating hours, increment slowly
      operatingHoursBase += random.nextLong(0, 2);
      telemetry.setOperatingHours(operatingHoursBase % 490); // Keep below threshold normally
    }

    // ==================== OPERATIONAL METRICS ====================
    telemetry.setTotalDistanceMeters(random.nextLong(100000, 5000000));
    telemetry.setTripCount(random.nextInt(100, 2000));

    return telemetry;
  }

  /** Generate GPS coordinate within mine site boundaries */
  private GpsCoordinate generateGpsCoordinate() {
    return GpsCoordinate.builder()
        .latitude(randomDouble(MIN_LATITUDE, MAX_LATITUDE))
        .longitude(randomDouble(MIN_LONGITUDE, MAX_LONGITUDE))
        .altitude(randomDouble(MIN_ALTITUDE, MAX_ALTITUDE))
        .accuracy(randomDouble(0.5, 3.0))
        .heading(randomDouble(0, 359))
        .speed(randomDouble(0.0, MAX_SPEED_EMPTY))
        .build();
  }

  /** Generate random double within range */
  private double randomDouble(double min, double max) {
    return min + (max - min) * random.nextDouble();
  }


  /** Generate a load location (shovel position) */
  public Location generateLoadLocation() {
    return Location.builder()
        .name("Shovel-" + random.nextInt(1, 6))
        .type(Location.LocationType.LOADING_POINT)
        .coordinate(
            GpsCoordinate.builder()
                .latitude(randomDouble(MIN_LATITUDE + 0.05, MIN_LATITUDE + 0.1))
                .longitude(randomDouble(MIN_LONGITUDE + 0.05, MIN_LONGITUDE + 0.1))
                .altitude(MIN_ALTITUDE + 50.0)
                .accuracy(randomDouble(0.0, 10.0))
                .heading(randomDouble(0, 359))
                .speed(randomDouble(MIN_SPEED, MAX_SPEED_LOADED))
                .build())
        .build();
  }

  /** Generate a dump location */
  public Location generateDumpLocation() {
    return Location.builder()
        .name("Dump-" + random.nextInt(1, 4))
        .type(Location.LocationType.DUMP_POINT)
        .coordinate(
            GpsCoordinate.builder()
                .latitude(randomDouble(MAX_LATITUDE - 0.1, MAX_LATITUDE - 0.05))
                .longitude(randomDouble(MAX_LONGITUDE - 0.1, MAX_LONGITUDE - 0.05))
                .altitude(MAX_ALTITUDE - 50.0)
                .accuracy(randomDouble(0.0, 10.0))
                .heading(randomDouble(0, 359))
                .speed(randomDouble(MIN_SPEED, MAX_SPEED_EMPTY))
                .build())
        .build();
  }
}
