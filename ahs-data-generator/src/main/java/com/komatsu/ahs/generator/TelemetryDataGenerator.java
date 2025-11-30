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
 */
public class TelemetryDataGenerator {

  private final ThreadLocalRandom random = ThreadLocalRandom.current();

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
  
  // Anomaly injection rates (percentage chance per telemetry generation)
  private static final double LOW_FUEL_ANOMALY_RATE = 0.02;      // 2% chance
  private static final double OVERHEATING_ANOMALY_RATE = 0.01;   // 1% chance
  private static final double HIGH_SPEED_ANOMALY_RATE = 0.03;    // 3% chance (for rapid decel CEP)

  /** Generate realistic telemetry for a vehicle based on its current state */
  public VehicleTelemetry generateTelemetry(String vehicleId, VehicleStatus status) {
    VehicleTelemetry telemetry = new VehicleTelemetry();
    telemetry.setVehicleId(vehicleId);
    telemetry.setTimestamp(Instant.now());

    // Generate GPS coordinates
    GpsCoordinate gps = generateGpsCoordinate();
    telemetry.setLocation(gps);
    
    // Check for anomaly injection
    boolean injectLowFuel = random.nextDouble() < LOW_FUEL_ANOMALY_RATE;
    boolean injectOverheating = random.nextDouble() < OVERHEATING_ANOMALY_RATE;
    boolean injectHighSpeed = random.nextDouble() < HIGH_SPEED_ANOMALY_RATE;

    // Generate metrics based on vehicle status
    switch (status) {
      case HAULING:
        // High speed anomaly for rapid deceleration CEP pattern (>50 kph)
        if (injectHighSpeed) {
          telemetry.setSpeedKph(randomDouble(55.0, 65.0)); // Above normal loaded speed
        } else {
          telemetry.setSpeedKph(randomDouble(20.0, MAX_SPEED_LOADED));
        }
        double maxLoad = vehicleId.contains("980E") ? MAX_LOAD_980E : MAX_LOAD_930E;
        telemetry.setPayloadTons(randomDouble(maxLoad * 0.8, maxLoad));
        telemetry.setLoaded(true);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(3.0, 12.0) : randomDouble(40.0, MAX_FUEL));
        break;

      case LOADING:
        telemetry.setSpeedKph(0.0);
        telemetry.setPayloadTons(randomDouble(50.0, 250.0));
        telemetry.setLoaded(false);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(5.0, 14.0) : randomDouble(50.0, MAX_FUEL));
        break;

      case DUMPING:
        telemetry.setSpeedKph(0.0);
        telemetry.setPayloadTons(randomDouble(0.0, 50.0));
        telemetry.setLoaded(false);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(4.0, 13.0) : randomDouble(40.0, MAX_FUEL));
        break;

      case IDLE:
        telemetry.setSpeedKph(0.0);
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(2.0, 10.0) : randomDouble(MIN_FUEL, MAX_FUEL));
        break;

      case ROUTING:
        // High speed anomaly for rapid deceleration CEP pattern
        if (injectHighSpeed) {
          telemetry.setSpeedKph(randomDouble(52.0, 60.0)); // Above threshold
        } else {
          telemetry.setSpeedKph(randomDouble(30.0, MAX_SPEED_EMPTY));
        }
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(6.0, 14.0) : randomDouble(30.0, MAX_FUEL));
        break;

      default:
        telemetry.setSpeedKph(0.0);
        telemetry.setPayloadTons(0.0);
        telemetry.setLoaded(false);
        telemetry.setFuelLevelPercent(injectLowFuel ? randomDouble(3.0, 12.0) : randomDouble(MIN_FUEL, MAX_FUEL));
    }


    // Engine temperature - inject overheating anomaly (>95°C for CEP pattern)
    if (injectOverheating) {
      telemetry.setEngineTemperatureCelsius(randomDouble(96.0, 105.0)); // Above threshold
    } else {
      telemetry.setEngineTemperatureCelsius(randomDouble(80.0, 94.0));
    }
    
    telemetry.setEngineRpm(
        telemetry.getSpeedKph() > 0 ? randomDouble(1200.0, 1800.0) : randomDouble(600.0, 800.0));
    telemetry.setHeadingDegrees(randomDouble(0.0, 360.0));
    telemetry.setTirePressureFrontLeftPsi(randomDouble(90.0, 110.0));
    telemetry.setTirePressureFrontRightPsi(randomDouble(90.0, 110.0));
    telemetry.setTirePressureRearLeftPsi(randomDouble(90.0, 110.0));
    telemetry.setTirePressureRearRightPsi(randomDouble(90.0, 110.0));
    telemetry.setBrakePressurePsi(randomDouble(85.0, 120.0));
    telemetry.setHydraulicPressurePsi(randomDouble(2200.0, 3200.0));

    return telemetry;
  }

  /** Generate GPS coordinate within mine site boundaries */
  private GpsCoordinate generateGpsCoordinate() {
    return GpsCoordinate.builder()
        .latitude(randomDouble(MIN_LATITUDE, MAX_LATITUDE))
        .longitude(randomDouble(MIN_LONGITUDE, MAX_LONGITUDE))
        .altitude(randomDouble(MIN_ALTITUDE, MAX_ALTITUDE))
        .accuracy(randomDouble(0.0, 10.0))
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
