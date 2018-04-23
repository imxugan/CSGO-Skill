/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.FragmentManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.transition.Fade
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast

import kotlinx.android.synthetic.main.include_progress_overlay.*
import kotlinx.android.synthetic.main.activity_main.*
import net.flare_esports.csgoskill.Constants.*
import org.json.JSONObject

class Main : AppCompatActivity(), BaseFragment.FragmentListener {

    companion object {

        // Fragment selection options for switchFragment()
        @JvmStatic val LOC_LOGIN  = 0
        @JvmStatic val LOC_HOME   = 1
        @JvmStatic val LOC_SIGNUP = 2

        // Intent options from Intro
        @JvmStatic val STEAMID    = "steamid"

    }

    private lateinit var context: Context
    private lateinit var db: Database
    private lateinit var fManager: FragmentManager
    private lateinit var handler: Handler
    private lateinit var prefs: SharedPreferences
    private var player: Player? = null

    private lateinit var LoginFrag: LoginFragment
    private lateinit var HomeFrag: HomeFragment

    private var shouldExit = false
    private var closing = false

    private val toLogin = Runnable { shouldExit = false; switchFragment(LOC_LOGIN) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.allowEnterTransitionOverlap = true
        window.enterTransition = Fade()

        context = this
        fManager = fragmentManager
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        db = Database(context, null)
        handler = Handler()

        LoginFrag = LoginFragment()
        HomeFrag = HomeFragment()

        val steamId = intent.getStringExtra(STEAMID) ?: ""
        if (steamId.isEmpty())
            switchFragment(LOC_LOGIN)
        else {
            switchFragment(LOC_HOME)
            handler.postDelayed({loadUser(steamId)}, 500)
        }

        bottomNavigation.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(LOC_HOME)
                R.id.nav_stats -> { } // TODO
                R.id.nav_settings -> { } // TODO
            }
            item.itemId != R.id.nav_settings
        }

    }

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

    override fun onResume() {
        super.onResume()

        val fragment = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
                ?: return switchFragment(LOC_LOGIN) // Go to login if no fragment is in the view, if that ever happens.
        when (fragment.name) {
            "login" -> {
                bottomNavigation.visibility = View.GONE
                topBar.visibility = View.GONE
            }
            "home" -> {
                bottomNavigation.visibility = View.VISIBLE
                topBar.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        if (closing) {return}
        if (shouldExit) {
            closing = true
            handler.removeCallbacksAndMessages(null)
            finishAndRemoveTask()
        }
        val fragment = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
                ?: return finishAndRemoveTask() // Close app if no fragment is in the view, if that ever happens.
        if (fragment.onBack()) {return}
        else when (fragment.name) {
            "login" -> {
                Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show()
                shouldExit = true
                handler.postDelayed({ shouldExit = false }, 2500)
            }
            "home" -> {
                shouldExit = true
                handler.postDelayed(toLogin, 600)
            }
        }
    }

    override fun getPlayer(): Player? {
        return player
    }

    override fun getHistoryStats(): JSONObject? {
        val player1 = player ?: return null
        return db.getHistoryStats(player1)
    }

    override fun getGrandStats(): JSONObject? {
        val player1 = player ?: return null
        return db.getGrandStats(player1)
    }

    override fun getCurrentStats(): JSONObject? {
        val player1 = player ?: return null
        return db.getCurrentStats(player1)
    }

    override fun loginPlayer(player: Player): Boolean {
        try {
            if (db.getPlayer(player.steamId) == null) {
                if (!db.insertUser(player))
                    throw db.lastError ?: Throwable("unexpected")
            }
            loadUser(player.steamId)
            return true
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Main.loginPlayer", e)
            var m = e.message ?: ""
            m = when (m) {
                "unexpected" -> {
                    "Unexpected error while logging in. Please report this."
                }
                else -> m
            }
            DynamicAlert(this, m).setTitle("Aw crap").show()
            return false
        }
    }

    override fun switchFragment(nextFragment: Int) {
        if (closing) {return}
        val fTransaction = fManager.beginTransaction()
        val previous = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
        if (previous != null) {
            previous.exitTransition = Fade().setDuration(300)
        }
        val fadeIn = Fade()
        fadeIn.duration = 200
        when (nextFragment) {
            LOC_HOME -> {
                if (previous == null || previous.name != "home") {
                    HomeFrag.enterTransition = fadeIn
                    fTransaction.replace(R.id.mainFragmentContainer, HomeFrag)
                    fTransaction.commit()
                }
                bottomNavigation.visibility = View.VISIBLE
                topBar.visibility = View.VISIBLE
            }
            LOC_LOGIN -> {
                if (previous == null || previous.name != "login") {
                    LoginFrag.enterTransition = fadeIn
                    fTransaction.replace(R.id.mainFragmentContainer, LoginFrag)
                    fTransaction.commit()
                }
                bottomNavigation.visibility = View.GONE
                topBar.visibility = View.GONE
            }
            else -> {
                if (DEVMODE)
                    Log.e("DEV", "Unknown fragment requested: $nextFragment")
            }
        }
    }

    override fun updatePlayer(): Boolean {
        try {
            toggleLoader(true)
            val player = this.player ?: return false
            if (!db.loginPlayer(player)) throw db.lastError ?: Throwable("unexpected")
            this.player = db.getPlayer(player.steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            return true
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Main.updateStats", e)
            var m = e.message ?: ""
            m = when (m) {
                "unexpected" -> {
                    "Unexpected error while logging in. Please report this."
                }
                else -> m
            }
            DynamicAlert(this, m).setTitle("Aw crap").show()
        }
        return false

    }

    override fun updateStats(): Boolean {
        try {
            val player = this.player ?: return false
            if (!db.updateStats(player))
                throw db.lastError ?: Throwable("unexpected")
            this.player = db.getPlayer(player.steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            return true
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Main.updateStats", e)
            var m = e.message ?: ""
            m = when (m) {
                "unexpected" -> {
                    "Unexpected error while logging in. Please report this."
                }
                else -> m
            }
            DynamicAlert(this, m).setTitle("Aw crap").show()
            return false
        }
    }

    private fun loadUser(steamId: String) {
        try{
            var player = db.getPlayer(steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            this.player = player
            // Attempt auto login to create new token
            if (!db.loginPlayer(player))
                throw db.lastError ?: Throwable("unexpected")
            player = db.getPlayer(steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            this.player = player
            updateStats()
            topPersonaView.text = player.persona
            val avatar = RoundedBitmapDrawableFactory.create(resources, InternetHelper.BitmapRequest(player.avatarUrl))
            avatar.cornerRadius = 10f
            topAvatarView.setImageDrawable(avatar)
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Main.loadUser", e)
            var m = e.message ?: ""
            m = when (m) {
                "unexpected" -> {
                    "Unexpected error while logging in. Please report this."
                }
                else -> m
            }
            DynamicAlert(this, m).setTitle("Aw crap").show()
        }
    }

    fun canHasServer(): Boolean {
        val check = db.checkVersion()
        if (DEVMODE) Log.d("Main.canHasServer", "Server connection checked")
        when (check) {
            1 -> {
                // Up to date, nothing to do
            }
            0 -> {
                //TODO, updates available
            }
            -1 -> {
                return false
            }
        }
        return true
    }

    fun toggleLoader(visible: Boolean) {
        fun run(visible: Boolean) {
            if (visible && progressOverlay.visibility == View.GONE) {
                progressOverlay.visibility = View.VISIBLE
                val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
                progressOverlay.startAnimation(fadeIn)

            } else if (!visible && progressOverlay.visibility == View.VISIBLE) {
                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_fast)
                fadeOut.setAnimationListener( Animer {
                    progressOverlay.visibility = View.GONE
                })
                progressOverlay.startAnimation(fadeOut)
            }
        }
        // Sneaky always-show-regardless-of-who-called-and-where function B-)
        val isUiThread = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                Looper.getMainLooper().isCurrentThread
            else
                Thread.currentThread() === Looper.getMainLooper().thread
        if (isUiThread)
            run(visible)
        else
            Handler(Looper.getMainLooper()).post { run(visible) }
    }

}
