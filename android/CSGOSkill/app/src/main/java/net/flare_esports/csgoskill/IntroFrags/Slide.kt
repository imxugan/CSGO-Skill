/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.app.Fragment

/**
 * A simple class for the [net.flare_esports.csgoskill.Intro] slides
 */
abstract class Slide: Fragment() {

    /** The listener that slides use to inform [net.flare_esports.csgoskill.Intro] that it has finished animating */
    abstract var slideListener: SlideListener?

    /** Names the slide so that [net.flare_esports.csgoskill.Intro] can better keep track of them */
    abstract val name: String

    /**
     * The interface that [net.flare_esports.csgoskill.Intro] implements to animate slides
     */
    interface SlideListener {

        /**
         * Called when the [net.flare_esports.csgoskill.Intro] should display the continue button
         *
         * @param currentFragment the currently visible slide
         */
        fun animationComplete(currentFragment: Slide)
    }

}
