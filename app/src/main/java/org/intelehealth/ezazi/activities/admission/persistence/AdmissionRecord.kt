package org.intelehealth.ezazi.activities.admission.persistence

/**
 * Raw, un-packed capture of everything the admission write needs. Holds no View and no Context; the
 * packing into stored formats is AdmissionValues' job.
 */
data class AdmissionRecord(
    val patientUuid: String,
    val providerUuid: String,
    val creatorUuid: String,
    val locationUuid: String,
    val admissionDate: String,
    val admissionTime: String,
    val totalBirthCount: String,
    val totalMiscarriageCount: String,
    val gravida: String,
    val labourOnset: String,
    val activeLabourDiagnosedDate: String,
    val activeLabourDiagnosedTime: String,
    val selectedRuptureMembrane: String,
    val membraneRupturedDate: String,
    val membraneRupturedTime: String,
    val riskFactors: String,
    val otherRiskFactorText: String,
    val hospitalMaternity: String,
    val hospitalOtherText: String,
    val hospitalId: String,
    val bedNumber: String,
    val primaryDoctorUuid: String,
    val primaryDoctorName: String,
    val secondaryDoctorUuid: String,
    val secondaryDoctorName: String,
    val lmpDate: String,
    val edd: String
)
