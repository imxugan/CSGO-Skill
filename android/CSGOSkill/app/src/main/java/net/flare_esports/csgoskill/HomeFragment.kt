/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils

import net.flare_esports.csgoskill.Constants.DEV_MODE
import net.flare_esports.csgoskill.Constants.ServerTime
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference

class HomeFragment : BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "home"

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var handler: Handler? = null

    private var timeSpecial: Int = Main.TIME_TODAY

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
        viewManager = LinearLayoutManager(main)
        recyclerView = view.findViewById(R.id.statsRecyclerView)
    }

    override fun onBack(): Boolean {
        //TODO
        return false
    }

    fun displayStats(timeRange: TimeRange?, special: Int) {
        timeSpecial = special
        ProcessStats(this).execute(timeRange)
        if (DEV_MODE) Log.d("Home.displayStats", "Running stats with $timeRange and index $special")
    }

    private class ProcessStats(me: HomeFragment) : AsyncTask<TimeRange?, Int, String?>() {

        private val myReference: WeakReference<HomeFragment> = WeakReference(me)

        override fun onPostExecute(result: String?) {
            val me: HomeFragment = myReference.get() ?: return
            if (result != null) {
                DynamicAlert(me.main, result).setTitle("Aw crap").show()
            }
        }

        // Returns an error message for a DynamicAlert if necessary
        override fun doInBackground(vararg params: TimeRange?) : String? {

            val me: HomeFragment = myReference.get() ?: return null

            me.main.toggleLoader(true)

            for (i in 1..3) {
                if (me.main.hasPlayer)
                    break
                if (i == 3) // Break, this is the last run
                    break
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    // AsyncTask canceled, bail out
                    return null
                }
            }

            if (!me.main.hasPlayer) return null

            try {
                val stats: JSONObject?
                val entries: JSONArray
                var historyStats = JSONObject()

                fun add(v: String, a: JSONObject, b: JSONObject): Int {
                    return a.optInt(v) + b.optInt(v)
                }

                if ((params.isEmpty() || params[0] == null) && me.timeSpecial == Main.TIME_TODAY) {
                    stats = me.main.getCurrentStats()

                    if (stats == null) {
                        throw Throwable("Unable to get player stats. Please report this.")
                    }

                    historyStats = historyStats
                            .put("time", stats.optInt("time"))
                            .put("kills", stats.optInt("kills"))
                            .put("deaths", stats.optInt("deaths"))
                            .put("shots", stats.optInt("shots"))
                            .put("hits", stats.optInt("hits"))
                            .put("heads", stats.optInt("heads"))
                            .put("damage", stats.optInt("damage"))
                            .put("rounds", stats.optInt("rounds"))
                            .put("matches", stats.optInt("matches"))
                            .put("rwins", stats.optInt("rwins"))
                            .put("mwins", stats.optInt("mwins"))
                            .put("plants", stats.optInt("plants"))
                            .put("defuse", stats.optInt("defuse"))
                            .put("hostage", stats.optInt("hostage"))
                            .put("mvps", stats.optInt("mvps"))
                            .put("contrib", stats.optInt("contrib"))
                            .put("money", stats.optInt("money"))

                } else {

                    // Default TimeRange is everything
                    val timeRange: TimeRange = if (params.isNotEmpty()) {
                        params[0] ?: TimeRange()
                    } else {
                        TimeRange()
                    }

                    stats = me.main.getHistoryStats()

                    if (stats == null) {
                        throw Throwable("Unable to get player stats. Please report this.")
                    }

                    entries = if (me.timeSpecial == Main.TIME_YEAR)
                                  stats.getJSONArray("monthly")
                              else
                                  stats.getJSONArray("daily")

                    var i = 0
                    var day : JSONObject

                    while (i < entries.length()) {
                        day = entries.getJSONObject(i)
                        i++
                        if (!timeRange.inRange(ServerTime.parse(day.getString("date")).time))
                            continue

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
                    }

                }

                me.viewAdapter = HomeStatsAdapter(historyStats, me.main)
                me.main.runOnUiThread {
                    me.recyclerView = me.recyclerView.apply {
                        setHasFixedSize(true)
                        layoutManager = me.viewManager
                        adapter = me.viewAdapter
                        me.recyclerView.viewTreeObserver.addOnPreDrawListener( object : ViewTreeObserver.OnPreDrawListener {
                            override fun onPreDraw(): Boolean {
                                me.recyclerView.viewTreeObserver.removeOnPreDrawListener(this)

                                for (j in 0 until me.recyclerView.childCount) {
                                    val v = me.recyclerView.getChildAt(j)
                                    val animation: Animation = AnimationUtils.loadAnimation(me.main, android.R.anim.slide_in_left)
                                    animation.startOffset = j * 200L
                                    v.startAnimation(animation)
                                }

                                return true
                            }
                        })
                    }
                }

                return null

            } catch (e: Throwable) {
                if (DEV_MODE) Log.e("HomeFragment.ProcessStats", e)
                return e.message ?: "Unexpected error while calculating stats. Please report this."

            } finally {
                me.main.toggleLoader(false)
            }

        }

    }

}
