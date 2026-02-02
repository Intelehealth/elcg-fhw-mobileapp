package org.intelehealth.ezazi.activities.homeActivity

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
                val obsList = obsDAO.getLatestObsByVisitAndConcepts(visit.uuid, riskConcepts)

                var totalScore = 0.0

                obsList.forEach { obs ->
                    totalScore += AlertScoreCalculator.calculate(obs, visit)
                }

                visit.alertFlagTotal = totalScore
                visit.visibilityOrder = when {
                    totalScore > 22 -> 3
                    totalScore >= 15 -> 2
                    else -> 1
                }
                val cervixObs = obsDAO.getCervixObsByVisit(visit.uuid, PartogramConstants.Params.CERVIX_PLOT.conceptId)
                val cervixState = CervixHistoryResolver.resolve(cervixObs)

                val cervixScore =
                    cervixState?.let {
                        CervixPlotEvaluator.calculateScore(listOf(it))
                    } ?: 0.0

                totalScore += cervixScore

            }
            visits
        }
    }

    /*  suspend fun processVisitsInBackground(
          visits: List<ActivePatientModel>,
          obsDAO: ObsDAO,
          riskConcepts: Set<String>
      ): List<ActivePatientModel> = withContext(Dispatchers.IO) {

          visits.forEach { visit ->

              val obsList = obsDAO.getLatestObsByVisitAndConcepts(
                  visit.uuid,
                  riskConcepts
              )

              var totalScore = 0.0

              obsList.forEach { obs ->

                  val comment = obs.comment.trim().uppercase()
                  val value = obs.value?.trim()?.uppercase() ?: return@forEach

                  when (comment) {

                      "G" -> Unit

                      "Y" -> totalScore += RiskWeightConfig.YELLOW_WEIGHT

                      "R" -> {
                          val key = RedKey(obs.conceptuuid, value)
                          totalScore += RiskWeightConfig.redWeights[key] ?: 0.0
                      }
                  }
              }

              visit.alertFlagTotal = totalScore

              visit.visibilityOrder = when {
                  totalScore > 22 -> 3
                  totalScore >= 15 -> 2
                  else -> 1
              }
          }

          visits
      }*/

   /* suspend fun processVisitsInBackground(
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

            var r = 0
            var y = 0
            var g = 0

            obsList.forEach { obs ->
                when (obs.comment.trim().uppercase()) {
                    "R" -> r++
                    "Y" -> y++
                    "G" -> g++
                }
            }

            val total = (2 * r) + (1 * y)

            visit.alertFlagTotal = total
            visit.visibilityOrder = when {
                total > 22 -> 3   // Red
                total >= 15 -> 2  // Yellow
                else -> 1         // Green
            }
        }

        visits
    }*/

