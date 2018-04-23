/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast

import net.flare_esports.csgoskill.InternetHelper.*
import net.flare_esports.csgoskill.Constants.*
import org.json.JSONArray
import org.json.JSONObject

class Database(
        private var context: Context,
        factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(context, NAME, factory, DB_VERSION) {

    companion object {
        private const val DB_VERSION = 1
        private const val NAME = "csgoskill.db"
        private const val USERS = "Users"

        private const val STEAMID = "steamID"
        private const val PROFILE = "profile"
        private const val STATS = "stats"
    }

    var newVersion = emptyArray<Int>()
        private set
    var lastError: Throwable? = null
        private set

    /**
     * Uses getPlayer() to return a list of all users on the device.
     *
     * @return Array<Player> of all users, using getPlayer()
     */
    val users: Array<Player>
        get() {
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT $STEAMID FROM $USERS WHERE 1", null)
            return if (c.count <= 0) {
                c.close()
                emptyArray()
            } else {
                var users = emptyArray<Player>()
                var user: Player?
                while (c.moveToNext()) {
                    user = getPlayer(c.getString(c.getColumnIndex(STEAMID)))
                    if (user != null) users = users.plus(user)
                }
                c.close()
                users
            }
        }

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE " + USERS + " (" +
                STEAMID + " TEXT UNIQUE," + // Steam ID
                PROFILE + " TEXT," +        // All the Profile info
                STATS + " TEXT)"            // All the player's stats
        )

    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        // Currently there are no plans to change the structure of the DB, so nothing is here

    }

    /**
     * <p>Asks the server for the most current version number. When outdated,
     * it will set a variable called 'newVersion' to the response before
     * returning.</p>
     *
     * @return 1 if up-to-date, 0 if updates available, -1 if error
     */
    fun checkVersion(): Int {
        try {
            if (!isOnline()) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            val response = HTTPRequest("http://api.csgo-skill.com/version")
            if (response.isEmpty()) {
                throw Throwable("No response")
            } else if (response == "Unable to resolve host \"api.csgo-skill.com\": No address associated with hostname") {
                throw Throwable("No connection")
            }
            val version = JSONArray(response)
            newVersion = arrayOf(version[0] as Int, version[1] as Int, version[2] as Int)
            if (VERSION[0] < newVersion[0] ||
                (VERSION[0] == newVersion[0] && VERSION[1] < newVersion[1]) ||
                (VERSION[0] == newVersion[0] && VERSION[1] == newVersion[1] && VERSION[2] < newVersion[2])) {
                return 0
            }
            return 1
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) {
                if (e.message == "No connection")
                    Log.e("Database.checkVersion", "No connection")
                else
                    Log.e("Database.checkVersion", e)
            }
        }

        return -1
    }

    /**
     * <p>Inserts the given user (Username, Steam ID, Email, Secret),
     * updatePlayer() should be called immediately after.</p>
     *
     * @param player The Player object to insert
     * @return True if successful, false otherwise.
     */
    fun insertUser(player: Player): Boolean {
        try {
            val sql = writableDatabase
            val values = ContentValues()
            values.put(STEAMID, player.steamId)
            values.put(PROFILE, player.toString())
            if (sql.insertOrThrow(USERS, null, values) != -1L) {
                return true
            }
        } catch (e: Throwable) {
            lastError = e
            if (DEVMODE) Log.e("Database.insertUser", e)
        }

        return false
    }

    /**
     * Effectively logs the player in, updating to match the server.
     *
     * @param player The Player to update
     * @return True if successful, false if failed
     */
    fun loginPlayer(player: Player): Boolean {
        try {
            if (!isOnline()) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/login")
                    .put("post", JSONObject()
                            .put("steamid", player.steamId)
                            .put("token", player.token)
                    )
            request = HTTPJsonRequest(request)
            if (request == null)
                throw Throwable("null")
            if (!request.optBoolean("success")) {
                throw Throwable(request.optString("reason"))
            } else {
                // Got the player!
                val newPlayer = Player(request.getJSONObject("profile"))
                val sql = writableDatabase
                val values = ContentValues()
                values.put(PROFILE, newPlayer.toString())
                if (sql.update(USERS, values, "$STEAMID=?", arrayOf(newPlayer.steamId)) != 1) {
                    throw Throwable("update-fail")
                }
                return true
            }
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Database.loginPlayer", e)
            var m = e.message ?: ""
            m = if (m.startsWith("error code")) {
                "Login failed with error code " + m.substring(11)
            } else {
                when (m) {
                    "bad-steamid" -> {
                        "Your Steam ID wasn't accepted, please logout and login again. What? How'd you do that?"
                    }
                    "bad-token" -> {
                        "Your login token wasn't accepted, please logout and login again. What? How'd you do that?"
                    }
                    "login-required" -> {
                        "Been a while? Your login has expired, please logout and login again."
                    }
                    "not-found" -> {
                        "Who are you?"
                    }
                    "bad-request" -> {
                        "Sorry, I sent the wrong stuff to the serv- What? How'd you do that?"
                    }
                    "update-fail" -> {
                        "You were successfully logged in, but the device was unable to update your account data. Profile information and stats data may appear outdated, and clearing the app data cache and logging back in should fix this. Don't do it until you have an internet connection, however!"
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
     * Pulls all current player stats from server and updates them.
     *
     * @param player The Player to update
     * @return True if successful, false if failed
     */
    fun updateStats(player: Player): Boolean {
        try {
            if (!isOnline()) Toast.makeText(context, R.string.no_internet_warning, Toast.LENGTH_SHORT).show()
            var request = JSONObject()
                    .put("url", "http://api.csgo-skill.com/stats/" + player.steamId)
            request = HTTPJsonRequest(request)
            if (request == null) {
                throw Throwable("null")
            }
            if (!request.optBoolean("success")) {
                throw Throwable(request.optString("reason"))
            } else {
                // Got stats!
                val sql = writableDatabase
                val values = ContentValues()
                values.put(STATS, request.optJSONObject("stats").toString())
                if (sql.update(USERS, values, "$STEAMID=?", arrayOf(player.steamId)) != 1) {
                    throw Throwable("update-fail")
                }
                return true
            }
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Database.updateStats", e)
            var m = e.message ?: ""
            m = if (m.startsWith("error code")) {
                "Stat update failed with error code " + m.substring(11)
            } else {
                when (m) {
                    "not-found" -> {
                        "Who are you?"
                    }
                    "update-fail" -> {
                        "You were successfully logged in, but the device was unable to update your account data. Profile information and stats data may appear outdated, and clearing the app data cache and logging back in should fix this. Don't do it until you have an internet connection, however!"
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
     * Gets the Player object for a steamId
     *
     * @param steamId The Steam ID of the user
     * @return The Player object, or null if error occurred.
     */
    fun getPlayer(steamId: String): Player? {
        try {
            // Grab stuff
            val sql = writableDatabase
            val c = sql.rawQuery("SELECT $PROFILE FROM $USERS WHERE $STEAMID = \"$steamId\"", null)
            if (c.count <= 0) {
                c.close()
                throw Throwable("not-found")
            } else {
                c.moveToFirst()
                val result = Player(JSONObject(c.getString(c.getColumnIndex(PROFILE))))
                c.close()
                return result
            }
        } catch (e: Throwable) {
            var m = e.message ?: ""
            m = when (m) {
                "not-found" -> {
                    "Who are you?"
                }
                else -> {
                    if (DEVMODE) Log.e("Database.getPlayer", e)
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
            if (DEVMODE) Log.e("Database.getGrandStats", e)
            var m = e.message ?: ""
            m = when (m) {
                "not-found" -> {
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
            if (DEVMODE) Log.e("Database.getHistoryStats", e)
            var m = e.message ?: ""
            m = when (m) {
                "not-found" -> {
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
            if (DEVMODE) Log.e("Database.getCurrentStats", e)
            var m = e.message ?: ""
            m = when (m) {
                "not-found" -> {
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
        val c = sql.rawQuery("SELECT $STATS FROM $USERS WHERE $STEAMID = \"${player.steamId}\"", null)
        if (c.count <= 0) {
            c.close()
            throw Throwable("not-found")
        } else {
            c.moveToFirst()
            val result = JSONObject(c.getString(c.getColumnIndex(STATS)))
            c.close()
            return result
        }
    }

}
