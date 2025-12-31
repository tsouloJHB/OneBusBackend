package com.backend.onebus.constants;

public class RuleConstants {

    // Operational Rules
    public static final String TERMINAL_DWELL_TIME = "TERMINAL_DWELL_TIME";
    public static final String AUTO_INACTIVATE_ON_IDLE = "AUTO_INACTIVATE_ON_IDLE";
    public static final String OFF_ROUTE_ALERTING = "OFF_ROUTE_ALERTING";

    // Direction & Inference Rules (currently implemented defaults)
    public static final String AUTO_FLIP_AT_TERMINAL = "AUTO_FLIP_AT_TERMINAL";
    public static final String FIRST_GPS_SOUTHBOUND = "FIRST_GPS_SOUTHBOUND";
    public static final String CIRCULAR_ROUTE_MODE = "CIRCULAR_ROUTE_MODE";
    public static final String PROXIMITY_SENSITIVITY = "PROXIMITY_SENSITIVITY";
    public static final String MANUAL_DIRECTION_OVERRIDE = "MANUAL_DIRECTION_OVERRIDE";

    // Tracking & Privacy Rules
    public static final String GEOFENCED_TRACKING = "GEOFENCED_TRACKING";
    public static final String SHIFT_BASED_TRACKING = "SHIFT_BASED_TRACKING";
    public static final String GHOSTING_PREVENTION = "GHOSTING_PREVENTION";

    // Categories
    public static final String CATEGORY_OPERATIONAL = "OPERATIONAL";
    public static final String CATEGORY_DIRECTION = "DIRECTION_INFERENCE";
    public static final String CATEGORY_TRACKING = "TRACKING";

    // Types
    public static final String TYPE_BOOLEAN = "BOOLEAN";
    public static final String TYPE_NUMBER = "NUMBER";
    public static final String TYPE_STRING = "STRING";
}
