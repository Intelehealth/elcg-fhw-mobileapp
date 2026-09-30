package org.intelehealth.ezazi.activities.patientDetailActivity

import android.database.sqlite.SQLiteDatabase
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO.Columns
import org.intelehealth.ezazi.stage3.Utils.DeliveryDetailsConcept
import org.intelehealth.ezazi.ui.visit.model.VisitOutcome
import org.intelehealth.ezazi.utilities.ObstetricValueReader
import org.intelehealth.ezazi.utilities.UuidDictionary

/**
 * Reads a patient's closed visits for the Past Visits section. Obstetric values go through
 * [ObstetricValueReader], which carries the region gate, so Nepal and everywhere else share
 * this one path.
 */
object PastVisitLoader {

    private val ADMISSION_COLUMNS = listOf(
        Columns.ADMISSION_DATE,
        Columns.ACTIVE_LABOR_DIAGNOSED,
        Columns.RISK_FACTORS,
        Columns.PARITY
    )

    /** The literal negation of VisitsDAO.fetchActiveVisitUuid's predicate, so "closed" is defined once. */
    private const val CLOSED_VISITS =
        "SELECT V.uuid FROM tbl_visit V WHERE V.patientuuid = ? " +
            "AND V.voided IN ('0','false','FALSE') " +
            "AND ((V.enddate IS NOT NULL AND V.enddate <> '') " +
            "OR EXISTS (SELECT 1 FROM tbl_encounter E WHERE E.visituuid = V.uuid " +
            "AND E.encounter_type_uuid = ? AND E.voided = '0')) " +
            "ORDER BY V.startdate DESC"

    /** Several queries per visit, so never call this on the main thread. */
    @JvmStatic
    fun loadForPatient(patientUuid: String?, deceasedLabel: String): List<PastVisitDetails> {
        if (patientUuid.isNullOrEmpty()) return emptyList()
        val db = AppConstants.inteleHealthDatabaseHelper.readableDatabase
        return closedVisitUuids(db, patientUuid).map { visitUuid ->
            detailsFor(db, patientUuid, visitUuid, deceasedLabel)
        }
    }

    private fun closedVisitUuids(db: SQLiteDatabase, patientUuid: String): List<String> {
        val uuids = mutableListOf<String>()
        db.rawQuery(CLOSED_VISITS, arrayOf(patientUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE)).use {
            while (it.moveToNext()) it.getString(0)?.let { uuid -> uuids.add(uuid) }
        }
        return uuids
    }

    private fun detailsFor(
        db: SQLiteDatabase,
        patientUuid: String,
        visitUuid: String,
        deceasedLabel: String
    ): PastVisitDetails {
        val admission = ObstetricValueReader.values(patientUuid, visitUuid, ADMISSION_COLUMNS)
        val stage3 = encounterUuid(db, visitUuid, UuidDictionary.DELIVERY_OUTCOME_STAGE3)
        val visitComplete = encounterUuid(db, visitUuid, UuidDictionary.ENCOUNTER_VISIT_COMPLETE)
        val outcome = if (visitComplete.isEmpty()) null else ObsDAO().getCompletedVisitType(visitComplete)

        return PastVisitDetails(
            visitUuid = visitUuid,
            admissionDate = admission[Columns.ADMISSION_DATE].orEmpty(),
            activeLabourDiagnosed = admission[Columns.ACTIVE_LABOR_DIAGNOSED].orEmpty(),
            deliveryDate = obsValue(db, stage3, DeliveryDetailsConcept.DATE_OF_DELIVERY.uuid),
            riskFactors = admission[Columns.RISK_FACTORS].orEmpty(),
            parity = admission[Columns.PARITY].orEmpty(),
            modeOfDelivery = obsValue(db, stage3, DeliveryDetailsConcept.MODE_OF_DELIVERY.uuid),
            babyStatus = outcome?.babyOutcome.orEmpty(),
            motherStatus = motherStatus(db, visitComplete, outcome, deceasedLabel),
            hasReport = stage3.isNotEmpty()
        )
    }

    /**
     * Deceased wins; otherwise the disposition recorded against REFER_TYPE, which is where every
     * stage's completion dialog stores what happened to the mother.
     */
    private fun motherStatus(
        db: SQLiteDatabase,
        visitComplete: String,
        outcome: VisitOutcome?,
        deceasedLabel: String
    ): String {
        if (outcome?.isHasMotherDeceased == true) return deceasedLabel
        return obsValue(db, visitComplete, UuidDictionary.REFER_TYPE)
    }

    private fun encounterUuid(db: SQLiteDatabase, visitUuid: String, typeUuid: String): String {
        db.rawQuery(
            "SELECT uuid FROM tbl_encounter WHERE visituuid = ? AND encounter_type_uuid = ? " +
                "AND voided IN ('0','false','FALSE') ORDER BY encounter_time DESC LIMIT 1",
            arrayOf(visitUuid, typeUuid)
        ).use { if (it.moveToFirst()) return it.getString(0).orEmpty() }
        return ""
    }

    /** Written fresh rather than reused: every cursor loop in ObsDAO drops rows with an empty comment. */
    private fun obsValue(db: SQLiteDatabase, encounterUuid: String, conceptUuid: String): String {
        if (encounterUuid.isEmpty()) return ""
        db.rawQuery(
            "SELECT value FROM tbl_obs WHERE encounteruuid = ? AND conceptuuid = ? " +
                "AND voided IN ('0','false','FALSE') ORDER BY modified_date DESC LIMIT 1",
            arrayOf(encounterUuid, conceptUuid)
        ).use { if (it.moveToFirst()) return it.getString(0).orEmpty() }
        return ""
    }
}
