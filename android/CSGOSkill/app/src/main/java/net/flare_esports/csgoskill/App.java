/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    private static App me;

    public App() {
        me = this;
    }

    public static Context getContext() {
        return me;
    }

}
