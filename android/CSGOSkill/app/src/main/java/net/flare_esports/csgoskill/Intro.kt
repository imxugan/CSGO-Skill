/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.animation.Animator
import android.animation.AnimatorInflater
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.*

import kotlinx.android.synthetic.main.activity_intro.*
import net.flare_esports.csgoskill.IntroFrags.*

class Intro : AppCompatActivity(), Slide.SlideListener {

    internal lateinit var db: Database
    internal lateinit var handler: Handler
    internal lateinit var fragmentManager: FragmentManager
    internal lateinit var context: Context

    internal lateinit var slide1: Frag1
    internal lateinit var slide2: Fragment
    internal lateinit var slide3: Fragment

    internal lateinit var fadeOut: Animation
    internal lateinit var fadeIn: Animation

    internal var hasAnimated: Boolean = false
    internal var firstRun: Boolean = false
    internal var connected: Boolean = false

    private val splashing = Runnable { this.splashing() }
    private val startIntro = Runnable { this.startIntro() }
    private val toMain = Runnable { this.toMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        db = Database(this, null)
        handler = Handler()
        fragmentManager = getFragmentManager()
        context = this

        hasAnimated = false
        connected = false

        fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

        Thread(splashing).start()
    }

    private fun startIntro() {
        fabStartIntro.animation = fadeIn
        fabStartIntro.visibility = View.VISIBLE
        fabStartIntro.setOnClickListener {
            var fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) { fabStartIntro.visibility = View.GONE }
                override fun onAnimationRepeat(animation: Animation) {}
            })
            fabStartIntro.startAnimation(fadeOut)
            fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) { imageLogo.visibility = View.GONE }
                override fun onAnimationRepeat(animation: Animation) {}
            })
            imageLogo.startAnimation(fadeOut)
            slide1 = Frag1()
            fragmentManager = getFragmentManager()
            val fragmentTransaction = fragmentManager.beginTransaction()
            slide1.enterTransition = Fade().setDuration(1000)
            fragmentTransaction.replace(R.id.intro_frag_container, slide1)
            handler.postDelayed ({ fragmentTransaction.commit() }, 800)
        }

    }

    override fun nextSlide(currentFragment: Fragment) {
        when (currentFragment) {
            slide1 -> {
                /*val fragmentTransaction = fragmentManager.beginTransaction()
                slide1.exitTransition = Fade().setDuration(1000)*/
                DynamicAlert(context, "Hey it works!").show()
            }
        }
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
     * first time the app is being run (loading straight to the introduction slides), and if not,
     * attempts to load a connected profile should it be the only one, otherwise starting Login flow
     */
    private fun splashing() {
        Looper.prepare()
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
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
            DynamicAlert(context,
                    R.string.first_run_no_internet_warning)
                    .setDismissAction(DialogInterface.OnDismissListener { handler.postDelayed(startIntro, 200) }).show()
        }
        if (!connected) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()

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
                handler.postDelayed(startIntro, 200)
            }
        } else {
            runOnUiThread(toMain)
        }
    }
}
