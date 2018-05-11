package net.flare_esports.csgoskill

import org.json.JSONArray
import org.json.JSONObject

class ProcessStats {

    var lastError: Throwable? = null

    private val keys: Array<String> = arrayOf(
            "kills", "deaths", "time", "heads", "rwins", "rounds", "mwins", "matches", "shots", "hits",
            "damage", "plants", "defuse", "hostage", "contrib", "money", "mvps", "knife", "nades",
            "fires", "flash", "csnipe", "backfire", "doms", "revs", "overkill", "pistolwin", "donation",
            "wins_cbble", "wins_dust2", "wins_inferno", "wins_nuke", "wins_train", "rnds_cbble",
            "rnds_dust2", "rnds_inferno", "rnds_nuke", "rnds_train",

            "g0s", "g0h", "g0k", "g2s", "g2h", "g2k", "g3s", "g3h", "g3k", "g4s", "g4h", "g4k",
            "g5s", "g5h", "g5k", "g6s", "g6h", "g6k", "g8s", "g8h", "g8k", "g10s", "g10h", "g10k",
            "g11s", "g11h", "g11k", "g12s", "g12h", "g12k", "g14s", "g14h", "g14k", "g15s", "g15h", "g15k",
            "g16s", "g16h", "g16k", "g17s", "g17h", "g17k", "g18s", "g18h", "g18k", "g19s", "g19h", "g19k",
            "g20s", "g20h", "g20k", "g21s", "g21h", "g21k", "g22s", "g22h", "g22k", "g23s", "g23h", "g23k",
            "g24s", "g24h", "g24k", "g25s", "g25h", "g25k", "g26s", "g26h", "g26k", "g27s", "g27h", "g27k",
            "g28s", "g28h", "g28k", "g29s", "g29h", "g29k", "g30s", "g30h", "g30k", "g31s", "g31h", "g31k",
            "g32s", "g32h", "g32k", "g35s", "g35k"
    )

    fun run(timeRange: TimeRange, main: Main) : JSONObject? {

        try {

            if (!main.hasPlayer) throw Throwable("Main did not have player, make sure you wait until it does before calling me!")

            fun add(v: String, a: JSONObject, b: JSONObject): Int {
                return a.optInt(v) + b.optInt(v)
            }

            fun combine(current: JSONObject, entries: JSONArray) {
                val size = entries.length()
                var flag = false
                var entry: JSONObject
                var i = 0

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

                    for(key in keys) {
                        current.put(key, add(key, current, entry))
                    }

                }


            }

            val stats: JSONObject = main.getHistoryStats() ?: throw Throwable("Unable to get player stats. Please report this.")

            val total = JSONObject()

            // Add data representing today, if in range
            val current: JSONObject = main.getCurrentStats() ?: JSONObject()
            val now: JSONObject = current.optJSONObject("now") ?: JSONObject()
            val recent: JSONObject = current.optJSONObject("recent") ?: JSONObject()

            if (timeRange.inRange(now.optLong("updated")) && timeRange.inRange(recent.optLong("updated"))) {
                var temp: Int
                for (key in keys) {
                    // Recent >= Now
                    temp = now.optInt(key)
                    total.put(key, Math.max(recent.optInt(key), temp) - temp)
                }
            }

            combine(total, stats.getJSONArray("daily"))
            combine(total, stats.getJSONArray("monthly"))

            return total

        } catch (e: Throwable) {
            lastError = e
            return null
        }

    }

}
