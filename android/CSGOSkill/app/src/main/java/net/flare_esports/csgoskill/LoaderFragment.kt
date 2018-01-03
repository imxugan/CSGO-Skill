/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

// This fragment simply has a semi-transparent background and a spinning loader
// graphic. Useful for blocking the UI when doing important loading things.

class LoaderFragment : BaseFragment() {

    internal lateinit var view: View
    internal lateinit var context: Context
    override var lMain: FragmentListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_loader, container, false)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        lMain = context as FragmentListener
    }

    override fun onDetach() {
        super.onDetach()
        lMain = null
    }

}
