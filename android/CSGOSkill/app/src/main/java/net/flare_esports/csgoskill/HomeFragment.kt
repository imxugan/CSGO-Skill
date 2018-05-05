/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar

import kotlinx.android.synthetic.main.fragment_home.*

import net.flare_esports.csgoskill.Constants.DEVMODE
import org.json.JSONObject
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "home"

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var handler: Handler? = null
    private val displayStats = Runnable { this.displayStats() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        main = context as Main
        listener = context
        handler = Handler()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        handler?.removeCallbacksAndMessages(null)
        handler = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*val kdChart = view.findViewById(R.id.graph) as ProgressBar
        val kills = 256
        val deaths = 200
        val percentage = (kills * 1000) / (kills + deaths)
        handler?.postDelayed({animateProgressCircle(kdChart, percentage)}, 500)*/

        viewManager = LinearLayoutManager(main)
        recyclerView = view.findViewById(R.id.statsRecyclerView)
        Thread(displayStats).start()
    }

    override fun onBack(): Boolean {
        //TODO
        return false
    }

    @SuppressLint("SimpleDateFormat")
    fun displayStats() {
        try {
            Looper.prepare()
        } catch (e: RuntimeException) {
            // Don't worry about this, the Main fragment may have called this twice and would create
            // RuntimeException: Only one Looper may be created per thread
            Log.e("HomeFragment.displayStats()", e)
        }
        var stats: JSONObject? = null

        // Attempt 3 times to get the stats
        main.toggleLoader(true)
        for (i in 1..3) {
            stats = main.getHistoryStats()
            if (stats != null)
                break
            if (i == 3) // Break, this is the last run
                break
            Thread.sleep(500)
        }
        main.toggleLoader(false)

        if (stats == null) {
            // TODO: report error
            return
        }

        try {
            val daily = stats.getJSONArray("daily")
            var historyStats = JSONObject()
            var i = 0
            var day : JSONObject
            var date: Long
            val twoWeeks = 0 //System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000

            // UTC setup
            val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            df.timeZone = TimeZone.getTimeZone("UTC")

            fun add(v: String, a: JSONObject, b: JSONObject): Int {
                return a.optInt(v) + b.optInt(v)
            }

            while (i < daily.length()) {
                day = daily.getJSONObject(i)
                date = df.parse(day.getString("date")).time
                if (date < twoWeeks)
                    break // All remaining entries are too far back
                historyStats = historyStats
                        .put("time", add("time", day, historyStats))
                        .put("kills", add("kills", day, historyStats))
                        .put("deaths", add("deaths", day, historyStats))
                        .put("shots", add("shots", day, historyStats))
                        .put("hits", add("hits", day, historyStats))
                        .put("heads", add("heads", day, historyStats))
                        .put("damage", add("damage", day, historyStats))
                        .put("rounds", add("rounds", day, historyStats))
                        .put("matches", add("matches", day, historyStats))
                        .put("rwins", add("rwins", day, historyStats))
                        .put("mwins", add("mwins", day, historyStats))
                        .put("plants", add("plants", day, historyStats))
                        .put("defuse", add("defuse", day, historyStats))
                        .put("hostage", add("hostage", day, historyStats))
                        .put("mvps", add("mvps", day, historyStats))
                        .put("contrib", add("contrib", day, historyStats))
                        .put("money", add("money", day, historyStats))
                i++
            }

            viewAdapter = StatsAdapter(historyStats, main)
            main.runOnUiThread {
                recyclerView = recyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = viewManager
                    adapter = viewAdapter
                    recyclerView.viewTreeObserver.addOnPreDrawListener(
                            object : ViewTreeObserver.OnPreDrawListener {

                                override fun onPreDraw(): Boolean {
                                    recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                                    for (j in 0 until recyclerView.childCount) {
                                        val v = recyclerView.getChildAt(j)
                                        val animation: Animation = AnimationUtils.loadAnimation(main, android.R.anim.slide_in_left)
                                        animation.startOffset = j * 80L
                                        v.startAnimation(animation)
                                    }

                                    return true
                                }
                            })
                }
            }
        } catch (e: Throwable) {
            if (DEVMODE) Log.e("Main.loginPlayer", e)
            val m = e.message ?: "Unexpected error while calculating stats. Please report this."
            DynamicAlert(main, m).setTitle("Aw crap").show()
        }
    }

    /* fun animateProgressCircle(chart: ProgressBar, progress: Int, rotationOffset: Int, duration: Long = 1000) {
        val rot = ((chart.max / 2f) - progress) / chart.max.toFloat() * 180f
        val aP = ObjectAnimator.ofInt(chart, "progress", chart.progress, progress)
        val aR = ObjectAnimator.ofFloat(chart, "rotation", 90f + rotationOffset, rot + rotationOffset)
        aP.interpolator = DecelerateInterpolator()
        aR.interpolator = DecelerateInterpolator()
        aP.setDuration(duration).start()
        aR.setDuration(duration).start()
    } */

}
