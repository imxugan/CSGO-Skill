/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2017. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.app.Fragment

abstract class Slide: Fragment() {

    abstract var slideListener: SlideListener?

    interface SlideListener {
        fun animationComplete(currentFragment: Fragment)
    }

}
