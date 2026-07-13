package org.intelehealth.ezazi.activities.visitCreation;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.intelehealth.ezazi.activities.patientDetailActivity.PatientDetailActivity;
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.EncounterDAO;
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO;
import org.intelehealth.ezazi.database.dao.VisitsDAO;
import org.intelehealth.ezazi.models.dto.EncounterDTO;
import org.intelehealth.ezazi.models.dto.VisitAttributeDTO;
import org.intelehealth.ezazi.models.dto.VisitDTO;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.StringUtils;
import org.intelehealth.ezazi.utilities.UuidDictionary;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.utils.DateTimeUtils;

import java.util.UUID;

/**
 * Creates a new labor visit from the obstetric intake: inserts the visit and its baseline
 * attributes, writes the obstetric intake as visit attributes, seeds the first stage-1
 * encounter, then opens the timeline. Value formats mirror the legacy patient-attribute
 * storage so downstream data consumers are unaffected.
 */
public class VisitCreator {

    private VisitCreator() {
    }

    public static void createVisitAndOpenTimeline(Activity activity, String patientUuid, String name,
                                                  String patientNameTimeline, ObstetricVisitData data) {
        SessionManager sessionManager = new SessionManager(activity);
        String providerId = sessionManager.getProviderID();
        String visitUuid = UUID.randomUUID().toString();
        String startDate = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT);
        String encounterTime = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT);

        VisitDTO visitDTO = new VisitDTO();
        visitDTO.setUuid(visitUuid);
        visitDTO.setPatientuuid(patientUuid);
        visitDTO.setStartdate(startDate);
        visitDTO.setVisitTypeUuid(UuidDictionary.VISIT_TELEMEDICINE);
        visitDTO.setLocationuuid(sessionManager.getLocationUuid());
        visitDTO.setSyncd(false);
        visitDTO.setEnddate(null);
        visitDTO.setCreatoruuid(sessionManager.getCreatorID());

        try {
            new VisitsDAO().insertPatientToDB(visitDTO);

            VisitAttributeListDAO attributes = new VisitAttributeListDAO();
            attributes.insertVisitAttributes(visitUuid, AppConstants.OBSTETRICIAN_GYNECOLOGIST, PatientDetailActivity.VISIT_DR_SPECIALITY);
            attributes.insertVisitAttributes(visitUuid, providerId, PatientDetailActivity.VISIT_HOLDER);
            attributes.insertVisitAttributes(visitUuid, "$", PatientDetailActivity.VISIT_READ_STATUS);
            attributes.insertVisitAttributes(visitUuid, "false", UuidDictionary.DECISION_PENDING);
            writeObstetricAttributes(attributes, visitUuid, data);

            EncounterDAO encounterDAO = new EncounterDAO();
            EncounterDTO encounterDTO = new EncounterDTO();
            encounterDTO.setUuid(UUID.randomUUID().toString());
            encounterDTO.setVisituuid(visitUuid);
            encounterDTO.setEncounterTime(encounterTime);
            encounterDTO.setProvideruuid(providerId);
            encounterDTO.setEncounterTypeUuid(encounterDAO.getEncounterTypeUuid("Stage1_Hour1_1"));
            encounterDTO.setSyncd(false);
            encounterDTO.setVoided(0);
            encounterDAO.createEncountersToDB(encounterDTO);
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        Intent intent = new Intent(activity, TimelineVisitSummaryActivity.class);
        intent.putExtra("patientUuid", patientUuid);
        intent.putExtra("visitUuid", visitUuid);
        intent.putExtra("name", name);
        intent.putExtra("patientNameTimeline", patientNameTimeline);
        intent.putExtra("tag", "new");
        intent.putExtra("encounter_time", encounterTime);
        intent.putExtra("Stage1_Hour1_1", "Stage1_Hour1_1");
        intent.putExtra("providerID", providerId);
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Writes the 15 obstetric fields as visit attributes, keeping the exact legacy value
     * formats (parity `births,miscarriages`, membrane `U` or `date time`, doctors
     * `uuid@#@name`, bed `NA` when empty, etc.).
     */
    private static void writeObstetricAttributes(VisitAttributeListDAO dao, String visitUuid,
                                                 ObstetricVisitData data) throws DAOException {
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.gravida), VisitAttributeDTO.Columns.GRAVIDA.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.admissionDate), VisitAttributeDTO.Columns.ADMISSION_DATE.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.admissionTime), VisitAttributeDTO.Columns.ADMISSION_TIME.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.totalBirths + "," + data.totalMiscarriages), VisitAttributeDTO.Columns.PARITY.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.laborOnset), VisitAttributeDTO.Columns.LABOR_ONSET.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.activeLaborDiagnosedDate + " " + data.activeLaborDiagnosedTime), VisitAttributeDTO.Columns.ACTIVE_LABOR_DIAGNOSED.uuid);
        String membrane = data.membraneRupturedUnknown ? "U"
                : StringUtils.getValue(data.membraneRupturedDate + " " + data.membraneRupturedTime);
        dao.insertVisitAttributes(visitUuid, membrane, VisitAttributeDTO.Columns.MEMBRANE_RUPTURED_TIMESTAMP.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.riskFactors), VisitAttributeDTO.Columns.RISK_FACTORS.uuid);
        String hospital = data.hospitalMaternity != null && data.hospitalMaternity.equalsIgnoreCase("Other")
                ? data.hospitalOther : data.hospitalMaternity;
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(hospital), VisitAttributeDTO.Columns.HOSPITAL_MATERNITY.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.primaryDoctorUuid) + "@#@" + data.primaryDoctorName, VisitAttributeDTO.Columns.PRIMARY_DOCTOR.uuid);
        if (data.secondaryDoctorName != null && !data.secondaryDoctorName.isEmpty()) {
            dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.secondaryDoctorUuid) + "@#@" + data.secondaryDoctorName, VisitAttributeDTO.Columns.SECONDARY_DOCTOR.uuid);
        }
        String bed = TextUtils.isEmpty(data.bedNumber) ? AppConstants.NOT_APPLICABLE : StringUtils.getValue(data.bedNumber);
        dao.insertVisitAttributes(visitUuid, bed, VisitAttributeDTO.Columns.BED_NUMBER.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.lastMenstrualPeriod), VisitAttributeDTO.Columns.LAST_MENSTRUAL_PERIOD.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.estimatedDeliveryDate), VisitAttributeDTO.Columns.ESTIMATED_DELIVERY_DATE.uuid);
        dao.insertVisitAttributes(visitUuid, StringUtils.getValue(data.hospitalId), VisitAttributeDTO.Columns.HOSPITAL_ID.uuid);
    }
}
