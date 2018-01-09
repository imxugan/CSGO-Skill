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
import android.os.Looper
import android.os.Message
import android.support.constraint.ConstraintLayout
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*

import net.flare_esports.csgoskill.Constants.*
import net.flare_esports.csgoskill.InternetHelper.*
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

    private var goodPersona: Boolean = false
    private var goodEmail: Boolean = false

    private lateinit var animShake: Animation

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

        handler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    0 -> checkPersona(msg.obj as String)
                    1 -> checkEmail(msg.obj as String)
                    else -> return
                }
                loginRegisterButton.isEnabled = goodPersona && goodEmail
            }
        }

        loginUsernameInputLayout = view.findViewById(R.id.loginUsernameInputLayout)
        loginUsernameInput = view.findViewById(R.id.loginUsernameInput)
        loginUsernameInput.addTextChangedListener(Typer()
            .afterChanged { s: Editable? ->
                handler.removeMessages(0)
                handler.sendMessageDelayed(Message.obtain(handler, 0, s.toString()), 1000)
            }
        )

        loginEmailInputLayout = view.findViewById(R.id.loginEmailInputLayout)
        loginEmailInput = view.findViewById(R.id.loginEmailInput)
        loginEmailInput.addTextChangedListener(Typer()
            .afterChanged { s: Editable? ->
                handler.removeMessages(1)
                handler.sendMessageDelayed(Message.obtain(handler, 1, s.toString()), 1000)
            }
        )

        loginRegisterButton = view.findViewById(R.id.loginRegisterButton)

        loginLoginButton.setOnClickListener { startLogin() }
        loginRegisterButton.setOnClickListener { registerAccount() }

        animShake = AnimationUtils.loadAnimation(context, R.anim.shake)

        return view
    }

    override fun onBack(): Boolean {
        when (stage) {
            "web" -> {
                // Close webview and show login button
                loginWebView.visibility = View.GONE
                loginLoginButton.visibility = View.VISIBLE
                context.toggleFullscreen(false)
                stage = "login"
            }
            "register" -> {
                // Reset register view and restart login
                loginAvatarView.setImageResource(android.R.color.transparent)
                loginPersonaView.setText(R.string.loading)
                loginUsernameInput.text.clear()
                loginEmailInput.text.clear()
                loginUsernameInputLayout.isErrorEnabled = false
                loginUsernameInputLayout.error = ""
                loginUsernameInput.error = ""
                loginEmailInputLayout.isErrorEnabled = false
                loginEmailInputLayout.error = ""
                loginEmailInput.error = ""
                loginAvatarLoader.visibility = View.VISIBLE
                loginRegisterView.visibility = View.GONE
                verifyCode = ""
                steamId = ""
                startLogin()
            }
            else -> return false
        }
        return true
    }

    private fun checkPersona(persona: String): Boolean {
        goodPersona = false
        if (persona.length < 3 || persona.length > 30) {
            loginUsernameInputLayout.isErrorEnabled = true
            loginUsernameInputLayout.error = "Username must be between 3-30 characters"
            loginUsernameInput.error = "Username must be between 3-30 characters"
            return false
        }
        goodPersona = true
        loginUsernameInputLayout.isErrorEnabled = false
        loginUsernameInput.error = null
        return true
    }

    private fun checkEmail(email: String): Boolean {
        goodEmail = false
        if (email.length < 5 || email.length > 50) {
            loginEmailInputLayout.isErrorEnabled = true
            loginEmailInputLayout.error = "Email must be between 5-50 characters"
            loginEmailInput.error = "Email must be between 5-50 characters"
            return false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmailInputLayout.isErrorEnabled = true
            loginEmailInputLayout.error = "Email is not valid"
            loginEmailInput.error = "Email is not valid"
            return false
        }
        goodEmail = true
        loginEmailInputLayout.isErrorEnabled = false
        loginEmailInput.error = null
        return true
    }

    private fun checkInputs(): Boolean {
        val persona = loginUsernameInput.text.toString()
        val email = loginEmailInput.text.toString()

        loginUsernameInputLayout.isErrorEnabled = false
        loginUsernameInput.error = null
        loginEmailInputLayout.isErrorEnabled = false
        loginEmailInput.error = null

        val valid = checkPersona(persona)
        if (!valid) { loginUsernameInput.startAnimation(animShake) }
        if (!checkEmail(email)) { loginEmailInput.startAnimation(animShake); return false }
        return valid
    }

    private fun registerAccount() {
        loginRegisterButton.isEnabled = false
        context.toggleLoader(true)
        if (!checkInputs()) {
            context.toggleLoader(false)
            return
        }
        try {
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/addAccount")
                    .put("post", JSONObject()
                            .put("steamid", steamId)
                            .put("verify", verifyCode)
                            .put("name", loginUsernameInput.text.toString())
                            .put("email", loginEmailInput.text.toString()))
            request = HTTPJsonRequest(request)
            if (request.length() < 1 || request.has("message"))
                throw Throwable("Failed to communicate with server.\n" +
                        if (request.length() < 1) "No response"
                        else request.getString("message"))
            if (!request.getBoolean("success")) {
                val err = request.getString("error")
                when (err) {
                    "TRY AGAIN" -> {
                        // "Try and do that again, please"
                        // User may have to go back and redo the login part
                        throw Throwable("That didn't work. Try logging in again.")
                    }
                    "1312" -> {
                        // Failed to connect to MySQL DB
                        throw Throwable("The server failed. Try again.")
                    }
                    "1323" -> {
                        // Verify code doesn't match
                        throw Throwable("That didn't work. Try logging in again.")
                    }
                    "1325" -> {
                        // Bad inputs, recheck them
                        checkInputs()
                    }
                    "13242" -> {
                        // Username already being used
                        loginUsernameInputLayout.isErrorEnabled = true
                        loginUsernameInputLayout.error = "Username already exists"
                        loginUsernameInput.error = "Username already exists"
                        loginUsernameInput.startAnimation(animShake)
                    }
                    "13243" -> {
                        // Email already being used
                        loginEmailInputLayout.isErrorEnabled = true
                        loginEmailInputLayout.error = "Email already in use"
                        loginEmailInput.error = "Email already in use"
                        loginEmailInput.startAnimation(animShake)
                    }
                    "13223" -> {
                        // Account is no longer valid?
                        throw Throwable("That didn't work. Try logging in again.")
                    }
                    "13263" -> {
                        // After going through the whole register undeterred,
                        // the insert query failed :(
                        throw Throwable("That didn't work. Try logging in again.")
                    }
                }
            } else {
                if (request.has("error")) {
                    val err = request.getString("error")
                    when (err) {
                        "14262" -> {
                            // A rare case where the user previously attached
                            // their Steam account through the website, and
                            // have just created a full account, but the type
                            // change in the stats table failed, although we
                            // still have the secret key and the account is
                            // otherwise functioning correctly.
                        }
                        "14263" -> {
                            // Nearly identical to the above error, except the
                            // account has NO stats table row. This is clearly
                            // much more serious.
                        }
                    }
                    // In either case, we may call addAccount again, but with
                    // just the Steam ID, and it might repair the account.
                    var repair = JSONObject()
                            .put("url", "http://api.csgo-skill.com/addAccount?steamid=" + steamId)
                    repair = HTTPJsonRequest(repair)
                    if (repair.length() < 1 || repair.has("message"))
                        throw Throwable("Failed to communicate with server. Account has been registered, but there are some problems. Please report this at csgo-skill.com/support\n" +
                                if (repair.length() < 1) "No response"
                                else repair.getString("message"))
                    if (repair.getBoolean("success")) {
                        // Everything is OKAY now! Now back to your regularly scheduled program.
                    } else {
                        // Doh! We failed twice to setup the stats row. Big problems!
                        throw Throwable("Account has been registered, but there are some problems. Please report this at csgo-skill.com/support")
                    }
                }
                if (!request.has("secret")) {
                    // Another rare case where the account is being created,
                    // but was already created in the stats table as a user.
                    // How this happened probably isn't important, but if the
                    // user simply tries signing in again, it will find the
                    // account and properly login. So, we'll just do that.
                    onBack()
                    onBack()
                    Toast.makeText(context, R.string.login_successful_but_error, Toast.LENGTH_LONG).show()
                    // Calling onBack twice will reset the fragment and allow
                    // the user to login again.
                } else {
                    // YAY! Account has been completely and correctly registered.
                    lMain?.onLogin(steamId, request.getString("secret"))
                }
            }
        } catch (e: Throwable) {
            if (devmode) Log.e("LoginFragment.registerAccount", e)
            DynamicAlert(context, e.message).show()
        }
        context.toggleLoader(false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startLogin() {

        if (!isOnline()) {
            Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            // We won't return yet, just alert that there is (((likely))) no internet
        }

        if (!context.canHasServer()) {
            Toast.makeText(context, R.string.failed_server_connection, Toast.LENGTH_LONG).show()
            // We definitely shouldn't try anything if the version script failed
            return
        }

        loginWebView.settings.javaScriptEnabled = true // Yes, I'm sure
        loginWebView.webViewClient = object: WebViewClient() { // This forces the webview layout
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                context.toggleLoader(true)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                context.toggleLoader(false)
            }
        }
        loginWebView.webChromeClient = object: WebChromeClient() {
            override fun onConsoleMessage(cM: ConsoleMessage): Boolean {
                val message = cM.message() // Message looks like 'FLARE-ESPORTS:message'

                if (!message.startsWith("FLARE-ESPORTS:", true))
                    return false
                context.toggleLoader(true)
                try {
                    val response = JSONObject(message.substring(14))

                    if (!response.getBoolean("success")) {
                        throw Throwable(response.getString("error"))
                    }

                    // There are two possibilities here:
                    // - The account already exists, and we are getting the steamId and secret.
                    // - The account has been reserved, and we are getting the steamId and verify code.

                    if (response.has("secret")) {
                        lMain?.onLogin(response.getString("steamid"), response.getString("secret"))
                        context.toggleLoader(false)
                        return true
                    } else if (response.has("verify")) {
                        stage = "register"
                        context.toggleFullscreen(false)
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
                                if (devmode) Log.d("LoginFragment.onConsoleMessage", request.toString())
                                throw Throwable("When grabbing the name and avatar, profile failed to deliver")
                            }
                            avatar = BitmapRequest(request.getString("avatar"))
                            handler.post {
                                loginAvatarView.setImageBitmap(avatar)
                                loginAvatarLoader.visibility = View.GONE
                                loginPersonaView.text = request.getString("steamname")
                                loginUsernameInput.setText(request.getString("steamname"))
                            }
                        }
                        context.toggleLoader(false)
                        return true
                    }

                    onBack() // This brings us back to the login button
                    DynamicAlert(context, "Login failed, please try again.").show()
                    throw Throwable("Something happened with the console message")

                } catch (e: Throwable) {
                    if (devmode) Log.e("LoginFragment.startLogin", e)
                }
                context.toggleLoader(false)
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
