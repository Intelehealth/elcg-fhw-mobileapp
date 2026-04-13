package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class SaveDeliveryDetailsUseCase(
    private val repository: DeliveryRepository
) {

    operator fun invoke(
        encounterDTO: EncounterDTO,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String
    ): Boolean {
        return repository.saveDeliveryDetails(encounterDTO, deliveryDetails,creatorUuid)
    }
}