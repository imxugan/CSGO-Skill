/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.ActivityOptions
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
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

    private lateinit var db: Database
    private lateinit var handler: Handler
    private lateinit var fManager: FragmentManager
    private lateinit var context: Context

    private lateinit var slide1: Frag1
    private lateinit var slide2: Frag2
    private lateinit var slide3: Frag3

    private lateinit var fadeOut: Animation
    private lateinit var fadeIn: Animation

    private var hasAnimated: Boolean = false
    private var firstRun: Boolean = false
    private var connected: Boolean = false

    private val splashing = Runnable { this.splashing() }
    private val startIntro = Runnable { this.startIntro() }
    private val toMain = Runnable { this.toMain() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        db = Database(this, null)
        handler = Handler()
        fManager = fragmentManager
        context = this

        hasAnimated = false
        connected = false

        fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_long)
        fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)

        window.exitTransition = Fade()

        Thread(splashing).start()
    }

    private fun startIntro() {
        slide1 = Frag1()
        slide2 = Frag2()
        slide3 = Frag3()
        ibContinue.animation = fadeIn
        ibContinue.imageTintList = ContextCompat.getColorStateList(context, R.color.colorPrimary)
        ibContinue.visibility = View.VISIBLE
        ibContinue.setOnClickListener {
            var fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
            fadeOut.setAnimationListener( Animer {
                ibContinue.visibility = View.INVISIBLE
                ibContinue.imageTintList = ContextCompat.getColorStateList(context, R.color.colorWhite)
            })
            ibContinue.startAnimation(fadeOut)
            fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
            fadeOut.setAnimationListener( Animer {
                imageLogo.visibility = View.GONE
            })
            imageLogo.startAnimation(fadeOut)
            val fragmentTransaction = fManager.beginTransaction()
            slide1.enterTransition = Fade().setDuration(1000)
            fragmentTransaction.add(R.id.introFragmentContainer, slide1)
            handler.postDelayed ({ fragmentTransaction.commit() }, 800)
        }

    }

    override fun animationComplete(currentFragment: Fragment) {

        if (currentFragment == slide3) ibContinue.setImageResource(R.drawable.ic_check_white_48dp)
        ibContinue.visibility = View.VISIBLE
        ibContinue.startAnimation(fadeIn)
        ibContinue.setOnClickListener {

            val exitFade = Fade()
            exitFade.duration = 700
            val enterFade = Fade()
            enterFade.duration = 700
            enterFade.startDelay = 750

            if (currentFragment == slide1 || currentFragment == slide2 || currentFragment == slide3) {
                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
                fadeOut.setAnimationListener( Animer {
                    ibContinue.visibility = View.GONE
                })
                ibContinue.startAnimation(fadeOut)
            }

            val fragmentTransaction = fManager.beginTransaction()
            when (currentFragment) {
                slide1 -> {

                    slide1.exitTransition = exitFade
                    slide2.enterTransition = enterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide2)
                }
                slide2 -> {

                    slide2.exitTransition = exitFade
                    slide3.enterTransition = enterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide3)
                }
                slide3 -> {

                    slide3.exitTransition = exitFade

                    fragmentTransaction.remove(slide3)
                    handler.postDelayed(toMain, 750)
                }
            }
            fragmentTransaction.commitAllowingStateLoss()
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
            if (devmode) Log.e("DEV", e.message)
        }

        if (!tempId.isEmpty() && users.size == 1) {
            intent.putExtra(INAME_OPEN, HOME_FRAG)
            intent.putExtra(INAME_USER, tempId)
        } else {
            intent.putExtra(INAME_OPEN, LOGIN_FRAG)
        }

        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@Intro).toBundle())
        handler.postDelayed({ finish() }, 1000)
    }

    /* This is the meat of the operations done by Intro. It starts the animations for the views,
     * it then checks the internet connection (showing related warnings), determines if this is the
     * first time the app is being run (loading straight to the introduction slides), and if not,
     * attempts to load a connected profile should it be the only one, otherwise starting Login flow
     */
    private fun splashing() {
        Looper.prepare()
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        animation.setAnimationListener( Animer { hasAnimated = true } )
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
                    .setDismissAction{ handler.postDelayed(startIntro, 200) }.show()
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
                        if (devmode) Log.e("DEV", e.message)
                    }

                }
                handler.postDelayed(startIntro, 200)
            }
        } else {
            runOnUiThread(toMain)
        }
    }
}
