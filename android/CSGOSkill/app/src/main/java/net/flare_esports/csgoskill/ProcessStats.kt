package net.flare_esports.csgoskill

import org.json.JSONArray
import org.json.JSONObject

class ProcessStats {

    var lastError: Throwable? = null

    fun run(timeRange: TimeRange, main: Main) : JSONObject? {

        try {

            if (!main.hasPlayer) throw Throwable("Main did not have player, make sure you wait until it does before calling me!")

            fun add(v: String, a: JSONObject, b: JSONObject): Int {
                return a.optInt(v) + b.optInt(v)
            }

            fun combine(current: JSONObject, entries: JSONArray): JSONObject {
                val size = entries.length()
                var flag = false
                var entry: JSONObject
                var i = 0
                var result = current

                while (i < size) {
                    entry = entries.getJSONObject(i)
                    i++

                    if (!timeRange.inRange(Constants.ServerTime.parse(entry.getString("date")).time)) {
                        // This makes it so that if we go inRange(), and then back out, it will stop
                        // processing because no other dates should be within the time range.
                        if (flag) {
                            break
                        }
                        continue
                    }

                    flag = true

                    result = result
                            .put("time", add("time", current, entry))
                            .put("kills", add("kills", current, entry))
                            .put("deaths", add("deaths", current, entry))
                            .put("shots", add("shots", current, entry))
                            .put("hits", add("hits", current, entry))
                            .put("heads", add("heads", current, entry))
                            .put("damage", add("damage", current, entry))
                            .put("rounds", add("rounds", current, entry))
                            .put("matches", add("matches", current, entry))
                            .put("rwins", add("rwins", current, entry))
                            .put("mwins", add("mwins", current, entry))
                            .put("plants", add("plants", current, entry))
                            .put("defuse", add("defuse", current, entry))
                            .put("hostage", add("hostage", current, entry))
                            .put("mvps", add("mvps", current, entry))
                            .put("contrib", add("contrib", current, entry))
                            .put("money", add("money", current, entry))
                }

                return result

            }

            val stats: JSONObject = main.getHistoryStats() ?: throw Throwable("Unable to get player stats. Please report this.")

            var total = JSONObject()
            total = combine(total, stats.getJSONArray("daily"))
            total = combine(total, stats.getJSONArray("monthly"))

            return total

        } catch (e: Throwable) {
            lastError = e
            return null
        }

    }

}
