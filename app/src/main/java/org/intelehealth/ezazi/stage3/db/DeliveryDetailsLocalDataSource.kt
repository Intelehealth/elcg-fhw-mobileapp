package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.database.dao.VisitsDAO
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.partogram.CardGenerationEngine

class DeliveryDetailsLocalDataSource(
    private val obsDAO: ObsDAO,
    private val encounterDAO: EncounterDAO,
    private val visitDAO: VisitsDAO
) {
    fun insertEncounter(encounterDTO: EncounterDTO): Boolean {
        return encounterDAO.insertDeliveryOutcomeStage3(encounterDTO)
    }

    fun saveDeliveryObsData(obsList: List<ObsDTO>): Boolean {
        return obsDAO.insertObsToDb(obsList, "")
    }

    fun updateDecisionPendingFlagAndUnSyncVisitFlag(visitUuid: String, value: String) {
        return visitDAO.updateVisitDecisionPendingFlag(visitUuid, value)
    }

    fun createStage3FirstEncounter(visitUuid: String, encounterTypeName: String): Boolean {
        return encounterDAO.createStage3FirstEncounter(visitUuid, encounterTypeName)
    }

}