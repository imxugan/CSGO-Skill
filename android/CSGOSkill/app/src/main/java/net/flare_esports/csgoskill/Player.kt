/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.graphics.Bitmap
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.InternetHelper.*
import org.json.JSONObject

/**
 * A general Player object to make working with the user account easier
 */
@Suppress("MemberVisibilityCanBePrivate")
class Player (profile: JSONObject) {

    private val _profileUrl: String = profile.getString("profileurl")
    private val _avatarUrl: String = profile.getString("avatarurl")
    private val _token: Token = Token(profile.getJSONObject("token"))
    private var dlAvatar: Boolean = false

    // Basic profile stuff

    /** The player's Steam ID */
    val steamId: String = profile.getString("steam_id")

    /** The player's Persona name */
    var persona: String = profile.getString("persona")
        set(persona) {
            if (Player.validPersona(persona) == "ok")
                field = persona
        }

    /** The player's profile link */
    var profileUrl: String = _profileUrl
        private set
        get()="https://steamcommunity.com/$_profileUrl"

    /** The player's avatar url */
    var avatarUrl: String = _avatarUrl
        private set
        get()="https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/${_avatarUrl}_full.jpg"

    /** The time this account was created */
    var created: Int = profile.getInt("created")
        private set

    /** The profile status object */
    var status: JSONObject = profile.getJSONObject("status")
        private set

    /** Stores the player's avatar, only downloading it when requested */
    var avatarImg: Bitmap? = null
        get() {
            val self = this
            if (field == null && !this.dlAvatar) {
                self.dlAvatar = true
                Thread().run {
                    try {
                        self.avatarImg = Player.downloadAvatar(self.avatarUrl)
                    } catch (e: Throwable) {
                        self.avatarImg = null
                        if (DEV_MODE) Log.e("Player.getAvatarImg", e)
                    }
                    self.dlAvatar = false
                }
            }
            return field
        }

    /** Holds the full JSONObject token, however it returns only the token value when requested
     * @see isTokenValid */
    var token: Any
        set(token) {
            if (token is JSONObject)
                this._token.setToken(token)
            else
                throw Throwable("Incorrect type set for Player.token! Must use JSONObject when setting!")
        }
        get() = this._token.value

    // Extra stuff for users

    /** The custom username for the player */
    var username: String = profile.optString("username")
        set(username) {
            val newName = Player.validUsername(username)
            if (newName.substring(0..2) == "ok")
                field = newName.substring(3)
        }

    /** The email associated with the player */
    var email: String = profile.optString("email")
        set(email) {
            if (Player.validEmail(email) == "ok")
                field = email
        }

    /** Email subscriptions, a future feature! */
    var subscription: JSONObject = profile.optJSONObject("subscription") ?: JSONObject()
        private set

    /** The player's verified status */
    var verified: Boolean = profile.optBoolean("verified")
        private set

    /** Special notifications from the server */
    var notify: String = profile.optString("notify")

    /** Returns <code>true</code> if <code>accountType == "user"</code> */
    val isUser get(): Boolean = this.accountType == "user"

    /** The player's account type */
    val accountType get(): String = this.status.getString("level")

    /**
     * Checks if the player's token is still valid
     *
     * @return <code>true</code> if the token is valid, <code>false</code> otherwise
     */
    fun isTokenValid(): Boolean {
        return Token.isValid(this._token)
    }

    override fun toString(): String {
        try {
            return JSONObject()
                    .put("steam_id", steamId)
                    .put("persona", persona)
                    .put("profileurl", _profileUrl)
                    .put("avatarurl", _avatarUrl)
                    .put("created", created)
                    .put("status", status)
                    .put("token", JSONObject()
                            .put("value", _token.value)
                            .put("time", _token.time))
                    .put("username", username)
                    .put("email", email)
                    .put("subscription", subscription)
                    .put("verified", verified)
                    .put("notify", notify)
                    .toString()
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Player.toString()", e)
            return ""
        }
    }

    companion object {
        /**
         * Validates a persona
         *
         * @param persona name to validate
         * @return "ok" if good, error message otherwise
         */
        @JvmStatic
        fun validPersona(persona: String): String {
            return if (persona.length !in 3..35) {
                "Persona must be between 3-35 characters"
            } else {
                "ok"
            }
        }

        /**
         * Filters and validates a username
         *
         * @param username name to validate
         * @return "ok" followed by the filtered username if good, error message otherwise
         */
        @JvmStatic
        fun validUsername(username: String): String {
            val newName = filterUsername(username)
            return if (newName.length !in 3..35) {
                "Username must be URL safe and be 3-35 characters"
            } else {
                "ok $newName"
            }
        }

        /**
         * Filters a username
         *
         * @param username name to filter
         * @return the filtered username
         */
        @JvmStatic
        fun filterUsername(username: String): String {
            val bannedChars: CharArray = charArrayOf(
                    '.', '~', ' ', ':', '/', '\\', '?', '&', '!', '#',
                    '[', ']', '@', '$', '\'', '"', '(', ')', '*', '+',
                    ',', ';', '=', '%', '^', '{', '}', '`', '<', '>', '|'
            )
            val newName = StringBuilder()
            var i = 0
            var c: Char
            while (i < username.length) {
                c = username[i]
                if (!bannedChars.contains(c)) {
                    newName.append(Character.toLowerCase(c))
                }
                i++
            }
            return newName.toString()
        }

        /**
         * VERY roughly validates an email address
         *
         * @param email address to validate
         * @return "ok" if good, error message otherwise
         */
        @JvmStatic
        fun validEmail(email: String): String {
            return when {
                (email.length !in 4..50) -> "Email must be between 4-50 characters"
                (email.indexOf('@') < 1) -> "Email does not seem to be valid"
                else -> "ok"
            }
        }

        /**
         * Gets a [Bitmap] image from a shortened Steam avatar url. You should not call this!
         *
         * @param shortLink shortened avatar link
         * @return [Bitmap] image, may be null
         */
        @JvmStatic
        fun downloadAvatar(shortLink: String): Bitmap? {
            val link = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/${shortLink}_full.jpg"
            return BitmapRequest(link)
        }

    }

}
