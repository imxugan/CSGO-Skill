/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.util.Log;

import static net.flare_esports.csgoskill.Constants.*;

class IntenetCheck {

    public static boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (Throwable e) {
            if (devmode) { Log.d("DEV",e.getMessage()); }
        }
        return false;
    }

}
