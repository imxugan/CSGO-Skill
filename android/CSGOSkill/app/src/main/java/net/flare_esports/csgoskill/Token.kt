/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import org.json.JSONObject
import java.lang.System.currentTimeMillis

/**
 * A better way for storing our Token objects
 */
class Token(token: JSONObject) {

    /** The secret hash value for the token */
    var value: String = ""
        private set

    /** The time this token was created */
    var time: Int = currentTimeMillis().toInt()
        private set

    init {
        this.setToken(token)
    }

    /**
     * Sets the token
     *
     * @param token token string
     * @param time time token was created
     * @return this Token
     */
    fun setToken(token: String, time: Number = currentTimeMillis()): Token {
        this.value = token
        this.time = time.toInt()
        return this
    }

    /**
     * Sets the token
     *
     * @param token [JSONObject] containing mappings for <code>value</code> and <code>time</code>
     * @return this Token
     */
    fun setToken(token: JSONObject): Token {
        this.value = token.optString("value", "")
        this.time = token.optInt("time", currentTimeMillis().toInt())
        return this
    }

    companion object {

        /** The hard token validity limit, which is 6 days on our server */
        @JvmStatic
        val LIMIT = 6 * 24 * 60 * 60 // Default 6 day limit

        /**
         * Checks if the token is valid
         *
         * @param token Token object to check
         * @param limit time limit for the [token]
         * @return <code>true</code> if valid, <code>false</code> otherwise
         */
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

        /**
         * Checks if the token is valid
         *
         * @param token [JSONObject] with mapping for <code>value</code> and <code>time</code>
         * @param limit time limit for the [token]
         * @return <code>true</code> if valid, <code>false</code> otherwise
         */
        @JvmStatic
        fun isValid(token: JSONObject, limit: Number = Token.LIMIT): Boolean {
            return Token.isValid(Token(token), limit)
        }
    }

}
