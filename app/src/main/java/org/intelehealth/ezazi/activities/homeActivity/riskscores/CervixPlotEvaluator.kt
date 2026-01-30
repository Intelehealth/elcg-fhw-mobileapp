package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.models.dto.ObsDTO
import java.text.SimpleDateFormat
import java.util.Locale
object CervixPlotEvaluator {

    fun calculateScore(
        cervixObs: List<ObsDTO>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {

        if (cervixObs.isEmpty()) return 0.0

        // Sort by actual time (not String)
        val sorted = cervixObs.sortedBy {
            it.obsServerModifiedDate.toMillis()
        }

        val latestObs = sorted.last()
        val cervixValue = latestObs.value.toIntOrNull() ?: return 0.0

        // Cervix = 10 is always green
        val threshold =
            CervixPlotConfig.thresholdHours[cervixValue]
                ?: return 0.0

        // Find when this cervix value started
        var startTimeMillis = latestObs.obsServerModifiedDate.toMillis()

        for (i in sorted.size - 2 downTo 0) {
            if (sorted[i].value == latestObs.value) {
                startTimeMillis = sorted[i].obsServerModifiedDate.toMillis()
            } else {
                break
            }
        }

        val durationHours =
            (nowMillis - startTimeMillis).toDouble() / (1000.0 * 60 * 60)

        return if (durationHours >= threshold)
            CervixPlotConfig.RED_SCORE
        else
            0.0
    }
}

/* ---- Extension function (keep outside the object or at top-level) ---- */

fun String.toMillis(): Long {
    val formatter = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        Locale.getDefault()
    )
    return formatter.parse(this)?.time ?: 0L
}
