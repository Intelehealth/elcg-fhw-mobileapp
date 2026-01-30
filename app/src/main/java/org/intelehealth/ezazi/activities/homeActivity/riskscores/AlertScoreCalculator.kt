package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.partogram.PartogramConstants

object AlertScoreCalculator {

    fun calculate(
        obs: ObsDTO,
        visit: ActivePatientModel
    ): Double {

        // Cervix plot special case
        if (obs.conceptuuid == PartogramConstants.Params.CERVIX_PLOT.conceptId) {
            return CervixPlotEvaluator.calculateScore(obs, visit)
        }

        return when (obs.comment.trim().uppercase()) {
            "G" -> 0.0
            "Y" -> RiskWeightConfig.YELLOW_WEIGHT
            "R" -> calculateRedScore(obs)
            else -> 0.0
        }
    }

    private fun calculateRedScore(obs: ObsDTO): Double {
        val key = RedKey(
            obs.conceptuuid,
            obs.value
        )
        return RiskWeightConfig.redWeights[key] ?: 0.0
    }
}
