package org.intelehealth.ezazi.activities.homeActivity.riskscores

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.activities.homeActivity.RiskConcepts
import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.partogram.PartogramConstants

object VisitRiskProcessorInBatch {
    suspend fun processVisitsInBackground(
        visits: List<ActivePatientModel>,
        batchSize: Int = 50 // process 50 visits per batch to avoid memory spikes
    ): List<ActivePatientModel> = withContext(Dispatchers.IO) {
        val obsDAO = ObsDAO()
        val encounterDAO = EncounterDAO()

        val allProcessedVisits = mutableListOf<ActivePatientModel>()

        visits.chunked(batchSize).forEach { batch ->
            batch.forEach { visit ->

                val obsList = obsDAO.getLatestObsByVisitAndConcepts(
                    visit.uuid,
                    RiskConcepts.ALL_RISK_CONCEPTS
                )
                var totalScore = 0.0
               /* obsList.forEach { obs ->
                    totalScore += AlertScoreCalculator.calculateUpdated(obs, visit)
                }*/

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
                    val cervixObs = obsDAO.getCervixObsByVisit(
                        visit.uuid,
                        PartogramConstants.Params.CERVIX_PLOT.conceptId
                    )

                    val cervixState = CervixHistoryResolver.resolve(cervixObs)
                    totalScore += cervixState?.let { CervixPlotEvaluator.calculateScore(listOf(it)) } ?: 0.0
                }

                visit.alertFlagTotal = totalScore

                allProcessedVisits.add(visit)
            }
        }

        allProcessedVisits
    }
}