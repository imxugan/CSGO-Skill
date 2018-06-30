/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast

import kotlinx.android.synthetic.main.fragment_settings_account.*
import kotlinx.android.synthetic.main.dialog_change_email.*
import org.json.JSONObject

/**
 * Account specific settings
 */
class SettingsAccountFragment : Settings.SettingsFragment() {

    override lateinit var settings: Settings
    override val name: String = "account"

    private lateinit var animShake: Animation
    private lateinit var newEmail: String
    private lateinit var player: Player

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        settings = context as Settings
        player = settings.player
        animShake = AnimationUtils.loadAnimation(context, R.anim.shake)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings_account, container, false)
    }

    override fun onBack(): Boolean {
        // TODO
        return false
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        if (settings.newEmail.isNotEmpty()) {
            settingEmailText.text = Html.fromHtml("<b>New</b> ${Html.escapeHtml(newEmail)} [unverified]<br><b>Old</b> ${Html.escapeHtml(player.email)}")
        } else {
            settingEmailText.text = player.email
        }

        settingPersonaText.text = player.persona

        newEmail = player.email

        settingEmail.setOnClickListener {
            // TODO: Open dialog to change email address
            DynamicAlert(settings)
                    .setView(R.layout.dialog_change_email)
                    .setTitle("Change Email")
                    .setPositive(Runnable {
                        val m = Player.validEmail(emailInput.text.toString())
                        if (m != "ok") {
                            emailInputLayout.isErrorEnabled = true
                            emailInputLayout.error = m
                            emailInput.error = m
                        } else {
                            emailInputLayout.isErrorEnabled = false
                            emailInput.error = null
                        }

                        if (emailInputLayout.isErrorEnabled) {
                            emailInput.startAnimation(animShake)
                            return@Runnable
                        }

                        newEmail = emailInput.text.toString()

                        if (newEmail != player.email) {

                            // POST to server
                            InternetHelper.HTTPJsonRequest(JSONObject()
                                    .put("url", "https://api.csgo-skill.com/user/" + settings.username)
                                    .put("post", JSONObject()
                                            .put("steamid", settings.)))

                            settings.newEmail = newEmail

                            @Suppress("DEPRECATION") // Just lazy, don't want to wrap this in a build check
                            settingEmailText.text = Html.fromHtml("<b>New</b> ${Html.escapeHtml(newEmail)} [unverified]<br><b>Old</b> ${Html.escapeHtml(settings.email)}")
                        }
                    })
                    .setNegative(android.R.string.cancel)
                    .setCancelable(true)
                    .show()
        }

        settingPersona.setOnClickListener {
            // TODO: Change settingPersonaText to EditText and focus, showing confirm dialog when unfocused, then saving on server

        }

    }

}
