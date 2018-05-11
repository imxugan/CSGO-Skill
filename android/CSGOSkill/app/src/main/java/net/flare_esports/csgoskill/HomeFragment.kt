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
import java.lang.ref.WeakReference

class HomeFragment : BaseFragment() {

    override lateinit var main: Main
    override var listener: FragmentListener? = null
    override val name: String = "home"

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var handler: Handler? = null

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

    fun displayStats(timeRange: TimeRange) {
        DisplayStats(this).execute(timeRange)
        if (DEV_MODE) Log.d("Home.displayStats", "Running stats with $timeRange")
    }

    private class DisplayStats(me: HomeFragment) : AsyncTask<TimeRange?, Int, String?>() {

        private val myReference: WeakReference<HomeFragment> = WeakReference(me)

        override fun onPreExecute() {
            val me: HomeFragment = myReference.get() ?: return
            me.main.toggleLoader(true)
        }

        override fun onPostExecute(result: String?) {
            val me: HomeFragment = myReference.get() ?: return
            if (result != null) {
                DynamicAlert(me.main, result).setTitle("Aw crap").show()
            }
            me.main.toggleLoader(false)
        }

        // Returns an error message for a DynamicAlert if necessary
        override fun doInBackground(vararg params: TimeRange?): String? {

            try {
                val me: HomeFragment = myReference.get() ?: return null

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

                val timeRange: TimeRange = if (params.isNotEmpty()) params[0] ?: TimeRange()
                else TimeRange()

                val process = ProcessStats()
                val result = process.run(timeRange, me.main) ?:
                (throw process.lastError ?: Throwable("Unexpected error while calculating stats. Please report this."))

                me.viewAdapter = HomeStatsAdapter(result, me.main)
                me.main.runOnUiThread {
                    me.recyclerView = me.recyclerView.apply {
                        setHasFixedSize(true)
                        layoutManager = me.viewManager
                        adapter = me.viewAdapter
                        me.recyclerView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
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
                if (DEV_MODE) Log.e("HomeFragment.DisplayStats", e)
                return e.message
            }


        }
    }

}
