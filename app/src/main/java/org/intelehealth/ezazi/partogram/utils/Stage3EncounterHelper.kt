package org.intelehealth.ezazi.partogram.utils

object Stage3EncounterHelper {


      //Returns required interval (in minutes) for next encounter

    fun getRequiredIntervalMinutes(encounterName: String?): Int {

        if (encounterName.isNullOrEmpty()) return 0

        // If delivery outcome encounter → immediately create first monitoring card
        if (encounterName.equals("DELIVERY_OUTCOME_STAGE3", true)) {
            return 0
        }

        val parts = encounterName.lowercase()
            .replace("stage3_", "")
            .replace("hour", "")
            .split("_")

        if (parts.size != 2) return 0

        val hourNumber = parts[0].toIntOrNull() ?: return 0

        return when (hourNumber) {
            1 -> 15
            2 -> 30
            3, 4 -> 60
            else -> 0
        }
    }


    // Next encounter generator ONLY for Stage 3
    fun getNextStage3Encounter(encounterName: String?): String? {

        if (encounterName.isNullOrEmpty()) return null

        // If delivery outcome encounter → start Stage3 monitoring
        if (encounterName.equals("DELIVERY_OUTCOME_STAGE3", true)) {
            return "Stage3_Hour1_1"
        }

        val parts = encounterName.lowercase()
            .replace("stage3_", "")
            .replace("hour", "")
            .split("_")

        if (parts.size != 2) return null

        var hourNumber = parts[0].toInt()
        var cardNumber = parts[1].toInt()

        when (hourNumber) {

            1 -> {
                if (cardNumber < 4) {
                    cardNumber++
                } else {
                    hourNumber = 2
                    cardNumber = 1
                }
            }

            2 -> {
                if (cardNumber < 2) {
                    cardNumber++
                } else {
                    hourNumber = 3
                    cardNumber = 1
                }
            }

            3 -> {
                hourNumber = 4
                cardNumber = 1
            }

            4 -> {
                return null
            }
        }

        return "Stage3_Hour${hourNumber}_${cardNumber}"
    }
}