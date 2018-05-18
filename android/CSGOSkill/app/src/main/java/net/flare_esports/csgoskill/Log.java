/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill;

/**
 * Custom Logging class for generally better logging
 */
class Log {

    private static final String PACK_NAME = "CSGO-Skill";

    /**
     * Basic debug logging
     *
     * @param location where this message is coming from
     * @param message string message
     */
    static void d(String location, String message) {
        android.util.Log.d(PACK_NAME, location + ": " + message);
    }

    /**
     * Basic error logging
     *
     * @param location where this error is coming from
     * @param error {@link Throwable} to log
     */
    static void e(String location, Throwable error) {
        if (error == null)
            android.util.Log.e(PACK_NAME, location + ": Throwable");
        else
            android.util.Log.e(PACK_NAME, location + ": " + error.getMessage(), error);
    }

    /**
     * Even more basic error logging
     *
     * @param location where this error is coming from
     * @param message string error
     */
    static void e(String location, String message) {
        android.util.Log.e(PACK_NAME, location + ": " + message);
    }

}
