/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.app.Fragment


abstract class BaseFragment : Fragment() {

    abstract var lMain: FragmentListener?

    interface FragmentListener {

        /**
         * <p>Notifies when valid account details have been retrieved, or the
         * user has signed up and the account needs to be added to device.</p>
         *
         * @param steamId The Steam ID of the account.
         * @param secret  The secret key for the account.
         */
        fun onLogin(steamId: String, secret: String)

        /**
         * Notifies when an account has been selected.
         *
         * @param steamId The Steam ID of the account.
         */
        fun onLogin(steamId: String)

        /**
         * Requests to change the current fragment.
         *
         * @param nextFragment The id of the fragment to load.
         */
        fun switchFragment(nextFragment: String)

        /**
         * Request to update the user information.
         */
        fun updateUser(): Boolean

    }

}
