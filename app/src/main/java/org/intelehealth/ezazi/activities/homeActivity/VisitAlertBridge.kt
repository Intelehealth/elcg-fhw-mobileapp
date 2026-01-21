package org.intelehealth.ezazi.activities.homeActivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.ActivePatientModel

object VisitAlertBridge {

    @JvmStatic
    fun processVisits(
        scope: CoroutineScope,
        visits: List<ActivePatientModel>,
        obsDAO: ObsDAO,
        riskConcepts: Set<String>,
        onResult: (@JvmSuppressWildcards List<ActivePatientModel>) -> Unit
    ) {
        scope.launch {
            val result =
                VisitAlertProcessor.processVisitsInBackground(
                    visits,
                    obsDAO,
                    riskConcepts
                )

            onResult(result)
        }
    }
}