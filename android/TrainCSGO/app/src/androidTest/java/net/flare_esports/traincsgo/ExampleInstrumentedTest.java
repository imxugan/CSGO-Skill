/*
 * Created by the Dev Team from Flare E-Sports on 11/26/17 8:29 AM
 * Copyright (c) 2017. All rights reserved
 *
 * Last modified 9/5/17 12:47 PM
 */

package net.flare_esports.traincsgo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("net.flare_esports.traincsgo", appContext.getPackageName());
    }
}
