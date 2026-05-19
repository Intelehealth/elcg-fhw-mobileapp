package org.intelehealth.ezazi.stage3.db

import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.stage3.Utils.DeliveryDetailsConcept
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import java.util.UUID

class DeliveryDetailsObsMapper{

    fun mapToObsList(
        encounterUuid: String,
        deliveryDetails: DeliveryDetails,
        creatorUuid: String
    ): List<ObsDTO> {

        val list = mutableListOf<ObsDTO>()

        fun add(concept: DeliveryDetailsConcept, value: String?) {
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

        add(DeliveryDetailsConcept.DATE_OF_DELIVERY, deliveryDetails.dateOfDelivery)
        add(DeliveryDetailsConcept.TIME_OF_DELIVERY, deliveryDetails.timeOfDelivery)
        add(DeliveryDetailsConcept.MODE_OF_DELIVERY, deliveryDetails.modeOfDelivery)
        add(DeliveryDetailsConcept.PERINEAL_TEAR, deliveryDetails.perinealTear)
        add(DeliveryDetailsConcept.DEGREE_OF_PERINEAL_TEAR, deliveryDetails.degreeOfPerinealTear)
        add(DeliveryDetailsConcept.PLACENTA_MEMBRANE_STATUS, deliveryDetails.placentaMembraneStatus)
        add(DeliveryDetailsConcept.PLACENTA_DELIVERY_TIME, deliveryDetails.timeOfPlacentaDelivery)
        add(DeliveryDetailsConcept.PLACENTA_CORD_ABNORMALITY, deliveryDetails.placentalOrCordAbnormality)
        add(DeliveryDetailsConcept.MEDICATIONS_AMTSL, deliveryDetails.amtsl)
        add(DeliveryDetailsConcept.BIRTH_TYPE, deliveryDetails.typeOfBirth)
        add(DeliveryDetailsConcept.BABY_GENDER, deliveryDetails.babyGender)

        if (deliveryDetails.typeOfBirth.equals("Live Birth", true)) {
            add(DeliveryDetailsConcept.APGAR_SCORE_1_MIN, deliveryDetails.apgarScore1Min)
            add(DeliveryDetailsConcept.APGAR_SCORE_5_MIN, deliveryDetails.apgarScore5Min)
            add(DeliveryDetailsConcept.BIRTH_WEIGHT, deliveryDetails.birthWeightGrams)
            add(DeliveryDetailsConcept.SKIN_CONTACT, deliveryDetails.skinToSkinContact)
            add(DeliveryDetailsConcept.BREASTFED_FIRSTHOUR, deliveryDetails.breastfeedWithin1Hour)
            add(DeliveryDetailsConcept.CONGENITAL_ANOMALY, deliveryDetails.congenitalAnomalies)
            add(DeliveryDetailsConcept.RESUSCITATION, deliveryDetails.resuscitation)
        }
       /* if (deliveryDetails.typeOfBirth.equals("Still Birth", true)) {
            add(DeliveryDetailsConcept.TYPE_OF_STILL_BIRTH, deliveryDetails.typeOfStillBirth)
        }*/
        return list
    }
}