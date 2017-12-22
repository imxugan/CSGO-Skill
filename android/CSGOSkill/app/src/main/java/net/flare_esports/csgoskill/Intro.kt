/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.*

import kotlinx.android.synthetic.main.activity_intro.*

class Intro : AppCompatActivity() {

    internal lateinit var db: Database

    internal var hasAnimated: Boolean = false
    internal var firstRun: Boolean = false
    internal var connected: Boolean = false

    private val splashing = Runnable { this.splashing() }
    private val toIntroduction = Runnable { this.toIntroduction() }
    private val toMain = Runnable { this.toMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        db = Database(this, null)
        hasAnimated = false
        connected = false

        Thread(splashing).start()

    }

    private fun toIntroduction() {
        /*Intent intent = new Intent(Intro.this, Introduction.class);
        startActivity(intent);
        finish();*/
    }

    private fun toMain() {
        val intent = Intent(this@Intro, MainActivity::class.java)

        val users = db.users
        // Try to get the first Steam ID, and if the only entry, auto load it.
        // tempId must be initialized to an empty string!!
        var tempId = ""
        try {
            tempId = users[0].getString("steamid")
        } catch (e: Throwable) {
            if (devmode) Log.d("DEV", e.message)
        }

        if (!tempId.isEmpty() && users.size == 1) {
            intent.putExtra("open", "home")
            intent.putExtra("user", tempId)
        } else {
            intent.putExtra("open", "login")
        }

        startActivity(intent)
        finishAfterTransition()
    }

    /* This is the meat of the operations done by Intro. It starts the animations for the views,
     * it then checks the internet connection (showing related warnings), determines if this is the
     * first time the app is being run (loading straight to the Introduction activity), and if not,
     * attempts to load a connected profile should it be the only one, otherwise starting Login flow
     */
    private fun splashing() {
        Looper.prepare()
        val animation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                hasAnimated = true
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        imageLogo.animation = animation

        connected = isOnline()
        if (devmode) Log.d("DEV", "Connection: " + connected)

        val prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        firstRun = devmode || prefs.getBoolean("firstRun", true)

        var alerted = false
        if (firstRun && !connected) {
            // Request that first timers turn on the internet before using, but they don't HAVE to
            alerted = true
            DynamicAlert(this,
                    R.string.first_run_no_internet_warning,
                    DynamicAlert.THEME_DEFAULT)
                    .setDismissAction(DialogInterface.OnDismissListener { runOnUiThread(toIntroduction) }).show()
        }
        if (!connected) Toast.makeText(this, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()

        // Regardless of internet connection, start the app.
        if (firstRun) {
            if (!alerted) { // Don't auto load the activity with an Alert Dialog, wait for dismissal
                if (!hasAnimated) {
                    try {
                        Thread.sleep(1250)
                    } // Yeah, yeah so what it looks nicer
                    catch (e: Throwable) {
                        if (devmode) Log.d("DEV", e.message)
                    }

                }
                runOnUiThread(toIntroduction)
            }
        } else {
            runOnUiThread(toMain)
        }
    }
}
