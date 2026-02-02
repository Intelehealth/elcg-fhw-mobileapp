package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.partogram.PartogramConstants

object AlertScoreCalculator {

    fun calculate(
        obs: ObsDTO,
        visit: ActivePatientModel
    ): Double {

        return when (obs.comment.trim().uppercase()) {
            "G" -> 0.0
            "Y" -> RiskWeightConfig.YELLOW_WEIGHT
            "R" -> calculateRedScore(obs)
            else -> 0.0
        }
    }

    private fun calculateRedScore(obs: ObsDTO): Double {
        val rawValue = obs.value ?: return 0.0
        val conceptId = obs.conceptuuid

        val numericValue = rawValue.toDoubleOrNull()

        return if (numericValue != null) {
            calculateNumericRed(conceptId, numericValue)
        } else {
            calculateCategoricalRed(conceptId, rawValue)
        }
    }
    private fun calculateNumericRed(
        conceptId: String,
        value: Double
    ): Double {
        return when (conceptId) {

            PartogramConstants.Params.BASELINE_FHR.conceptId ->
                if (value < 110 || value > 160) 1.0 else 0.0

            PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId ->
                if (value <= 2 || value > 5) 1.0 else 0.0

            PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId ->
                if (value < 20 || value > 60) 1.0 else 0.0

            PartogramConstants.Params.PULSE.conceptId ->
                if (value < 60 || value > 120) 1.0 else 0.0

            PartogramConstants.Params.TEMPERATURE.conceptId ->
                if (value < 35 || value >= 37.5) 1.0 else 0.0

            else -> 0.0
        }
    }
    private fun calculateCategoricalRed(
        conceptId: String,
        value: String
    ): Double {
        val key = RedKey(conceptId, value.trim())
        return RiskWeightConfig.redWeights[key] ?: 0.0
    }

}
