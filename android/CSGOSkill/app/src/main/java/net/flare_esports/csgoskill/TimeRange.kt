package net.flare_esports.csgoskill

import java.util.*
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR

class TimeRange {

    private val start: Calendar
    private val end: Calendar

    constructor() {
        start = Calendar.getInstance()
        // Fix to earliest time
        start.timeInMillis = 0

        end = Calendar.getInstance()
        // Fix to start of next day
        end.add(DAY_OF_MONTH, 1)
        end.set(
                end.get(YEAR),
                end.get(MONTH),
                end.get(DAY_OF_MONTH),
                0,
                0,
                0
        )
    }

    /**
     * Creates a TimeRange object, from [daysInPast] to now.
     *
     * @param daysInPast number of days in the past
     */
    constructor(daysInPast: Int) : this(daysInPast, daysInPast)

    /**
     * Creates a TimeRange object, from [daysInPast] to [range] days later
     *
     * @param daysInPast number of days in the past
     * @param range range of days
     */
    constructor(daysInPast: Int, range: Int) {
        start = Calendar.getInstance()

        // Make days negative
        val days: Int = if (daysInPast > 0) -daysInPast else daysInPast

        val rng: Int = when {
            range > -days -> -days // If range is greater than days, then go until the end of today
            range < 0 -> -range    // Make range positive
            else -> range
        }

        start.add(DAY_OF_MONTH, days)
        // Fix to START of day
        start.set(
                start.get(YEAR),
                start.get(MONTH),
                start.get(DAY_OF_MONTH),
                0,
                0,
                0
        )

        end = Calendar.getInstance()
        // Fix to end of last day
        end.add(DAY_OF_MONTH, days + rng)
        end.set(
                end.get(YEAR),
                end.get(MONTH),
                end.get(DAY_OF_MONTH),
                23,
                59,
                0
        )
    }

    constructor(start: Calendar, end: Calendar) {
        this.start = start
        this.end = end
    }

    fun inRange(time: Long): Boolean {
        return time in start.timeInMillis..end.timeInMillis
    }

    override fun toString(): String {
        return "TimeRange(start=${start.timeInMillis}, end=${end.timeInMillis})"
    }

}
