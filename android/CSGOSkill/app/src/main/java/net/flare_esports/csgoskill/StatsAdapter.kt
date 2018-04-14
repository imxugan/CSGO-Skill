/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.json.JSONObject

class StatsAdapter(private val data: JSONObject, private val main: Main) :
        RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    companion object {

        // Number-Type conversions
        @JvmStatic val TIME_PLAYED    = 0
        @JvmStatic val KILLS_DEATHS   = 1
        @JvmStatic val ACCURACY_HEADS = 2
        @JvmStatic val DAMAGE         = 3
        @JvmStatic val ROUND_MATCH    = 4
        @JvmStatic val OBJECTIVE      = 5
        @JvmStatic val SCORING        = 6
        @JvmStatic val MONEY          = 7

    }

    inner class ViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsAdapter.ViewHolder {
        // Use the the base, we'll fill the insides later
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.statsv_base, parent, false) as ConstraintLayout

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position) {
            TIME_PLAYED -> {
                val inflater = main.layoutInflater
                val layout = inflater.inflate(R.layout.statsv_time_played, holder.layout, false)

                val hoursNumber = layout.findViewById<AppCompatTextView>(R.id.hoursNumber)
                val minutesNumber = layout.findViewById<AppCompatTextView>(R.id.minutesNumber)
                val secondsNumber = layout.findViewById<AppCompatTextView>(R.id.secondsNumber)

                val hoursLabel = layout.findViewById<AppCompatTextView>(R.id.hoursLabel)
                val minutesLabel = layout.findViewById<AppCompatTextView>(R.id.minutesLabel)
                val secondsLabel = layout.findViewById<AppCompatTextView>(R.id.secondsLabel)

                val days = (data.optInt("time") / 60 / 60 / 24).toString()
                val hours = (data.optInt("time") / 60 / 60 % 24).toString()
                val minutes = (data.optInt("time") / 60 % 60).toString()
                val seconds = (data.optInt("time") % 60).toString()

                if (days.toInt() > 0) {
                    hoursNumber.text = if (days.length > 1) days else "0$days"
                    minutesNumber.text = if (hours.length > 1) hours else "0$hours"
                    secondsNumber.text = if (minutes.length > 1) minutes else "0$minutes"
                    hoursLabel.text = "days"
                    minutesLabel.text = "hours"
                    secondsLabel.text = "minutes"
                } else {
                    hoursNumber.text = if (hours.length > 1) hours else "0$hours"
                    minutesNumber.text = if (minutes.length > 1) minutes else "0$minutes"
                    secondsNumber.text = if (seconds.length > 1) seconds else "0$seconds"
                }

                holder.layout.addView(layout)
            }
        }
    }

    override fun getItemCount(): Int = 7

}
