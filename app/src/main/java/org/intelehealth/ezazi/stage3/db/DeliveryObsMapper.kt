package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.stage3.Utils.DeliveryConcept
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import java.util.UUID

class DeliveryObsMapper{

    fun mapToObsList(
        encounterUuid: String,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String
    ): List<ObsDTO> {

        val list = mutableListOf<ObsDTO>()

        fun add(concept: DeliveryConcept, value: String?) {
            if (value.isNullOrEmpty()) return
            val obs = ObsDTO().apply {
                uuid = UUID.randomUUID().toString()
                this.encounteruuid = encounterUuid
                this.conceptuuid = concept.uuid
                this.value = value
                this.voided = 0
                this.creatorUuid = creatorUuid
                this.comment = comment
            }

            list.add(obs)
        }

        add(DeliveryConcept.DATE_OF_DELIVERY, deliveryDetails.dateOfDelivery)
        add(DeliveryConcept.TIME_OF_DELIVERY, deliveryDetails.timeOfDelivery)
        add(DeliveryConcept.MODE_OF_DELIVERY, deliveryDetails.modeOfDelivery)
        add(DeliveryConcept.PERINEAL_TEAR, deliveryDetails.perinealTear)
        add(DeliveryConcept.DEGREE_OF_PERINEAL_TEAR, deliveryDetails.degreeOfPerinealTear)
        add(DeliveryConcept.PLACENTA_MEMBRANE_STATUS, deliveryDetails.placentaMembraneStatus)
        add(DeliveryConcept.PLACENTA_DELIVERY_TIME, deliveryDetails.timeOfPlacentaDelivery)
        add(DeliveryConcept.PLACENTA_CORD_ABNORMALITY, deliveryDetails.placentalOrCordAbnormality)
        add(DeliveryConcept.MEDICATIONS_AMTSL, deliveryDetails.amtsl)
        add(DeliveryConcept.BIRTH_TYPE, deliveryDetails.typeOfBirth)
        add(DeliveryConcept.BABY_GENDER, deliveryDetails.babyGender)

        if (deliveryDetails.typeOfBirth.equals("Live Birth", true)) {
            add(DeliveryConcept.APGAR_SCORE_1_MIN, deliveryDetails.apgarScore1Min)
            add(DeliveryConcept.APGAR_SCORE_5_MIN, deliveryDetails.apgarScore5Min)
            add(DeliveryConcept.BIRTH_WEIGHT, deliveryDetails.birthWeightGrams)
            add(DeliveryConcept.SKIN_CONTACT, deliveryDetails.skinToSkinContact)
            add(DeliveryConcept.BREASTFED_FIRSTHOUR, deliveryDetails.breastfeedWithin1Hour)
            add(DeliveryConcept.CONGENITAL_ANOMALY, deliveryDetails.congenitalAnomalies)
            add(DeliveryConcept.RESUSCITATION, deliveryDetails.resuscitation)
        }

        return list
    }
}