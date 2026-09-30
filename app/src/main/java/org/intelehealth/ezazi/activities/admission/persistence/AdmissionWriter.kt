package org.intelehealth.ezazi.activities.admission.persistence

import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO
import org.intelehealth.ezazi.database.dao.VisitsDAO
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.models.dto.ObsDTO
import org.intelehealth.ezazi.models.dto.VisitDTO
import org.intelehealth.klivekit.utils.DateTimeUtils
import org.intelehealth.ezazi.utilities.UuidDictionary
import java.util.UUID

/**
 * Writes an admission as one transaction. Mirrors the visit creation at
 * PatientDetailActivity.java:267-320, which is the only other place the app creates a visit.
 */
object AdmissionWriter {

    private const val VISIT_READ_STATUS_NEW = "$"
    private const val DECISION_PENDING_FALSE = "false"
    private const val STAGE1_HOUR1_1 = "Stage1_Hour1_1"
    private const val TAG = "AdmissionWriter"

    class WriteFailedException(message: String) : Exception(message)

    /**
     * Returns the visit uuid. Blocking; call from Dispatchers.IO. A patient already admitted keeps
     * that visit rather than gaining a second, but her admission values are still written to it.
     */
    fun write(record: AdmissionRecord, values: List<Pair<String, String>>): String {
        val db = AppConstants.inteleHealthDatabaseHelper.getWriteDb()
        val openVisit = VisitsDAO().fetchActiveVisitUuid(record.patientUuid)
        val isNewVisit = openVisit.isEmpty()
        val visitUuid = if (isNewVisit) UUID.randomUUID().toString() else openVisit
        val startDate = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT)

        db.beginTransaction()
        try {
            if (isNewVisit) {
                insertVisit(record, visitUuid, startDate)
                insertVisitAttributes(record, visitUuid)
            }
            insertAdmissionEncounter(record, visitUuid, values)
            if (isNewVisit) insertStageOneEncounter(record, visitUuid)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return visitUuid
    }

    private fun insertVisit(record: AdmissionRecord, visitUuid: String, startDate: String) {
        val visit = VisitDTO().apply {
            uuid = visitUuid
            patientuuid = record.patientUuid
            startdate = startDate
            visitTypeUuid = UuidDictionary.VISIT_TELEMEDICINE
            locationuuid = record.locationUuid
            syncd = false
            enddate = null
            creatoruuid = record.creatorUuid
        }
        if (!VisitsDAO().insertPatientToDB(visit)) {
            throw WriteFailedException("visit insert returned false")
        }
    }

    /**
     * Two of these four are the WHERE clause of the Active Patients list, so an omission hides the
     * woman while leaving her visit open. All four are required.
     */
    private fun insertVisitAttributes(record: AdmissionRecord, visitUuid: String) {
        val dao = VisitAttributeListDAO()
        val attributes = listOf(
            AppConstants.OBSTETRICIAN_GYNECOLOGIST to UuidDictionary.VISIT_DR_SPECIALITY,
            record.providerUuid to UuidDictionary.VISIT_HOLDER,
            VISIT_READ_STATUS_NEW to UuidDictionary.VISIT_READ_STATUS,
            DECISION_PENDING_FALSE to UuidDictionary.DECISION_PENDING
        )
        attributes.forEach { (value, typeUuid) ->
            if (!dao.insertVisitAttributes(visitUuid, value, typeUuid)) {
                throw WriteFailedException("visit attribute insert returned false for $typeUuid")
            }
        }
    }

    /**
     * The encounter type comes from the constant, never from getEncounterTypeUuid("Admission"): that
     * reads tbl_uuid_dictionary, which an upgraded device never re-seeds, and a miss returns "" and
     * silently writes nothing.
     */
    private fun insertAdmissionEncounter(
        record: AdmissionRecord,
        visitUuid: String,
        values: List<Pair<String, String>>
    ) {
        val encounterUuid = UUID.randomUUID().toString()
        val encounter = EncounterDTO().apply {
            uuid = encounterUuid
            visituuid = visitUuid
            encounterTime = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT)
            provideruuid = record.providerUuid
            encounterTypeUuid = UuidDictionary.ENCOUNTER_ADMISSION
            syncd = false
            voided = 0
        }
        if (!EncounterDAO().createEncountersToDB(encounter)) {
            throw WriteFailedException("Admission encounter insert returned false")
        }

        val createdDate = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT)
        val obs = values.map { (conceptUuid, value) ->
            ObsDTO().apply {
                encounteruuid = encounterUuid
                conceptuuid = conceptUuid
                setValue(value)
                creator = record.creatorUuid
                creatorUuid = record.creatorUuid
                setCreatedDate(createdDate)
                voided = 0
            }
        }
        if (!ObsDAO().insertObsToDb(obs, TAG)) {
            throw WriteFailedException("admission obs insert returned false")
        }
    }

    private fun insertStageOneEncounter(record: AdmissionRecord, visitUuid: String) {
        val dao = EncounterDAO()
        val encounter = EncounterDTO().apply {
            uuid = UUID.randomUUID().toString()
            visituuid = visitUuid
            encounterTime = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT)
            provideruuid = record.providerUuid
            encounterTypeUuid = dao.getEncounterTypeUuid(STAGE1_HOUR1_1)
            syncd = false
            voided = 0
        }
        if (!dao.createEncountersToDB(encounter)) {
            throw WriteFailedException("Stage1_Hour1_1 encounter insert returned false")
        }
    }
}
