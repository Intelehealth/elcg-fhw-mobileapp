package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.models.dto.ObsDTO

object CervixHistoryResolver {

    fun resolve(cervixObs: List<ObsDTO>): CervixState? {

        if (cervixObs.isEmpty()) return null

        // Sort by time (important!)
        val sorted = cervixObs.sortedBy {
            it.obsServerModifiedDate.toMillis()
        }

        val latestObs = sorted.last()
        val currentValue = latestObs.value.toIntOrNull() ?: return null

        var startTimeMillis =
            latestObs.obsServerModifiedDate.toMillis()

        // Walk backwards to find when this value started
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
            startTimeMillis = startTimeMillis
        )
    }
}
