/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.FragmentManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.transition.Fade
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils

import kotlinx.android.synthetic.main.activity_main.*
import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.*


// This activity will simply manage the fragments, and pass information between
// them as required.

class MainActivity : AppCompatActivity(), BaseFragment.FragmentListener {

    private var steamId: String = ""
    private var secret: String = ""
    private var persona: String = ""
    private var email: String = ""
    private var status: String = ""
    private var accountUrl: String = ""
    private var avatarUrl: String = ""
    private var avatarImg: Bitmap? = null

    private var downloadingAvatar: Boolean = false

    private lateinit var context: Context
    private lateinit var db: Database
    private lateinit var fManager: FragmentManager

    private lateinit var LoginFrag: LoginFragment
    private lateinit var HomeFrag: HomeFragment

    // BEGIN OVERRIDES
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.allowEnterTransitionOverlap = true
        window.enterTransition = Fade()

        context = this
        fManager = fragmentManager

        db = Database(context, null)

        LoginFrag = LoginFragment()
        HomeFrag = HomeFragment()

        //loadUser(intent.getStringExtra(INAME_USER))
        Handler().postDelayed({switchFragment(intent.getStringExtra(INAME_OPEN))}, 500)
    }

    override fun onBackPressed() {
        val fragment = fManager.findFragmentById(R.id.mainFragmentContainer) as BaseFragment
        if (fragment.onBack()) {return}
        //TODO
    }

    // Selecting account known to device, and hopefully the server
    override fun onLogin(steamId: String) {
        val check = loadUser(steamId)
        if (check == USER_FOUND) {
            if (db.updateUser(steamId, secret)) {
                switchFragment(HOME_FRAG)
            } else {
                if (devmode) DynamicAlert(context, db.lastError()!!.message).show()
                else DynamicAlert(context, R.string.safe_error_content)
                        .setTitle(R.string.safe_error_title).show()
            }
        } else {
            //TODO: User does not exist on device, allow user to try different account or create one
            if (devmode) Log.e("DEV", "Something happened.")
        }
    }

    // Logging in with account known to server, should add to device
    override fun onLogin(steamId: String, secret: String) {
        val check = loadUser(steamId)
        if (check != BAD_USER) {
            if (db.updateUser(steamId, secret)) {
                switchFragment(HOME_FRAG)
            } else {
                if (devmode) DynamicAlert(context, db.lastError()!!.message).show()
                else DynamicAlert(context, R.string.safe_error_content)
                        .setTitle(R.string.safe_error_title).show()
            }
        } else {
            //TODO: What should happen here?
            if (devmode) Log.e("DEV", "Something happened.")
        }
    }

    // Account created on server, should add to device

    override fun switchFragment(nextFragment: String) {
        val fTransaction = fManager.beginTransaction()
        val previous = fManager.findFragmentById(R.id.mainFragmentContainer)
        if (previous != null) {
            previous.exitTransition = Fade().setDuration(300)
        }
        val fadeIn = Fade()
        fadeIn.duration = 200
        when (nextFragment) {
            HOME_FRAG -> {
                HomeFrag.enterTransition = fadeIn
                fTransaction.replace(R.id.mainFragmentContainer, HomeFrag)
                fTransaction.commit()
            }
            LOGIN_FRAG -> {
                LoginFrag.enterTransition = fadeIn
                fTransaction.replace(R.id.mainFragmentContainer, LoginFrag)
                fTransaction.commit()
            }
            else -> {
                if (devmode)
                    Log.e("DEV","Unknown fragment requested: " + nextFragment)
            }
        }
    }

    override fun updateUser(): Boolean {
        toggleLoader(true)
        if (db.updateUser(steamId, secret)) {
            return true
        }
        if (devmode) DynamicAlert(context, db.lastError()!!.message).show()
        else DynamicAlert(context, R.string.safe_error_content).setTitle(R.string.safe_error_title).show()
        return false
    }

    // END OVERRIDES

    private fun loadUser(steamId: String): Int {
        if (steamId.isEmpty())
            return BAD_USER

        val info = db.getUserInfo(steamId)
        if (info.length() == 0)
            return NO_USER

        try {
            this.steamId = steamId
            persona = info.getString("username")
            secret = info.getString("secret")
            email = info.getString("email")
            status = info.getString("status")
            avatarUrl = info.getString("avatar")
            accountUrl = info.getString("url")
            getAvatarImg(true)
            return USER_FOUND
        } catch (e: Throwable) {
            if (devmode) Log.e("DEV", e.message)
        }
        return BAD_USER
    }

    fun getAvatarImg(override: Boolean = false): Bitmap? {
        // Load image in a different thread, so we can get back to the user
        // quickly. Fragments using the image should check this a few times
        // over a couple seconds before giving up
        if ((override || avatarImg == null) && !downloadingAvatar)
            Thread().run { downloadingAvatar = true; avatarImg = BitmapFactory.decodeStream(RawRequest(avatarUrl)); downloadingAvatar = false }
        return avatarImg
        // BitmapFactory.decodeResource(resources, R.drawable.default_avatar_1)
    }

    fun getPersona(): String { return persona }
    fun getSecret(): String { return secret }
    fun getSteamId(): String { return steamId }
    fun getEmail(): String { return email }
    fun getStatus(): String { return status }
    fun getAccountUrl(): String { return accountUrl }
    fun getAvatarUrl(): String { return avatarUrl }

    fun toggleLoader(visible: Boolean) {
        if (visible && fragmentLoaderContainer.visibility == View.GONE) {
            fragmentLoaderContainer.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
            fragmentLoaderContainer.startAnimation(fadeIn)

        } else if (fragmentLoaderContainer.visibility == View.VISIBLE) {
            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_fast)
            fadeOut.setAnimationListener( Animer {
                fragmentLoaderContainer.visibility = View.GONE
            })
            fragmentLoaderContainer.startAnimation(fadeOut)
        }
    }

}
