/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import kotlinx.android.synthetic.main.fragment_settings_base.*
import net.flare_esports.csgoskill.Preferences.AUTO_LOGIN
import net.flare_esports.csgoskill.Preferences.QUICK_EXIT

/**
 * Settings for basic, app-wide stuff.
 */
class SettingsBaseFragment : Settings.SettingsFragment() {

    override lateinit var settings: Settings
    override val name: String = "base"

    private var handler: Handler? = null
    private var optionEnable = true

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        settings = context as Settings
        handler = Handler()
    }

    override fun onDetach() {
        super.onDetach()
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings_base, container, false)
    }

    override fun onBack(): Boolean {
        // TODO
        return false
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        settingAutoLoginSwitch.isChecked = Preferences.getBoolean(AUTO_LOGIN, true)
        settingQuickExitSwitch.isChecked = Preferences.getBoolean(QUICK_EXIT, true)

        settingLanguage.setOnClickListener {
            DynamicAlert(settings)
                    .setHTML("English is the only available language for CSGO Skill, for now!<br><br>If you are bilingual, then please consider helping to translate this app by clicking <b>TRANSLATE</b> below! Otherwise, just click <b>CANCEL</b> to dismiss this dialog.")
                    .setTitle("Notice!")
                    .setPositive("Translate", Runnable {
                        val viewIntent = Intent(ACTION_VIEW, Uri.parse("https://csgo-skill.com/repo"))
                        startActivity(viewIntent)
                    })
                    .setNegative(android.R.string.cancel)
                    .setCancelable(true) // Should be cancelable if there is literally a CANCEL button
                    .show()
        }

        settingAutoLogin.setOnClickListener {
            if (optionEnable) {
                optionEnable = false
                handler?.postDelayed({ optionEnable = true }, 250)

                if (Preferences.getBoolean(AUTO_LOGIN, true)) {
                    settingAutoLoginSwitch.isChecked = false
                    Preferences.setBoolean(AUTO_LOGIN, false)
                    Toast.makeText(settings, "Auto Login disabled", Toast.LENGTH_SHORT).show()
                } else {
                    settingAutoLoginSwitch.isChecked = true
                    Preferences.setBoolean(AUTO_LOGIN, true)
                    Toast.makeText(settings, "Auto Login enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        settingQuickExit.setOnClickListener {
            if (optionEnable) {
                optionEnable = false
                handler?.postDelayed({ optionEnable = true }, 250)

                if (Preferences.getBoolean(QUICK_EXIT, true)) {
                    settingQuickExitSwitch.isChecked = false
                    Preferences.setBoolean(QUICK_EXIT, false)
                    Toast.makeText(settings, "Quick Exit disabled", Toast.LENGTH_SHORT).show()
                } else {
                    settingQuickExitSwitch.isChecked = true
                    Preferences.setBoolean(QUICK_EXIT, true)
                    Toast.makeText(settings, "Quick Exit enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }

        github.setOnClickListener {
            DynamicAlert(settings)
                    .setHTML("This project <b>needs you!</b><br><br>Help make this app shine by contributing on GitHub! There's many ways you can help. Start by clicking <b>CONTRIBUTE</b> to open the CSGO Skill repository.")
                    .setTitle("To Victory!")
                    .setPositive("Contribute", Runnable {
                        val viewIntent = Intent(ACTION_VIEW, Uri.parse("https://csgo-skill.com/repo"))
                        startActivity(viewIntent)
                    })
                    .setNegative(android.R.string.cancel)
                    .setCancelable(true)
                    .show()
        }

        support.setOnClickListener {
            // Although I hate the ordering of the buttons (I'd rather it be Donate, Share, No Thanks)
            // I'm going to leave it how it is because I trust that Google knows what they're doing
            DynamicAlert(settings)
                    .setHTML("This project <b>needs you!</b><br><br>This app survives on user contributions. If you want to help development, consider <b>sharing</b> it on social media or <b>donating</b>. Every little bit helps!<br><br>Clicking <b>DONATE</b> will open another dialog with more information.")
                    .setTitle("To Victory!")
                    .setPositive("Share", Runnable {
                        val baseIntent = Intent.createChooser(Intent(), "Share")

                        // Get all packages that handle ACTION_SEND
                        val sendIntent = Intent(ACTION_SEND)
                        sendIntent.type = "text/plain"

                        val appList = settings.packageManager.queryIntentActivities(sendIntent, 0)
                        val intentList = arrayListOf<LabeledIntent>()

                        for (app in appList) {
                            val name = app.activityInfo.packageName

                            // Blacklisted intents
                            if (name.contains("bluetooth", true) ||
                                    name.contains("calendar", true))
                                continue

                            val intent = Intent()
                            intent.component = ComponentName(app.activityInfo.packageName, app.activityInfo.name)
                            intent.action = ACTION_SEND
                            intent.type = "text/plain"

                            if (name.contains("twitter", true) ||
                                    name.contains("facebook", true)) {
                                intent.putExtra(Intent.EXTRA_TEXT, "Up your game in #CSGO with the CSGO Skill app! Download #CSGOSkill for free on the Play Store.")
                            } else if (name.contains("mms", true) || name.contains("messaging", true)) {
                                intent.putExtra(Intent.EXTRA_TEXT, "Check out the CSGO Skill app, you can download it from the Play Store!")
                            } else {
                                intent.putExtra(Intent.EXTRA_TEXT, "Up your game in CSGO with the CSGO Skill app! Download CSGO Skill for free on the Play Store.")
                            }

                            intentList.add(LabeledIntent(intent, name, app.loadLabel(settings.packageManager), app.icon))
                        }

                        val extraIntents = intentList.toArray(arrayOfNulls<LabeledIntent>(intentList.size))

                        baseIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
                        startActivity(baseIntent)
                    })
                    .setButton("Donate", Runnable { settings.runOnUiThread {
                        DynamicAlert(settings)
                                .setHTML("By clicking <b>DONATE</b>, we'll try to open a browser to the following URL:<br><b>https://csgo-skill.com/donate</b><br><br>This page has more information about donations, including where the money goes and how it's spent. Donating will not unlock additional features, however it will ultimately lead to faster development and more goodies for everyone ;)")
                                .setTitle("Can I Get A Drop?")
                                .setPositive("Donate", Runnable {
                                    val viewIntent = Intent(ACTION_VIEW, Uri.parse("https://csgo-skill.com/donate"))
                                    startActivity(viewIntent)
                                })
                                .setNegative(android.R.string.cancel)
                                .setCancelable(true)
                                .show()
                    }})
                    .setNegative(android.R.string.cancel)
                    .setCancelable(true)
                    .show()
        }
    }
}
