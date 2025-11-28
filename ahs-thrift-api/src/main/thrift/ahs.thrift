namespace java com.komatsu.ahs.thrift

/**
 * GPS Coordinate structure
 */
struct TGpsCoordinate {
    1: required double latitude,
    2: required double longitude,
    3: required double altitude,
    4: required double accuracy,
    5: required double heading,
    6: required double speed,
    7: required i64 timestamp
}

/**
 * Vehicle telemetry data
 */
struct TTelemetry {
    1: required string vehicleId,
    2: required i64 timestamp,
    3: required TGpsCoordinate location,
    4: required double speedKph,
    5: required double headingDegrees,
    6: required double engineRpm,
    7: required double fuelLevelPercent,
    8: required double batteryLevelPercent,
    9: required double engineTemperatureCelsius,
    10: required double payloadTons,
    11: required bool isLoaded,
    12: required double brakePressurePsi,
    13: required double hydraulicPressurePsi
}

/**
 * Vehicle state enumeration
 */
enum TVehicleState {
    IDLE = 1,
    LOADING = 2,
    HAULING = 3,
    DUMPING = 4,
    RETURNING = 5,
    MAINTENANCE = 6,
    CHARGING = 7,
    EMERGENCY_STOP = 8
}

/**
 * Vehicle command types
 */
enum TCommandType {
    START = 1,
    STOP = 2,
    PAUSE = 3,
    RESUME = 4,
    EMERGENCY_STOP = 5,
    ASSIGN_ROUTE = 6,
    UPDATE_SPEED = 7,
    RETURN_TO_BASE = 8
}

/**
 * Vehicle command structure
 */
struct TVehicleCommand {
    1: required string commandId,
    2: required string vehicleId,
    3: required TCommandType commandType,
    4: required i64 timestamp,
    5: optional string routeId,
    6: optional double targetSpeed,
    7: optional string parameters
}

/**
 * Command response
 */
struct TCommandResponse {
    1: required string commandId,
    2: required bool success,
    3: optional string message,
    4: required i64 timestamp
}

/**
 * Vehicle status
 */
struct TVehicleStatus {
    1: required string vehicleId,
    2: required TVehicleState state,
    3: required bool autonomousModeEnabled,
    4: required bool operationalStatus,
    5: required TTelemetry telemetry,
    6: optional string assignedRouteId,
    7: optional string currentLocationId,
    8: required i64 lastUpdateTime
}

/**
 * Alert severity levels
 */
enum TAlertSeverity {
    INFO = 1,
    WARNING = 2,
    CRITICAL = 3,
    EMERGENCY = 4
}

/**
 * Alert structure
 */
struct TAlert {
    1: required string alertId,
    2: required string vehicleId,
    3: required TAlertSeverity severity,
    4: required string alertType,
    5: required string message,
    6: required i64 timestamp,
    7: optional string additionalData
}

/**
 * AHS Vehicle Service
 * Provides RPC methods for vehicle control and monitoring
 */
service VehicleService {
    
    /**
     * Send a command to a vehicle
     */
    TCommandResponse sendCommand(1: TVehicleCommand command),
    
    /**
     * Get current vehicle status
     */
    TVehicleStatus getVehicleStatus(1: string vehicleId),
    
    /**
     * Get telemetry data for a vehicle
     */
    TTelemetry getTelemetry(1: string vehicleId),
    
    /**
     * Update vehicle state
     */
    bool updateVehicleState(1: string vehicleId, 2: TVehicleState newState),
    
    /**
     * Get list of all vehicles
     */
    list<string> listVehicles(),
    
    /**
     * Heartbeat to check service availability
     */
    bool heartbeat()
}
