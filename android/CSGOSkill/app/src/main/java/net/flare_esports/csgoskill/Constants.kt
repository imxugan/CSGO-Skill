/*
 * Created by the Dev Team for CSGO Skill.
 * Copyright (c) 2018. All rights reserved.
 */

package net.flare_esports.csgoskill

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.support.annotation.ArrayRes
import android.support.v4.content.ContextCompat.getColor
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


internal object Constants {

    const val DEV_MODE = true // Set to false before release!!

    //                      v0.1.0
    val VERSION = intArrayOf(0,1,0)
    fun getVersion(): String {
        return "v${VERSION[0]}.${VERSION[1]}.${VERSION[2]}"
    }

    /* // ERROR CONSTANTS // */

    /** No internet connection is available */
    const val NO_INTERNET = "no-internet"

    /** Internet may be available, but CSGO Skill servers aren't responding */
    const val NO_API = "no-api"

    /** Internet request returned an empty response */
    const val NO_RESPONSE = "no-response"

    /** Indicates a profile was not found, either on the device or on the server */
    const val NOT_FOUND = "not-found"

    /** An update failed, typically in the database */
    const val UPDATE_FAIL = "update-fail"

    /** An internet request failed so spectacularly that we have no information about what happened */
    const val REQUEST_FAIL = "request-fail"

    /** A plain internet request timed out */
    const val REQUEST_TIMEOUT = "request-timeout"


    @SuppressLint("SimpleDateFormat")
    val ServerTimeFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    init {
        if (DEV_MODE) Log.d("Constants", "initializing Constants.kt")

        ServerTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * A basic spinner class for the app, dark theme
     */
    class DarkSpinner : BaseAdapter {

        private val items: Array<String>
        private val size: Int
        private val inflater: LayoutInflater

        private val dropdown: Int = R.layout.spinner_dropdown
        private val listItem: Int = R.layout.spinner_view

        @Suppress("ConvertSecondaryConstructorToPrimary")
        constructor(activity: Activity, @ArrayRes arrayRes: Int) {
            items = activity.resources.getStringArray(arrayRes)
            size = items.size
            inflater = activity.layoutInflater
        }

        override fun getCount(): Int = size

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return createView(position, convertView, parent, dropdown)
        }

        override fun getItem(position: Int): Any {
            return items[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            return createView(position, convertView, parent, listItem)
        }

        private fun createView(position: Int, convertView: View?, parent: ViewGroup?, resource: Int): View {
            val view: View = convertView ?: inflater.inflate(resource, parent, false)
            val text = view as TextView

            val item: Any = getItem(position)
            if (item is String) {
                text.text = item
            } else {
                text.text = item.toString()
            }

            return view
        }

    }

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
        chart.isLogEnabled = DEV_MODE

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

    fun defaultBarDataSet(context: Context, yValues: List<BarEntry>): BarDataSet {
        // No label, we'll use our won labels instead of the built-in ones
        val set = BarDataSet(yValues, "")

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

    class KDValueFormatter(private val killSet: LineDataSet, private val deathSet: LineDataSet): IValueFormatter {

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

}
