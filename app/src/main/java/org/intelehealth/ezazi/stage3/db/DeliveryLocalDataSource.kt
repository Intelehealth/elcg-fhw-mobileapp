package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.models.dto.ObsDTO

class DeliveryLocalDataSource(
    private val obsDAO: ObsDAO,
    private val encounterDAO: EncounterDAO,
) {
    fun insertEncounter(encounterDTO: EncounterDTO): Boolean {
        return encounterDAO.insertDeliveryOutcomeStage3(encounterDTO)
    }

    fun saveDeliveryObsData(obsList: List<ObsDTO>): Boolean {
        return obsDAO.insertObsToDb(obsList, "")
    }
}