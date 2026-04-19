package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class SaveDeliveryDetailsUseCase(
    private val repository: DeliveryDetailsRepository
) {

    operator fun invoke(
        encounterDTO: EncounterDTO,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String,
        encounterTypeName: String
    ): Boolean {
        return repository.saveDeliveryDetails(encounterDTO, deliveryDetails,creatorUuid, encounterTypeName)
    }
}