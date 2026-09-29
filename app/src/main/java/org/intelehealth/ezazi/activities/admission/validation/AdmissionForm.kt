package org.intelehealth.ezazi.activities.admission.validation

import androidx.annotation.StringRes

/**
 * Snapshot of every value the Save-time rules read. Holds no View and no Context, and is taken on the
 * main thread inside onSaveClicked after the hospitalMaternity fixup and after patientContextJob.join().
 */
data class AdmissionForm(
    val admissionDate: String,
    val admissionTime: String,
    val selectedRuptureMembrane: String,
    val membraneRupturedDate: String,
    val membraneRupturedTime: String,
    val totalBirth: String,
    val totalMiscarriage: String,
    val labourOnset: String,
    val activeLabourDiagnosedDate: String,
    val activeLabourDiagnosedTime: String,
    val riskFactorsText: String,
    val isOtherRiskFactorVisible: Boolean,
    val otherRiskFactorText: String,
    val hospitalMaternity: String,
    val hospitalOtherText: String,
    val lmpDate: String,
    val primaryDoctorText: String,
    val ruptureMembraneText: String,
    val gravida: String
)

enum class AdmissionField {
    ADMISSION_DATE,
    ADMISSION_TIME,
    SAC_RUPTURED_DATE,
    SAC_RUPTURED_TIME,
    TOTAL_BIRTH,
    TOTAL_MISCARRIAGE,
    LABOUR_ONSET,
    LABOUR_DIAGNOSED_DATE,
    LABOUR_DIAGNOSED_TIME,
    RISK_FACTORS,
    OTHER_RISK_FACTOR,
    HOSPITAL_MATERNITY,
    HOSPITAL_OTHER,
    LMP,
    PRIMARY_DOCTOR,
    RUPTURE_MEMBRANE,
    GRAVIDA
}

/**
 * One failed rule. [suffix] exists because the admission-date floor builds its message by concatenation
 * and a string resource alone cannot express that.
 */
data class Failure(
    val field: AdmissionField,
    @StringRes val message: Int,
    val suffix: String = ""
)
