/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils

import net.flare_esports.csgoskill.R
import kotlinx.android.synthetic.main.fragment_introslide1.*

class Frag1 : Slide() {

    internal lateinit var view: View
    internal lateinit var context: Context

    override lateinit var slideListener: SlideListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        slideListener = context as SlideListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_introslide1, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        Handler().postDelayed({ fabSlide1.visibility = View.VISIBLE; fabSlide1.animation = animation }, 1700)
        fabSlide1.setOnClickListener { slideListener.nextSlide(this) }
    }
}
