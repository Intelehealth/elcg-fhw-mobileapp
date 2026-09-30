package org.intelehealth.ezazi.activities.addNewPatient;

import static org.intelehealth.ezazi.utilities.SupportUtils.enableProperPadding;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.patientDetailActivity.PatientDetailActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.ImagesDAO;
import org.intelehealth.ezazi.database.dao.ImagesPushDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.SyncDAO;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO;
import org.intelehealth.ezazi.models.dto.PatientDTO;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.AppRegion;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.StringUtils;
import org.intelehealth.klivekit.utils.DateTimeUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.intelehealth.ezazi.optimized_sync.network.NetworkStatus;
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.jetbrains.annotations.NotNull;

public class AddNewPatientActivity extends BaseActionBarActivity
        implements AddNewPatientActivity.RegistrationStepHost {
    private static final String TAG = "AddNewPatientActivity";
    private static final String KEY_STEP_INDEX = "stepIndex";
    private static final String KEY_DRAFT = "draft";
    public static final int NO_STEP = -1;
    public static final int PAGE_PERSONAL = 0;
    public static final int PAGE_ADDRESS = 1;
    public static final int PAGE_OTHER = 2;

    private PatientRegistrationDraft draft = new PatientRegistrationDraft();
    private int stepCount = 3;
    private int currentStepIndex = PAGE_PERSONAL;
    private int previousStepIndex = NO_STEP;

    /** What a step fragment is allowed to ask of the Activity. Nothing calls it until S3. */
    public interface RegistrationStepHost {
        PatientRegistrationDraft draft();

        String resolveUuid();

        void onStepCompleted();

        void onStepBack();
    }

    @Override
    public PatientRegistrationDraft draft() {
        return draft;
    }

    /**
     * The one uuid decision in registration. Editing reuses the patient's uuid; creating mints once
     * and remembers it, so every step of one registration resolves to the same value.
     */
    @Override
    public String resolveUuid() {
        String editing = draft.getEditingPatientUuid();
        if (draft.getFromSummary() && editing != null && !editing.isEmpty()) return editing;
        if (draft.getNewPatientUuid() == null) {
            draft.setNewPatientUuid(UUID.randomUUID().toString());
        }
        return draft.getNewPatientUuid();
    }

    /**
     * The four values with no field on any step. They are the Activity's because no fragment owns
     * them, and the registration number additionally must not be re-minted on an edit.
     */
    private void addOwnedAttributes(List<PatientAttributesDTO> attrList) {
        PatientsDAO dao = new PatientsDAO();
        PatientDTO patient = draft.getPatient();
        String uuid = resolveUuid();

        if (!draft.getFromSummary()) {
            int num = (int) (Math.random() * (99999999 - 100 + 1) + 100);
            String regNumber = regNumberPart(patient.getCountry()) + "/"
                    + regNumberPart(patient.getStateprovince()) + "/"
                    + regNumberPart(patient.getCityvillage()) + "/" + num;
            attrList.add(attr(dao, uuid, PatientAttributesDTO.Columns.REGISTRATION_NUMBER.value, regNumber));
            attrList.add(attr(dao, uuid, PatientAttributesDTO.Columns.PATIENT_REGISTRATION_START_DATE_TIME.value,
                    new SessionManager(this).getPatientRegistrationDateTime()));
        }

        attrList.add(attr(dao, uuid, PatientAttributesDTO.Columns.ALTERNATE_NO.value,
                StringUtils.getValue(draft.getAlternateNumber())));
        attrList.add(attr(dao, uuid, PatientAttributesDTO.Columns.PROFILE_IMG_TIMESTAMP.value,
                AppConstants.dateAndTimeUtils.currentDateTime()));
    }

    private PatientAttributesDTO attr(PatientsDAO dao, String patientUuid, String colKey, String value) {
        PatientAttributesDTO a = new PatientAttributesDTO();
        a.setUuid(UUID.randomUUID().toString());
        a.setPatientuuid(patientUuid);
        a.setPersonAttributeTypeUuid(dao.getUuidForAttribute(colKey));
        a.setValue(value);
        return a;
    }

    private String regNumberPart(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.length() >= 2 ? value.substring(0, 2) : value;
    }

    /** Save, then push, then leave. The order is the shipped one and the push is best-effort. */
    public void completeRegistration(List<PatientAttributesDTO> stepAttributes) {
        List<PatientAttributesDTO> attrList = new ArrayList<>(stepAttributes);
        addOwnedAttributes(attrList);
        draft.getPatient().setPatientAttributesDTOList(attrList);
        draft.getPatient().setSyncd(false);
        if (!savePatient(attrList)) {
            if (!draft.getFromSummary()) {
                Toast.makeText(this, "Error adding data", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        pushIfOnline();
        openPatientDetail();
    }

    /** The only patient write in registration. A false return is the DAO's own, not an exception. */
    private boolean savePatient(List<PatientAttributesDTO> attrList) {
        PatientDTO patient = draft.getPatient();
        String uuid = resolveUuid();
        try {
            if (draft.getFromSummary()) {
                boolean upd = new PatientsDAO().updatePatientToDBNew(patient, uuid, attrList);
                boolean img = new ImagesDAO().updatePatientProfileImages(patient.getPatientPhoto(), uuid);
                return upd && img;
            }
            patient.setCreatedAt(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
            boolean ins = new PatientsDAO().insertPatientToDB(patient, uuid);
            new ImagesDAO().insertPatientProfileImages(patient.getPatientPhoto(), uuid);
            return ins;
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }
    }

    private void pushIfOnline() {
        if (NetworkConnection.isOnline(getApplication())) {
            new SyncDAO().pushDataApi();
            new ImagesPushDAO().patientProfileImagesPush();
        }
    }

    /** privacy rides only on the create path, exactly as the fragment sent it. */
    private void openPatientDetail() {
        PatientDTO patient = draft.getPatient();
        Intent i = new Intent(this, PatientDetailActivity.class);
        i.putExtra("patientUuid", resolveUuid());
        i.putExtra("patientName", patient.getFirstname() + " " + patient.getLastname());
        i.putExtra("tag", "newPatient");
        if (!draft.getFromSummary()) {
            i.putExtra("privacy", draft.getPrivacyValue());
            draft.setDobToDb(null);
            new SessionManager(this).savePatientRegistrationDateTime("");
        }
        i.putExtra("hasPrescription", "false");
        startActivity(i);
        finish();
    }

    /** The last step saves instead of advancing; which step is last is buildSteps()' answer, not a constant. */
    @Override
    public void onStepCompleted() {
        if (isLastStep()) {
            completeRegistration(new ArrayList<>());
        } else {
            showStep(currentStepIndex + 1);
        }
    }

    @Override
    public void onStepBack() {
        if (currentStepIndex > PAGE_PERSONAL) showStep(currentStepIndex - 1);
    }

    /** A step was navigated to rather than freshly started - what the fragments used to ask getArguments(). */
    public boolean arrivedFromAStep() {
        return previousStepIndex != NO_STEP;
    }
//    private ViewPager2 pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_add_new_patient);
        super.onCreate(savedInstanceState);
        restoreDraft(savedInstanceState);
        readEntryExtras();
        buildSteps();
        initUI(savedInstanceState == null);
        setupActionBar();
        enableProperPadding(AddNewPatientActivity.this);
    }

    @Override
    protected int getScreenTitle() {
        return R.string.add_patient;
    }

    /** Restored before readEntryExtras, so the entry values are always the Intent's. */
    private void restoreDraft(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        Object saved = savedInstanceState.getSerializable(KEY_DRAFT);
        if (saved instanceof PatientRegistrationDraft) draft = (PatientRegistrationDraft) saved;
        currentStepIndex = savedInstanceState.getInt(KEY_STEP_INDEX, PAGE_PERSONAL);
    }

    /** privacy is null on the edit path and editDetails is a hardcoded true everywhere, so neither is carried. */
    private void readEntryExtras() {
        Intent in = getIntent();
        draft.setEditingPatientUuid(in.getStringExtra("patientUuid"));
        draft.setFromSummary(in.getBooleanExtra("fromSummary", false));
        draft.setPrivacyValue(in.getStringExtra("privacy"));
    }

    private void initUI(boolean isFreshStart) {
        View viewToolbar = findViewById(R.id.toolbar_common);
        Toolbar toolbar = viewToolbar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v ->
                onBackPressed()
        );

        inflateStepStrip();

        if (isFreshStart) {
            showStep(PAGE_PERSONAL);
        } else {
            updateStepIndicator(currentStepIndex);
        }

//        pager = findViewById(R.id.viewPager);
//        pager.setUserInputEnabled(false);
//        pager.setAdapter(new PatientTabPagerAdapter(getSupportFragmentManager(), getLifecycle()));
//        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
//            @Override
//            public void onPageSelected(int position) {
//                super.onPageSelected(position);
//                changeCurrentButtonState(position);
//            }
//        });

//        pager.setCurrentItem(0);
    }

    /**
     * Nepal collects the obstetric values on a third step; every other region collects them on the
     * per-visit Admission screen, so registration there is two steps.
     */
    private void buildSteps() {
        stepCount = AppRegion.collectsAdmissionDataAtRegistration() ? 3 : 2;
    }

    private void inflateStepStrip() {
        ViewGroup tabs = findViewById(R.id.tabs);
        tabs.removeAllViews();
        getLayoutInflater().inflate(
                stepCount >= 3 ? R.layout.add_patient_tabs : R.layout.add_patient_tabs_two_step, tabs);
    }

    private Fragment createStep(int index) {
        if (index == PAGE_ADDRESS) return new PatientAddressInfoFragment();
        if (index == PAGE_OTHER) return new PatientOtherInfoFragment();
        return new PatientPersonalInfoFragment();
    }

    public void showStep(int index) {
        previousStepIndex = currentStepIndex == index ? NO_STEP : currentStepIndex;
        currentStepIndex = index;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_add_patient, createStep(index))
                .commit();
        updateStepIndicator(index);
    }

    public boolean isLastStep() {
        return currentStepIndex == stepCount - 1;
    }

    /**
     * Resets every pill first: navigating backwards used to leave the pill you came from lit. The
     * third pill is optional because the two-step strip does not have one.
     */
    private void updateStepIndicator(int position) {
        TextView tvPersonal = findViewById(R.id.tv_personal_info);
        TextView tvAddress = findViewById(R.id.tv_address_info);
        TextView tvOther = findViewById(R.id.tv_other_info);

        reset(tvPersonal);
        reset(tvAddress);
        reset(tvOther);

        if (position == PAGE_PERSONAL) {
            tvPersonal.setSelected(true);
        } else if (position == PAGE_ADDRESS) {
            tvPersonal.setActivated(true);
            tvAddress.setSelected(true);
        } else if (position == PAGE_OTHER && tvOther != null) {
            tvPersonal.setActivated(true);
            tvAddress.setActivated(true);
            tvOther.setSelected(true);
        }
    }

    private void reset(TextView pill) {
        if (pill == null) return;
        pill.setSelected(false);
        pill.setActivated(false);
    }

    /** Which step the user arrived from, replacing the three fromXScreen bundle flags. */
    public boolean cameFrom(int step) {
        return previousStepIndex == step;
    }

    /*
        private void setscreen(Fragment fragment) {
            // Bundle data
            Bundle bundle = new Bundle();
            bundle.putSerializable("patientDTO", (Serializable) patientdto);
            Log.v(TAG, "reltion: " + patientID_edit);
            if (patientID_edit != null) {
                bundle.putString("patientUuid", patientID_edit);
            } else {
                bundle.putString("patientUuid", patientdto.getUuid());
            }
            bundle.putBoolean("fromFirstScreen", true);
            bundle.putBoolean("fromSecondScreen", true);
            bundle.putBoolean("fromThirdScreen", true);
            bundle.putBoolean("patient_detail", true);
            fragment.setArguments(bundle); // passing data to Fragment

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_firstscreen, fragment)
                    .commit();
        }
    */
    @Override
    public void onBackPressed() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(this)
                .content(getString(R.string.are_you_want_go_back))
                .positiveButtonLabel(R.string.yes)
                .build();

        dialog.setListener(() -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        dialog.show(getSupportFragmentManager(), dialog.getClass().getCanonicalName());

//        MaterialAlertDialogBuilder alertdialogBuilder = new MaterialAlertDialogBuilder(this);
//        alertdialogBuilder.setMessage();
//        alertdialogBuilder.setPositiveButton(R.string.generic_yes, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                Intent i_back = new Intent(getApplicationContext(), HomeActivity.class);
//                startActivity(i_back);
//            }
//        });
//        alertdialogBuilder.setNegativeButton(R.string.generic_no, null);
//
//        AlertDialog alertDialog = alertdialogBuilder.create();
//        alertDialog.show();
//
//        Button positiveButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
//        Button negativeButton = alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
//
//        positiveButton.setTextColor(getResources().getColor(R.color.colorPrimary));
//        //positiveButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
//
//        negativeButton.setTextColor(getResources().getColor(R.color.colorPrimary));
//        //negativeButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
//        IntelehealthApplication.setAlertDialogCustomTheme(this, alertDialog);
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_STEP_INDEX, currentStepIndex);
        outState.putSerializable(KEY_DRAFT, draft);
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