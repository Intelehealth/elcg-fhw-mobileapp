package org.intelehealth.ezazi.activities.addNewPatient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.intelehealth.ezazi.activities.patientDetailActivity.PatientDetailActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.ImagesDAO;
import org.intelehealth.ezazi.database.dao.ImagesPushDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.SyncDAO;
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO;
import org.intelehealth.ezazi.models.dto.PatientDTO;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.StringUtils;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates (or updates) a patient from the registration flow using only demographic data.
 * Obstetric intake was moved to the visit flow, so registration persists just the patient
 * plus the registration number, alternate number and profile-image timestamp attributes.
 */
public class PatientRegistrationSaver {

    private PatientRegistrationSaver() {
    }

    public static void savePatient(Activity activity, PatientDTO patientDTO, String alternateNumber,
                                   boolean fromSummary, String patientUuidUpdate) {
        SessionManager sessionManager = new SessionManager(activity);
        String uuid = (fromSummary && patientUuidUpdate != null && !patientUuidUpdate.isEmpty())
                ? patientUuidUpdate : UUID.randomUUID().toString();
        patientDTO.setUuid(uuid);
        patientDTO.setCreatorUuid(sessionManager.getCreatorID());

        PatientsDAO patientsDAO = new PatientsDAO();
        List<PatientAttributesDTO> attributes = buildDemographicAttributes(patientsDAO, patientDTO, uuid, alternateNumber);
        patientDTO.setPatientAttributesDTOList(attributes);
        patientDTO.setSyncd(false);

        ImagesDAO imagesDAO = new ImagesDAO();
        try {
            boolean saved;
            if (fromSummary) {
                saved = patientsDAO.updatePatientToDBNew(patientDTO, uuid, attributes)
                        && imagesDAO.updatePatientProfileImages(patientDTO.getPatientPhoto(), uuid);
            } else {
                patientDTO.setCreatedAt(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
                saved = patientsDAO.insertPatientToDB(patientDTO, uuid);
                imagesDAO.insertPatientProfileImages(patientDTO.getPatientPhoto(), uuid);
            }
            pushIfOnline(activity);
            if (saved) {
                if (!fromSummary) clearSelectedDob(activity);
                openPatientDetail(activity, uuid, patientDTO);
            } else {
                Toast.makeText(activity, "Error of adding the data", Toast.LENGTH_SHORT).show();
            }
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static List<PatientAttributesDTO> buildDemographicAttributes(PatientsDAO patientsDAO,
                                                                         PatientDTO patientDTO,
                                                                         String uuid, String alternateNumber) {
        List<PatientAttributesDTO> attributes = new ArrayList<>();

        int number = (int) (Math.random() * (99999999 - 100 + 1) + 100);
        String registrationNumber = patientDTO.getCountry().substring(0, 2) + "/"
                + patientDTO.getStateprovince().substring(0, 2) + "/"
                + patientDTO.getCityvillage().substring(0, 2) + "/" + number;
        attributes.add(attribute(patientsDAO, uuid, PatientAttributesDTO.Columns.REGISTRATION_NUMBER.value, registrationNumber));
        attributes.add(attribute(patientsDAO, uuid, PatientAttributesDTO.Columns.ALTERNATE_NO.value, StringUtils.getValue(alternateNumber)));
        attributes.add(attribute(patientsDAO, uuid, PatientAttributesDTO.Columns.PROFILE_IMG_TIMESTAMP.value, AppConstants.dateAndTimeUtils.currentDateTime()));
        return attributes;
    }

    private static PatientAttributesDTO attribute(PatientsDAO patientsDAO, String patientUuid,
                                                  String attributeName, String value) {
        PatientAttributesDTO attribute = new PatientAttributesDTO();
        attribute.setUuid(UUID.randomUUID().toString());
        attribute.setPatientuuid(patientUuid);
        attribute.setPersonAttributeTypeUuid(patientsDAO.getUuidForAttribute(attributeName));
        attribute.setValue(value);
        return attribute;
    }

    private static void pushIfOnline(Activity activity) {
        if (!NetworkConnection.isOnline(activity)) return;
        try {
            new SyncDAO().pushDataApi();
            new ImagesPushDAO().patientProfileImagesPush();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static void openPatientDetail(Activity activity, String uuid, PatientDTO patientDTO) {
        Intent intent = new Intent(activity, PatientDetailActivity.class);
        intent.putExtra("patientUuid", uuid);
        intent.putExtra("patientName", patientDTO.getFirstname() + " " + patientDTO.getLastname());
        intent.putExtra("tag", "newPatient");
        intent.putExtra("privacy", activity.getIntent().getStringExtra("privacy"));
        intent.putExtra("hasPrescription", "false");
        activity.startActivity(intent);
        activity.finish();
    }

    private static void clearSelectedDob(Activity activity) {
        SharedPreferences pref = activity.getApplicationContext().getSharedPreferences("dobPatient", 0);
        pref.edit().putString("dobPatient", "").apply();
    }
}
