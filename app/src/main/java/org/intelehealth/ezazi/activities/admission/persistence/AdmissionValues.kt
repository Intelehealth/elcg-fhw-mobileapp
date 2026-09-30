package org.intelehealth.ezazi.activities.admission.persistence

import org.intelehealth.ezazi.utilities.ObstetricValueFormats
import org.intelehealth.ezazi.utilities.UuidDictionary

/**
 * Packs an AdmissionRecord into the fifteen stored values, byte-for-byte as
 * PatientOtherInfoFragment.getPatientAttributes() (:1820-1866) writes them today. Only the store
 * changes; a format change here would corrupt data already collected from the other region.
 */
object AdmissionValues {

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

    private fun parity(record: AdmissionRecord): String =
        ObstetricValueFormats.parity(record.totalBirthCount, record.totalMiscarriageCount)

    private fun activeLabourDiagnosed(record: AdmissionRecord): String =
        ObstetricValueFormats.timestamp(record.activeLabourDiagnosedDate, record.activeLabourDiagnosedTime)

    private fun membraneRuptured(record: AdmissionRecord): String = ObstetricValueFormats.membraneRuptured(
        record.selectedRuptureMembrane, record.membraneRupturedDate, record.membraneRupturedTime
    )

    private fun riskFactors(record: AdmissionRecord, otherRiskLabel: String): String =
        ObstetricValueFormats.riskFactors(record.riskFactors, otherRiskLabel, record.otherRiskFactorText)

    private fun hospitalMaternity(record: AdmissionRecord): String =
        ObstetricValueFormats.hospitalMaternity(record.hospitalMaternity, record.hospitalOtherText)

    private fun primaryDoctor(record: AdmissionRecord): String =
        ObstetricValueFormats.doctor(record.primaryDoctorUuid, record.primaryDoctorName)

    private fun secondaryDoctor(record: AdmissionRecord): String =
        ObstetricValueFormats.doctor(record.secondaryDoctorUuid, record.secondaryDoctorName)

    private fun bedNumber(record: AdmissionRecord): String =
        ObstetricValueFormats.bedNumber(record.bedNumber)
}
