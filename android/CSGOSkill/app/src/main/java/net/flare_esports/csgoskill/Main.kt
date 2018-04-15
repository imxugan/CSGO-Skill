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

    private var baseUiVisibility: Int = 0
    private var shouldExit = false

    private val toLogin = Runnable { shouldExit = false; switchFragment(LOC_LOGIN) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.allowEnterTransitionOverlap = true
        window.enterTransition = Fade()
        baseUiVisibility = window.decorView.systemUiVisibility

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

    override fun onBackPressed() {
        if (shouldExit) {
            handler.removeCallbacks(toLogin)
            finishAndRemoveTask()
        }
        val fragment = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
                ?: return finish() // Close app if no fragment is in the view, if that ever happens.
        if (fragment.onBack()) {return}
        else when (fragment.name) {
            "login" -> {
                Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show()
                shouldExit = true
            }
            "home" -> {
                shouldExit = true
                handler.postDelayed(toLogin, 500)
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
            if (!db.loginPlayer(player))
                throw db.lastError ?: Throwable("unexpected")
            this.player = db.getPlayer(player.steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            return updateStats()
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
                    bottomNavigation.visibility = View.VISIBLE
                }
            }
            LOC_LOGIN -> {
                if (previous == null || previous.name != "login") {
                    LoginFrag.enterTransition = fadeIn
                    fTransaction.replace(R.id.mainFragmentContainer, LoginFrag)
                    fTransaction.commit()
                    bottomNavigation.visibility = View.GONE
                }
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
            val player = db.getPlayer(steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            this.player = player
            // Attempt auto login to create new token
            if (!db.loginPlayer(player))
                throw db.lastError ?: Throwable("unexpected")
            this.player = db.getPlayer(steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            updateStats()
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

    fun toggleFullscreen(fullscreen: Boolean) {
        var newVis = baseUiVisibility
        val decor = window.decorView
        if (fullscreen)
            newVis = newVis or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LOW_PROFILE
        val changed = newVis != decor.systemUiVisibility
        if (changed)
            decor.systemUiVisibility = newVis
    }

}
