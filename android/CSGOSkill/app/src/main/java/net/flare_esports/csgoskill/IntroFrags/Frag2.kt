/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import net.flare_esports.csgoskill.R
import kotlinx.android.synthetic.main.fragment_introslide2.*

class Frag2 : Slide() {

    internal lateinit var view: View
    internal lateinit var context: Context
    override var slideListener: SlideListener? = null
    override val name: String = "slide2"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        slideListener = context as SlideListener
    }

    override fun onDetach() {
        super.onDetach()
        slideListener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_introslide2, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Handler().postDelayed({ slideListener?.animationComplete(this) }, 1500)
    }
}
