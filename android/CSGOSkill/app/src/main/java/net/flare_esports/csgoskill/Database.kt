/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.Constants.NOT_FOUND
import net.flare_esports.csgoskill.Constants.NO_API
import net.flare_esports.csgoskill.Constants.NO_INTERNET
import net.flare_esports.csgoskill.Constants.NO_RESPONSE
import net.flare_esports.csgoskill.Constants.REQUEST_FAIL
import net.flare_esports.csgoskill.Constants.UPDATE_FAIL
import net.flare_esports.csgoskill.Constants.VERSION
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles all database interactions both on the device and with the CSGO Skill servers, mainly for
 * working with player profiles.
 */
class Database(
        context: Context,
        factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(context, NAME, factory, DB_VERSION) {

    companion object {
        private const val DB_VERSION = 1
        private const val NAME = "csgoskill.db"
        private const val USERS = "Users"

        private const val STEAM_ID = "steamID"
        private const val PROFILE = "profile"
        private const val STATS = "stats"
    }

    /**
     * Enumeration for checking app updates
     */
    enum class Updating {
        /** Updates are available */
        UPDATES_AVAILABLE,

        /** App is up-to-date */
        UP_TO_DATE,

        /** Update check failed */
        FAILED
    }

    /** Contains the newest available version, initially the current app version */
    @Suppress("MemberVisibilityCanBePrivate")
    var newVersion: IntArray = Constants.VERSION
        private set

    /** Holds the most recently thrown DB error */
    var lastError: Throwable? = null
        private set

    /**
     * Uses [getPlayer] to return a list of all users on the device.
     *
     * @return [Array] of all users
     */
    val users: Array<Player>
        get() {
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT $STEAM_ID FROM $USERS WHERE 1", null)
            return if (c.count <= 0) {
                c.close()
                emptyArray()
            } else {
                var users = emptyArray<Player>()
                var user: Player?
                while (c.moveToNext()) {
                    user = getPlayer(c.getString(c.getColumnIndex(STEAM_ID)))
                    if (user != null) users = users.plus(user)
                }
                c.close()
                users
            }
        }

    /**
     * Runs once when the app if first started, creating the database to hold all player information.
     * The process is automatic, so please don't call directly
     *
     * @param sqLiteDatabase database object to initialize
     */
    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE " + USERS + " (" +
                STEAM_ID + " TEXT UNIQUE," + // Steam ID
                PROFILE + " TEXT," +        // All the Profile info
                STATS + " TEXT)"            // All the player's stats
        )

    }

    /**
     * Updates previous database versions to the current version. Since the current database
     * structure is not planned to be changed, only added to, there should be no reason to ever do
     * anything inside this function.
     *
     * @param sqLiteDatabase database object to upgrade
     * @param oldVersion previous database version
     * @param newVersion new database version
     */
    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        // Currently there are no plans to change the structure of the DB, so nothing is here

    }

    /**
     * <p>Asks the server for the most current version number. When outdated,
     * it will set a variable called 'newVersion' to the response before
     * returning.</p>
     *
     * @return [Updating] enum item
     */
    fun checkVersion(): Updating {
        try {
            if (!isOnline()) {
                lastError = Throwable(NO_INTERNET)
                return Updating.FAILED
            }
            val response = HTTPRequest("http://api.csgo-skill.com/version")
            if (response.isEmpty()) {
                throw Throwable(NO_RESPONSE)
            } else if (response.startsWith("unable to resolve host", true)) {
                throw Throwable(NO_API)
            }
            val version = JSONArray(response)
            newVersion = intArrayOf(version[0] as Int, version[1] as Int, version[2] as Int)
            if (VERSION[0] < newVersion[0] ||
                (VERSION[0] == newVersion[0] && VERSION[1] < newVersion[1]) ||
                (VERSION[0] == newVersion[0] && VERSION[1] == newVersion[1] && VERSION[2] < newVersion[2])) {
                return Updating.UPDATES_AVAILABLE
            }
            // Reset newVersion
            newVersion = VERSION
            return Updating.UP_TO_DATE
        } catch (e: Throwable) {
            lastError = e
            if (DEV_MODE)  Log.d("Database.checkVersion", e.message)
        }

        return Updating.FAILED
    }

    /**
     * <p>Inserts the given user (Username, Steam ID, Email, Secret),
     * [Main.updatePlayer] should be called immediately after.</p>
     *
     * @param player The [Player] to insert
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    fun insertUser(player: Player): Boolean {
        try {
            val sql = writableDatabase
            val values = ContentValues()
            values.put(STEAM_ID, player.steamId)
            values.put(PROFILE, player.toString())
            if (sql.insertOrThrow(USERS, null, values) != -1L) {
                return true
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEV_MODE) Log.e("Database.insertUser", e)
        }

        return false
    }

    /**
     * Logs the [player] into the server, and syncs profile information.
     *
     * @param player The [Player] to update
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @see updateStats
     */
    fun loginPlayer(player: Player): Boolean {
        try {
            if (!isOnline()) {
                lastError = Throwable(NO_INTERNET)
                return false
            }
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/login")
                    .put("post", JSONObject()
                            .put("steamid", player.steamId)
                            .put("token", player.token)
                    )
            request = HTTPJsonRequest(request)
            if (request.optString("response", "not empty").isEmpty())
                throw Throwable(NO_RESPONSE)
            if (!request.optBoolean("success")) {
                throw Throwable(request.optString("reason", REQUEST_FAIL))
            } else {
                // Got the player!
                val newPlayer = Player(request.getJSONObject("profile"))
                val sql = writableDatabase
                val values = ContentValues()
                values.put(PROFILE, newPlayer.toString())
                if (sql.update(USERS, values, "$STEAM_ID=?", arrayOf(newPlayer.steamId)) != 1) {
                    throw Throwable(UPDATE_FAIL)
                }
                return true
            }
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Database.loginPlayer", e)
            var m = e.message ?: ""
            m = when {
                m.startsWith("error code") ->
                    "Login failed with error code " + m.substring(11)
                m.startsWith("unable to resolve", true) ->
                    NO_API
                else -> when (m) {
                    "bad-steamid" -> {
                        "Your Steam ID wasn't accepted, please logout and login again. What? How'd you do that?"
                    }
                    "bad-token" -> {
                        "Your login token wasn't accepted, please logout and login again. What? How'd you do that?"
                    }
                    "bad-request" -> {
                        "Sorry, I sent the wrong stuff to the server. What? How'd you do that?"
                    }
                    "login-required" -> {
                        "Been a while? Your login has expired, please logout and login again."
                    }
                    NOT_FOUND -> {
                        "Who are you?"
                    }
                    UPDATE_FAIL -> {
                        "You were successfully logged in, but the device was unable to update your account data. Profile information and stats data may appear outdated, and clearing the app data cache and logging back in should fix this. Don't do it until you have an internet connection, however!"
                    }
                    REQUEST_FAIL -> {
                        "Request failed spectacularly."
                    }
                    else -> {
                        "Unexpected error. Please report this."
                    }
                }
            }
            lastError = Throwable(m)
        }

        return false
    }

    /**
     * Syncs the [player] stats with the server.
     *
     * @param player The [Player] to update
     * @return <code>true</code> if successful, <code>false</code> otherwise
     * @see loginPlayer
     */
    fun updateStats(player: Player): Boolean {
        try {
            if (!isOnline()) {
                lastError = Throwable(NO_INTERNET)
                return false
            }
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/stats/" + player.steamId)
            request = HardHTTPJsonRequest(request)
            if (request.optString("response").isEmpty()) {
                throw Throwable(NO_RESPONSE)
            }
            if (!request.optBoolean("success")) {
                throw Throwable(request.optString("reason", REQUEST_FAIL))
            } else {
                // Got stats!
                val sql = writableDatabase
                val values = ContentValues()
                values.put(STATS, request.optJSONObject("stats").toString())
                if (sql.update(USERS, values, "$STEAM_ID=?", arrayOf(player.steamId)) != 1) {
                    throw Throwable(UPDATE_FAIL)
                }
                return true
            }
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Database.updateStats", e)
            var m = e.message ?: ""
            m = when {
                m.startsWith("error code") ->
                    "Stat update failed with error code " + m.substring(11)
                m.startsWith("unable to resolve", true) ->
                    NO_API
                else -> when (m) {
                    NOT_FOUND -> {
                        "Who are you?"
                    }
                    UPDATE_FAIL -> {
                        "You were successfully logged in, but the device was unable to update your account data. Profile information and stats data may appear outdated, and clearing the app data cache and logging back in should fix this. Don't do it until you have an internet connection, however!"
                    }
                    NO_RESPONSE -> {
                        "Received an empty response."
                    }
                    REQUEST_FAIL -> {
                        "Request failed spectacularly."
                    }
                    else -> {
                        "Unexpected error. Please report this."
                    }
                }
            }
            lastError = Throwable(m, e)
        }

        return false
    }

    /**
     * Gets the Player object for a steamId
     *
     * @param steamId The Steam ID of the user
     * @return The Player object, or null if error occurred.
     */
    fun getPlayer(steamId: String): Player? {
        try {
            // Grab stuff
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT $PROFILE FROM $USERS WHERE $STEAM_ID = \"$steamId\"", null)
            if (c.count <= 0) {
                c.close()
                throw Throwable(NOT_FOUND)
            } else {
                c.moveToFirst()
                val result = Player(JSONObject(c.getString(c.getColumnIndex(PROFILE))))
                c.close()
                return result
            }
        } catch (e: Throwable) {
            var m = e.message ?: ""
            m = when (m) {
                NOT_FOUND -> {
                    "Who are you?"
                }
                else -> {
                    if (DEV_MODE) Log.e("Database.getPlayer", e)
                    "Unexpected error. Please report this."
                }
            }
            lastError = Throwable(m)
        }

        return null
    }

    /**
     * Gets a JSONObject representing the given user's global tracked stats.
     *
     * @param player The Player to get stats from
     * @return JSONObject of the user's grand stats, or null if error occurred.
     */
    fun getGrandStats(player: Player): JSONObject? {
        try {
            return getStats(player).getJSONObject("grand")
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Database.getGrandStats", e)
            var m = e.message ?: ""
            m = when (m) {
                NOT_FOUND -> {
                    "Who are you?"
                }
                else -> {
                    "Unexpected error. Please report this."
                }
            }
            lastError = Throwable(m)
        }

        return null
    }

    /**
     * Gets a JSONObject representing the given user's stat history.
     *
     * @param player The Player to get stats from
     * @return JSONObject of the user's stat history, or null if error occurred.
     */
    fun getHistoryStats(player: Player): JSONObject? {
        try {
            return getStats(player).getJSONObject("history")
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Database.getHistoryStats", e)
            var m = e.message ?: ""
            m = when (m) {
                NOT_FOUND -> {
                    "Who are you?"
                }
                else -> {
                    "Unexpected error. Please report this."
                }
            }
            lastError = Throwable(m)
        }

        return null
    }

    /**
     * Gets a JSONObject of the most recently saved stats of user.
     *
     * @param player The Player to get stats from
     * @return JSONObject of the user's all-time stats, or null if error occurred.
     */
    fun getCurrentStats(player: Player): JSONObject? {
        try {
            return getStats(player).getJSONObject("current")
        } catch (e: Throwable) {
            if (DEV_MODE) Log.e("Database.getCurrentStats", e)
            var m = e.message ?: ""
            m = when (m) {
                NOT_FOUND -> {
                    "Who are you?"
                }
                else -> {
                    "Unexpected error. Please report this."
                }
            }
            lastError = Throwable(m)
        }

        return null
    }

    /**
     * Get the JSONObject of all stats stored for the player.
     *
     * @param player The Player to get stats from
     * @return JSONObject of the player's stats
     * @throws Throwable if not found
     */
    @Throws(Throwable::class)
    private fun getStats(player: Player): JSONObject {
        val sql = writableDatabase
        val c = sql.rawQuery("SELECT $STATS FROM $USERS WHERE $STEAM_ID = \"${player.steamId}\"", null)
        if (c.count <= 0) {
            c.close()
            throw Throwable(NOT_FOUND)
        } else {
            c.moveToFirst()
            val result = JSONObject(c.getString(c.getColumnIndex(STATS)))
            c.close()
            return result
        }
    }

}
