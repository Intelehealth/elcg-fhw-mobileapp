package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class SaveDeliveryDetailsUseCase(
    private val repository: DeliveryRepository
) {

    operator fun invoke(
        encounterUuid: String,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String
    ): Boolean {
        return repository.saveDeliveryDetails(
            encounterUuid,
            deliveryDetails,creatorUuid
        )
    }
}