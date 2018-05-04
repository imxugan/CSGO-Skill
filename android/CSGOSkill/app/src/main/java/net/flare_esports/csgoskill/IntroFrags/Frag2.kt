/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill.IntroFrags

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.content.ContextCompat.getColor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*

import net.flare_esports.csgoskill.R
import kotlinx.android.synthetic.main.fragment_introslide2.*
import net.flare_esports.csgoskill.Constants
import java.util.*

class Frag2 : Slide() {

    internal lateinit var view: View
    internal lateinit var context: Context
    override var slideListener: SlideListener? = null
    override val name: String = "slide2"

    private lateinit var topChart: LineChart
    private lateinit var bottomChart: BarChart

    private var handler: Handler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        slideListener = context as SlideListener
        handler = Handler()
    }

    override fun onDetach() {
        super.onDetach()
        slideListener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_introslide2, container, false)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        // Create charts
        topChart = Constants.defaultBarLineChart("line", context) as LineChart
        bottomChart = Constants.defaultBarLineChart("bar", context) as BarChart

        topChart.id = View.generateViewId()
        bottomChart.id = View.generateViewId()

        val params = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        topChart.layoutParams = params
        bottomChart.layoutParams = params

        // Create sets
        val killSet = Constants.defaultLineDataSet(context)
        killSet.color = getColor(context, R.color.blue)
        killSet.addEntry(Entry(0.5f, 56f))
        killSet.addEntry(Entry(1.5f, 41f))
        killSet.addEntry(Entry(2.5f, 28f))
        killSet.addEntry(Entry(3.5f, 51f))
        killSet.addEntry(Entry(4.5f, 36f))
        killSet.addEntry(Entry(5.5f, 58f))
        killSet.addEntry(Entry(6.5f, 75f))
        killSet.addEntry(Entry(7.5f, 62f))
        killSet.addEntry(Entry(8.5f, 22f))
        killSet.addEntry(Entry(9.5f, 36f))
        killSet.addEntry(Entry(10.5f, 32f))
        killSet.addEntry(Entry(11.5f, 51f))
        killSet.addEntry(Entry(12.5f, 44f))
        killSet.addEntry(Entry(13.5f, 76f))

        val deathSet = Constants.defaultLineDataSet(context)
        deathSet.color = getColor(context, R.color.red)
        deathSet.addEntry(Entry(0.5f, 45f))
        deathSet.addEntry(Entry(1.5f, 56f))
        deathSet.addEntry(Entry(2.5f, 22f))
        deathSet.addEntry(Entry(3.5f, 44f))
        deathSet.addEntry(Entry(4.5f, 40f))
        deathSet.addEntry(Entry(5.5f, 57f))
        deathSet.addEntry(Entry(6.5f, 50f))
        deathSet.addEntry(Entry(7.5f, 70f))
        deathSet.addEntry(Entry(8.5f, 29f))
        deathSet.addEntry(Entry(9.5f, 34f))
        deathSet.addEntry(Entry(10.5f, 42f))
        deathSet.addEntry(Entry(11.5f, 39f))
        deathSet.addEntry(Entry(12.5f, 27f))
        deathSet.addEntry(Entry(13.5f, 52f))

        val roundValues = ArrayList<BarEntry>()
        roundValues.add(BarEntry(0.5f, floatArrayOf(-143f, 176f)))
        roundValues.add(BarEntry(1.5f, floatArrayOf(-179f, 207f)))
        roundValues.add(BarEntry(2.5f, floatArrayOf(-101f, 170f)))
        roundValues.add(BarEntry(3.5f, floatArrayOf(-144f, 211f)))
        roundValues.add(BarEntry(4.5f, floatArrayOf(-205f, 214f)))
        roundValues.add(BarEntry(5.5f, floatArrayOf(-137f, 222f)))
        roundValues.add(BarEntry(6.5f, floatArrayOf(-88f, 128f)))
        roundValues.add(BarEntry(7.5f, floatArrayOf(-125f, 172f)))
        roundValues.add(BarEntry(8.5f, floatArrayOf(-105f, 133f)))
        roundValues.add(BarEntry(9.5f, floatArrayOf(-195f, 196f)))
        roundValues.add(BarEntry(10.5f, floatArrayOf(-161f, 199f)))
        roundValues.add(BarEntry(11.5f, floatArrayOf(-59f, 76f)))

        val roundSet = Constants.defaultBarDataSet(context, roundValues)
        roundSet.setColors(getColor(context, R.color.red), getColor(context, R.color.green))

        topChart.data = LineData(killSet, deathSet)
        topChart.data.setValueFormatter(Constants.KDValueFormatter(killSet, deathSet))
        bottomChart.data = BarData(roundSet)

        var now = Calendar.getInstance()
        now.add(Calendar.DATE, -14)
        topChart.xAxis.valueFormatter = Constants.DateValueFormatter(now.timeInMillis)
        topChart.xAxis.granularity = 1f
        topChart.xAxis.isGranularityEnabled = true
        topChart.xAxis.setCenterAxisLabels(true)
        topChart.xAxis.axisMinimum = 0.2f
        topChart.xAxis.axisMaximum = 13.8f
        topChart.setVisibleXRange(5.3f, 5.3f)

        now = Calendar.getInstance()
        now.add(Calendar.MONTH, -11)
        bottomChart.xAxis.valueFormatter = Constants.MonthValueFormatter(now.timeInMillis)
        bottomChart.xAxis.granularity = 1f
        bottomChart.xAxis.isGranularityEnabled = true
        bottomChart.xAxis.setCenterAxisLabels(true)
        bottomChart.axisLeft.resetAxisMinimum() // Reset 0 minimum
        bottomChart.axisLeft.spaceBottom = 11f
        bottomChart.setVisibleXRange(6.3f, 6.3f)
        bottomChart.setDrawValueAboveBar(false)

        handler?.postDelayed({
            exampleChartHolderOne.addView(topChart)
            exampleChartHolderOne.visibility = View.VISIBLE
            exampleChartHolderOne.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_medium))
            topChart.animateX(1500, Easing.EasingOption.Linear)
            topChart.moveViewToAnimated(8.5f, 0f, YAxis.AxisDependency.LEFT, 1650)
        }, 1200)

        handler?.postDelayed({
            exampleChartHolderTwo.addView(bottomChart)
            exampleChartHolderTwo.visibility = View.VISIBLE
            exampleChartHolderTwo.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in_medium))
            bottomChart.animateX(1500)
            bottomChart.moveViewToAnimated(6f, 0f, YAxis.AxisDependency.LEFT, 1650)
        }, 2000)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handler?.postDelayed({ slideListener?.animationComplete(this) }, 1500)
    }
}
