/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.animation.ObjectAnimator
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext

class StatsAdapter(private val data: JSONObject, private val main: Main) :
        RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    companion object {

        // Number-Type conversions
        @JvmStatic val TIME_PLAYED    = 0
        @JvmStatic val KILLS_DEATHS   = 1
        @JvmStatic val ACCURACY_HEADS = 2
        @JvmStatic val ROUND_MATCH    = 3
        @JvmStatic val OBJECTIVE      = 4
        @JvmStatic val SCORING        = 5
        @JvmStatic val DAMAGE         = 6
        @JvmStatic val MONEY          = 7
        @JvmStatic val LIST_LENGTH    = 8

    }

    private var lastPosition: Int = -1

    inner class ViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout) {
        fun clearAnimation() {
            layout.clearAnimation()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsAdapter.ViewHolder {
        // Use the the base, we'll fill the insides later
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.statsv_base, parent, false) as ConstraintLayout

        return ViewHolder(layout)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        holder.clearAnimation()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val inflater = main.layoutInflater
        val container = holder.layout.findViewById<ConstraintLayout>(R.id.statsvBaseView)
        val layout: View
        when (position) {
            TIME_PLAYED -> {
                layout = inflater.inflate(R.layout.statsv_time_played, container, false)

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

                container.addView(layout)
            }

            KILLS_DEATHS -> {
                layout = inflater.inflate(R.layout.statsv_kd, container, false)

                val killsNumber = layout.findViewById<AppCompatTextView>(R.id.killsNumber)
                val deathsNumber = layout.findViewById<AppCompatTextView>(R.id.deathsNumber)
                val graphNumber = layout.findViewById<AppCompatTextView>(R.id.graphKDNumber)
                val graph = layout.findViewById<ProgressBar>(R.id.graphKD)

                val kills = Math.max(0, data.optInt("kills"))
                val deaths = Math.max(0, data.optInt("deaths"))
                val ratio = BigDecimal((kills * 1.0) / Math.max(1.0, deaths * 1.0)).round(MathContext(3)).toDouble().toString()
                val progress = Math.max(0, Math.min(graph.max, ((kills * 1.0) / (kills + deaths) * graph.max).toInt()))

                killsNumber.text = kills.toString()
                deathsNumber.text = deaths.toString()
                graphNumber.text = if (ratio.length > 3) ratio else "${ratio}0"

                val rot = ((graph.max / 2f) - progress) / graph.max.toFloat() * 180f
                val aP = ObjectAnimator.ofInt(graph, "progress", 0, progress)
                val aR = ObjectAnimator.ofFloat(graph, "rotation", 90f, rot)
                aP.interpolator = DecelerateInterpolator()
                aR.interpolator = DecelerateInterpolator()
                aP.setDuration(1000).start()
                aR.setDuration(1000).start()

                container.addView(layout)
            }

            ACCURACY_HEADS -> {
                layout = inflater.inflate(R.layout.statsv_acc_hs, container, false)

                val accuracyNumber = layout.findViewById<AppCompatTextView>(R.id.accuracyNumber)
                val headshotNumber = layout.findViewById<AppCompatTextView>(R.id.headshotNumber)

                val hits = Math.max(0, data.optInt("hits"))
                val shots = Math.max(0, data.optInt("shots"))
                val accuracy = BigDecimal((hits * 1.0) / Math.max(1.0, shots + hits * 1.0) * 100).round(MathContext(3)).toDouble().toString()

                val heads = Math.max(0, data.optInt("heads"))
                val kills = Math.max(0, data.optInt("kills"))
                val headshot = BigDecimal((heads * 1.0) / Math.max(1.0, heads + kills * 1.0) * 100).round(MathContext(3)).toDouble().toString()

                accuracyNumber.text = "$accuracy%"
                headshotNumber.text = "$headshot%"

                container.addView(layout)
            }

            ROUND_MATCH -> {
                layout = inflater.inflate(R.layout.statsv_win, container, false)

                val roundWinsNumber = layout.findViewById<AppCompatTextView>(R.id.roundWins)
                val roundPlaysNumber = layout.findViewById<AppCompatTextView>(R.id.roundPlays)
                val roundPercentNumber = layout.findViewById<AppCompatTextView>(R.id.roundPercent)

                val matchWinsNumber = layout.findViewById<AppCompatTextView>(R.id.matchWins)
                val matchPlaysNumber = layout.findViewById<AppCompatTextView>(R.id.matchPlays)
                val matchPercentNumber = layout.findViewById<AppCompatTextView>(R.id.matchPercent)

                val roundWins = Math.max(0, data.optInt("rwins"))
                val roundPlay = Math.max(0, data.optInt("rounds"))
                val matchWins = Math.max(0, data.optInt("mwins"))
                val matchPlay = Math.max(0, data.optInt("matches"))

                val roundPercent = BigDecimal((roundWins * 1.0) / Math.max(1.0, roundPlay * 1.0) * 100).round(MathContext(3)).toDouble().toString()
                val matchPercent = BigDecimal((matchWins * 1.0) / Math.max(1.0, matchPlay * 1.0) * 100).round(MathContext(3)).toDouble().toString()

                roundWinsNumber.text = roundWins.toString()
                roundPlaysNumber.text = roundPlay.toString()
                roundPercentNumber.text = "$roundPercent%"

                matchWinsNumber.text = matchWins.toString()
                matchPlaysNumber.text = matchPlay.toString()
                matchPercentNumber.text = "$matchPercent%"

                container.addView(layout)
            }

            OBJECTIVE -> {
                layout = inflater.inflate(R.layout.statsv_objective, container, false)

                val plantNumber = layout.findViewById<AppCompatTextView>(R.id.plantNumber)
                val defuseNumber = layout.findViewById<AppCompatTextView>(R.id.defuseNumber)
                val hostageNumber = layout.findViewById<AppCompatTextView>(R.id.rescueNumber)

                val plants = Math.max(0, data.optInt("plants"))
                val defuse = Math.max(0, data.optInt("defuse"))
                val rescues = Math.max(0, data.optInt("hostage"))

                plantNumber.text = plants.toString()
                defuseNumber.text = defuse.toString()
                hostageNumber.text = rescues.toString()

                container.addView(layout)
            }

            SCORING -> {
                layout = inflater.inflate(R.layout.statsv_score, container, false)

                val mvpNumber = layout.findViewById<AppCompatTextView>(R.id.mvpNumber)
                val scoreNumber = layout.findViewById<AppCompatTextView>(R.id.scoreNumber)

                val mvps = Math.max(0, data.optInt("mvps"))
                val score = Math.max(0, data.optInt("contrib"))

                mvpNumber.text = mvps.toString()
                scoreNumber.text = score.toString()

                container.addView(layout)
            }

            DAMAGE -> {
                layout = inflater.inflate(R.layout.statsv_damage, container, false)

                val damageNumber = layout.findViewById<AppCompatTextView>(R.id.damageNumber)

                val damage = Math.max(0, data.optInt("damage"))

                damageNumber.text = damage.toString()

                container.addView(layout)
            }

            MONEY -> {
                layout = inflater.inflate(R.layout.statsv_money, container, false)

                val moneyNumber = layout.findViewById<AppCompatTextView>(R.id.moneyNumber)

                val money = Math.max(0, data.optInt("money")).toString()

                moneyNumber.text = "\$$money"

                container.addView(layout)
            }
        }
        setAnimation(holder.layout, position)
    }

    private fun setAnimation(view: View, position: Int) {
        if (position > lastPosition) {
            val animation: Animation = AnimationUtils.loadAnimation(main, android.R.anim.slide_in_left)
            view.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = LIST_LENGTH

}
