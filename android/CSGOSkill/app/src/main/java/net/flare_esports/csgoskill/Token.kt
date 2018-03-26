/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import org.json.JSONObject
import java.lang.System.currentTimeMillis

class Token {
    var value: String = ""
        private set
    var time: Int = currentTimeMillis().toInt()
        private set

    constructor(token: String = "", time: Number = currentTimeMillis()) {
        setToken(token, time)
    }

    constructor(token: JSONObject) {
        this.setToken(token)
    }

    fun setToken(token: String, time: Number = currentTimeMillis()): Token {
        this.value = token
        this.time = time.toInt()
        return this
    }

    fun setToken(token: JSONObject): Token {
        this.value = token.optString("value", "")
        this.time = token.optInt("time", currentTimeMillis().toInt())
        return this
    }

    companion object {
        @JvmStatic
        private val LIMIT = 6 * 24 * 60 * 60 // Default 6 day limit

        @JvmStatic
        fun isValid(token: Token, limit: Number = Token.LIMIT): Boolean {
            val value = token.value
            val time = token.time
            if (value.length == 64) {
                if (!Regex("/^[0-9a-fA-F]+$/").matches(value)) { return false }
                val seconds = Math.abs(time - currentTimeMillis()) / 1000
                return seconds < limit.toInt()
            }
            return false
        }

        @JvmStatic
        fun isValid(token: JSONObject, limit: Number = Token.LIMIT): Boolean {
            return Token.isValid(Token(token), limit)
        }
    }

}
