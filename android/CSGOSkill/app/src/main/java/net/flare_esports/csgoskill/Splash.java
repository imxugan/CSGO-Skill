/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import static net.flare_esports.csgoskill.Constants.*;
import static net.flare_esports.csgoskill.InternetHelper.*;

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

        /* Oh, simply */ splashing.start();

    }

    /* This is the meat of the operations done by Splash. It starts the animations for the views,
     * it then checks the internet connection (showing related warnings), determines if this is the
     * first time the app is being run (loading straight to the Introduction activity), and if not,
     * attempts to load a connected profile should it be the only one, otherwise starting Login flow
     */
    private Thread splashing = new Thread(new Runnable() {@Override public void run() {
        Looper.prepare();
        ImageView logo = findViewById(R.id.imageLogo);
        Animation animation = AnimationUtils.loadAnimation(Splash.this, R.anim.slide_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) { hasAnimated = true; }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        logo.setAnimation(animation);

        connected = isOnline();
        if (devmode) Log.d("DEV", "Connection: " + connected);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        firstRun = devmode || prefs.getBoolean("firstRun", true);

        boolean alerted = false;
        if (firstRun && !connected) {
            // Request that first timers turn on the internet before using, but they don't HAVE to
            alerted = true;
            new DynamicAlert(Splash.this,
                    R.string.first_run_no_internet_warning,
                    DynamicAlert.THEME_DEFAULT)
                    .setDismissAction(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            try { Thread.sleep(500); } catch (Throwable e) { if (devmode) Log.d("DEV", e.getMessage()); }
                            Intent intent = new Intent(Splash.this, Introduction.class);
                            startActivity(intent);
                            finish();
                        }
                    }).show();
        }
        if (!connected) Toast.makeText(Splash.this, R.string.no_internet_warning, Toast.LENGTH_SHORT).show();

        // Regardless of internet connection, start the app.
        if (firstRun) {
            if (!alerted) { // Don't auto load the activity with an Alert Dialog, wait for dismissal
                Intent intent = new Intent(Splash.this, Introduction.class);
                if (!hasAnimated) {
                    try { Thread.sleep(1250); } // Yeah, yeah so what it looks nicer
                    catch (Throwable e) { if (devmode) Log.d("DEV", e.getMessage()); }
                }
                startActivity(intent);
                finish();
            }
        } else {
            Intent intent = new Intent(Splash.this, MainActivity.class);

            JSONObject[] users = db.getUsers();
            // Try to get the first Steam ID, and if the only entry, auto load it.
            // tempId must be initialized to an empty string!!
            String tempId = "";
            try {
                tempId = users[0].getString("steamid");
            } catch (Throwable e) {
                if (devmode) Log.d("DEV", e.getMessage());
            }
            if (!tempId.isEmpty() && users.length == 1) {
                intent.putExtra("open", "home");
                intent.putExtra("user", tempId);
            } else {
                intent.putExtra("open", "login");
            }

            startActivity(intent);
            finish();
        }
    }});
}
