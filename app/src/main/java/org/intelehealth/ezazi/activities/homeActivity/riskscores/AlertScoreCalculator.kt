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

            "Y" -> {
                val yellowWeight = RiskWeightConfig.YELLOW_WEIGHT
                yellowWeight
            }
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

    private fun calculateCategoricalRed(
        conceptId: String,
        value: String
    ): Double {
        val trimmedValue = value.trim()
        val key = RedKey(conceptId, trimmedValue)

        val weight = RiskWeightConfig.redWeights[key]
        return weight ?: 0.0
    }

    private fun calculateNumericRed(
        conceptId: String,
        value: Double
    ): Double {
        val redWeight = when (conceptId) {
            PartogramConstants.Params.BASELINE_FHR.conceptId ->
                if (value < 110 || value >= 160) 1.0 else 0.0

            PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId ->
                if (value <= 2 || value > 5) 1.0 else 0.0

            PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId ->
                if (value < 20 || value > 60) 1.0 else 0.0

            PartogramConstants.Params.PULSE.conceptId ->
                if (value < 60 || value >= 120) 1.0 else 0.0

            PartogramConstants.Params.TEMPERATURE.conceptId ->
                if (value < 35 || value >= 37.5) 1.0 else 0.0

            PartogramConstants.Params.SYSTOLIC_BP.conceptId ->
                if (value < 80 || value >= 140) 1.0 else 0.0

            PartogramConstants.Params.DIASTOLIC_BP.conceptId ->
                if (value >= 90) 1.0 else 0.0

            else -> 0.0
        }

        return redWeight
    }

}
