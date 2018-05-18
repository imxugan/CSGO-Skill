package net.flare_esports.csgoskill

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.animation.Animation

/**
 * A simple StatsAdapter wrapper class, to make creating one a small bit easier
 */
abstract class StatsAdapter : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    private var lastPosition: Int = -1

    /** Animation for showing new views, null removes any animation */
    abstract var animation: Animation?

    /**
     * Animates new views as they are added to the bottom of the list, not animating already seen
     * views. It looks better this way, seriously.
     */
    protected fun setAnimation(view: View, position: Int) {
        if (position > lastPosition) {
            if (animation != null) view.startAnimation(animation)
            lastPosition = position
        }
    }

    inner class ViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout) {
        fun clearAnimation() {
            layout.clearAnimation()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.clearAnimation()
    }

}
