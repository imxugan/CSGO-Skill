/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * A class for tracking preferences, better than the built-in preferences!
 */
object Preferences {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.getContext())

    private var listener: (preferences: SharedPreferences, key: String, value: Any?) -> Unit = { _, _, _ ->  }

    /* // Preference Names // */

    /** Automatic login if there is one account on the device */
    @JvmStatic val AUTO_LOGIN = "autoLogin"

    /** Quick-exit when double tapping BACK on the Home screen */
    @JvmStatic val QUICK_EXIT = "quickExit"

    /** If the quick-exit tip has been shown, informing the user about the functionality */
    @JvmStatic val QUICK_EXIT_NOTICE = "quickExitNotice"

    /** Whether or not this is the first time the app has been run */
    @JvmStatic val FIRST_RUN = "firstRun"

    /* // End Preference Names // */

    /**
     * Sets a listener for changes to preferences
     */
    fun setListener(listener: (preferences: SharedPreferences, key: String, value: Any?) -> Unit): Preferences {
        this.listener = listener
        prefs.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            this@Preferences.listener.invoke(sharedPreferences, key, getAll()[key])
        }
        return this
    }

    /**
     * Normal preference existence checker
     *
     * @param key key
     * @return <code>true</code> if set, <code>false</code> otherwise
     */
    fun has(key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * Normal everything getter
     *
     * @return [Map] of all preferences
     */
    fun getAll(): Map<String, *> {
        return prefs.all
    }

    /* // GETTERS // */

    /**
     * Gets a boolean value at [key]
     *
     * @param key key
     * @param default fallback boolean value, false by default
     * @return boolean value at [key], or [default] if not set
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)

    /**
     * Gets a float value at [key]
     *
     * @param key key
     * @param default fallback float value, 0 by default
     * @return float value at [key], or [default] if not set
     */
    fun getFloat(key: String, default: Float = 0f): Float = prefs.getFloat(key, default)

    /**
     * Gets an int value at [key]
     *
     * @param key key
     * @param default fallback int value, 0 by default
     * @return int value at [key], or [default] if not set
     */
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)

    /**
     * Gets a long value at [key]
     *
     * @param key key
     * @param default fallback long value, 0 by default
     * @return long value at [key], or [default] if not set
     */
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    /**
     * Gets a string value at [key]
     *
     * @param key key
     * @param default fallback string value, empty string by default
     * @return string value at [key], or [default] if not set
     */
    fun getString(key: String, default: String = ""): String = prefs.getString(key, default)

    /**
     * Gets a Set<String> value at [key]
     *
     * @param key key
     * @param default fallback, an empty set by default
     * @return Set<String> value at [key], or [default] if not set
     */
    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> = prefs.getStringSet(key, default)

    /* // SETTERS // */

    fun setBoolean(key: String, boolean: Boolean): Preferences {
        prefs.edit().putBoolean(key, boolean).apply()
        return this
    }

    fun setFloat(key: String, float: Float): Preferences {
        prefs.edit().putFloat(key, float).apply()
        return this
    }

    fun setInt(key: String, int: Int): Preferences {
        prefs.edit().putInt(key, int).apply()
        return this
    }

    fun setLong(key: String, long: Long): Preferences {
        prefs.edit().putLong(key, long).apply()
        return this
    }

    fun setString(key: String, string: String): Preferences {
        prefs.edit().putString(key, string).apply()
        return this
    }

    fun setStringSet(key: String, set: Set<String>): Preferences {
        prefs.edit().putStringSet(key, set).apply()
        return this
    }

}
