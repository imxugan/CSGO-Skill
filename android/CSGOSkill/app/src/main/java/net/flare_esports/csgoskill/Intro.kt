/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.ActivityOptions
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.transition.Fade
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.Database.Updating.*

import kotlinx.android.synthetic.main.activity_intro.*
import net.flare_esports.csgoskill.IntroFrags.*
import net.flare_esports.csgoskill.Preferences.AUTO_LOGIN
import net.flare_esports.csgoskill.Preferences.FIRST_RUN
import java.util.*
import java.util.prefs.PreferenceChangeEvent

/**
 * Splash screen activity, responsible for also auto-logging in the player from the device. It only
 * uses the internet to check if connection is available, and does not login to the server.
 */
class Intro : AppCompatActivity(), Slide.SlideListener {

    private lateinit var db: Database
    private lateinit var handler: Handler
    private lateinit var fManager: FragmentManager
    private lateinit var context: Context

    private lateinit var slide1: Frag1
    private lateinit var slide2: Frag2
    private lateinit var slide3: Frag3

    private lateinit var fragEnterFade: Fade
    private lateinit var fragExitFade: Fade

    private var hasAnimated: Boolean = false
    private var firstRun: Boolean = false
    private var connected: Boolean = false
    private var closing: Boolean = false
    private var switching: Boolean = false
    private var shouldFinish: Boolean = false

    private val splashing = Runnable { this.splashing() }
    private val startIntro = Runnable { this.startIntro() }
    private val toMain = Runnable { this.toMain() }

    /**
     * onCreate() function for the activity. Sets up the fade animations and starts [splashing]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        if (!isTaskRoot) {
            finish()
            return
        }

        db = Database(this, null)
        handler = Handler()
        fManager = fragmentManager
        context = this

        hasAnimated = false
        connected = false

        fragExitFade = Fade()
        fragExitFade.duration = 700

        fragEnterFade = Fade()
        fragEnterFade.duration = 700
        fragEnterFade.startDelay = 750

        window.exitTransition = fragExitFade

        Thread(splashing).start()
    }

    /**
     * onStop() function for the activity. Calls [finish] when the Intro starts the Main activity
     */
    override fun onStop() {
        super.onStop()
        if (DEV_MODE) Log.d("Intro", "STOPPED")
        if (shouldFinish)
            finish()
    }

    /**
     * Responsible for hiding the navigation when this activity is in focus
     */
    override fun onWindowFocusChanged(hasFocus:Boolean) {
            super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                       View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    /**
     * Handles BACK button presses, going backwards in the intro slides if applicable, or closing
     * the app completely otherwise.
     */
    override fun onBackPressed() {
        val previous = fManager.findFragmentById(R.id.introFragmentContainer) as Slide?

        if (previous != null && !switching && !closing) {
            switching = true
            // Disable and hide the NEXT button
            continueButton.isEnabled = false
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
            fadeOut.setAnimationListener( Animer {
                continueButton.visibility = View.GONE
            })
            continueButton.startAnimation(fadeOut)

            val fragmentTransaction = fManager.beginTransaction()
            when (previous.name) {
                "slide2" -> {
                    slide2.exitTransition = fragExitFade
                    slide1.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide1)
                }
                "slide3" -> {
                    continueButton.setImageResource(R.drawable.ic_arrow_forward_white_48dp)
                    slide3.exitTransition = fragExitFade
                    slide2.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide2)
                }
                else -> {
                    switching = false
                    closing = true // Signal app is closing
                }
            }
            fragmentTransaction.commitAllowingStateLoss()
            handler.postDelayed({switching = false}, 1450)
        } else if (!switching && !closing) {
            closing = true
        }

        if (closing && !switching) { // Using "switching" prevents BACK button spam
            switching = true
            handler.removeCallbacksAndMessages(null) // Remove pending stuff
            val toast = Toast.makeText(this, R.string.closing_app_message, Toast.LENGTH_SHORT)
            toast.show()
            handler.postDelayed({
                toast.cancel() // Hide the message sooner
                finishAndRemoveTask() // Kindly kill the app completely
            }, 2000)
        }

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

        if (!closing) {
            continueButton.animation = fadeIn
            continueButton.visibility = View.VISIBLE
            continueButton.imageTintList = ContextCompat.getColorStateList(context, R.color.primary)
        }

        // Set continueButton onClick method
        continueButton.setOnClickListener {

            // Disable button
            continueButton.isEnabled = false

            if (!closing) {
                switching = true
                // Fade out the stuff
                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
                fadeOut.setAnimationListener( Animer {
                    imageLogo.visibility = View.GONE

                    continueButton.visibility = View.INVISIBLE
                    continueButton.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
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

    }

    /**
     * Captures the fragment slides' "animationComplete" message, signalling to show the continue
     * button and load the next slide, or calls [toMain] when on the last slide
     */
    override fun animationComplete(currentFragment: Slide) {
        switching = false
        // Set the continueButton to a check mark for the last slide
        if (currentFragment.name == "slide3") continueButton.setImageResource(R.drawable.ic_check_white_48dp)

        // Fade in the button and enable it

        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
        fadeIn.setAnimationListener( Animer().setStart {
            continueButton.isEnabled = true
        })

        continueButton.visibility = View.VISIBLE
        continueButton.startAnimation(fadeIn)
        continueButton.setOnClickListener { if (!switching && !closing) {
            switching = true
            // Disable and fade out the button
            continueButton.isEnabled = false
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
            fadeOut.setAnimationListener( Animer {
                continueButton.visibility = View.GONE
            })
            continueButton.startAnimation(fadeOut)

            val fragmentTransaction = fManager.beginTransaction()
            when (currentFragment.name) {
                "slide1" -> {

                    slide1.exitTransition = fragExitFade
                    slide2.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide2)
                }
                "slide2" -> {

                    slide2.exitTransition = fragExitFade
                    slide3.enterTransition = fragEnterFade

                    fragmentTransaction.replace(introFragmentContainer.id, slide3)
                }
                "slide3" -> {

                    Preferences.setBoolean(FIRST_RUN, false)
                    slide3.exitTransition = fragExitFade

                    fragmentTransaction.remove(slide3)
                    handler.postDelayed(toMain, 750)
                }
            }
            fragmentTransaction.commitAllowingStateLoss()
        } }
    }

    private fun toMain() {
        val intent = Intent(this@Intro, Main::class.java)

        if (Preferences.getBoolean(AUTO_LOGIN, true)) {
            val users = db.users
            // Try to get the first Steam ID, and if the only entry, auto load it.
            if (users.size == 1) {
                intent.putExtra(Main.STEAM_ID, users[0].steamId)
            }
        }
        imageLogo.clearAnimation()
        if (!closing) {
            shouldFinish = true
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this@Intro).toBundle())
        }
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
        if (DEV_MODE) Log.d("Intro.splashing", "Connection: $connected")

        val check = db.checkVersion()
        if (DEV_MODE) Log.d("Intro.splashing", "Checked: $check")
        when (check) {
            UP_TO_DATE -> {
                // Up to date, nothing to do
            }
            UPDATES_AVAILABLE -> {
                // TODO, updates available
            }
            FAILED -> {
                Toast.makeText(context, R.string.failed_server_connection, Toast.LENGTH_LONG).show()
            }
        }

        firstRun = Preferences.getBoolean(FIRST_RUN, true)// || DEV_MODE
        if (!connected) {
            if (firstRun) {
                handler.postDelayed({runOnUiThread {
                    // More apparent request that first timers turn on the internet before using, although they don't HAVE to
                    DynamicAlert(context, R.string.first_run_no_internet_warning)
                            .setDismissAction { handler.postDelayed(startIntro, 200) }
                            .setCancelable(true) // Allow back button cancel, just to be safe
                            .show()
                }}, if (!hasAnimated) 2200L else 200L)
            } else {
                Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            }
        }

        if (firstRun) {
            // Show special message for users who have their locale set to non-english, begging them to help translate the app
            if (Locale.getDefault().language != "en") {
                handler.postDelayed({runOnUiThread {
                    DynamicAlert(context)
                            .setHTML("Hey there! If you can read this, then CSGO Skill <b>needs you!</b><br><br>If you are bilingual, then please consider helping to translate this app! Once you finish the login process, just tap the gear icon when you see it and select the Contribute on GitHub option.<br><br>GL HF!")
                            .setDismissAction { handler.postDelayed(startIntro, 200) }
                            .setCancelable(true) // Allow back button cancel, just to be safe
                            .show()
                }}, if (!hasAnimated) 2200L else 200L)
            } else {
                handler.postDelayed(startIntro,
                        if (!hasAnimated) 2200L
                        else 200L
                )
            }
        } else {
            handler.postDelayed({runOnUiThread(toMain)},
                    if (!hasAnimated) 2200L
                    else 200L
            )
        }
    }
}
