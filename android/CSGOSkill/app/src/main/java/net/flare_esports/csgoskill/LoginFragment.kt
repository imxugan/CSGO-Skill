/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TextInputLayout
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

//import kotlinx.android.synthetic.main.fragment_login.*
import net.flare_esports.csgoskill.Constants.*
import net.flare_esports.csgoskill.InternetHelper.RawRequest
import org.json.JSONObject

// This fragment will handle the full login and sign up flow, only returning
// the login credentials as a Steam ID and Secret

class LoginFragment : BaseFragment() {

    internal lateinit var view: View
    internal lateinit var context: MainActivity
    private lateinit var handler: Handler
    override var lMain: FragmentListener? = null
    override val name: String = "login"

    private lateinit var loginAvatarLoader: ProgressBar
    private lateinit var loginAvatarView: ImageView
    private lateinit var loginLoginButton: Button
    private lateinit var loginPersonaView: TextView
    private lateinit var loginRegisterView: ConstraintLayout
    private lateinit var loginWebView: WebView

    private lateinit var loginUsernameInputLayout: TextInputLayout
    private lateinit var loginUsernameInput: EditText

    private lateinit var loginEmailInputLayout: TextInputLayout
    private lateinit var loginEmailInput: EditText

    private lateinit var loginRegisterButton: Button

    private var verifyCode: String = ""
    private var steamId: String = ""
    private var avatar: Bitmap? = null
    private var stage: String = "login"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context as MainActivity
        lMain = context
    }

    override fun onDetach() {
        super.onDetach()
        lMain = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_login, container, false)

        loginAvatarLoader = view.findViewById(R.id.loginAvatarLoader)
        loginAvatarView = view.findViewById(R.id.loginAvatarView)
        loginLoginButton = view.findViewById(R.id.loginLoginButton)
        loginPersonaView = view.findViewById(R.id.loginPersonaView)
        loginRegisterView = view.findViewById(R.id.loginRegisterView)
        loginWebView = view.findViewById(R.id.loginWebView)

        loginUsernameInputLayout = view.findViewById(R.id.loginUsernameInputLayout)
        loginUsernameInput = view.findViewById(R.id.loginUsernameInput)

        loginEmailInputLayout = view.findViewById(R.id.loginEmailInputLayout)
        loginEmailInput = view.findViewById(R.id.loginEmailInput)

        loginRegisterButton = view.findViewById(R.id.loginRegisterButton)

        handler = Handler()

        loginLoginButton.setOnClickListener { startLogin() }
        loginRegisterButton.setOnClickListener { registerAccount() }

        return view
    }

    override fun onBack(): Boolean {
        when (stage) {
            "web" -> {
                //TODO
            }
            "register" -> {
                //TODO
            }
            else -> return false
        }
        return true
    }

    private fun registerAccount() {

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startLogin() {

        loginWebView.settings.javaScriptEnabled = true // Yes, I'm sure
        loginWebView.webViewClient = WebViewClient() // This forces the webview layout
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
                            stage = "register"
                            verifyCode = response.getString("verify")
                            steamId = response.getString("steamid")
                            val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_medium)
                            fadeIn.setAnimationListener(Animer {
                                loginRegisterView.visibility = View.VISIBLE
                                loginWebView.visibility = View.GONE
                            })
                            val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_medium)
                            loginRegisterView.startAnimation(fadeIn)
                            loginWebView.startAnimation(fadeOut)
                            Thread().run {
                                var request = JSONObject()
                                        .put("url",
                                                "http://api.csgo-skill.com/profile?id=" + steamId)
                                request = InternetHelper.HTTPJsonRequest(request)
                                if (request.length() < 1 || request.has("message")) {
                                    //TODO, this shouldn't have happened
                                    throw Throwable("When grabbing the name and avatar, profile failed to deliver")
                                }
                                avatar = BitmapFactory.decodeStream(RawRequest(request.getString("avatar")))
                                handler.post {
                                    loginAvatarView.setImageBitmap(avatar)
                                    loginAvatarLoader.visibility = View.GONE
                                    loginPersonaView.text = request.getString("steamname")
                                    loginUsernameInput.setText(request.getString("steamname"))
                                }
                            }
                        } else {
                            //TODO, what happened?
                            throw Throwable("Something happened with the console message")
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
        stage = "web"
        context.toggleFullscreen(true)
        loginLoginButton.visibility = View.GONE
        loginWebView.visibility = View.VISIBLE
        loginWebView.loadUrl("http://api.csgo-skill.com/login?app")
    }

}
