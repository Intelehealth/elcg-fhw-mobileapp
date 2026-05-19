package org.intelehealth.ezazi.activities.homeActivity.riskscores

import android.util.Log
import com.google.gson.Gson
import org.intelehealth.ezazi.models.dto.ObsDTO

object CervixHistoryResolver {

    fun resolve(cervixObs: List<ObsDTO>): CervixState? {

        if (cervixObs.isEmpty()) return null

        // Sort by time
        val sorted = cervixObs.sortedBy {
            it.obsServerModifiedDate?.toMillis() ?: 0L
        }

        val latestObs = sorted.last()

        val rawValue = latestObs.value ?: return null

        val currentValue = rawValue.toIntOrNull() ?: return null

        // val currentValue = latestObs.value.toIntOrNull() ?: return null

        var startTimeMillis =
            latestObs.obsServerModifiedDate.toMillis()

        // check backward to find when this value started
        for (i in sorted.size - 2 downTo 0) {
            if (sorted[i].value == latestObs.value) {
                startTimeMillis =
                    sorted[i].obsServerModifiedDate.toMillis()
            } else {
                break
            }
        }

        return CervixState(
            value = currentValue,
            startTimeMillis = startTimeMillis,
            obsServerModifiedDate = latestObs.obsServerModifiedDate
        )
    }
}
