/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

internal class Animer : AnimationListener {

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

    fun setStart(start: () -> Unit) {
        this.start = start
    }

    fun setEnd(end: () -> Unit) {
        this.start = end
    }

    fun setRepeat(repeat: () -> Unit) {
        this.repeat = repeat
    }

    override fun onAnimationStart(animation: Animation) { start }

    override fun onAnimationEnd(animation: Animation) { end }

    override fun onAnimationRepeat(animation: Animation) { repeat }

}
