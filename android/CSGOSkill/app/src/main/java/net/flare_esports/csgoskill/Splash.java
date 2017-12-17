/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import static net.flare_esports.csgoskill.Constants.*;

public class Splash extends AppCompatActivity {

    Database db;
    boolean hasAnimated;
    boolean firstRun;
    boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        db = new Database(this, null);
        hasAnimated = false;
        connected = false;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                ImageView logo = findViewById(R.id.imageLogo);
                Animation animation = AnimationUtils.loadAnimation(Splash.this, R.anim.slide_up);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) {}
                    @Override public void onAnimationEnd(Animation animation) { hasAnimated = true; }
                    @Override public void onAnimationRepeat(Animation animation) {}
                });
                logo.setAnimation(animation);

                connected = IntenetCheck.isOnline();
                if (devmode) Log.d("DEV", "Connection: " + connected);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                firstRun = devmode || prefs.getBoolean("firstRun", true);

                if (firstRun && !connected) {
                    // Request that first timers turn on the internet before using, but they don't HAVE to
                    new DynamicAlert(Splash.this, R.string.first_run_no_internet_warning).show();
                }
                // Regardless of internet connection, start the app.

                if (firstRun) {
                    Intent intent = new Intent(Splash.this, Introduction.class);
                    if (!hasAnimated) {
                        try { Thread.sleep(1250); } // Yeah, yeah so what it looks nicer
                        catch (Throwable e) { if (devmode) Log.d("DEV", e.getMessage()); }
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(Splash.this, Introduction.class);
                }

            }
        });

    }

}
