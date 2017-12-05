/*
 * Created by the Dev Team from Flare E-Sports on 12/5/17 4:13 PM
 * Copyright (c) 2017. All rights reserved.
 *
 * Last modified 12/5/17 4:03 PM
 */

package net.flare_esports.csgoskill;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("net.flare_esports.csgoskill", appContext.getPackageName());
    }
}
