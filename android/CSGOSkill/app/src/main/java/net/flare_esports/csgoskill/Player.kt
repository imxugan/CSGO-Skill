/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.graphics.Bitmap
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.InternetHelper.*
import org.json.JSONObject

class Player
(profile: JSONObject) {

    // Basic profile stuff
    var steamId:    String = profile.getString("steam_id")
        private set
    var persona:    String = profile.getString("persona")
        set(persona) {
            if (Player.validPersona(persona) == "ok")
                field = persona
        }

    private var _profileUrl: String = profile.getString("profileurl")
    var profileUrl: String = _profileUrl
        private set
        get() = "https://steamcommunity.com/$_profileUrl"

    private var _avatarUrl: String = profile.getString("avatarurl")
    var avatarUrl:  String = _avatarUrl
        private set
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/${_avatarUrl}_full.jpg"

    var created:    Int    = profile.getInt("created")
        private set
    var status:     JSONObject = profile.getJSONObject("status")
        private set

    // Avatar stuff
    private var dlAvatar: Boolean = false
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

    // Token stuff
    private var _token: Token = Token(profile.getJSONObject("token"))
    var token: Any
        set(token) {
            if (token is JSONObject)
                this._token.setToken(token)
            else
                throw Throwable("Incorrect type set for Player.token! Must use JSONObject when setting!")
        }
        get() = this._token.value

    // Extra stuff for users
    var username:     String     = profile.optString("username")
        set(username) {
            val newName = Player.validUsername(username)
            if (newName.substring(0..2) == "ok")
                field = newName.substring(3)
        }
    var email:        String     = profile.optString("email")
        set(email) {
            if (Player.validEmail(email) == "ok")
                field = email
        }
    var subscription: JSONObject = profile.optJSONObject("subscription") ?: JSONObject()
        private set
    var verified:     Boolean    = profile.optBoolean("verified")
        private set
    var notify:       String     = profile.optString("notify")

    // Determine if Player is a user
    val isUser get(): Boolean = this.accountType == "user"

    // Quickly get account type
    val accountType get(): String = this.status.getString("level")

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
        @JvmStatic
        fun validPersona(persona: String): String {
            return if (persona.length !in 3..35) {
                "Persona must be between 3-35 characters"
            } else {
                "ok"
            }
        }

        @JvmStatic
        fun validUsername(username: String): String {
            val newName = filterUsername(username)
            return if (newName.length !in 3..35) {
                "Username must be URL safe and be 3-35 characters"
            } else {
                "ok $newName"
            }
        }

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

        @JvmStatic
        fun validEmail(email: String): String {
            return if (email.length !in 4..50) {
                "Email must be between 4-50 characters"
            } else if (email.indexOf('@') < 1) {
                "Email does not seem to be valid"
            } else {
                "ok"
            }
        }

        @JvmStatic
        fun downloadAvatar(shortLink: String): Bitmap {
            val link = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/${shortLink}_full.jpg"
            return BitmapRequest(link)
        }

    }

}
