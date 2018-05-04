/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat.getColor
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.listener.BarLineChartTouchListener
import com.github.mikephil.charting.utils.ViewPortHandler
import java.util.*
import kotlin.math.roundToInt


internal object Constants {

    const val DEVMODE = true // Set to false before release!!

    //                      v0.1.0
    val VERSION = intArrayOf(0,1,0)

    fun defaultBarLineChart(chartType: String, context: Context): BarLineChartBase<*> {
        // Setup chart
        val chart = when (chartType) {
            "line" -> LineChart(context)
            "bar" -> BarChart(context)
            else -> throw IllegalArgumentException("chartType must be \"line\" or \"bar\"")
        }

        // Rounded Background
        //chart.setBackgroundColor(getColor(context, R.color.transparent))
        chart.background = context.getDrawable(R.drawable.chart_rounded_background)

        // Black grid background
        //chart.setDrawGridBackground(true)
        //chart.setGridBackgroundColor(getColor(context, R.color.black))

        // No border
        chart.setDrawBorders(false)

        // No legend
        chart.legend.isEnabled = false

        // No description
        chart.description.isEnabled = false

        // Charts should be horizontally scrollable ONLY
        chart.isDragYEnabled = false
        chart.isDragXEnabled = true
        chart.setScaleEnabled(false)
        chart.isDoubleTapToZoomEnabled = false

        // Charts should never be totally empty for long
        chart.setNoDataText(context.getString(R.string.loading))

        // Set logging
        chart.isLogEnabled = DEVMODE

        // Disable highlighting
        chart.isHighlightPerDragEnabled = false
        chart.isHighlightPerTapEnabled = false

        // Remove chart padding
        chart.minOffset = 0f

        // Format axis
        val xAxis = chart.xAxis
        val yAxis = chart.axisLeft

        // Disable right axis
        chart.axisRight.isEnabled = false

        xAxis.axisMinimum = 0f
        yAxis.axisMinimum = 0f

        xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE

        yAxis.spaceTop = 11f    // 11% top padding
        yAxis.spaceBottom = 200f // 10% bottom padding

        // Don't draw vertical lines
        xAxis.setDrawGridLines(false)
        xAxis.gridColor = getColor(context, R.color.transparent)

        // Force bottom line
        xAxis.setDrawAxisLine(true)
        xAxis.axisLineColor = getColor(context, R.color.primaryDark)

        // This forces 5 lines across the graph
        yAxis.setLabelCount(6, true)

        // Hide text labels on yAxis, grid lines will still be drawn
        yAxis.setDrawLabels(false)

        // Force left-most line
        yAxis.setDrawAxisLine(true)

        yAxis.gridColor = getColor(context, R.color.primaryDark)
        yAxis.axisLineColor = getColor(context, R.color.primaryDark)

        // Text style
        yAxis.textColor = getColor(context, R.color.transparent)
        xAxis.textColor = getColor(context, R.color.white)
        xAxis.textSize = 10f // this dimension is in DP, and will remain so
        xAxis.typeface = Typeface.create("sans-serif-smallcaps", Typeface.NORMAL)

        // Set value toggle with double tap, and temporary display with single tap
        chart.onTouchListener = object: BarLineChartTouchListener(chart, chart.viewPortHandler.matrixTouch, 3f) {
            // Toggle chart values
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                val sets = chart.data.dataSets
                for (iSet in sets) {
                    val set = iSet as DataSet<*>
                    set.setDrawValues(!set.isDrawValuesEnabled)
                }
                chart.invalidate()

                return super.onSingleTapUp(e)
            }
        }

        return chart
    }

    fun defaultLineDataSet(context: Context): LineDataSet {
        // No label, we'll use our own labels instead of the built-in ones
        val set = LineDataSet(null, "")

        // All graphs are left to right
        set.axisDependency = LEFT

        // We'll manually toggle values when graph is selected/ in focus
        set.setDrawValues(false)

        set.lineWidth = 2.5f // in DP

        // Default to white, often this will be changed
        set.color = getColor(context, R.color.white)
        set.setCircleColor(getColor(context, R.color.white))

        // Default to 2.5f, same as line width, to nicely round lines
        set.circleRadius = 2.5f // in DP

        // Nice large white text
        set.valueTextColor = getColor(context, R.color.white)
        set.valueTextSize = 10f // this dimension is in DP, and will remain so

        set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set.cubicIntensity = 0.5f

        // Disable highlighting
        set.isHighlightEnabled = false

        return set
    }

    fun defaultBarDataSet(context: Context, yVals: List<BarEntry>): BarDataSet {
        // No label, we'll use our won labels instead of the built-in ones
        val set = BarDataSet(yVals, "")

        // All graphs are left to right
        set.axisDependency = LEFT

        // We'll manually toggle values when graph is selected/ in focus
        set.setDrawValues(false)

        // Default to white, often this will be changed
        set.color = getColor(context, R.color.white)

        // Nice large white text
        set.valueTextColor = getColor(context, R.color.white)
        set.valueTextSize = 10f // this dimension is in DP, and will remain so

        // Disable highlighting
        set.isHighlightEnabled = false

        return set
    }

    class DateValueFormatter(startTime: Long): IAxisValueFormatter {

        val start: Calendar = Calendar.getInstance()

        init {
            start.timeInMillis = startTime
        }

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            val result = Calendar.getInstance()
            result.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DATE))
            result.add(Calendar.DATE, value.roundToInt())
            // Months are 0-based, so we must add 1
            return (result.get(Calendar.MONTH) + 1).toString() + "/" + result.get(Calendar.DATE)
        }

    }

    class MonthValueFormatter(startTime: Long): IAxisValueFormatter {

        val start: Calendar = Calendar.getInstance()

        init {
            start.timeInMillis = startTime
        }

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            val result = Calendar.getInstance()
            result.set(start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DATE))
            result.add(Calendar.MONTH, value.roundToInt())
            return result.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
        }

    }

    class KDValueFormatter(val killSet: LineDataSet, val deathSet: LineDataSet): IValueFormatter {

        override fun getFormattedValue(value: Float, entry: Entry, dataSetIndex: Int, viewPortHandler: ViewPortHandler?): String {

            val index = killSet.getEntryIndex(entry.x, entry.y, DataSet.Rounding.CLOSEST)
            val kills = killSet.getEntryForIndex(index).y.toInt()
            val deaths = deathSet.getEntryForIndex(index).y.toInt()

            return if (kills > deaths && dataSetIndex == 0) {
                "$kills - $deaths"
            } else if (deaths > kills && dataSetIndex == 1) {
                "$kills - $deaths"
            } else if (kills == deaths && dataSetIndex == 0){
                "$kills - $deaths"
            } else {
                ""
            }
        }

    }

    class WinLossValueFormatter

}
