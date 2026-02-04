package org.intelehealth.ezazi.activities.homeActivity

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.activities.homeActivity.riskscores.AlertScoreCalculator
import org.intelehealth.ezazi.activities.homeActivity.riskscores.CervixHistoryResolver
import org.intelehealth.ezazi.activities.homeActivity.riskscores.CervixPlotEvaluator
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.partogram.PartogramConstants


object VisitAlertProcessor {

    suspend fun processVisitsInBackground(
        visits: List<ActivePatientModel>,
        obsDAO: ObsDAO,
        riskConcepts: Set<String>
    ): List<ActivePatientModel> = withContext(Dispatchers.IO) {

        visits.forEach { visit ->

            val obsList =
                obsDAO.getLatestObsByVisitAndConcepts(
                    visit.uuid,
                    riskConcepts
                )

            var totalScore = 0.0

            obsList.forEach { obs -> totalScore += AlertScoreCalculator.calculate(obs, visit)
            }

            val cervixObs =
                obsDAO.getCervixObsByVisit(
                    visit.uuid,
                    PartogramConstants.Params.CERVIX_PLOT.conceptId
                )

            val cervixState =
                CervixHistoryResolver.resolve(cervixObs)

            val cervixScore =
                cervixState?.let {
                    CervixPlotEvaluator.calculateScore(listOf(it))
                } ?: 0.0


            totalScore += cervixScore

            visit.alertFlagTotal = totalScore

            visit.visibilityOrder = when {
                totalScore > 22 -> 3
                totalScore >= 15 -> 2
                else -> 1
            }
        }

        visits
    }
}


