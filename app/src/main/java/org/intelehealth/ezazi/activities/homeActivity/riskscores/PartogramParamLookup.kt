package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.partogram.PartogramConstants

object PartogramParamLookup {
    private val map: Map<String, PartogramConstants.Params> =
        PartogramConstants.Params.values().associateBy { it.conceptId }

    fun fromConceptUuid(uuid: String): PartogramConstants.Params? =
        map[uuid]
}