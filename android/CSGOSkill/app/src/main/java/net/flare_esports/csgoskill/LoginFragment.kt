/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

import kotlinx.android.synthetic.main.fragment_login.*
import net.flare_esports.csgoskill.Constants.*
import net.flare_esports.csgoskill.InternetHelper.*
import org.json.JSONObject

class LoginFragment : BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "login"

    private var stage: String = "login"
    private lateinit var prefs: SharedPreferences

    override fun onAttach(context: Context) {
        super.onAttach(context)
        main = context as Main
        prefs = PreferenceManager.getDefaultSharedPreferences(main.baseContext)
        listener = context
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // Set onClick listeners for buttons
        loginButton.setOnClickListener { loginButton.isEnabled = false; login() }
    }

    override fun onBack(): Boolean {
        when (stage) {
            "login" -> {
                return false // Unnecessary, but this reads easier
            }
            "web" -> {
                // Close webview and show login button
                loginWebView.visibility = View.GONE
                loginButtonView.visibility = View.VISIBLE
                loginButton.isEnabled = true
                stage = "login"
            }
            else -> return false
        }
        return true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun login() {

        if (!isOnline()) {
            Toast.makeText(main, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            // We won't return yet, just alert that there is (((likely))) no internet
        }

        if (!main.canHasServer()) {
            Toast.makeText(main, R.string.failed_server_connection, Toast.LENGTH_LONG).show()
            // We definitely shouldn't try anything if the version script failed
            return
        }

        loginWebView.settings.javaScriptEnabled = true  // Yes, I'm sure
        loginWebView.settings.setAppCacheEnabled(false) // Saves storage space
        loginWebView.webViewClient = object: WebViewClient() { // This forces the webview layout
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                main.toggleLoader(true)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                main.toggleLoader(false)
            }
        }
        loginWebView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(cM: ConsoleMessage): Boolean {
                val message = cM.message() // Message looks like 'SKILL-BOT:message'

                if (!message.startsWith("SKILL-BOT:", true))
                    return false
                loginWebView.visibility = View.GONE
                main.toggleLoader(true)
                try {
                    val response = JSONObject(message.substring(10))

                    if (!response.optBoolean("success")) {
                        throw Throwable(response.optString("reason"))
                    }

                    // We got a profile, create the Player and let Main Activity login
                    if (listener?.loginPlayer(Player(response.getJSONObject("profile"))) == true)
                        main.switchFragment(Main.LOC_HOME)

                } catch (e: Throwable) {
                    onBack() // This brings us back to the login button
                    if (DEVMODE) Log.e("LoginFragment.login", e)
                    var m = e.message ?: ""
                    m = if (m.startsWith("error code")) {
                        "Login failed with error code " + m.substring(11)
                    } else {
                        when(m) {
                            "auth-canceled" -> {
                                "You successfully canceled the login"
                            }
                            "validate-failed" -> {
                                "Unable to validate your Steam Account, try logging in again"
                            }
                            "private-account" -> {
                                "Your Steam Account must be public to login"
                            }
                            "no-game" -> {
                                "You must own CS: GO to login"
                            }
                            "playtime" -> {
                                "Sorry, but you must have played CS: GO for at least 10 hours before logging in. This requirement exists to reduce the use of alternate accounts or players who are unlikely to continue using CSGO Skill. Please play the game for at least 10 hours before trying again. No exceptions to this rule will be made. If you really want to improve in CS: GO, playing for 10 hours should not be hard!"
                            }
                            "playtime-private", "private-stats-or-down" -> {
                                "Looks like your Game Details are private! In order to use CSGO Skill, you must have set your Game Details to be Public. Simply login to Steam on your computer or mobile device, go to your Privacy Settings, and change \"Game Details\" to \"PUBLIC\", and then try logging into CSGO Skill again."
                            }
                            else -> {
                                "There was an unexpected error. Please report this."
                            }
                        }
                    }
                    DynamicAlert(main, m).setTitle("Aw crap").show()
                }
                main.toggleLoader(false)
                return false
            }
        }
        stage = "web"
        loginButtonView.visibility = View.GONE
        loginWebView.visibility = View.VISIBLE
        loginWebView.loadUrl("http://api.csgo-skill.com/login")
    }
}
