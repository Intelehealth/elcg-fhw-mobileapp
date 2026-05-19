package org.intelehealth.ezazi.activities.homeActivity.riskscores

data class CervixState(
    val value: Int,
    val startTimeMillis: Long,
    var obsServerModifiedDate: String? = null
)