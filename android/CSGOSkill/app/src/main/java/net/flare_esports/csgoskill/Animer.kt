/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.animation.Animator
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

/**
 * Helps us set listeners without having to manually override each function
 * every time we need to make one.
 */

class Animer : AnimationListener, Animator.AnimatorListener {

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

    /**
     * Makes the given [start] the new [onAnimationStart] listener function, chainable
     *
     * @param start function to run when the animation starts
     * @return this [Animer]
     */
    fun setStart(start: () -> Unit): Animer {
        this.start = start
        return this
    }

    /**
     * Makes the given [end] the new [onAnimationEnd] listener function, chainable
     *
     * @param end function to run when the animation ends
     * @return this [Animer]
     */
    fun setEnd(end: () -> Unit): Animer {
        this.start = end
        return this
    }

    /**
     * Makes the given [repeat] the new [onAnimationRepeat] listener function, chainable
     *
     * @param repeat function to run when the animation repeats
     * @return this [Animer]
     */
    fun setRepeat(repeat: () -> Unit): Animer {
        this.repeat = repeat
        return this
    }

    override fun onAnimationStart(animation: Animation) { start() }

    override fun onAnimationEnd(animation: Animation) { end() }

    override fun onAnimationRepeat(animation: Animation) { repeat() }

    override fun onAnimationStart(animation: Animator?) { start() }

    override fun onAnimationEnd(animation: Animator?) { end() }

    override fun onAnimationRepeat(animation: Animator?) { repeat() }

    override fun onAnimationCancel(animation: Animator?) { end() }

}
