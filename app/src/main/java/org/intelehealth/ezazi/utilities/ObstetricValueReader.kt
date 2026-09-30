package org.intelehealth.ezazi.utilities

import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.database.dao.PatientsDAO
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO.Columns

/**
 * The one place that decides where an obstetric value is read from. Nepal collects these at
 * registration and stores them on the patient; every other region collects them per visit on the
 * Admission screen and stores them as obs. Read through here, never through either store directly.
 */
object ObstetricValueReader {

    /**
     * The four registration-only values are absent here on purpose: the registration number, the
     * profile-image timestamp, the alternate number and the registration start time have no obs and
     * stay patient attributes in both regions.
     */
    private val CONCEPT_BY_COLUMN: Map<Columns, String> = mapOf(
        Columns.BED_NUMBER to UuidDictionary.OBS_BED_NUMBER,
        Columns.ADMISSION_DATE to UuidDictionary.OBS_ADMISSION_DATE,
        Columns.ADMISSION_TIME to UuidDictionary.OBS_ADMISSION_TIME,
        Columns.PARITY to UuidDictionary.OBS_PARITY,
        Columns.LABOR_ONSET to UuidDictionary.OBS_LABOR_ONSET,
        Columns.ACTIVE_LABOR_DIAGNOSED to UuidDictionary.OBS_ACTIVE_LABOR_DIAGNOSED,
        Columns.MEMBRANE_RUPTURED_TIMESTAMP to UuidDictionary.OBS_MEMBRANE_RUPTURED_TIMESTAMP,
        Columns.RISK_FACTORS to UuidDictionary.OBS_RISK_FACTORS,
        Columns.HOSPITAL_MATERNITY to UuidDictionary.OBS_HOSPITAL_MATERNITY,
        Columns.PRIMARY_DOCTOR to UuidDictionary.OBS_PRIMARY_DOCTOR,
        Columns.SECONDARY_DOCTOR to UuidDictionary.OBS_SECONDARY_DOCTOR,
        Columns.GRAVIDA to UuidDictionary.OBS_GRAVIDA,
        Columns.lmp to UuidDictionary.OBS_LMP,
        Columns.EDD to UuidDictionary.OBS_EDD,
        Columns.HOSPITAL_ID to UuidDictionary.OBS_HOSPITAL_ID
    )

    /** Returns "" when absent, matching what getPatientAttributeValue has always returned. */
    @JvmStatic
    fun value(patientUuid: String?, visitUuid: String?, column: Columns): String {
        val concept = CONCEPT_BY_COLUMN[column]
        if (concept == null || AppRegion.collectsAdmissionDataAtRegistration()) {
            return attributeValue(patientUuid, column)
        }
        if (visitUuid.isNullOrEmpty()) return ""
        return ObsDAO().getAdmissionValues(visitUuid)[concept].orEmpty()
    }

    /** One query for several values of the same visit, so a row does not run fifteen of them. */
    @JvmStatic
    fun values(patientUuid: String?, visitUuid: String?, columns: List<Columns>): Map<Columns, String> {
        if (AppRegion.collectsAdmissionDataAtRegistration() || visitUuid.isNullOrEmpty()) {
            return columns.associateWith { attributeValue(patientUuid, it) }
        }
        val obs = ObsDAO().getAdmissionValues(visitUuid)
        return columns.associateWith { column ->
            val concept = CONCEPT_BY_COLUMN[column]
            if (concept == null) attributeValue(patientUuid, column) else obs[concept].orEmpty()
        }
    }

    private fun attributeValue(patientUuid: String?, column: Columns): String {
        if (patientUuid.isNullOrEmpty()) return ""
        return PatientsDAO().getPatientAttributeValue(patientUuid, column).orEmpty()
    }
}
