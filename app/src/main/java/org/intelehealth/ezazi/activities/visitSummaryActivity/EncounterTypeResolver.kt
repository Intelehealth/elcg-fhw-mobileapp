package org.intelehealth.ezazi.activities.visitSummaryActivity

import org.intelehealth.ezazi.models.dto.EncounterDTO

 class EncounterTypeResolver {

    fun resolve(dbValue: String?): EncounterDTO.Type {
        if (dbValue.isNullOrEmpty()) {
            return EncounterDTO.Type.NORMAL
        }

        // Backward compatibility
        if (dbValue.equals("SOS", ignoreCase = true)) {
            return EncounterDTO.Type.SOS
        }

        // New format support
        if (dbValue.uppercase().contains("_SOS")) {
            return EncounterDTO.Type.SOS
        }

        return EncounterDTO.Type.NORMAL
    }
}
