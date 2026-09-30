package org.intelehealth.ezazi.activities.admission.persistence

import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.utilities.UuidDictionary

/**
 * Packs an AdmissionRecord into the fifteen stored values, byte-for-byte as
 * PatientOtherInfoFragment.getPatientAttributes() (:1820-1866) writes them today. Only the store
 * changes; a format change here would corrupt data already collected from the other region.
 */
object AdmissionValues {

    private const val DOCTOR_SEPARATOR = "@#@"
    private const val MEMBRANE_UNKNOWN = "U"
    private const val MEMBRANE_INTACT = "I"
    private const val OPTION_UNKNOWN = "Unknown"
    private const val OPTION_INTACT = "Intact"
    private const val OPTION_OTHER = "other"

    /**
     * Returns concept uuid to stored value. Secondary doctor is omitted when no name was picked,
     * mirroring the guard at PatientOtherInfoFragment.java:1853, so this is fourteen or fifteen pairs.
     */
    fun pack(record: AdmissionRecord, otherRiskLabel: String): List<Pair<String, String>> {
        val values = mutableListOf<Pair<String, String>>()

        values += UuidDictionary.OBS_ADMISSION_DATE to record.admissionDate
        values += UuidDictionary.OBS_ADMISSION_TIME to record.admissionTime
        values += UuidDictionary.OBS_PARITY to parity(record)
        values += UuidDictionary.OBS_LABOR_ONSET to record.labourOnset
        values += UuidDictionary.OBS_ACTIVE_LABOR_DIAGNOSED to activeLabourDiagnosed(record)
        values += UuidDictionary.OBS_MEMBRANE_RUPTURED_TIMESTAMP to membraneRuptured(record)
        values += UuidDictionary.OBS_RISK_FACTORS to riskFactors(record, otherRiskLabel)
        values += UuidDictionary.OBS_HOSPITAL_MATERNITY to hospitalMaternity(record)
        values += UuidDictionary.OBS_PRIMARY_DOCTOR to primaryDoctor(record)
        if (record.secondaryDoctorName.isNotEmpty()) {
            values += UuidDictionary.OBS_SECONDARY_DOCTOR to secondaryDoctor(record)
        }
        values += UuidDictionary.OBS_BED_NUMBER to bedNumber(record)
        values += UuidDictionary.OBS_GRAVIDA to record.gravida
        values += UuidDictionary.OBS_LMP to record.lmpDate
        values += UuidDictionary.OBS_EDD to record.edd
        values += UuidDictionary.OBS_HOSPITAL_ID to record.hospitalId

        return values
    }

    /** Comma, no space. PatientOtherInfoFragment.java:1825. */
    private fun parity(record: AdmissionRecord): String =
        record.totalBirthCount + "," + record.totalMiscarriageCount

    /** One separator space; the time already carries its own trailing space. :1827. */
    private fun activeLabourDiagnosed(record: AdmissionRecord): String =
        record.activeLabourDiagnosedDate + " " + record.activeLabourDiagnosedTime

    /** Case-sensitive, and Known is the else branch. :1830-1841. */
    private fun membraneRuptured(record: AdmissionRecord): String = when (record.selectedRuptureMembrane) {
        OPTION_UNKNOWN -> MEMBRANE_UNKNOWN
        OPTION_INTACT -> MEMBRANE_INTACT
        else -> record.membraneRupturedDate + " " + record.membraneRupturedTime
    }

    /** The Other-High-Risk label is substituted with the typed reason. :1848-1850. */
    private fun riskFactors(record: AdmissionRecord, otherRiskLabel: String): String =
        if (record.riskFactors.contains(otherRiskLabel)) {
            record.riskFactors.replace(otherRiskLabel, record.otherRiskFactorText)
        } else {
            record.riskFactors
        }

    /** The literal "other" is never stored; the typed facility replaces it. :1798-1802. */
    private fun hospitalMaternity(record: AdmissionRecord): String =
        if (record.hospitalMaternity.trim().equals(OPTION_OTHER, ignoreCase = true)) {
            record.hospitalOtherText
        } else {
            record.hospitalMaternity
        }

    private fun primaryDoctor(record: AdmissionRecord): String =
        record.primaryDoctorUuid + DOCTOR_SEPARATOR + record.primaryDoctorName

    private fun secondaryDoctor(record: AdmissionRecord): String =
        record.secondaryDoctorUuid + DOCTOR_SEPARATOR + record.secondaryDoctorName

    /** Blank becomes "NA", not the empty string. :1858-1859. */
    private fun bedNumber(record: AdmissionRecord): String =
        record.bedNumber.ifEmpty { AppConstants.NOT_APPLICABLE }
}
