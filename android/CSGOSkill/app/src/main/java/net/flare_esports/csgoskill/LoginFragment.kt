/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button

//import kotlinx.android.synthetic.main.fragment_login.*
import net.flare_esports.csgoskill.Constants.*
import org.json.JSONObject

// This fragment will handle the full login and sign up flow, only returning
// the login credentials as a Steam ID and Secret

class LoginFragment : BaseFragment() {

    internal lateinit var view: View
    internal lateinit var context: Context
    override var lMain: FragmentListener? = null

    private lateinit var loginLoginButton: Button
    private lateinit var loginWebView: WebView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        lMain = context as FragmentListener
    }

    override fun onDetach() {
        super.onDetach()
        lMain = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_login, container, false)

        loginLoginButton = view.findViewById(R.id.loginLoginButton)
        loginWebView = view.findViewById(R.id.loginWebView)

        loginLoginButton.setOnClickListener { startLogin() }

        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startLogin() {
        loginWebView.settings.javaScriptEnabled = true // Yes, I'm sure
        loginWebView.settings.builtInZoomControls = true
        loginWebView.settings.allowFileAccess = false
        loginWebView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(cM: ConsoleMessage): Boolean {
                val message = cM.message() // Message looks like 'FLARE-ESPORTS:message'

                if (!message.startsWith("FLARE-ESPORTS:", true))
                    return false

                try {
                    val response = JSONObject(message.substring(14))
                    if (response.getBoolean("success")) {
                        // There are two possibilities here:
                        // - The account already exists, and we are getting the steamId and secret.
                        // - The account has been reserved, and we are getting the steamId and verify code.

                        if (response.has("secret")) {
                            lMain?.onLogin(response.getString("steamid"), response.getString("secret"))
                            return true
                        } else if (response.has("verify")) {
                            //TODO, continue account creation by asking for Username and Email
                        } else {
                            //TODO, what happened?
                        }

                    } else {
                        throw Throwable(response.getString("error"))
                    }
                } catch (e: Throwable) {
                    if (devmode) Log.e("DEV", e.message)
                    return false
                }

                return false
            }
        }
        loginLoginButton.visibility = View.GONE
        loginWebView.visibility = View.VISIBLE
        loginWebView.loadUrl("http://www.csgo-skill.com/api/login?app")
    }

}
