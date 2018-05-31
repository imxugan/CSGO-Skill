/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText

import kotlinx.android.synthetic.main.fragment_settings_account.*
import kotlinx.android.synthetic.main.dialog_change_email.*

/**
 * Account specific settings
 */
class SettingsAccountFragment : Settings.SettingsFragment() {

    override lateinit var settings: Settings
    override val name: String = "account"

    private lateinit var dialog: AlertDialog
    private lateinit var animShake: Animation

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        settings = context as Settings
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

        settingEmailText.text = settings.email
        settingPersonaText.text = settings.persona

        settingEmail.setOnClickListener {
            // TODO: Open dialog to change email address
            dialog = DynamicAlert(settings)
                    .setView(R.layout.dialog_change_email)
                    .setTitle("Change Email")
                    .setPositive("Update", Runnable {
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



                    }).create()
        }

        settingPersona.setOnClickListener {
            // TODO: Change settingPersonaText to EditText and focus, showing confirm dialog when unfocused, then saving on server
        }

    }

}
