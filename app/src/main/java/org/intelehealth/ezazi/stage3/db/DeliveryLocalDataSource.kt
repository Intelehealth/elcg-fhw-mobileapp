package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.dto.ObsDTO

class DeliveryLocalDataSource(
    private val obsDAO: ObsDAO
) {

    fun saveDeliveryObsData(obsList: List<ObsDTO>
    ): Boolean {
        return obsDAO.insertObsToDb(obsList, "")
    }
}