/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill;

class Log {

    private static final String PACK_NAME = "CSGO-Skill";

    /*static void d(String message) {
        Log.d(PACK_NAME, message);
    }*/

    static void d(String location, String message) {
        android.util.Log.d(PACK_NAME, location + ": " + message);
    }

    static void e(String location, Throwable error) {
        if (error == null)
            android.util.Log.e(PACK_NAME, location + ": Throwable");
        else
            android.util.Log.e(PACK_NAME, location + ": " + error.getMessage(), error);
    }

    static void e(String location, String message) {
        android.util.Log.e(PACK_NAME, location + ": " + message);
    }

}
