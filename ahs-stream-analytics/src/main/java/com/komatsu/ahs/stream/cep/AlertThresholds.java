package com.komatsu.ahs.stream.cep;

/**
 * Configuration class for CEP alert thresholds
 * Based on Komatsu 930E and 980E haul truck specifications
 */
public final class AlertThresholds {
    
    private AlertThresholds() {} // Utility class
    
    // ==================== SPEED THRESHOLDS ====================
    /** Maximum safe speed for loaded haul trucks (km/h) */
    public static final double MAX_SPEED_LOADED_KPH = 45.0;
    /** Maximum safe speed for empty haul trucks (km/h) */
    public static final double MAX_SPEED_EMPTY_KPH = 55.0;
    /** Critical overspeed threshold (km/h) */
    public static final double CRITICAL_SPEED_KPH = 60.0;
    /** Minimum speed for "moving" detection (km/h) */
    public static final double MIN_MOVING_SPEED_KPH = 0.5;
    /** Rapid deceleration threshold (km/h per second) */
    public static final double RAPID_DECEL_THRESHOLD = 5.0;
    
    // ==================== FUEL THRESHOLDS ====================
    /** Low fuel warning threshold (percent) */
    public static final double LOW_FUEL_WARNING_PERCENT = 20.0;
    /** Critical fuel level (percent) */
    public static final double CRITICAL_FUEL_PERCENT = 10.0;
    
    // ==================== BATTERY THRESHOLDS ====================
    /** Low battery warning threshold (percent) */
    public static final double LOW_BATTERY_WARNING_PERCENT = 30.0;
    /** Critical battery level (percent) */
    public static final double CRITICAL_BATTERY_PERCENT = 15.0;
    
    // ==================== ENGINE THRESHOLDS ====================
    /** Engine overheating warning (Celsius) */
    public static final double ENGINE_TEMP_WARNING_CELSIUS = 95.0;
    /** Engine critical temperature (Celsius) */
    public static final double ENGINE_TEMP_CRITICAL_CELSIUS = 105.0;
    /** Maximum safe engine RPM */
    public static final double ENGINE_MAX_RPM = 2100.0;
    /** Minimum idle RPM */
    public static final double ENGINE_MIN_IDLE_RPM = 650.0;
    
    // ==================== TIRE PRESSURE THRESHOLDS ====================
    /** Minimum tire pressure warning (PSI) - for Komatsu haul trucks */
    public static final double TIRE_PRESSURE_MIN_WARNING_PSI = 95.0;
    /** Critical low tire pressure (PSI) */
    public static final double TIRE_PRESSURE_CRITICAL_PSI = 85.0;
    /** Maximum tire pressure (PSI) */
    public static final double TIRE_PRESSURE_MAX_PSI = 130.0;
    /** Tire temperature warning (Celsius) */
    public static final double TIRE_TEMP_WARNING_CELSIUS = 80.0;
    
    // ==================== BRAKE THRESHOLDS ====================
    /** Minimum brake pressure warning (PSI) */
    public static final double BRAKE_PRESSURE_MIN_WARNING_PSI = 90.0;
    /** Critical brake pressure (PSI) */
    public static final double BRAKE_PRESSURE_CRITICAL_PSI = 70.0;
    /** Normal brake pressure range (PSI) */
    public static final double BRAKE_PRESSURE_NORMAL_PSI = 120.0;
    
    // ==================== HYDRAULIC THRESHOLDS ====================
    /** Minimum hydraulic pressure warning (PSI) */
    public static final double HYDRAULIC_PRESSURE_MIN_WARNING_PSI = 2500.0;
    /** Critical hydraulic pressure (PSI) */
    public static final double HYDRAULIC_PRESSURE_CRITICAL_PSI = 2000.0;
    /** Hydraulic fluid temperature warning (Celsius) */
    public static final double HYDRAULIC_TEMP_WARNING_CELSIUS = 85.0;
    /** Hydraulic fluid critical temperature (Celsius) */
    public static final double HYDRAULIC_TEMP_CRITICAL_CELSIUS = 95.0;
    
    // ==================== PAYLOAD THRESHOLDS ====================
    /** Maximum payload for 930E (tons) */
    public static final double MAX_PAYLOAD_930E_TONS = 320.0;
    /** Maximum payload for 980E (tons) */
    public static final double MAX_PAYLOAD_980E_TONS = 400.0;
    /** Overload warning threshold (percent over max) */
    public static final double OVERLOAD_WARNING_PERCENT = 105.0;
    
    // ==================== TIME THRESHOLDS ====================
    /** Stuck detection time window (milliseconds) */
    public static final long STUCK_DETECTION_WINDOW_MS = 120_000L;
    /** Communication loss detection window (milliseconds) */
    public static final long COMMUNICATION_LOSS_WINDOW_MS = 30_000L;
    /** Maintenance interval (operating hours) */
    public static final long MAINTENANCE_INTERVAL_HOURS = 500L;
    /** Pattern detection window (milliseconds) */
    public static final long PATTERN_WINDOW_MS = 60_000L;
    
    // ==================== NAVIGATION THRESHOLDS ====================
    /** Route deviation tolerance (meters) */
    public static final double ROUTE_DEVIATION_TOLERANCE_METERS = 5.0;
    /** GPS accuracy warning threshold (meters) */
    public static final double GPS_ACCURACY_WARNING_METERS = 2.0;
    
    // ==================== DIAGNOSTIC THRESHOLDS ====================
    /** Maximum diagnostic codes before warning */
    public static final int MAX_DIAGNOSTIC_CODES_WARNING = 3;
    /** Maximum diagnostic codes before critical */
    public static final int MAX_DIAGNOSTIC_CODES_CRITICAL = 5;
}
