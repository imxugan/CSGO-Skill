package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.app.Fragment
import android.app.FragmentManager
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.app.AppCompatActivity
import android.transition.Slide
import android.view.Gravity.LEFT
import android.view.Gravity.RIGHT
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE

import kotlinx.android.synthetic.main.activity_settings.*
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.Main.Companion.STEAM_ID

/**
 * A separate activity for changing app settings. The reason for using a separate activity instead
 * of a fragment is because it will be easier to change the settings across the app instantly, since
 * going back to [Main] will reload all the fragments with the new settings already.
 */
class Settings : AppCompatActivity() {

    companion object {

        /* switchFragment() options */

        /** Base settings screen constant for [switchFragment] */
        @JvmStatic val LOC_BASE = 0

        /** Account settings screen constant for [switchFragment] */
        @JvmStatic val LOC_ACCOUNT = 1

    }

    private lateinit var db: Database
    private lateinit var fManager: FragmentManager

    private lateinit var BaseSettings: SettingsBaseFragment
    private lateinit var AccountSettings: SettingsAccountFragment

    var persona = ""
    var username = ""
    var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        fManager = fragmentManager
        db = Database(this, null)

        BaseSettings = SettingsBaseFragment()
        AccountSettings = SettingsAccountFragment()

        val steamId = intent.getStringExtra(STEAM_ID) ?: ""
        if (steamId.isEmpty()) {
            Log.e("Settings.onCreate", "Um, no Steam ID was passed.")
        } else {
            val player = db.getPlayer(steamId)
            if (player == null) {
                Log.e("Settings.onCreate", "Database found no player for steam id $steamId")
            } else {
                LoadAvatar().execute(player.avatarUrl)
                userPersona.text = player.persona
                persona = player.persona
                if (player.isUser) {
                    userLevel.text = player.username
                    username = player.username
                    email = player.email
                } else {
                    userLevel.setTextColor(ContextCompat.getColorStateList(this, R.color.primary))
                    userLevel.text = "Tap here to become a user"
                }
            }
        }

        // Set bottom padding height
        val params = settingsBottomPadding.layoutParams
        params.height = Constants.getNavBarHeight(this)
        settingsBottomPadding.layoutParams = params

        if (username.isNotEmpty()) {
            // User account is logged in, direct to change account settings
            currentUser.setOnClickListener {
                switchFragment(LOC_ACCOUNT)
            }
        } else {
            // Basic player account, direct to sign up
            currentUser.setOnClickListener {
                DynamicAlert(this)
                        .setHTML("Right now, you are getting 30 days of tracked stat changes and limited total stat tracking. By signing up with an <b>email address</b> and <b>username</b>, you unlock 90 days of tracking and full total stat tracking, as well as monthly stat history, <b>forever</b>!<br><br>If that sounds awesome to you, click <b>SIGN UP</b>.<br><br>You'll have the chance to read the terms of use and privacy policy before signing up, as well as more information about the differences between your current account and a user account.")
                        .setTitle("To Victory!")
                        .setPositive("Sign Up", Runnable {
                            setResult(Main.RESULT_SIGNUP)
                            finish()
                        })
                        .setNegative(android.R.string.cancel)
                        .setCancelable(true)
                        .show()
            }
        }

        switchFragment(LOC_BASE)

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
            super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Intentionally allow the status and nav bars to be visible here
            window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onBackPressed() {
        val fragment = fManager.findFragmentById(R.id.settingsFragmentContainer) as SettingsFragment?
                ?: return finishAndRemoveTask() // Close settings if no fragment is in the view
        if (fragment.onBack()) {return}
        else when (fragment.name) {
            "base" -> {
                finishAndRemoveTask()
            }
            "account" -> {
                switchFragment(LOC_BASE)
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    fun switchFragment(nextFragment: Int) {
        val previous = fManager.findFragmentById(R.id.settingsFragmentContainer) as SettingsFragment?

        when (nextFragment) {
            LOC_BASE -> {
                if (previous == null) {
                    val transaction = fManager.beginTransaction()
                    transaction.add(R.id.settingsFragmentContainer, BaseSettings)
                    transaction.commit()
                } else if (previous.name == "account") {
                    previous.exitTransition = Slide(RIGHT).setDuration(300)
                    BaseSettings.enterTransition = Slide(LEFT).setDuration(300).setStartDelay(150)
                    val transaction = fManager.beginTransaction()
                    transaction.replace(R.id.settingsFragmentContainer, BaseSettings)
                    transaction.commit()
                }
            }
            LOC_ACCOUNT -> {
                if (previous == null) {
                    val transaction = fManager.beginTransaction()
                    transaction.add(R.id.settingsFragmentContainer, AccountSettings)
                    transaction.commit()
                } else if (previous.name == "base") {
                    previous.exitTransition = Slide(LEFT)
                    AccountSettings.enterTransition = Slide(RIGHT).setDuration(300).setStartDelay(150)
                    val transaction = fManager.beginTransaction()
                    transaction.replace(R.id.settingsFragmentContainer, AccountSettings)
                    transaction.commit()
                }
            }
            else -> {
                if (DEV_MODE)
                    Log.e("Settings.switchFragment", "Unknown fragment requested: $nextFragment")
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class LoadAvatar : AsyncTask<String?, Int, RoundedBitmapDrawable?>() {

        override fun onPostExecute(result: RoundedBitmapDrawable?) {
            userAvatar.setImageDrawable(result)
        }

        override fun doInBackground(vararg params: String?): RoundedBitmapDrawable? {
            return try {
                val avatar = RoundedBitmapDrawableFactory.create(resources, InternetHelper.HardBitmapRequest(params[0]))
                avatar.cornerRadius = 10f
                avatar
            } catch (e: Throwable) {
                if (DEV_MODE) Log.e("Settings.LoadAvatar", e)
                null
            }
        }

    }

    /**
     * The abstract class that all setting fragments inherit from.
     */
    abstract class SettingsFragment : Fragment() {

        /** Gives every fragment a reference to its [Settings] creator */
        abstract val settings: Settings

        /** Names the fragment so that [Settings] can better keep track of them */
        abstract val name: String

        /**
         * <p>Called when user presses the back button, should return False when
         * this activity should handle the event itself.</p>
         *
         * @return True if fragment handled the event, false if Settings should
         */
        abstract fun onBack(): Boolean

    }

}
