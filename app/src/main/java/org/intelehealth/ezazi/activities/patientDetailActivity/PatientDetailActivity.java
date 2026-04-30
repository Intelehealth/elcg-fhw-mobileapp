package org.intelehealth.ezazi.activities.patientDetailActivity;

import static org.intelehealth.ezazi.app.AppConstants.OBSTETRICIAN_GYNECOLOGIST;
import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.addNewPatient.AddNewPatientActivity;
import org.intelehealth.ezazi.activities.homeActivity.HomeActivity;
import org.intelehealth.ezazi.activities.searchPatientActivity.SearchPatientActivity;
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.EncounterDAO;
import org.intelehealth.ezazi.database.dao.ImagesDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO;
import org.intelehealth.ezazi.database.dao.VisitsDAO;
import org.intelehealth.ezazi.models.Patient;
import org.intelehealth.ezazi.models.dto.EncounterDTO;
import org.intelehealth.ezazi.models.dto.VisitDTO;
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;
import org.intelehealth.ezazi.utilities.DateAndTimeUtils;
import org.intelehealth.ezazi.utilities.DownloadFilesUtils;
import org.intelehealth.ezazi.utilities.FileUtils;
import org.intelehealth.ezazi.utilities.Logger;
import org.intelehealth.ezazi.utilities.NepaliDateConverter;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.UrlModifiers;
import org.intelehealth.ezazi.utilities.UuidDictionary;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.utils.DateTimeUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class PatientDetailActivity extends BaseActionBarActivity {

    private static final String TAG = PatientDetailActivity.class.getSimpleName();

    // ── Nepali month names (Romanised) ────────────────────────────────────────
    private static final String[] BS_MONTH_NAMES = {
            "Baisakh", "Jestha", "Asar", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

    // ── Fields ────────────────────────────────────────────────────────────────
    String patientName;
    String mGender;
    String visitUuid = null;
    List<String> visitUuidList;
    String patientUuid;
    String intentTag = "";
    String profileImage = "";
    String profileImage1 = "";
    SessionManager sessionManager = null;
    Patient patient = new Patient();
    TextView phoneView;
    EncounterDTO encounterDTO = new EncounterDTO();
    PatientsDAO patientsDAO = new PatientsDAO();
    private boolean hasLicense = false;
    private boolean returning;
    String phistory = "";
    String fhistory = "";
    LinearLayout previousVisitsList;
    String visitValue;
    private String encounterVitals = "";
    private String encounterAdultIntials = "";
    SQLiteDatabase db = null;
    ImageView editbtn;
    Button newVisit;
    IntentFilter filter;
    Myreceiver reMyreceive;
    ImageView photoView;
    ImagesDAO imagesDAO = new ImagesDAO();
    TextView idView;
    String privacy_value_selected;
    ImageView ivPrescription;
    private String hasPrescription = "";
    Context context;
    float float_ageYear_Month;
    List<String> encounterTypeUUIDListFor12Encounters = new ArrayList<>();
    String stage1Hr1_1_EncounterUuid, stage1Hr1_2_EncounterUuid;
    TextView tvBedNumber;

    public static final String VISIT_DR_SPECIALITY  = "3f296939-c6d3-4d2e-b8ca-d7f4bfd42c2d";
    public static final String VISIT_HOLDER         = "a0378be4-d9c6-4cb2-bbf5-777e27a32efc";
    public static final String VISIT_READ_STATUS    = "2e4b62a5-aa71-43e2-abc9-f4a777697b19";

    // ═════════════════════════════════════════════════════════════════════════
    //  Nepali DOB / Age helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Converts a Gregorian DOB string (yyyy-MM-dd) to a BS display string.
     * Display format: "DD MonthName YYYY"  e.g. "15 Baisakh 2055"
     *
     * Uses UTC parsing to stay consistent with how NepaliDateConverter stores
     * dates (also UTC-based after the Bug #1 fix).
     */
    private String gregDobToBsDisplay(String gregYyyyMmDd) {
        if (gregYyyyMmDd == null || gregYyyyMmDd.trim().isEmpty()) return "";
        try {
            // ── FIX: parse with UTC so we get the same day NepaliDateConverter stored ──
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(gregYyyyMmDd);
            int[] bs  = NepaliDateConverter.gregorianToBs(date);
            return String.format(Locale.ENGLISH, "%02d %s %d",
                    bs[2], BS_MONTH_NAMES[bs[1] - 1], bs[0]);
        } catch (Exception e) {
            Log.e(TAG, "gregDobToBsDisplay: failed for " + gregYyyyMmDd, e);
            return "";
        }
    }

    /**
     * Calculates age in completed years from a Gregorian yyyy-MM-dd DOB string.
     * Returns -1 if parsing fails.
     *
     * ── FIX (Bug #3 – leap-year off-by-one) ─────────────────────────────────
     * The original code used DAY_OF_YEAR for the "has birthday passed?" check,
     * which gives the wrong answer across leap/non-leap year boundaries:
     *
     *   DOB  = 2000-03-01  → DAY_OF_YEAR = 61  (2000 is a leap year)
     *   Today = 2001-03-01  → DAY_OF_YEAR = 60  (2001 is not)
     *   Raw diff = 1 year.  60 < 61 → age-- → 0  ← WRONG (should be 1)
     *
     * Fix: compare MONTH + DAY_OF_MONTH instead of DAY_OF_YEAR.
     * ────────────────────────────────────────────────────────────────────────
     */
    private int calcAgeYears(String gregYyyyMmDd) {
        if (gregYyyyMmDd == null || gregYyyyMmDd.trim().isEmpty()) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date birth = sdf.parse(gregYyyyMmDd);
            Calendar b   = Calendar.getInstance();
            b.setTime(birth);
            Calendar now = Calendar.getInstance();

            int age = now.get(Calendar.YEAR) - b.get(Calendar.YEAR);

            // ── FIX: use MONTH + DAY_OF_MONTH, not DAY_OF_YEAR ───────────────
            boolean birthdayNotYetThisYear =
                    now.get(Calendar.MONTH) < b.get(Calendar.MONTH)
                            || (now.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                            && now.get(Calendar.DAY_OF_MONTH) < b.get(Calendar.DAY_OF_MONTH));

            if (birthdayNotYetThisYear) age--;
            return age;
        } catch (Exception e) {
            Log.e(TAG, "calcAgeYears: failed for " + gregYyyyMmDd, e);
            return -1;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_patient_summary);
        super.onCreate(savedInstanceState);
        setupActionBar();
        enableProperPadding(PatientDetailActivity.this);

        sessionManager = new SessionManager(this);
        String language = sessionManager.getAppLanguage();
        if (!language.equalsIgnoreCase("")) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config,
                    getBaseContext().getResources().getDisplayMetrics());
        }

        View viewToolbar = findViewById(R.id.toolbar_common);
        Toolbar toolbar = viewToolbar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        sessionManager = new SessionManager(this);
        reMyreceive = new Myreceiver();
        filter = new IntentFilter("OpenmrsID");
        newVisit = findViewById(R.id.btnStartObservation);
        context = PatientDetailActivity.this;

        Intent intent = this.getIntent();
        if (intent != null) {
            patientUuid           = intent.getStringExtra("patientUuid");
            patientName           = intent.getStringExtra("patientName");
            hasPrescription       = intent.getStringExtra("hasPrescription");
            privacy_value_selected = intent.getStringExtra("privacy");
            intentTag             = intent.getStringExtra("tag");
            Logger.logD(TAG, "Patient ID: "     + patientUuid);
            Logger.logD(TAG, "Patient Name: "   + patientName);
            Logger.logD(TAG, "Intent Tag: "     + intentTag);
        }

        if (hasPrescription != null && hasPrescription.equalsIgnoreCase("true")) {
            ivPrescription.setImageDrawable(getResources().getDrawable(R.drawable.ic_prescription_green));
        }

        editbtn = findViewById(R.id.edit_button);
        editbtn.setOnClickListener(v -> {
            Intent intent2 = new Intent(PatientDetailActivity.this, AddNewPatientActivity.class);
            intent2.putExtra("patientUuid", patientUuid);
            intent2.putExtra("fromSummary", true);
            intent2.putExtra("editDetails", true);
            startActivity(intent2);
        });

        setDisplay(patientUuid);

        newVisit.setOnClickListener(v -> {
            String thisDate = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT);
            String uuid = UUID.randomUUID().toString();

            Intent intent2 = new Intent(PatientDetailActivity.this, TimelineVisitSummaryActivity.class);
            String fullName = patient.getFirst_name() + " " + patient.getLast_name();
            String patientfullName;
            if (patient.getMiddle_name() != null && !patient.getMiddle_name().equalsIgnoreCase("")
                    && !patient.getMiddle_name().isEmpty()) {
                patientfullName = patient.getFirst_name() + " " + patient.getMiddle_name() + " " + patient.getLast_name();
            } else {
                patientfullName = patient.getFirst_name() + " " + patient.getLast_name();
            }

            VisitDTO visitDTO = new VisitDTO();
            visitDTO.setUuid(uuid);
            visitDTO.setPatientuuid(patient.getUuid());
            visitDTO.setStartdate(thisDate);
            visitDTO.setVisitTypeUuid(UuidDictionary.VISIT_TELEMEDICINE);
            visitDTO.setLocationuuid(sessionManager.getLocationUuid());
            visitDTO.setSyncd(false);
            visitDTO.setEnddate(null);
            visitDTO.setCreatoruuid(sessionManager.getCreatorID());
            VisitsDAO visitsDAO = new VisitsDAO();

            try {
                visitsDAO.insertPatientToDB(visitDTO);
                VisitAttributeListDAO sa = new VisitAttributeListDAO();
                sa.insertVisitAttributes(uuid, OBSTETRICIAN_GYNECOLOGIST, VISIT_DR_SPECIALITY);
                sa.insertVisitAttributes(uuid, sessionManager.getProviderID(), VISIT_HOLDER);
                sa.insertVisitAttributes(uuid, "$", VISIT_READ_STATUS);
                sa.insertVisitAttributes(uuid, "false", UuidDictionary.DECISION_PENDING);
            } catch (DAOException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }

                String uuid = UUID.randomUUID().toString();
              /*  EncounterDAO encounterDAO = new EncounterDAO();
                encounterDTO = new EncounterDTO();
                encounterDTO.setUuid(UUID.randomUUID().toString());
                encounterDTO.setEncounterTypeUuid(encounterDAO.getEncounterTypeUuid("ENCOUNTER_VITALS"));
                encounterDTO.setEncounterTime(thisDate);
                encounterDTO.setVisituuid(uuid);
                encounterDTO.setSyncd(false);
                encounterDTO.setProvideruuid(sessionManager.getProviderID());
                Log.d("DTO", "DTO:detail " + encounterDTO.getProvideruuid());
                encounterDTO.setVoided(0);
                encounterDTO.setPrivacynotice_value(privacy_value_selected);//privacy value added.

                try {
                    encounterDAO.createEncountersToDB(encounterDTO);
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
*/
               /* // create encounter adultinitial
                EncounterDAO encounterDAO = new EncounterDAO();
                EncounterDTO encounterDTO = new EncounterDTO();
                encounterDTO.setUuid(UUID.randomUUID().toString());
                encounterDTO.setEncounterTypeUuid(encounterDAO.getEncounterTypeUuid("ENCOUNTER_ADULTINITIAL"));
                encounterDTO.setEncounterTime(AppConstants.dateAndTimeUtils.currentDateTime());
                encounterDTO.setVisituuid(uuid);
                encounterDTO.setSyncd(false);
                encounterDTO.setProvideruuid(sessionManager.getProviderID());
                Log.d("DTO", "DTOcomp: " + encounterDTO.getProvideruuid());
                encounterDTO.setVoided(0);
                try {
                    encounterDAO.createEncountersToDB(encounterDTO);
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                // end*/

               /* InteleHealthDatabaseHelper mDatabaseHelper = new InteleHealthDatabaseHelper(PatientDetailActivity.this);
                SQLiteDatabase sqLiteDatabase = mDatabaseHelper.getReadableDatabase();

                String CREATOR_ID = sessionManager.getCreatorID();
                returning = false;
                sessionManager.setReturning(returning);

                String[] cols = {"value"};
                Cursor cursor = sqLiteDatabase.query("tbl_obs", cols, "encounteruuid=? and conceptuuid=?",// querying for PMH (Past Medical History)
                        new String[]{encounterAdultIntials, UuidDictionary.RHK_MEDICAL_HISTORY_BLURB},
                        null, null, null);

                if (cursor.moveToFirst()) {
                    // rows present
                    do {
                        // so that null data is not appended
                        phistory = phistory + cursor.getString(0);

                    }
                    while (cursor.moveToNext());
                    returning = true;
                    sessionManager.setReturning(returning);
                }
                cursor.close();
                */

//                Cursor cursor1 = sqLiteDatabase.query("tbl_obs", cols, "encounteruuid=? and conceptuuid=?",// querying for FH (Family History)
//                        new String[]{encounterAdultIntials, UuidDictionary.RHK_FAMILY_HISTORY_BLURB},
//                        null, null, null);
//                if (cursor1.moveToFirst()) {
//                    // rows present
//                    do {
//                        fhistory = fhistory + cursor1.getString(0);
//                    }
//                    while (cursor1.moveToNext());
//                    returning = true;
//                    sessionManager.setReturning(returning);
//                }
//                cursor1.close();

                // Will display data for patient as it is present in database
                // Toast.makeText(PatientDetailActivity.this,"PMH: "+phistory,Toast.LENGTH_SHORT).sƒhow();
                // Toast.makeText(PatientDetailActivity.this,"FH: "+fhistory,Toast.LENGTH_SHORT).show();

                Intent intent2 = new Intent(PatientDetailActivity.this, TimelineVisitSummaryActivity.class);
                String fullName = patient.getFirst_name() + " " + patient.getLast_name();
                // For Timeline Notification...
                String patientfullName = "";
                if (patient.getMiddle_name() != null && !patient.getMiddle_name().equalsIgnoreCase("")
                        && !patient.getMiddle_name().isEmpty()) {
                    patientfullName = patient.getFirst_name() + " " + patient.getMiddle_name() + " " + patient.getLast_name();
                } else {
                    patientfullName = patient.getFirst_name() + " " + patient.getLast_name();
                }
                // end...

                // Visit is created when clicked on the New Visit button...
                VisitDTO visitDTO = new VisitDTO();
                visitDTO.setUuid(uuid);
                visitDTO.setPatientuuid(patient.getUuid());
                visitDTO.setStartdate(thisDate);
                visitDTO.setVisitTypeUuid(UuidDictionary.VISIT_TELEMEDICINE);
                visitDTO.setLocationuuid(sessionManager.getLocationUuid());
                visitDTO.setSyncd(false);
                visitDTO.setEnddate(null);
                visitDTO.setCreatoruuid(sessionManager.getCreatorID());//static
                VisitsDAO visitsDAO = new VisitsDAO();

                try {
                    Log.d(TAG, "onClick: check kz");
//                    ArrayList<VisitAttributeDTO> attributes = new ArrayList<>();
//                    VisitAttributeDTO general = VisitAttributeDTO.generateNew(uuid, "General Physician", VISIT_ATTR_TYPE_UUID);
//                    VisitAttributeDTO holder = VisitAttributeDTO.generateNew(uuid, sessionManager.getProviderID(), VISIT_HOLDER);
//                    attributes.add(general);
//                    attributes.add(holder);
//                    visitDTO.setVisitAttributeDTOS(attributes);
                    visitsDAO.insertPatientToDB(visitDTO);

                    VisitAttributeListDAO speciality_attributes = new VisitAttributeListDAO();
                    speciality_attributes
                            .insertVisitAttributes(uuid, OBSTETRICIAN_GYNECOLOGIST, VISIT_DR_SPECIALITY);
                    speciality_attributes
                            .insertVisitAttributes(uuid, sessionManager.getProviderID(), VISIT_HOLDER);
                    speciality_attributes
                            .insertVisitAttributes(uuid, "$", VISIT_READ_STATUS);
                    speciality_attributes
                            .insertVisitAttributes(uuid, "false", UuidDictionary.DECISION_PENDING);


                } catch (DAOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onClick: e message : " + e.getLocalizedMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                // end - visit

                // Create a static Stage1_Hr1_1 encounter so than to link all the other encounters.
                // Start - encounter
                boolean isInserted = false;
                EncounterDAO eDAO = new EncounterDAO();
                EncounterDTO eDTO = new EncounterDTO();
                stage1Hr1_1_EncounterUuid = UUID.randomUUID().toString();
                eDTO.setUuid(stage1Hr1_1_EncounterUuid);
                eDTO.setVisituuid(uuid);
                eDTO.setEncounterTime(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
                eDTO.setProvideruuid(sessionManager.getProviderID());
                eDTO.setEncounterTypeUuid(eDAO.getEncounterTypeUuid("Stage1_Hour1_1"));
                eDTO.setSyncd(false); // false as this is the one that is started and would be pushed in the payload...
                eDTO.setVoided(0);

                Log.d("DTO", "DTOcomp: " + eDTO.getProvideruuid());
                try {
                    isInserted = eDAO.createEncountersToDB(eDTO);
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                // end - encounter

                // This 23 ones would be created initially itself with sync = true so that they wont be pushed bt only created.
                  /*  addIntoEncounterList23UUIDs();
                    for (int i = 0; i < encounterTypeUUIDListFor12Encounters.size(); i++) {
                        create23EncountersForTimeline(uuid, encounterTypeUUIDListFor12Encounters.get(i));
                    }*/
                // end - Encounter


                intent2.putExtra("patientUuid", patientUuid);
                intent2.putExtra("visitUuid", uuid);
                intent2.putExtra("name", fullName);
                intent2.putExtra("patientNameTimeline", patientfullName);
                intent2.putExtra("tag", "new");
                intent2.putExtra("encounter_time", eDTO.getEncounterTime());
                intent2.putExtra("Stage1_Hour1_1", "Stage1_Hour1_1");
                intent2.putExtra("providerID", sessionManager.getProviderID());
                startActivity(intent2);
                finish();
            }

            intent2.putExtra("patientUuid", patientUuid);
            intent2.putExtra("visitUuid", uuid);
            intent2.putExtra("name", fullName);
            intent2.putExtra("patientNameTimeline", patientfullName);
            intent2.putExtra("tag", "new");
            intent2.putExtra("encounter_time", eDTO.getEncounterTime());
            intent2.putExtra("Stage1_Hour1_1", "Stage1_Hour1_1");
            intent2.putExtra("providerID", sessionManager.getProviderID());
            startActivity(intent2);
            finish();
        });

        Log.e(TAG, "onCreate: patient creator => " + patient.getCreatorUuid());
        if (!patient.getCreatorUuid().equals(sessionManager.getCreatorID())) {
            editbtn.setVisibility(View.GONE);
            newVisit.setEnabled(false);
        }
    }

    @Override
    protected int getScreenTitle() { return R.string.patient_info; }

    // ═════════════════════════════════════════════════════════════════════════
    //  setDisplay — loads patient data and populates the UI
    //  DOB is shown in BS format; Age is computed from the Gregorian DOB.
    // ═════════════════════════════════════════════════════════════════════════

        eDTO.setUuid(UUID.randomUUID().toString());
        eDTO.setVisituuid(uuid);
        eDTO.setEncounterTime(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
        eDTO.setProvideruuid(sessionManager.getProviderID());
        eDTO.setEncounterTypeUuid(eDAO.getEncounterTypeUuid(encounterTypeUUIDValue));
        eDTO.setSyncd(true); // so that this 23 encounters are just created but not pushed to the payload...
        eDTO.setVoided(0);

        Log.d("DTO", "DTOcomp: " + eDTO.getProvideruuid());
        try {
            eDAO.createEncountersToDB(eDTO);
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

    }

    // Add this 12 into the arraylist and then pass this arraylist to the function so as to optimize code and reduce no of lines...
    private void addIntoEncounterList23UUIDs() {
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour1_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour2_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour2_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour3_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour3_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour4_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour4_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour5_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour5_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour6_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour6_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour7_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour7_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour8_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour8_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour9_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour9_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour10_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour10_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour11_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour11_2");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour12_1");
        encounterTypeUUIDListFor12Encounters.add("Stage1_Hour12_2");
    }

//    private void LoadFamilyMembers() {
//
//        String houseHoldValue = "";
//        try {
//            houseHoldValue = patientsDAO.getHouseHoldValue(patientUuid);
//        } catch (DAOException e) {
//            FirebaseCrashlytics.getInstance().recordException(e);
//        }
//
//        if (!houseHoldValue.equalsIgnoreCase("")) {
//            //Fetch all patient UUID from houseHoldValue
//            try {
//                List<FamilyMemberRes> listPatientNames = new ArrayList<>();
//                List<String> patientUUIDs = new ArrayList<>(patientsDAO.getPatientUUIDs(houseHoldValue));
//                Log.e("patientUUIDs", "" + patientUUIDs);
//
//                for (int i = 0; i < patientUUIDs.size(); i++) {
//                    if (!patientUUIDs.get(i).equals(patientUuid)) {
//                        listPatientNames.addAll(patientsDAO.getPatientName(patientUUIDs.get(i)));
//                    }
//                }
//
//                if (listPatientNames.size() > 0) {
//                    tvNoFamilyMember.setVisibility(View.GONE);
//                    rvFamilyMember.setVisibility(View.VISIBLE);
//                    FamilyMemberAdapter familyMemberAdapter = new FamilyMemberAdapter(listPatientNames, this);
//                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//                    rvFamilyMember.setLayoutManager(linearLayoutManager);
//                    rvFamilyMember.setAdapter(familyMemberAdapter);
//                } else {
//                    tvNoFamilyMember.setVisibility(View.VISIBLE);
//                    rvFamilyMember.setVisibility(View.GONE);
//                }
//
//            } catch (DAOException e) {
//                FirebaseCrashlytics.getInstance().recordException(e);
//            }
//        }
//    }

    @Override
    protected void onStart() {
        // registerReceiver(reMyreceive, filter);
        ContextCompat.registerReceiver(this, reMyreceive, filter, ContextCompat.RECEIVER_EXPORTED);

        super.onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(reMyreceive);
        super.onDestroy();
    }

//    public float age_in_Decimal(String age) {
//        float age_float = 0;
//        //2 years 4 months 4 days
//        //int age_int = Integer.parseInt(age.replaceAll("[\\D]", "")); //244
//        String ageTrim = age.trim();
//        String year = String.valueOf(ageTrim.charAt(ageTrim.indexOf("years") - 1));
//        String month = String.valueOf(ageTrim.charAt(ageTrim.indexOf("months") - 1));
//        String result = year + " " + month;
//        //int month = age_.indexOf("months") - 1;
//
//        return age_float;
//    }


    public void setDisplay(String dataString) {

        // ── 1. Load patient from tbl_patient ─────────────────────────────────
        String[] patientColumns = {"uuid","openmrs_id","first_name","middle_name","last_name",
                "gender","date_of_birth","address1","address2","city_village",
                "state_province","postal_code","country","phone_number",
                "patient_photo","creatoruuid"};
        Cursor idCursor = db.query("tbl_patient", patientColumns, "uuid = ?",
                new String[]{dataString}, null, null, null);
        if (idCursor.moveToFirst()) {
            do {
                patient.setUuid(idCursor.getString(idCursor.getColumnIndexOrThrow("uuid")));
                patient.setOpenmrs_id(idCursor.getString(idCursor.getColumnIndexOrThrow("openmrs_id")));
                patient.setFirst_name(idCursor.getString(idCursor.getColumnIndexOrThrow("first_name")));
                patient.setMiddle_name(idCursor.getString(idCursor.getColumnIndexOrThrow("middle_name")));
                patient.setLast_name(idCursor.getString(idCursor.getColumnIndexOrThrow("last_name")));
                patient.setGender(idCursor.getString(idCursor.getColumnIndexOrThrow("gender")));
                patient.setDate_of_birth(idCursor.getString(idCursor.getColumnIndexOrThrow("date_of_birth")));
                patient.setAddress1(idCursor.getString(idCursor.getColumnIndexOrThrow("address1")));
                patient.setAddress2(idCursor.getString(idCursor.getColumnIndexOrThrow("address2")));
                patient.setCity_village(idCursor.getString(idCursor.getColumnIndexOrThrow("city_village")));
                patient.setState_province(idCursor.getString(idCursor.getColumnIndexOrThrow("state_province")));
                patient.setPostal_code(idCursor.getString(idCursor.getColumnIndexOrThrow("postal_code")));
                patient.setCountry(idCursor.getString(idCursor.getColumnIndexOrThrow("country")));
                patient.setPhone_number(idCursor.getString(idCursor.getColumnIndexOrThrow("phone_number")));
                patient.setPatient_photo(idCursor.getString(idCursor.getColumnIndexOrThrow("patient_photo")));
                patient.setCreatorUuid(idCursor.getString(idCursor.getColumnIndexOrThrow("creatoruuid")));
            } while (idCursor.moveToNext());
        }
        idCursor.close();

        // ── 2. Load patient attributes ────────────────────────────────────────
        Cursor idCursor1 = db.query("tbl_patient_attribute",
                new String[]{"value","person_attribute_type_uuid"},
                "patientuuid = ?", new String[]{dataString}, null, null, null);
        String name = "";
        if (idCursor1.moveToFirst()) {
            do {
                try {
                    name = patientsDAO.getAttributesName(
                            idCursor1.getString(idCursor1.getColumnIndexOrThrow("person_attribute_type_uuid")));
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                String val = idCursor1.getString(idCursor1.getColumnIndexOrThrow("value"));
                switch (name.toLowerCase()) {
                    case "caste":                  patient.setCaste(val); break;
                    case "telephone number":       patient.setPhone_number(val); break;
                    case "education level":        patient.setEducation_level(val); break;
                    case "economic status":        patient.setEconomic_status(val); break;
                    case "occupation":             patient.setOccupation(val); break;
                    case "son/wife/daughter":      patient.setSdw(val); break;
                    case "profileimagetimestamp":  profileImage1 = val; break;
                    case "ezazi registration number": patient.seteZaziRegNumber(val); break;
                }
            } while (idCursor1.moveToNext());
        }
        idCursor1.close();

        // ── 3. Bind UI references ─────────────────────────────────────────────
        idView          = findViewById(R.id.textView_ID);
        TextView patinetName     = findViewById(R.id.textView_name);
        TextView dobView         = findViewById(R.id.textView_DOB);
        TextView ageView         = findViewById(R.id.textView_age);
        TextView addrFinalView   = findViewById(R.id.textView_address_final);
        tvBedNumber              = findViewById(R.id.textView_bed_no);
        phoneView                = findViewById(R.id.textView_phone);
        ImageView whatsapp_no    = findViewById(R.id.whatsapp_no);
        ImageView calling        = findViewById(R.id.calling);
        TextView medHistView     = findViewById(R.id.textView_patHist);
        TextView famHistView     = findViewById(R.id.textView_famHist);
        TextView textView_UER_No = findViewById(R.id.textView_UER_No);

        textView_UER_No.setText(patient.geteZaziRegNumber());

        // ── 4. Config file (unchanged) ────────────────────────────────────────
        if (!sessionManager.getLicenseKey().isEmpty()) hasLicense = true;
        try {
            JSONObject obj = hasLicense
                    ? new JSONObject(Objects.requireNonNullElse(
                    FileUtils.readFileRoot(AppConstants.CONFIG_FILE_NAME, context),
                    String.valueOf(FileUtils.encodeJSON(context, AppConstants.CONFIG_FILE_NAME))))
                    : new JSONObject(String.valueOf(FileUtils.encodeJSON(this, AppConstants.CONFIG_FILE_NAME)));
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(getApplicationContext(), "JsonException" + e, Toast.LENGTH_LONG).show();
        }

        // ── 5. Bed number ─────────────────────────────────────────────────────
        try {
            String checkUUId = patient.getUuid();
            if (checkUUId != null && !checkUUId.isEmpty())
                tvBedNumber.setText(getBedNumber(checkUUId));
        } catch (DAOException e) {
            e.printStackTrace();
        }

        // ── 6. Patient name ───────────────────────────────────────────────────
        patientName = (patient.getMiddle_name() == null || patient.getMiddle_name().isEmpty())
                ? patient.getFirst_name() + " " + patient.getLast_name()
                : patient.getFirst_name() + " " + patient.getMiddle_name() + " " + patient.getLast_name();
        patinetName.setText(patientName);

        // ── 7. Profile photo ──────────────────────────────────────────────────
        try {
            profileImage = imagesDAO.getPatientProfileChangeTime(patientUuid);
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        if (patient.getPatient_photo() == null || patient.getPatient_photo().isEmpty()) {
            if (NetworkConnection.isOnline(getApplication())) profilePicDownloaded();
        }
        if (!profileImage.equalsIgnoreCase(profileImage1)) {
            if (NetworkConnection.isOnline(getApplication())) profilePicDownloaded();
        }

        // ── 8. OpenMRS ID ─────────────────────────────────────────────────────
        if (patient.getOpenmrs_id() != null && !patient.getOpenmrs_id().isEmpty()) {
            idView.setText("Patient ID: " + patient.getOpenmrs_id());
        } else {
            idView.setText(getString(R.string.patient_not_registered));
        }
        setTitle(patient.getOpenmrs_id());

        // ── 9. DOB in BS format ───────────────────────────────────────────────
        String gregDob = patient.getDate_of_birth();
        Log.d(TAG, "setDisplay: gregDob = " + gregDob);

        if (gregDob != null && !gregDob.isEmpty()) {
            String bsDobDisplay = gregDobToBsDisplay(gregDob);
            if (!bsDobDisplay.isEmpty()) {
                dobView.setText(bsDobDisplay + " BS");
            } else {
                dobView.setText(DateAndTimeUtils.getFormatedDateOfBirthAsView(gregDob) + " BS");
                Log.w(TAG, "setDisplay: BS conversion failed, showing Gregorian DOB");
            }
        } else {
            dobView.setText(getString(R.string.not_provided));
        }

        // ── 10. Age — "X years - Y months - Z days" ──────────────────────────
        //
        // ── FIX (Bug #2 – age format inconsistency) ──────────────────────────
        // Both screens now use DateAndTimeUtils.getAgeInYearMonthNew() which
        // returns the full "X years - Y months - Z days" string.
        // The Fragment's mAge field is an INPUT control so it still shows only
        // the integer year count — that is intentional and correct.
        // This detail screen always shows the full breakdown.
        // ─────────────────────────────────────────────────────────────────────
        float_ageYear_Month = DateAndTimeUtils.getFloat_Age_Year_Month(gregDob);
        int ageYears = calcAgeYears(gregDob);

        if (ageYears >= 0) {
            String ageDetail = DateAndTimeUtils.getAgeInYearMonthNew(gregDob, context);
            if (ageDetail != null && !ageDetail.trim().isEmpty()) {
                ageView.setText(ageDetail.trim());
            } else {
                // Fallback: build "X years - Y months - Z days" manually
                ageView.setText(buildAgeFallback(gregDob, ageYears));
            }
            Log.d(TAG, "setDisplay: ageYears = " + ageYears + " | detail = " + ageDetail);
        } else {
            ageView.setText(getString(R.string.not_provided));
        }

        // ── 11. Address ───────────────────────────────────────────────────────
        addrFinalView.setText(String.format("%s, %s",
                patient.getState_province(), patient.getCountry()));

        // ── 12. Phone ─────────────────────────────────────────────────────────
        phoneView.setText(patient.getPhone_number());
        if (patient.getPhone_number() == null || patient.getPhone_number().isEmpty()) {
            calling.setVisibility(View.GONE);
            whatsapp_no.setVisibility(View.GONE);
            phoneView.setText(getString(R.string.not_provided));
        }

        // ── 13. Visit history ─────────────────────────────────────────────────
        if (visitUuid != null && !visitUuid.isEmpty()) {
            CardView histCardView = findViewById(R.id.cardView_history);
            histCardView.setVisibility(View.GONE);
        } else {
            visitUuidList = new ArrayList<>();
            Cursor visitIDCursor = db.query("tbl_visit", null,
                    "patientuuid = ?", new String[]{patientUuid}, null, null, null);
            if (visitIDCursor != null && visitIDCursor.moveToFirst()) {
                do {
                    visitUuid = visitIDCursor.getString(visitIDCursor.getColumnIndexOrThrow("uuid"));
                    visitUuidList.add(visitUuid);
                } while (visitIDCursor.moveToNext());
            }
            if (visitIDCursor != null) visitIDCursor.close();

            for (String visituuid : visitUuidList) {
                Logger.logD(TAG, visituuid);
                EncounterDAO encounterDAO = new EncounterDAO();
                Cursor encounterCursor = db.query("tbl_encounter", null,
                        "visituuid = ?", new String[]{visituuid}, null, null, null);
                if (encounterCursor != null && encounterCursor.moveToFirst()) {
                    do {
                        String encType = encounterCursor.getString(
                                encounterCursor.getColumnIndexOrThrow("encounter_type_uuid"));
                        if (encounterDAO.getEncounterTypeUuid("ENCOUNTER_VITALS").equalsIgnoreCase(encType))
                            encounterVitals = encounterCursor.getString(encounterCursor.getColumnIndexOrThrow("uuid"));
                        if (encounterDAO.getEncounterTypeUuid("ENCOUNTER_ADULTINITIAL").equalsIgnoreCase(encType))
                            encounterAdultIntials = encounterCursor.getString(encounterCursor.getColumnIndexOrThrow("uuid"));
                    } while (encounterCursor.moveToNext());
                }
                if (encounterCursor != null) encounterCursor.close();
            }
            familyHistory(famHistView, patientUuid, encounterAdultIntials);
            pastMedicalHistory(medHistView, patientUuid, encounterAdultIntials);
        }

        // ── 14. Click listeners (WhatsApp / Call) ─────────────────────────────
        whatsapp_no.setOnClickListener(v -> {
            String phoneNumberWithCountryCode = "+91" + phoneView.getText().toString();
            String message = getString(R.string.hello_my_name_is) + sessionManager.getChwname();
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(String.format("https://api.whatsapp.com/send?phone=%s&text=%s",
                            phoneNumberWithCountryCode, message))));
        });
        calling.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneView.getText().toString()));
            startActivity(callIntent);
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Everything below is unchanged from the original
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    protected void onStart() {
        ContextCompat.registerReceiver(this, reMyreceive, filter, ContextCompat.RECEIVER_EXPORTED);
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(reMyreceive);
        super.onDestroy();
    }

    private void create23EncountersForTimeline(String uuid, String encounterTypeUUIDValue) {
        EncounterDAO eDAO = new EncounterDAO();
        EncounterDTO eDTO = new EncounterDTO();
        eDTO.setUuid(UUID.randomUUID().toString());
        eDTO.setVisituuid(uuid);
        eDTO.setEncounterTime(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
        eDTO.setProvideruuid(sessionManager.getProviderID());
        eDTO.setEncounterTypeUuid(eDAO.getEncounterTypeUuid(encounterTypeUUIDValue));
        eDTO.setSyncd(true);
        eDTO.setVoided(0);
        try { eDAO.createEncountersToDB(eDTO); }
        catch (DAOException e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    private void addIntoEncounterList23UUIDs() {
        String[] stages = {"Stage1_Hour1_2","Stage1_Hour2_1","Stage1_Hour2_2","Stage1_Hour3_1",
                "Stage1_Hour3_2","Stage1_Hour4_1","Stage1_Hour4_2","Stage1_Hour5_1",
                "Stage1_Hour5_2","Stage1_Hour6_1","Stage1_Hour6_2","Stage1_Hour7_1",
                "Stage1_Hour7_2","Stage1_Hour8_1","Stage1_Hour8_2","Stage1_Hour9_1",
                "Stage1_Hour9_2","Stage1_Hour10_1","Stage1_Hour10_2","Stage1_Hour11_1",
                "Stage1_Hour11_2","Stage1_Hour12_1","Stage1_Hour12_2"};
        for (String s : stages) encounterTypeUUIDListFor12Encounters.add(s);
    }

    public void profilePicDownloaded() {
        UrlModifiers urlModifiers = new UrlModifiers();
        String url = urlModifiers.patientProfileImageUrl(patientUuid);
        Observable<ResponseBody> dl = AppConstants.apiInterface.PERSON_PROFILE_PIC_DOWNLOAD(
                url, "Basic " + sessionManager.getEncoded());
        dl.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableObserver<ResponseBody>() {
                    @Override public void onNext(ResponseBody file) {
                        new DownloadFilesUtils().saveToDisk(file, patientUuid);
                    }
                    @Override public void onError(Throwable e) { Logger.logD(TAG, e.getMessage()); }
                    @Override public void onComplete() {
                        PatientsDAO pd = new PatientsDAO();
                        try { pd.updatePatientPhoto(patientUuid, AppConstants.IMAGE_PATH + patientUuid + ".jpg"); }
                        catch (DAOException e) { FirebaseCrashlytics.getInstance().recordException(e); }
                        try { imagesDAO.insertPatientProfileImages(AppConstants.IMAGE_PATH + patientUuid + ".jpg", patientUuid); }
                        catch (DAOException e) { FirebaseCrashlytics.getInstance().recordException(e); }
                    }
                });
    }

    public class Myreceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String id = "Patient ID: " + patientsDAO.getOpenmrsId(patientUuid);
                idView.setText(id);
            } catch (DAOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            setTitle(idView.getText());
        }
    }

    public void familyHistory(TextView famHistView, String patientuuid,
                              String EncounterAdultInitials_LatestVisit) {
        Cursor visitCursor = db.query("tbl_visit", new String[]{"uuid, startdate","enddate"},
                "patientuuid = ?", new String[]{patientuuid}, null, null, "startdate");
        previousVisitsList = findViewById(R.id.linearLayout_previous_visits);
        if (visitCursor.getCount() >= 1 && visitCursor.moveToLast()) {
            do {
                String visit_id = visitCursor.getString(visitCursor.getColumnIndexOrThrow("uuid"));
                Cursor encounterCursor = db.query("tbl_encounter", null,
                        "visituuid = ?", new String[]{visit_id}, null, null, null);
                if (encounterCursor != null) encounterCursor.close();

                String famHistSelection = "encounteruuid = ? AND conceptuuid = ? And voided!='1'";
                String[] famHistArgs = {EncounterAdultInitials_LatestVisit, UuidDictionary.RHK_FAMILY_HISTORY_BLURB};
                Cursor famHistCursor = db.query("tbl_obs", new String[]{"value"," conceptuuid"},
                        famHistSelection, famHistArgs, null, null, null);
                famHistCursor.moveToLast();
                String famHistValue;
                try { famHistValue = famHistCursor.getString(famHistCursor.getColumnIndexOrThrow("value")); }
                catch (Exception e) { famHistValue = ""; }
                finally { famHistCursor.close(); }

                famHistView.setText(famHistValue != null && !famHistValue.isEmpty()
                        ? Html.fromHtml(famHistValue) : getString(R.string.string_no_hist));
            } while (visitCursor.moveToPrevious());
        }
        visitCursor.close();
    }

    public void pastMedicalHistory(TextView medHistView, String patientuuid,
                                   String EncounterAdultInitials_LatestVisit) {
        Cursor visitCursor = db.query("tbl_visit", new String[]{"uuid, startdate","enddate"},
                "patientuuid = ?", new String[]{patientuuid}, null, null, "startdate");
        previousVisitsList = findViewById(R.id.linearLayout_previous_visits);
        if (visitCursor.getCount() >= 1 && visitCursor.moveToLast()) {
            do {
                String visit_id = visitCursor.getString(visitCursor.getColumnIndexOrThrow("uuid"));
                Cursor encounterCursor = db.query("tbl_encounter", null,
                        "visituuid = ?", new String[]{visit_id}, null, null, null);
                if (encounterCursor != null) encounterCursor.close();

                String medHistSelection = "encounteruuid = ? AND conceptuuid = ? And voided!='1'";
                String[] medHistArgs = {EncounterAdultInitials_LatestVisit, UuidDictionary.RHK_MEDICAL_HISTORY_BLURB};
                Cursor medHistCursor = db.query("tbl_obs", new String[]{"value"," conceptuuid"},
                        medHistSelection, medHistArgs, null, null, null);
                medHistCursor.moveToLast();
                String medHistValue;
                try { medHistValue = medHistCursor.getString(medHistCursor.getColumnIndexOrThrow("value")); }
                catch (Exception e) { medHistValue = ""; }
                finally { medHistCursor.close(); }

                medHistView.setText(medHistValue != null && !medHistValue.isEmpty()
                        ? Html.fromHtml(medHistValue) : getString(R.string.string_no_hist));
            } while (visitCursor.moveToPrevious());
        }
        visitCursor.close();
    }

    @Override
    protected void onStop() { super.onStop(); }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: finish(); return true;
            case R.id.detail_home:
                startActivity(new Intent(PatientDetailActivity.this, HomeActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Fallback age string when DateAndTimeUtils.getAgeInYearMonthNew() returns null/empty.
     * Format: "X years - Y months - Z days"
     */
    private String buildAgeFallback(String gregYyyyMmDd, int knownYears) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date birth = sdf.parse(gregYyyyMmDd);
            Calendar b   = Calendar.getInstance();
            b.setTime(birth);
            Calendar now = Calendar.getInstance();

            int years  = knownYears;
            // Advance birth by years to find remaining months
            b.add(Calendar.YEAR, years);

            int months = 0;
            while (b.compareTo(now) <= 0) {
                b.add(Calendar.MONTH, 1);
                months++;
            }
            b.add(Calendar.MONTH, -1); // step back one overshoot
            months = Math.max(0, months - 1);

            long remainMs   = now.getTimeInMillis() - b.getTimeInMillis();
            int  days       = (int) (remainMs / (1000L * 60 * 60 * 24));

            return years  + " " + getString(R.string.years)  + " - "
                    + months + " " + getString(R.string.months) + " - "
                    + days   + " " + getString(R.string.days);
        } catch (Exception e) {
            // Ultra-safe fallback
            return knownYears + " " + getString(R.string.years);
        }
    }


    private String getBedNumber(String patientuuid) throws DAOException {
        String bedNumber = null;
        Cursor idCursor = db.rawQuery(
                "SELECT value FROM tbl_patient_attribute where patientuuid = ? AND person_attribute_type_uuid='d0786817-68d9-4226-b311-3de68d534b9e'",
                new String[]{patientuuid});
        try {
            while (idCursor.moveToNext())
                bedNumber = idCursor.getString(idCursor.getColumnIndexOrThrow("value"));
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
        }
        idCursor.close();
        return bedNumber;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(PatientDetailActivity.this, SearchPatientActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onNetworkAvailable(@NotNull NetworkStatus status) {
        super.onNetworkAvailable(status);
    }

    @Override
    public void onNetworkChanged(@NotNull NetworkStatus status) {
        super.onNetworkChanged(status);
    }

    @Override
    public void onNetworkLost() {
        super.onNetworkLost();
    }
}