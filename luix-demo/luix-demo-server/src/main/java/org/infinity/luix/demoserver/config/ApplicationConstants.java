package org.infinity.luix.demoserver.config;

import java.util.Locale;

/**
 * Application constants.
 */
public final class ApplicationConstants {
    public static final String   BASE_PACKAGE        = "org.infinity.luix.demoserver";
    public static final String   SPRING_PROFILE_DEMO = "demo";
    public static final String   SPRING_PROFILE_PROD = "prod";
    public static final String[] AVAILABLE_PROFILES  = new String[]{SPRING_PROFILE_DEMO, SPRING_PROFILE_PROD};
    public static final Locale   SYSTEM_LOCALE       = Locale.SIMPLIFIED_CHINESE;
}
