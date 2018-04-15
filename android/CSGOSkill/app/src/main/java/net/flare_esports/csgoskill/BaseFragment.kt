/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.Fragment
import org.json.JSONObject


abstract class BaseFragment : Fragment() {

    abstract val main: Main
    abstract val listener: FragmentListener?
    abstract val name: String

    /**
     * <p>Called when user presses the back button, should return False when
     * the Main Activity should handle the event itself.</p>
     *
     * @return True if fragment handled the event, false if Main Activity should
     */
    abstract fun onBack(): Boolean

    interface FragmentListener {

        /**
         * <p>Notifies when valid account details have been retrieved, and the
         * user needs to be logged in.</p>
         *
         * @param  player The generated Player object
         * @return True if Main Activity handled the login, false if account is not on device
         */
        fun loginPlayer(player: Player): Boolean

        /**
         * Requests to change the current fragment.
         *
         * @param nextFragment The id of the fragment to load
         */
        fun switchFragment(nextFragment: Int)

        /**
         * Request to update the user information.
         *
         * @return True if successful, false + alert otherwise
         */
        fun updatePlayer(): Boolean

        /**
         * Request to update the user stats.
         *
         * @return True if successful, false + alert otherwise
         */
        fun updateStats(): Boolean

        /**
         * Requests the Player object for the logged in account.
         *
         * @return the Player object
         */
        fun getPlayer(): Player?

        /**
         * Requests the Player's history stats.
         *
         * @return JSONObject of stat history
         */
        fun getHistoryStats(): JSONObject?

        /**
         * Requests the Player's grand stats.
         *
         * @return JSONObject of stat history
         */
        fun getGrandStats(): JSONObject?

        /**
         * Requests the Player's current stats.
         *
         * @return JSONObject of stat history
         */
        fun getCurrentStats(): JSONObject?

    }

}
