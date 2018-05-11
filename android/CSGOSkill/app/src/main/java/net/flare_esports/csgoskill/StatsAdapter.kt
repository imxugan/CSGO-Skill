package net.flare_esports.csgoskill

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView

abstract class StatsAdapter : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    inner class ViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout) {
        fun clearAnimation() {
            layout.clearAnimation()
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.clearAnimation()
    }

}
