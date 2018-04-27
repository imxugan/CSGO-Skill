/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import kotlinx.android.synthetic.main.fragment_signup.*
import net.flare_esports.csgoskill.Constants.DEVMODE
import net.flare_esports.csgoskill.InternetHelper.*
import org.json.JSONObject

class SignupFragment: BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "signup"

    private lateinit var handler: Handler
    private var player: Player? = null
    private lateinit var animShake: Animation

    override fun onAttach(context: Context) {
        super.onAttach(context)
        main = context as Main
        listener = context
        player = listener?.getPlayer()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        animShake = AnimationUtils.loadAnimation(main, R.anim.shake)

        // Validation as they type looper
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                    0 -> checkPersona()
                    1 -> checkUsername()
                    2 -> checkEmail()
                    else -> return
                }
            }
        }

        // Add listeners to the fields
        personaInput.addTextChangedListener(Typer()
                .afterChanged {
                    handler.removeMessages(0)
                    handler.sendEmptyMessageDelayed(0, 1000)
                }
        )

        usernameInput.addTextChangedListener(Typer()
                .afterChanged {
                    handler.removeMessages(1)
                    // Send immediately so live filter works
                    handler.sendEmptyMessage(1)
                }
        )

        emailInput.addTextChangedListener(Typer()
                .afterChanged {
                    handler.removeMessages(2)
                    handler.sendEmptyMessageDelayed(2, 1000)
                }
        )

        // Set onClick listener for button
        signupButton.setOnClickListener { signupButton.isEnabled = false; signup() }

        // Populate persona and avatar
        avatarView.setImageBitmap(player?.avatarImg)
        avatarLoader.visibility = View.GONE
        personaView.text = player?.persona ?: "Unknown"
    }

    override fun onBack(): Boolean {
        // TODO: Detect if fields have input, and verify that user wants to lose progress
        return false
    }

    private fun checkPersona(): Boolean {
        val m = Player.validPersona(personaInput.text.toString())
        if (m != "ok") {
            personaInputLayout.isErrorEnabled = true
            personaInputLayout.error = m
            personaInput.error = m
        } else {
            personaInputLayout.isErrorEnabled = false
            personaInput.error = null
        }
        return personaInputLayout.isErrorEnabled
    }

    private fun checkUsername(): Boolean {
        val newName = Player.filterUsername(usernameInput.text.toString())
        usernameInput.setText(newName)
        return newName.length in 3..35
    }

    private fun checkEmail(): Boolean {
        val m = Player.validEmail(emailInput.text.toString())
        if (m != "ok") {
            emailInputLayout.isErrorEnabled = true
            emailInputLayout.error = m
            emailInput.error = m
        } else {
            emailInputLayout.isErrorEnabled = false
            emailInput.error = null
        }
        return emailInputLayout.isErrorEnabled
    }

    private fun goodInputs(): Boolean {
        // Run all checks
        val p = checkPersona()
        val u = checkUsername()
        val e = checkEmail()

        // Ensure only one box shakes at a time
        if (!p) {
            personaInput.startAnimation(animShake)
        } else if (!u) {
            usernameInput.startAnimation(animShake)
        } else if (!e) {
            emailInput.startAnimation(animShake)
        }

        return p && u && e
    }

    private fun signup() {
        signupButton.isEnabled = false
        main.toggleLoader(true)
        if (!goodInputs()) {
            main.toggleLoader(false)
            return
        }
        try {
            var player1 = player ?: throw Throwable("not-found")
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/signup")
                    .put("post", JSONObject()
                            .put("steamid", player1.steamId)
                            .put("persona", personaInput.toString())
                            .put("username", usernameInput.text.toString())
                            .put("email", emailInput.text.toString()))
                    .put("token", player1.token)

            request = HTTPJsonRequest(request)
            if (request == null)
                throw Throwable("null")
            if (!request.optBoolean("success")) {
                throw Throwable(request.optString("reason"))
            } else {
                // Success! Notify, then re-login with new info
                player1 = Player(request.getJSONObject("profile"))
                if (listener?.loginPlayer(player1) == true)
                    DynamicAlert(main)
                            .setTitle("Congratulations!")
                            .setMessage("${player1.persona}, you are now a USER!\n\nPlease check your email (${player1.email}) for a verification link. You need to verify your email within 2 days, otherwise you will lose your USER status and everything you just did will be undone.\n\nIf that email address is incorrect, you can change it and resend the verification link by going to the Settings.")
                            .setDismissAction {
                                main.switchFragment(Main.LOC_HOME)
                            }
                            .show()
            }


        } catch (e: Throwable) {
            if (DEVMODE) Log.e("SignupFragment.signup", e)
            var m = e.message ?: ""
            m = if (m.startsWith("error code")) {
                "Sign up failed with error code " + m.substring(11)
            } else {
                when (m) {
                    "no-response" -> {
                        "Failed to communicate with server.\nThe response was empty."
                    }
                    "timed-out" -> {
                        "Failed to communicate with the server.\nThe request timed out."
                    }
                    "bad-steamid" -> {
                        "Unable to signup with this account. Please go back and login again (press Back)"
                    }
                    "login", "bad-token" -> {
                        "The login token is invalid or has expired. Please go back and login again (press Back)"
                    }
                    "bad-email" -> {
                        "The email you submitted was denied. What? How'd you do that?"
                    }
                    "bad-username" -> {
                        "The username you submitted was denied. What? How'd you do that?"
                    }
                    "bad-persona" -> {
                        "The persona you submitted was denied. What? How'd you do that?"
                    }
                    "already-exists" -> {
                        "This account is already signed up. Please go back and login again (press Back)\nWait, what?"
                    }
                    "name-in-use" -> {
                        "The username you picked already exists, try another."
                    }
                    "email-in-use" -> {
                        "The email you picked already exists, maybe you have already created an account with it?"
                    }
                    "name-in-use,email-in-use" -> {
                        "Both the username and email you picked already exists. Please contact us if you need to change your linked Steam account."
                    }
                    "not-found" -> {
                        "The account you are trying to sign up with wasn't found, you need to login with it first. Please go back and login again (press Back)\nWait, what?"
                    }
                    "null" -> {
                        "Failed to communicate with server.\nNULL request."
                    }
                    else -> {
                        "Unexpected error. Please report this."
                    }
                }
            }
            DynamicAlert(main, m).setTitle("Aw crap").show()
        }
        main.toggleLoader(false)
    }

}
