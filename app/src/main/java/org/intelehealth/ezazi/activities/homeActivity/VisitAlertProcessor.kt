package org.intelehealth.ezazi.activities.homeActivity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.models.dto.ObsDTO


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
    }
}
