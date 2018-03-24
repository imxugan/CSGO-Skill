/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

/**
 * Helps us set listeners without having to manually override each function
 * every time we need to make one.
 */

class Animer : AnimationListener {

    private var start: () -> Unit = {}
    private var end: () -> Unit = {}
    private var repeat: () -> Unit = {}

    // Although pointless to use alone, this works great with setRepeat or setStart
    constructor()

    constructor(end: () -> Unit) {
        this.end = end
    }

    constructor(start: () -> Unit, end: () -> Unit) {
        this.start = start
        this.end = end
    }

    constructor(start: () -> Unit, end: () -> Unit, repeat: () -> Unit) {
        this.start = start
        this.end = end
        this.repeat = repeat
    }

    fun setStart(start: () -> Unit): Animer {
        this.start = start
        return this
    }

    fun setEnd(end: () -> Unit): Animer {
        this.start = end
        return this
    }

    fun setRepeat(repeat: () -> Unit): Animer {
        this.repeat = repeat
        return this
    }

    override fun onAnimationStart(animation: Animation) { start() }

    override fun onAnimationEnd(animation: Animation) { end() }

    override fun onAnimationRepeat(animation: Animation) { repeat() }

}
