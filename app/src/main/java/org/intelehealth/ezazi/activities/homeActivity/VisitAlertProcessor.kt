package org.intelehealth.ezazi.activities.homeActivity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.activities.homeActivity.riskscores.AlertScoreCalculator
import org.intelehealth.ezazi.activities.homeActivity.riskscores.CervixHistoryResolver
import org.intelehealth.ezazi.activities.homeActivity.riskscores.CervixPlotEvaluator
import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.partogram.PartogramConstants


object VisitAlertProcessor {

    suspend fun processVisitsInBackground(
        visits: List<ActivePatientModel>,
    ): List<ActivePatientModel> = withContext(Dispatchers.IO) {
        val obsDAO = ObsDAO()
        val encounterDAO = EncounterDAO()

        visits.forEach { visit ->

            val obsList =
                obsDAO.getLatestObsByVisitAndConcepts(
                    visit.uuid,
                    RiskConcepts.ALL_RISK_CONCEPTS
                )
            var totalScore = 0.0

            ///obsList.forEach { obs -> totalScore += AlertScoreCalculator.calculateUpdated(obs, visit) } //old code commented due to bp changes
            // Separate BP obs (Systolic/Diastolic) from all other obs
            val bpConceptIds = setOf(
                PartogramConstants.Params.SYSTOLIC_BP.conceptId,
                PartogramConstants.Params.DIASTOLIC_BP.conceptId
            )

            val bpObsList = obsList.filter { it.conceptuuid in bpConceptIds }
            val nonBpObsList = obsList.filter { it.conceptuuid !in bpConceptIds }

            // Non-BP params
            nonBpObsList.forEach { obs -> totalScore += AlertScoreCalculator.calculateUpdated(obs, visit) }

            // BP pair: combine into a single risk contribution (max 1.0), regardless of
            // whether one or both of Systolic/Diastolic are flagged "R"
            if (bpObsList.isNotEmpty()) {
                val bpScore = bpObsList
                    .map { AlertScoreCalculator.calculateUpdated(it, visit) }
                    .maxOrNull() ?: 0.0
                totalScore += bpScore
            }

            if(!encounterDAO.isStage3Started(visit.uuid)){
                val cervixObs = obsDAO.getCervixObsByVisit(visit.uuid, PartogramConstants.Params.CERVIX_PLOT.conceptId)

                val cervixState = CervixHistoryResolver.resolve(cervixObs)

                val cervixScore = cervixState?.let { CervixPlotEvaluator.calculateScore(listOf(it)) } ?: 0.0
                totalScore += cervixScore
            }

            visit.alertFlagTotal = totalScore

            /*visit.visibilityOrder = when {
                totalScore > 3.5 -> 3
                (totalScore in 0.5..3.5) -> 2
                else -> 1
            }
            val encounterUUID = visit.latestEncounterId
            if (!encounterUUID.isNullOrBlank()) {
                val isSubmitted = obsDAO.checkObsExistsOrNot(encounterUUID)
                if (isSubmitted == 1) { // not yet filled
                    visit.obsExistsFlag = true
                    visit.visibilityOrder = 4
                }
            }*/
        }

        visits
    }
}


