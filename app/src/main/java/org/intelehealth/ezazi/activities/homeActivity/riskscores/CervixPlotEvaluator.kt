package org.intelehealth.ezazi.activities.homeActivity.riskscores


object CervixPlotEvaluator {

    fun calculateScore(
        cervixObs: List<CervixState>,
        nowMillis: Long = System.currentTimeMillis()
    ): Double {

        if (cervixObs.isEmpty()) {
            return 0.0
        }

        // There should be ONLY ONE state
        val state = cervixObs.last()

        val cervixValue = state.value
        val startTimeMillis = state.startTimeMillis

        val threshold = CervixPlotConfig.thresholdHours[cervixValue]
        if (threshold == null) {
            return 0.0
        }

        val durationHours =
            (nowMillis - startTimeMillis).toDouble() / (1000 * 60 * 60)


        val score =
            if (durationHours >= threshold) {
                CervixPlotConfig.RED_SCORE
            } else {
                0.0
            }

        return score
    }
}
