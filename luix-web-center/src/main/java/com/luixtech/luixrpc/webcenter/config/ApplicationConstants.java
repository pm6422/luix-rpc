package com.luixtech.luixrpc.webcenter.config;

import java.util.Locale;

/**
 * Application constants.
 */
public final class ApplicationConstants {
    public static final String   BASE_PACKAGE        = "com.luixtech.luixrpc.webcenter";
    public static final String   SPRING_PROFILE_DEV  = "dev";
    public static final String   SPRING_PROFILE_DEMO = "demo";
    public static final String   SPRING_PROFILE_PROD = "prod";
    public static final String[] AVAILABLE_PROFILES  = new String[]{SPRING_PROFILE_DEV, SPRING_PROFILE_DEMO, SPRING_PROFILE_PROD};
    public static final Locale   SYSTEM_LOCALE       = Locale.US;
    public static final String   DEFAULT_REG         = "consul://localhost:8500/registry";
}
