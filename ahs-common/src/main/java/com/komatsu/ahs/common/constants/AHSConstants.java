package com.komatsu.ahs.common.constants;

/**
 * Constants for Autonomous Haulage System
 */
public final class AHSConstants {
    
    private AHSConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    
    // Vehicle Types
    public static final String VEHICLE_TYPE_930E = "930E";
    public static final String VEHICLE_TYPE_980E = "980E";
    public static final String VEHICLE_TYPE_HD785 = "HD785-7";
    
    // Vehicle States
    public static final String STATE_IDLE = "IDLE";
    public static final String STATE_LOADING = "LOADING";
    public static final String STATE_HAULING = "HAULING";
    public static final String STATE_DUMPING = "DUMPING";
    public static final String STATE_RETURNING = "RETURNING";
    public static final String STATE_MAINTENANCE = "MAINTENANCE";
    public static final String STATE_CHARGING = "CHARGING";
    public static final String STATE_EMERGENCY_STOP = "EMERGENCY_STOP";
    
    // Safety Envelope Constants (meters)
    public static final double SAFETY_ENVELOPE_FRONT = 15.0;
    public static final double SAFETY_ENVELOPE_REAR = 10.0;
    public static final double SAFETY_ENVELOPE_SIDES = 8.0;
    
    // Network Constants
    public static final String NETWORK_TYPE_PRIVATE_LTE = "PRIVATE_LTE";
    public static final int DEFAULT_THRIFT_PORT = 9090;
    public static final int DEFAULT_KAFKA_PORT = 9092;
    
    // Kafka Topics
    public static final String TOPIC_TELEMETRY = "ahs.telemetry";
    public static final String TOPIC_VEHICLE_STATUS = "ahs.vehicle.status";
    public static final String TOPIC_ALERTS = "ahs.alerts";
    public static final String TOPIC_COMMANDS = "ahs.commands";
    public static final String TOPIC_GPS = "ahs.gps";
    public static final String TOPIC_COLLISION_DETECTION = "ahs.collision.detection";
    
    // Timing Constants (milliseconds)
    public static final long TELEMETRY_INTERVAL_MS = 1000;  // 1 second
    public static final long GPS_UPDATE_INTERVAL_MS = 500;   // 500ms
    public static final long HEARTBEAT_INTERVAL_MS = 5000;   // 5 seconds
    public static final long COMMAND_TIMEOUT_MS = 3000;      // 3 seconds
    
    // Thresholds
    public static final double MAX_SPEED_KPH = 60.0;
    public static final double CRITICAL_BATTERY_PERCENT = 15.0;
    public static final double LOW_FUEL_PERCENT = 20.0;
    public static final int MAX_PAYLOAD_TONS = 400;
}
