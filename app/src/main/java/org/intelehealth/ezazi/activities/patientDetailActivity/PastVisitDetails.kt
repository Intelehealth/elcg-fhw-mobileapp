package org.intelehealth.ezazi.activities.patientDetailActivity

/**
 * One closed visit as the Past Visits card shows it. [expanded] is collapse state rather than
 * data, and lives here so a recycled row cannot inherit its predecessor's.
 */
data class PastVisitDetails(
    val visitUuid: String,
    val admissionDate: String,
    val activeLabourDiagnosed: String,
    val deliveryDate: String,
    val riskFactors: String,
    val parity: String,
    val modeOfDelivery: String,
    val babyStatus: String,
    val motherStatus: String,
    val hasReport: Boolean,
    var expanded: Boolean = false
)
