package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class DeliveryRepository(
    private val localDataSource: DeliveryLocalDataSource,
    private val mapper: DeliveryObsMapper
) {

    fun saveDeliveryDetails(
        encounterUuid: String,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String
    ): Boolean {

        val obsList = mapper.mapToObsList(encounterUuid, deliveryDetails, creatorUuid)

        return localDataSource.saveDeliveryObsData(obsList)
    }
}