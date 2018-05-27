/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.webkit.WebSettings.LOAD_NO_CACHE
import android.widget.*

import kotlinx.android.synthetic.main.fragment_login.*
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.InternetHelper.*
import org.json.JSONObject

/**
 * Handles user login with a custom WebView
 */
class LoginFragment : BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "login"

    private var stage: String = "login"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        main = context as Main
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

        if (!main.checkApi()) {
            Toast.makeText(main, R.string.failed_server_connection, Toast.LENGTH_LONG).show()
            // We definitely shouldn't try anything if the version script failed
            return
        }

        loginWebView.settings.javaScriptEnabled = true  // Yes, I'm sure
        loginWebView.settings.setAppCacheEnabled(false) // Saves storage space
        loginWebView.settings.cacheMode = LOAD_NO_CACHE // Force no-cache

        loginWebView.webViewClient = object: WebViewClient() { // This forces the webview layout
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                main.toggleLoader(true)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                main.toggleLoader(false)
                // This fixes the UI glitch where the Two-factor Authentication box is displayed
                // too far down the page, hiding the input box behind the keyboard.
                // Here, we detect when it is created and then change it's location
                loginWebView?.evaluateJavascript("""
                    // Disable the stupid form autocompletion bullshit
                    document.getElementById("steamAccountName").setAttribute("autocomplete", "off")

                    var mu = function(mutationsList) {
                        console.log("something happened")

                        for (var mutation of mutationsList) {
                            if (mutation.type == "childList") {

                                var list = mutation.addedNodes

                                // Make sure the proper node was created
                                for (var item of list) {

                                    // Find the background div
                                    if (item.classList.contains("newmodal_background")) {
                                        console.log("found a newmodal_background element")
                                        // Get the previous sibling with proper class, and change it's style
                                        var modal = item.previousElementSibling
                                        if (modal) {
                                            console.log("got the previous sibling!")
                                            // Set up Steam style rules for the modal
                                            var w = window.outerWidth
                                            var h = window.outerHeight
                                            var l = (w - modal.offsetWidth) / 2
                                            var t = 10 // Set the top higher on the page

                                            var style = "position:fixed;z-index:1000;"
                                            style += "max-width:" + (w - 20) + "px;"
                                            style += "left:" + l + "px;"
                                            style += "top:" + t + "px;"

                                            modal.setAttribute("style", style)

                                            // Scroll back to top of page
                                            window.scrollTo(0,0)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    var target = document.body

                    var config = { childList: true }

                    var observer = new MutationObserver(mu)
                    observer.observe(target, config)
                """.trimIndent(), {_ ->
                    if (DEV_MODE) Log.d("LoginFragment.WV.evaluateJavascript", "Updated the page with JS!")
                })
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
                    if (DEV_MODE) Log.e("LoginFragment.login", e)
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
        loginWebView.loadUrl("https://api.csgo-skill.com/login")
    }
}
