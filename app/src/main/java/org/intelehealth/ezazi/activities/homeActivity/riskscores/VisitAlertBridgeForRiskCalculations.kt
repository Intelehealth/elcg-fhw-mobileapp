package org.intelehealth.ezazi.activities.homeActivity.riskscores

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO
import org.intelehealth.ezazi.models.ActivePatientModel
import org.intelehealth.ezazi.utilities.UuidDictionary

object VisitAlertBridgeForRiskCalculations {

    @JvmStatic
    fun processVisits(
        scope: CoroutineScope,
        visits: List<ActivePatientModel>,
        result: (@JvmSuppressWildcards List<ActivePatientModel>) -> Unit
    ) {
        scope.launch {
            val processedVisits =
                VisitRiskProcessorInBatch.processVisitsInBackground(visits, batchSize = 50)

            val visitAttributeListDAO = VisitAttributeListDAO()
            processedVisits.forEach { visit ->
                visitAttributeListDAO.upsertVisitAttribute(
                    visit.uuid,
                    UuidDictionary.VISIT_RISK,
                    visit.alertFlagTotal.toString()
                )
            }

            // Optional callback
            result(processedVisits)
        }
    }
}