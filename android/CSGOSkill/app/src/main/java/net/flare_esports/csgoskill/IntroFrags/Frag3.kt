/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import kotlinx.android.synthetic.main.fragment_introslide3.*
import net.flare_esports.csgoskill.Animer
import net.flare_esports.csgoskill.R

class Frag3 : Slide() {

    internal lateinit var view: View
    internal lateinit var context: Context
    override var slideListener: SlideListener? = null
    override val name: String = "slide3"

    private var adapter: ViewPagerAdapter = ViewPagerAdapter()
    private var handler: Handler? = null
    private var images: IntArray = intArrayOf(R.drawable.intro_tip1, R.drawable.intro_tip2)


    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        slideListener = context as SlideListener
        handler = Handler()
    }

    override fun onDetach() {
        super.onDetach()
        slideListener = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_introslide3, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Handler().postDelayed({ slideListener?.animationComplete(this) }, 1500)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {

        tip_slider.adapter = adapter

        // Simulate drag to next image
        handler?.postDelayed({
            tip_slider?.beginFakeDrag()

            val animation = ObjectAnimator.ofInt(adapter, "fakeDrag", 0, -300)
            animation.duration = 1200
            animation.addListener( Animer { tip_slider?.endFakeDrag(); adapter.currentOffset = 0 })
            animation.start()
        }, 1800)

        // Simulate drag back to first image
        handler?.postDelayed({
            tip_slider?.beginFakeDrag()

            val animation = ObjectAnimator.ofInt(adapter, "fakeDrag", 0, 300)
            animation.duration = 1200
            animation.addListener( Animer { tip_slider?.endFakeDrag(); adapter.currentOffset = 0 })
            animation.start()
        }, 3250)
    }

    inner class ViewPagerAdapter : PagerAdapter() {
        var currentOffset: Int = 0

        // This method is used by the ObjectAnimator created to demonstrate the view pager
        @Suppress("unused")
        fun setFakeDrag(progress: Int) {
            if (tip_slider?.isFakeDragging == true) {
                val offset = (((tip_slider?.width ?: 0) / 100.0) * progress).toInt()
                val dragBy = offset - currentOffset

                tip_slider?.fakeDragBy(dragBy.toFloat())

                currentOffset = offset
            }
        }

        override fun getCount(): Int {
            return images.size
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view == obj
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {

            val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
            val img = ImageView(context)
            img.layoutParams = params
            img.setImageResource(images[position])

            container.addView(img)
            return img
        }
        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

    }

}
