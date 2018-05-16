/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.app.FragmentManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.preference.PreferenceManager
import android.support.annotation.ArrayRes
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.transition.Fade
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*

import kotlinx.android.synthetic.main.include_progress_overlay.*
import kotlinx.android.synthetic.main.activity_main.*
import net.flare_esports.csgoskill.Constants.DEV_MODE
import org.json.JSONObject
import java.util.*


class Main : AppCompatActivity(), BaseFragment.FragmentListener {

    companion object {

        // Fragment selection options for switchFragment()
        @JvmStatic val LOC_LOGIN  = 0
        @JvmStatic val LOC_HOME   = 1
        @JvmStatic val LOC_SIGNUP = 2

        // Intent options from Intro
        @JvmStatic val STEAM_ID    = "steam_id"

        // Spinner time options
        @JvmStatic val TIME_TODAY = 0
        @JvmStatic val TIME_YESTERDAY = 1
        @JvmStatic val TIME_WEEK = 2
        @JvmStatic val TIME_WEEK2 = 3
        @JvmStatic val TIME_MONTH = 4
        @JvmStatic val TIME_MONTH_LAST = 5
        @JvmStatic val TIME_30DAYS = 6
        @JvmStatic val TIME_90DAYS = 7
        @JvmStatic val TIME_YEAR = 8

        /*
        <item>Today</item>
        <item>Yesterday</item>
        <item>1 Week</item>
        <item>2 Weeks</item>
        <item>This Month</item>
        <item>Last Month</item>
        <item>30 Days</item>
        <item>90 Days</item>
        <item>This Year</item>
         */

    }

    private lateinit var context: Context
    private lateinit var db: Database
    private lateinit var fManager: FragmentManager
    private lateinit var handler: Handler
    private lateinit var prefs: SharedPreferences
    private lateinit var player: Player

    @Suppress("PrivatePropertyName")
    private lateinit var LoginFrag: LoginFragment

    @Suppress("PrivatePropertyName")
    private lateinit var HomeFrag: HomeFragment

    private var loading = false
    private var shouldExit = false
    private var closing = false

    private val toLogin = Runnable { shouldExit = false; switchFragment(LOC_LOGIN) }
    private val hideVersionNumber = Runnable { hideVersionNumber() }

    var hasPlayer: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.allowEnterTransitionOverlap = true
        window.enterTransition = Fade().setDuration(700)
        // Prevents status bar being shown when spinner is selected
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        context = this
        fManager = fragmentManager
        prefs = PreferenceManager.getDefaultSharedPreferences(baseContext)

        db = Database(context, null)
        handler = Handler()

        LoginFrag = LoginFragment()
        HomeFrag = HomeFragment()

        topVersionNumber.text = Constants.getVersion()
        handler.postDelayed(hideVersionNumber, 5000)

        topMenuSpinner.adapter = TimeSpinner(R.array.time_options)
        topMenuSpinner.setSelection(TIME_WEEK2)

        // Reflection hack to set custom popup height
        try {
            val popup = Spinner::class.java.getDeclaredField("mPopup")
            popup.isAccessible = true

            // Get private mPopup member variable and try cast to ListPopupWindow
            val popupWindow = popup.get(topMenuSpinner) as android.widget.ListPopupWindow

            // Set popupWindow height to 210dp
            popupWindow.height = (210 * Resources.getSystem().displayMetrics.density).toInt()
        } catch (e: Throwable) {
            // silently fail...
        }

        topMenuSpinner.setSpinnerEventsListener(object: CustomSpinner.OnSpinnerEventsListener {
            override fun onSpinnerOpened() {
                handler.removeCallbacks(hideVersionNumber)
                showVersionNumber()
            }
            override fun onSpinnerClosed() { hideVersionNumber() }
        })

        val steamId = intent.getStringExtra(STEAM_ID) ?: ""
        if (steamId.isEmpty())
            switchFragment(LOC_LOGIN)
        else {
            val player = db.getPlayer(steamId)
            if (player == null) {
                switchFragment(LOC_LOGIN)
            } else {
                switchFragment(LOC_HOME)
                loginPlayer(player)
            }
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
            //super.onWindowFocusChanged(hasFocus)
        Log.d("Main", "Window has focus: $hasFocus")
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
        if (shouldExit && prefs.getBoolean("quickExit", true)) {
            this.shouldExit = false
            handler.removeCallbacksAndMessages(null)
            if (!prefs.getBoolean("quickExitNotice", false)) {
                DynamicAlert(this)
                        .setTitle("Notice!")
                        .setMessage("Double tapping BACK on the Home Screen will quick-exit CSGO Skill. We won't exit right now, since this is your first time. In the future, double tapping BACK will quick-exit CSGO Skill!")
                        .setDismissAction {
                            prefs.edit()
                                    .putBoolean("quickExit", true)
                                    .putBoolean("quickExitNotice", true)
                                    .apply()
                        }
                        .show()
            } else {
                closing = true
                finishAndRemoveTask()
            }
            return
        }
        val fragment = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
                ?: return finishAndRemoveTask() // Close app if no fragment is in the view, if that ever happens.
        if (fragment.onBack()) {return}
        else when (fragment.name) {
            "login" -> {
                Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show()
                this.shouldExit = true
                handler.postDelayed({ this.shouldExit = false }, 2500)
            }
            "home" -> {
                this.shouldExit = true
                handler.postDelayed({
                    if (closing || !this.shouldExit) return@postDelayed
                    this.shouldExit = false
                    DynamicAlert(this)
                            .setTitle("Logout?")
                            .setMessage("Pressing back on the Home screen logs you out. Are you sure you want to logout?")
                            .setPositive({ ->
                                handler.postDelayed(toLogin, 100)
                            })
                            .setNegative()
                            .show()
                }, 500)
            }
        }
    }

    override fun getPlayer(): Player {
        return player
    }

    override fun getHistoryStats(): JSONObject? {
        return if (hasPlayer) { db.getHistoryStats(player) } else { null }
    }

    override fun getGrandStats(): JSONObject? {
        return if (hasPlayer) { db.getGrandStats(player) } else { null }
    }

    override fun getCurrentStats(): JSONObject? {
        return if (hasPlayer) { db.getCurrentStats(player) } else { null }
    }

    override fun loginPlayer(player: Player): Boolean {
        try {
            val tPlayer = db.getPlayer(player.steamId)

            // Insert player if not in DB, failing if unable to insert
            if (tPlayer == null && !db.insertUser(player))
                throw db.lastError ?: Throwable("unexpected")

            // Attempt to login, showing error if present, but continuing regardless
            if (!db.loginPlayer(player)) {
                val m = when((db.lastError ?: Throwable("unexpected")).message) {
                    "no-internet" -> {
                        "No internet connection. Player information may be outdated."
                    }
                    "no-api" -> {
                        "Failed to connect to the CSGO Skill servers. Player information may be outdated."
                    }
                    else -> {
                        "Unexpected error while logging in. Player information may be outdated. Please report this."
                    }
                }
                DynamicAlert(this, m).setTitle("Aw crap").show()
            }

            // Collect the player, failing otherwise
            this.player = tPlayer ?: (db.getPlayer(player.steamId) ?: throw (db.lastError ?: Throwable("unexpected")))
            hasPlayer = true

            LoadUser().execute()
            return true
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Main.loginPlayer", e)
            var m = e.message ?: ""
            m = when (m) {
                "unexpected" -> {
                    "Unexpected error while logging in. Please report this."
                }
                else -> m
            }
            DynamicAlert(this, m).setTitle("Aw crap").setDismissAction {
                switchFragment(LOC_LOGIN) // Bring the user back to login
            }.show()
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
                topMenuSpinner.itemSelectedListener = { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                    if (DEV_MODE) Log.d("Main.topMenuSpinner.itemSelectedListener", "Item $position selected!")
                    // Small delay to disguise UI lag
                    handler.postDelayed({refreshStats()}, 200)
                }
            }
            LOC_LOGIN -> {
                if (previous == null || previous.name != "login") {
                    LoginFrag.enterTransition = fadeIn
                    fTransaction.replace(R.id.mainFragmentContainer, LoginFrag)
                    fTransaction.commit()
                }
                bottomNavigation.visibility = View.GONE
                topBar.visibility = View.GONE
                topMenuSpinner.itemSelectedListener = null
            }
            else -> {
                if (DEV_MODE)
                    Log.e("DEV", "Unknown fragment requested: $nextFragment")
            }
        }
    }

    override fun updatePlayer(): Boolean {
        try {
            toggleLoader(true)
            if (!db.loginPlayer(player)) throw db.lastError ?: Throwable("unexpected")
            this.player = db.getPlayer(player.steamId) ?: (throw db.lastError ?: Throwable("unexpected"))
            return true
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Main.updateStats", e)
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

    override fun updateStats() {
        try {
            UpdateStats().execute()
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Main.updateStats", e)
            val m = e.message ?: "Unexpected error while getting stats. Please report this."
            DynamicAlert(this, m).setTitle("Aw crap").show()
        }
    }

    private fun refreshStats() {
        try {
            val frag = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment?
            if (frag != null && frag.name == "home") {
                HomeFrag.displayStats(getCurrentTimeRange())
            }
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Main.refreshStats", e)
            val m = e.message ?: "Unexpected error while refreshing stats. Please report this."
            DynamicAlert(this, m).setTitle("Aw crap").show()
        }
    }

    private fun getCurrentTimeRange() : TimeRange {
        return when (topMenuSpinner.selectedItemPosition) {
            TIME_TODAY -> TimeRange(0)
            TIME_YESTERDAY -> TimeRange(1, 0)
            TIME_WEEK -> TimeRange(7)
            TIME_WEEK2 -> TimeRange(14)
            TIME_MONTH -> { // Start of this month to now
                // Start is the beginning of the month
                val start = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val end = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                start.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), 1, 0, 0, 0)

                TimeRange(start, end)
            }
            TIME_MONTH_LAST -> { // The previous month
                val start = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val end = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                // Start is the beginning of the previous month
                start.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH) - 1, 1, 0, 0, 0)
                // Sync end to start
                end.timeInMillis = start.timeInMillis

                // Snap to end of last day of month
                end.add(Calendar.MONTH, 1)
                end.add(Calendar.DAY_OF_MONTH, -1)
                end.set(end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH), 23, 59, 0)

                TimeRange(start, end)
            }
            TIME_30DAYS -> TimeRange(30)
            TIME_90DAYS -> TimeRange(90)
            TIME_YEAR -> {
                val start = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val end = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                // Start is beginning of the year
                start.set(start.get(Calendar.YEAR), 0, 1, 0, 0, 0)

                TimeRange(start, end)
            }
            else -> TimeRange() // Everything
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadUser : AsyncTask<Void?, Int, RoundedBitmapDrawable?>() {

        override fun onPostExecute(result: RoundedBitmapDrawable?) {
            // Do this after the avatar request, in case it failed
            updateStats()

            topPersonaView.text = player.persona
            topAvatarView.setImageDrawable(result)
            if (player.notify.isNotEmpty()) {
                val m = when (player.notify) {
                    "email-verify" -> {
                        "You still haven't verified your email address, please make sure you verify ${player.email} soon or you're account will return to a player account!"
                    }
                    "private-account" -> {
                        "Looks like your Steam account has been set to private! Unless you make it fully public again, your persona and avatar won't be updated, and stat tracking will not work!"
                    }
                    "no-game" -> {
                        "Looks like you no longer own CS: GO. How did that happen? Until you have it, your persona and avatar cannot be updated."
                    }
                    "playtime-private", "private-stats-or-down" -> {
                        "Looks like your Game Details are private! In order to use CSGO Skill, you must have set your Game Details to be Public. Simply login to Steam on your computer or mobile device, go to your Privacy Settings, and change \"Game Details\" to \"PUBLIC\", and then try logging into CSGO Skill again."
                    }
                    else -> {
                        "Something went wrong while logging you in! The app should still function normally for the most part, but your profile information and stats might not properly sync. Steam servers could just be offline for a moment. Wait for while and this error will probably go away."
                    }
                }
                DynamicAlert(this@Main, m).setTitle("Notice!").show()
            }
        }

        override fun doInBackground(vararg params: Void?): RoundedBitmapDrawable? {
            return try{
                val avatar = RoundedBitmapDrawableFactory.create(resources, InternetHelper.HardBitmapRequest(player.avatarUrl))
                avatar.cornerRadius = 10f
                avatar
            } catch (e: Throwable) {
                if (DEV_MODE) Log.e("Main.LoadUser", e)
                var m = e.message ?: ""
                m = when (m) {
                    "unexpected" -> {
                        "Unexpected error while logging in. Please report this."
                    }
                    else -> m
                }
                runOnUiThread {
                    DynamicAlert(this@Main, m).setTitle("Aw crap").show()
                }
                null
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class UpdateStats : AsyncTask<Void?, Int, String?>() {

        override fun onPostExecute(result: String?) {
            if (result != null)
                DynamicAlert(this@Main, result).setTitle("Aw crap").show()
        }

        override fun doInBackground(vararg params: Void?): String? {
            return try {
                db.updateStats(player)
                null
            } catch (e: Throwable) {
                if (DEV_MODE) Log.e("Main.UpdateStats", e)
                var m = e.message ?: ""
                m = when (m) {
                    "unexpected" -> {
                        "Unexpected error while logging in. Please report this."
                    }
                    else -> m
                }
                m
            }
        }

    }

    fun canHasServer(): Boolean {
        val check = db.checkVersion()
        if (DEV_MODE) Log.d("Main.canHasServer", "Server connection checked")
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
            if (visible && !loading) {
                loading = true
                progressOverlay.visibility = View.VISIBLE
                val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
                progressOverlay.startAnimation(fadeIn)

            } else if (!visible && loading) {
                loading = false
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

    private fun showVersionNumber() {
        if (topVersionNumber.visibility == View.VISIBLE)
            return
        topVersionNumber.clearAnimation()
        topVersionNumber.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_left))
        topVersionNumber.visibility = View.VISIBLE
    }

    private fun hideVersionNumber() {
        if (topVersionNumber.visibility == View.GONE)
            return
        topVersionNumber.clearAnimation()
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_out_right)
        anim.setAnimationListener( Animer {
            topVersionNumber.visibility = View.GONE
        })
        topVersionNumber.startAnimation(anim)
    }

    inner class TimeSpinner : BaseAdapter {

        private val items: Array<String>
        private val size: Int
        private val inflater: LayoutInflater

        private val dropdown: Int = R.layout.spinner_dropdown//android.R.layout.simple_spinner_dropdown_item
        private val listItem: Int = R.layout.spinner_view//android.R.layout.simple_spinner_item

        @Suppress("ConvertSecondaryConstructorToPrimary")
        constructor(@ArrayRes arrayRes: Int) {
            items = this@Main.resources.getStringArray(arrayRes)
            size = items.size
            inflater = this@Main.layoutInflater
        }

        override fun getCount(): Int = size

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return createView(position, convertView, parent, dropdown)
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return createView(position, convertView, parent, listItem)
        }

        private fun createView(position: Int, convertView: View?, parent: ViewGroup?, resource: Int): View {
            val view: View = convertView ?: inflater.inflate(resource, parent, false)
            val text = view as TextView

            val item: Any = getItem(position)
            if (item is String) {
                text.text = item
            } else {
                text.text = item.toString()
            }

            return view
        }

    }

}
