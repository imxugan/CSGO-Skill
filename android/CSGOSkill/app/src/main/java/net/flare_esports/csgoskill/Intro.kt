/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.ActivityOptions
import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.view.View
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
    private lateinit var prefs: SharedPreferences

    private lateinit var slide1: Frag1
    private lateinit var slide2: Frag2
    private lateinit var slide3: Frag3

    private lateinit var fragEnterFade: Fade
    private lateinit var fragExitFade: Fade

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
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        hasAnimated = false
        connected = false

        fragExitFade = Fade()
        fragExitFade.duration = 700

        fragEnterFade = Fade()
        fragEnterFade.duration = 700
        fragEnterFade.startDelay = 750

        window.exitTransition = Fade()

        Thread(splashing).start()
    }

    override fun onBackPressed() {
        finishAndRemoveTask()
    }

    private fun startIntro() {

        // Load fragments
        slide1 = Frag1()
        slide2 = Frag2()
        slide3 = Frag3()

        // Start continueButton animation
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
        fadeIn.setAnimationListener( Animer().setStart {
            continueButton.isEnabled = true
        })
        continueButton.animation = fadeIn
        continueButton.visibility = View.VISIBLE
        continueButton.imageTintList = ContextCompat.getColorStateList(context, R.color.colorPrimary)

        // Set continueButton onClick method
        continueButton.setOnClickListener {

            // Disable button
            continueButton.isEnabled = false

            // Fade out the stuff
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
            fadeOut.setAnimationListener( Animer {
                imageLogo.visibility = View.GONE

                continueButton.visibility = View.INVISIBLE
                continueButton.imageTintList = ContextCompat.getColorStateList(context, R.color.colorWhite)
            })

            continueButton.startAnimation(fadeOut)
            imageLogo.startAnimation(fadeOut)

            // Open the first fragment after 800ms
            val fragmentTransaction = fManager.beginTransaction()
            slide1.enterTransition = Fade().setDuration(1000)
            fragmentTransaction.add(R.id.introFragmentContainer, slide1)
            handler.postDelayed ({ fragmentTransaction.commit() }, 800)
        }

    }

    // Catch the fragment animation complete messages
    override fun animationComplete(currentFragment: Fragment) {

        // Set the continueButton to a check mark for the last slide
        if (currentFragment == slide3) continueButton.setImageResource(R.drawable.ic_check_white_48dp)

        // Fade in the button and enable it

        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
        fadeIn.setAnimationListener( Animer().setStart {
            continueButton.isEnabled = true
        })

        continueButton.visibility = View.VISIBLE
        continueButton.startAnimation(fadeIn)
        continueButton.setOnClickListener {

            // Disable and fade out the button
            continueButton.isEnabled = false
            if (currentFragment == slide1 || currentFragment == slide2 || currentFragment == slide3) {
                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
                fadeOut.setAnimationListener( Animer {
                    continueButton.visibility = View.GONE
                })
                continueButton.startAnimation(fadeOut)
            }

            val fragmentTransaction = fManager.beginTransaction()
            when (currentFragment) {
                slide1 -> {

                    slide1.exitTransition = fragExitFade
                    slide2.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide2)
                }
                slide2 -> {

                    slide2.exitTransition = fragExitFade
                    slide3.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide3)
                }
                slide3 -> {

                    prefs.edit().putBoolean("firstRun", false).apply()
                    slide3.exitTransition = fragExitFade

                    fragmentTransaction.remove(slide3)
                    handler.postDelayed(toMain, 750)
                }
            }
            fragmentTransaction.commitAllowingStateLoss()
        }
    }

    private fun toMain() {
        val intent = Intent(this@Intro, Main::class.java)

        val users = db.users
        // Try to get the first Steam ID, and if the only entry, auto load it.
        if (users.size == 1) {
            intent.putExtra(Main.STEAMID, users[0].steamId)
        }
        imageLogo.clearAnimation()
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
        val slideUp = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        slideUp.setAnimationListener( Animer { hasAnimated = true } )
        imageLogo.animation = slideUp

        connected = isOnline()
        if (DEVMODE) Log.d("Intro.splashing", "Connection: $connected")
        if (!connected) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()

        val check = db.checkVersion()
        if (DEVMODE) Log.d("Intro.splashing", "Checked: $check")
        when (check) {
            1 -> {
                // Up to date, nothing to do
            }
            0 -> {
                // TODO, updates available
            }
            -1 -> {
                Toast.makeText(context, R.string.failed_server_connection, Toast.LENGTH_LONG).show()
            }
        }

        firstRun = prefs.getBoolean("firstRun", true) //|| DEVMODE

        if (firstRun && !connected) {
            // Request that first timers turn on the internet before using, but they don't HAVE to
            DynamicAlert(context,
                    R.string.first_run_no_internet_warning)
                    .setDismissAction{ handler.postDelayed(startIntro, 200) }.show()
        } else if (firstRun) {
            if (!hasAnimated) {
                try {
                    Thread.sleep(2000)
                } // Yeah, yeah so what it looks nicer
                catch (e: Throwable) {
                    if (DEVMODE) Log.e("Intro.splashing", e)
                }
            }
            handler.postDelayed(startIntro, 200)
        } else {
            if (!hasAnimated) {
                try {
                    Thread.sleep(2000)
                } // Yeah, yeah so what it looks nicer
                catch (e: Throwable) {
                    if (DEVMODE) Log.e("Intro.splashing", e)
                }
            }
            runOnUiThread(toMain)
        }
    }
}
