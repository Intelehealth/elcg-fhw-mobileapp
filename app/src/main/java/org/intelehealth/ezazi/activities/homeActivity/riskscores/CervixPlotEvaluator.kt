package org.intelehealth.ezazi.activities.homeActivity.riskscores

import java.text.SimpleDateFormat
import java.util.Locale

object CervixPlotEvaluator {

    fun calculateScore(
        cervixObs: List<CervixState>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {

        if (cervixObs.isEmpty()) return 0.0

        // Sort by actual time
        val sorted = cervixObs.sortedBy {
            it.obsServerModifiedDate?.toMillis()
        }

        val latestObs = sorted.last()
        val cervixValue = latestObs.value ?: return 0.0

        // Cervix = 10 is always green
        val threshold =
            CervixPlotConfig.thresholdHours[cervixValue]
                ?: return 0.0

        // Find when this cervix value started
        var startTimeMillis = latestObs.obsServerModifiedDate?.toMillis()

        for (i in sorted.size - 2 downTo 0) {
            if (sorted[i].value == latestObs.value) {
                startTimeMillis = sorted[i].obsServerModifiedDate?.toMillis()
            } else {
                break
            }
        }

        val start = startTimeMillis ?: return 0.0

        val durationHours = (nowMillis - start).toDouble() / (1000.0 * 60 * 60)

        return if (durationHours >= threshold)
            CervixPlotConfig.RED_SCORE
        else
            0.0
    }
}
fun String.toMillis(): Long {
    val formatter = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        Locale.getDefault()
    )
    return formatter.parse(this)?.time ?: 0L
}
