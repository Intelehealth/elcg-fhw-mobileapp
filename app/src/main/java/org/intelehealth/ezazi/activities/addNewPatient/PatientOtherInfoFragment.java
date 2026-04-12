package org.intelehealth.ezazi.activities.addNewPatient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.patientDetailActivity.PatientDetailActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.ImagesDAO;
import org.intelehealth.ezazi.database.dao.ImagesPushDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.ProviderDAO;
import org.intelehealth.ezazi.database.dao.SyncDAO;
import org.intelehealth.ezazi.models.Patient;
import org.intelehealth.ezazi.models.dto.PatientAttributesDTO;
import org.intelehealth.ezazi.models.dto.PatientAttributesModel;
import org.intelehealth.ezazi.models.dto.PatientDTO;
import org.intelehealth.ezazi.models.dto.ProviderDTO;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment;
import org.intelehealth.ezazi.ui.dialog.SingleChoiceDialogFragment;
import org.intelehealth.ezazi.ui.dialog.ThemeTimePickerDialog;
import org.intelehealth.ezazi.ui.dialog.adapter.RiskFactorMultiChoiceAdapter;
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem;
import org.intelehealth.ezazi.ui.validation.FirstLetterUpperCaseInputFilter;
import org.intelehealth.ezazi.utilities.DateAndTimeUtils;
import org.intelehealth.ezazi.utilities.FileUtils;
import org.intelehealth.ezazi.utilities.Logger;
import org.intelehealth.ezazi.utilities.NepaliDateConverter;
import org.intelehealth.ezazi.utilities.NetworkConnection;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.StringUtils;
import org.intelehealth.ezazi.utilities.UuidGenerator;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.utils.DateTimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * PatientOtherInfoFragment
 *
 * All date fields (Admission Date, Active Labour Diagnosed Date, Amniotic Sac Ruptured Date,
 * LMP, EDD) now use a Nepali (BS) 3-wheel NumberPicker for input.
 *
 * Display format : "YYYY-MonthName-DD"  (e.g. "2080-Baisakh-15")
 * Storage format : Gregorian "dd/MM/yyyy" — unchanged from original.
 *
 * Validations per field:
 *   Admission Date       – min: today−10 days  |  max: today (no future)
 *   LMP                  – min: today−44 weeks  |  max: today (no future)
 *   EDD                  – min: today−3 weeks   |  max: today+3 weeks  (Naegele auto-fill on LMP)
 *   Active Labour Date   – no explicit BS-level date restriction; time validation (≤15 h) unchanged
 *   Sac Ruptured Date    – no future; no lower limit
 */
public class PatientOtherInfoFragment extends Fragment {
    private static final String TAG = "PatientPersonalInfoFrag";

    // ── BS month names ─────────────────────────────────────────────────────
    private static final String[] BS_MONTH_NAMES = {
            "Baisakh", "Jestha", "Asar", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

    /** Gregorian display/storage format used throughout this fragment (unchanged). */
    private static final String GREG_FMT = "dd/MM/yyyy";

    public static PatientOtherInfoFragment getInstance() {
        return new PatientOtherInfoFragment();
    }

    // ── UI refs ────────────────────────────────────────────────────────────
    View view;
    private AutoCompleteTextView mRiskFactorsTextView, mPrimaryDoctorTextView, mSecondaryDoctorTextView;
    Context mContext;
    TextInputEditText mAdmissionDateTextView, mAdmissionTimeTextView,
            mTotalBirthEditText, mTotalMiscarriageEditText,
            mActiveLaborDiagnosedDateTextView, mActiveLaborDiagnosedTimeTextView,
            mMembraneRupturedDateTextView, mMembraneRupturedTimeTextView,
            etBedNumber, etHospitalOther, mGravidaEdittext, mHospitalId;
    MaterialButton btnBack, btnNext;
    TextView optionHospital, optionMaternity, optionOther;
    Intent i_privacy;

    // ── State strings (Gregorian dd/MM/yyyy unless noted) ─────────────────
    private String mAdmissionDateString = "", mAdmissionTimeString = "";
    private String mTotalBirthCount = "0", mTotalMiscarriageCount = "0";
    private String mLaborOnsetString = "";
    private String mHospitalMaternityString = "";
    private String mActiveLaborDiagnosedDate = "", mActiveLaborDiagnosedTime = "";
    private String mMembraneRupturedDate = "", mMembraneRupturedTime = "";
    private String mRiskFactorsString = "", mPrimaryDoctorUUIDString = "", mSecondaryDoctorUUIDString = "";
    private List<String> mSelectedRiskFactorList = new ArrayList<>();
    private String mAlternateNumberString = "", mWifeDaughterOfString = "";
    private String mOthersString = "";
    String privacy_value;
    private boolean mIsEditMode = false;
    private List<ProviderDTO> mProviderDoctorList = new ArrayList<>();
    String patientID_edit;
    Patient patient1 = new Patient();
    private boolean hasLicense = false;
    SessionManager sessionManager = null;
    UuidGenerator uuidGenerator = new UuidGenerator();
    String uuid = "";
    PatientDTO patientDTO = new PatientDTO();
    CheckBox mUnknownMembraneRupturedCheckBox;
    ImagesDAO imagesDAO = new ImagesDAO();
    boolean patient_detail = false;
    String patientUuidUpdate = "";
    boolean fromSummary = false;
    private PatientAddressInfoFragment secondScreen;
    boolean fromThirdScreen = false, fromSecondScreen = false;
    TextView tvSpontaneous, tvInduced;
    int MY_REQUEST_CODE = 5555;
    PatientsDAO patientsDAO = new PatientsDAO();
    private TextView tvErrorAdmissionDate, tvErrorAdmissionTime, tvErrorTotalBirth,
            tvErrorTotalMiscarriage, tvErrorLabourOnset,
            tvErrorSacRupturedDate, tvErrorSacRupturedTime,
            tvErrorPrimaryDoctor, tvErrorSecondaryDoctor,
            tvErrorBedNumber, tvErrorLabourDiagnosedDate, tvErrorLabourDiagnosedTime,
            tvErrorRiskFactor, tvErrorHospital, tvErrorHospitalOther;
    private MaterialCardView cardAdmissionDate, cardAdmissionTime, cardTotalBirth,
            cardTotalMiscarraige, cardSacRupturedDate, cardSacRupturedTime,
            cardPrimaryDoctor, cardSecondaryDoctor, cardBedNumber,
            cardDiagnosedDate, cardDiagnosedTime, dropdownRiskFactors,
            cardOtherRisk, cardHospitalOther;
    private LinearLayout layoutErrorLabourOnset, layoutSacRuptured, cardOptions;
    private boolean isUnknownChecked;
    private PatientAttributesModel patientAttributesModel;
    private List<ErrorManagerModel> errorDetailsList;
    private NestedScrollView scrollviewOtherInfo;
    private EditText etHighRisk;
    private TextView tvErrorHighRisk;
    private boolean isParityWarningDialogShown = false;
    boolean isGravidaManuallyEdited = false;
    boolean isGravidaEdited = false;
    private String mLmpDate = "", mEDD = "";
    private TextInputEditText mLmpDateTextView, mEDDTextView;
    private TextView tvErrorLmpDate, tvErrorEDD, tvErrorGravida, tvErrorHospitalId;
    private String patientUuid = "";

    // ═════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_patient_other_info, container, false);
        mContext = getActivity();
        sessionManager = new SessionManager(mContext);
        initUI();
        return view;
    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        btnBack.setOnClickListener(v -> onBackInsertIntopatientDTO());
        btnNext.setOnClickListener(v -> onPatientCreateClicked());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Init
    // ═════════════════════════════════════════════════════════════════════

    private void initUI() {
        mAdmissionDateTextView           = view.findViewById(R.id.et_admission_date);
        mAdmissionTimeTextView           = view.findViewById(R.id.et_admission_time);
        mTotalBirthEditText              = view.findViewById(R.id.et_total_birth);
        mTotalMiscarriageEditText        = view.findViewById(R.id.et_total_miscarriage);
        tvSpontaneous                    = view.findViewById(R.id.et_spontaneous);
        tvInduced                        = view.findViewById(R.id.et_induced);
        mActiveLaborDiagnosedDateTextView = view.findViewById(R.id.et_labor_diagnosed_date);
        mActiveLaborDiagnosedTimeTextView = view.findViewById(R.id.et_labor_diagnosed_time);
        mMembraneRupturedDateTextView    = view.findViewById(R.id.et_sac_ruptured_date);
        mMembraneRupturedTimeTextView    = view.findViewById(R.id.et_sac_ruptured_time);
        optionHospital                   = view.findViewById(R.id.option_hospital);
        optionMaternity                  = view.findViewById(R.id.option_maternity);
        optionOther                      = view.findViewById(R.id.option_other);
        mPrimaryDoctorTextView           = view.findViewById(R.id.autotv_primary_doctor);
        mSecondaryDoctorTextView         = view.findViewById(R.id.autotv_secondary_doctor);
        etBedNumber                      = view.findViewById(R.id.et_bed_number);
        btnBack                          = view.findViewById(R.id.btn_back_address);
        btnNext                          = view.findViewById(R.id.btn_next_address);
        mUnknownMembraneRupturedCheckBox = view.findViewById(R.id.mUnknownMembraneRupturedCheckBox);
        mRiskFactorsTextView             = view.findViewById(R.id.autotv_risk_factors);
        dropdownRiskFactors              = view.findViewById(R.id.dropdown_risk_factors);
        etHighRisk                       = view.findViewById(R.id.etOtherRiskFactor);
        tvErrorHighRisk                  = view.findViewById(R.id.tv_error_risk_factor_other);
        cardOtherRisk                    = view.findViewById(R.id.cardOtherRiskFactor);
        etHospitalOther                  = view.findViewById(R.id.et_hospital_other);
        scrollviewOtherInfo              = view.findViewById(R.id.scroll_other_info);
        mGravidaEdittext                 = view.findViewById(R.id.et_gravida);
        mHospitalId                      = view.findViewById(R.id.et_hospital_id);
        tvErrorHospitalId                = view.findViewById(R.id.tv_hospital_id_error);

        View layoutLmpEdd = view.findViewById(R.id.view_lmp_edd_layout);
        mLmpDateTextView = layoutLmpEdd.findViewById(R.id.et_lmp);
        mEDDTextView     = layoutLmpEdd.findViewById(R.id.et_edd);
        tvErrorLmpDate   = view.findViewById(R.id.tv_lmp_error);
        tvErrorEDD       = view.findViewById(R.id.tv_edd_error);

        etHospitalOther.setFilters(new InputFilter[]{new FirstLetterUpperCaseInputFilter()});

        // Disable soft keyboard for all date fields – picker driven
        disableSoftInput(mAdmissionDateTextView, mActiveLaborDiagnosedDateTextView,
                mMembraneRupturedDateTextView, mLmpDateTextView, mEDDTextView);

        handleValidations();
        handleOptionsForMaternity();

        ProviderDAO providerDAO = new ProviderDAO();
        try { mProviderDoctorList = providerDAO.getDoctorList(); } catch (DAOException e) { e.printStackTrace(); }

        handleAllClickListeners();

        // Edit-mode from intent
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("patientUuid")) {
            mIsEditMode   = true;
            patientID_edit = intent.getStringExtra("patientUuid");
            patient1.setUuid(patientID_edit);
            setscreen(patientID_edit);
            updateUI(patient1);
        }

        secondScreen = new PatientAddressInfoFragment();
        if (getArguments() != null) {
            patientDTO             = (PatientDTO) getArguments().getSerializable("patientDTO");
            fromSecondScreen       = getArguments().getBoolean("fromSecondScreen");
            patient_detail         = getArguments().getBoolean("patient_detail");
            mAlternateNumberString = getArguments().getString("mAlternateNumberString");
            fromSummary            = getArguments().getBoolean("fromSummary");
            patientUuidUpdate      = getArguments().getString("patientUuidUpdate");
            patientAttributesModel = (PatientAttributesModel) getArguments().getSerializable("patientAttributes");

            if (fromSecondScreen && patientAttributesModel != null) updateUIForUserFromAddressTab();
        }
    }

    private void disableSoftInput(EditText... fields) {
        for (EditText f : fields) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                f.setShowSoftInputOnFocus(false);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Nepali Date Picker (generic)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Date constraint container for the BS picker.
     * All limits are expressed in Gregorian for easy Calendar arithmetic.
     */
    private static class DateConstraint {
        /** null = no limit */
        final Calendar minGreg;
        final Calendar maxGreg;
        DateConstraint(Calendar min, Calendar max) { minGreg = min; maxGreg = max; }
    }

    /**
     * Shows the 3-wheel BS date picker with the given constraints.
     *
     * @param titleRes      String resource for dialog title
     * @param currentBsDate int[]{y,m,d} current value, or null for default (maxGreg-clamped today)
     * @param constraint    Gregorian min/max (either may be null for open-ended)
     * @param listener      Called with (bsYear, bsMonth, bsDay) on OK
     */
    private void showNepaliDatePicker(int titleRes,
                                      int[] currentBsDate,
                                      DateConstraint constraint,
                                      OnBsDateSelectedListener listener) {

        // ── determine BS limits ───────────────────────────────────────────
        int[] minBs = null, maxBs = null;
        if (constraint.minGreg != null)
            minBs = NepaliDateConverter.gregorianToBs(constraint.minGreg.getTime());
        if (constraint.maxGreg != null)
            maxBs = NepaliDateConverter.gregorianToBs(constraint.maxGreg.getTime());

        // ── default initial values ────────────────────────────────────────
        int initY, initM, initD;
        if (currentBsDate != null && currentBsDate[0] > 0) {
            initY = currentBsDate[0]; initM = currentBsDate[1]; initD = currentBsDate[2];
        } else if (maxBs != null) {
            initY = maxBs[0]; initM = maxBs[1]; initD = maxBs[2];
        } else {
            int[] today = NepaliDateConverter.getCurrentBsDate();
            initY = today[0]; initM = today[1]; initD = today[2];
        }

        int absMinYear = minBs != null ? minBs[0] : 2000;
        int absMaxYear = maxBs != null ? maxBs[0]
                : NepaliDateConverter.getCurrentBsDate()[0] + 5; // generous future cap

        // ── build pickers ─────────────────────────────────────────────────
        NumberPicker yearPicker  = new NumberPicker(mContext);
        NumberPicker monthPicker = new NumberPicker(mContext);
        NumberPicker dayPicker   = new NumberPicker(mContext);

        yearPicker.setMinValue(absMinYear);
        yearPicker.setMaxValue(absMaxYear);
        yearPicker.setValue(initY);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(BS_MONTH_NAMES);
        monthPicker.setValue(initM);

        refreshDayPicker(dayPicker, initY, initM, minBs, maxBs);
        dayPicker.setValue(Math.min(initD, dayPicker.getMaxValue()));

        // ── reactions to year/month changes ──────────────────────────────
        final int[] finalMinBs = minBs;
        final int[] finalMaxBs = maxBs;

        NumberPicker.OnValueChangeListener onYearMonthChange = (picker, oldVal, newVal) -> {
            int y = yearPicker.getValue();
            int m = monthPicker.getValue();

            // Clamp months at boundary years
            clampMonthPicker(monthPicker, y, finalMinBs, finalMaxBs);
            m = monthPicker.getValue();

            refreshDayPicker(dayPicker, y, m, finalMinBs, finalMaxBs);
        };
        yearPicker.setOnValueChangedListener(onYearMonthChange);
        monthPicker.setOnValueChangedListener(onYearMonthChange);

        // ── layout ────────────────────────────────────────────────────────
        android.widget.LinearLayout layout = new android.widget.LinearLayout(mContext);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        layout.addView(yearPicker, lp);
        layout.addView(monthPicker, lp);
        layout.addView(dayPicker, lp);

        new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(titleRes) + " (BS)")
                .setView(layout)
                .setPositiveButton(R.string.ok, (d, w) ->
                        listener.onSelected(yearPicker.getValue(),
                                monthPicker.getValue(),
                                dayPicker.getValue()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clampMonthPicker(NumberPicker mp, int y, int[] minBs, int[] maxBs) {
        int lo = 1, hi = 12;
        if (minBs != null && y == minBs[0]) lo = minBs[1];
        if (maxBs != null && y == maxBs[0]) hi = maxBs[1];
        mp.setMinValue(lo);
        mp.setMaxValue(hi);
        if (mp.getValue() < lo) mp.setValue(lo);
        if (mp.getValue() > hi) mp.setValue(hi);
    }

    private void refreshDayPicker(NumberPicker dp, int y, int m, int[] minBs, int[] maxBs) {
        int maxDay = NepaliDateConverter.getDaysInBsMonth(y, m);
        int minDay = 1;
        if (minBs != null && y == minBs[0] && m == minBs[1]) minDay = minBs[2];
        if (maxBs != null && y == maxBs[0] && m == maxBs[1]) maxDay = Math.min(maxDay, maxBs[2]);
        dp.setMinValue(minDay);
        dp.setMaxValue(maxDay);
        if (dp.getValue() < minDay) dp.setValue(minDay);
        if (dp.getValue() > maxDay) dp.setValue(maxDay);
    }

    /** Callback for the generic BS picker. */
    interface OnBsDateSelectedListener {
        void onSelected(int bsYear, int bsMonth, int bsDay);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Gregorian ↔ BS helpers
    // ═════════════════════════════════════════════════════════════════════

    /** Formats a BS date for display. */
    private String formatBsDate(int y, int m, int d) {
        return String.format(Locale.ENGLISH, "%d-%s-%02d", y, BS_MONTH_NAMES[m - 1], d);
    }

    /** Converts a Gregorian Date → Gregorian dd/MM/yyyy string (DB format). */
    private String toGregFmt(Date date) {
        return new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).format(date);
    }

    /**
     * Parses a stored Gregorian "dd/MM/yyyy" string → BS display string.
     * Returns the input string unchanged if parsing fails.
     */
    private String gregToDisplay(String gregDdMmYyyy) {
        if (gregDdMmYyyy == null || gregDdMmYyyy.isEmpty()) return "";
        try {
            Date d = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).parse(gregDdMmYyyy);
            int[] bs = NepaliDateConverter.gregorianToBs(d);
            return formatBsDate(bs[0], bs[1], bs[2]);
        } catch (Exception e) { return gregDdMmYyyy; }
    }

    /**
     * Returns int[]{bsY, bsM, bsD} from a Gregorian "dd/MM/yyyy" string,
     * or null on failure.
     */
    private int[] gregStringToBs(String gregDdMmYyyy) {
        if (gregDdMmYyyy == null || gregDdMmYyyy.isEmpty()) return null;
        try {
            Date d = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).parse(gregDdMmYyyy);
            return NepaliDateConverter.gregorianToBs(d);
        } catch (Exception e) { return null; }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Per-field date picker launchers
    // ═════════════════════════════════════════════════════════════════════

    /** Admission Date – min: today−10 days, max: today */
    private void pickAdmissionDate() {
        Calendar min = Calendar.getInstance(); min.add(Calendar.DAY_OF_MONTH, -10);
        Calendar max = Calendar.getInstance();
        showNepaliDatePicker(
                R.string.select_admission_date,
                gregStringToBs(mAdmissionDateString),
                new DateConstraint(min, max),
                (y, m, d) -> {
                    Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                    mAdmissionDateString = toGregFmt(greg);
                    mAdmissionDateTextView.setText(formatBsDate(y, m, d));
                    tvErrorAdmissionDate.setVisibility(View.GONE);
                    cardAdmissionDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                    // Re-validate time if already set
                    if (!mAdmissionTimeString.isEmpty())
                        validateNotFutureDateTime(mAdmissionDateString, mAdmissionTimeString, "admissionTimeString");
                });
    }

    /** LMP – min: today−44 weeks, max: today */
    private void pickLmpDate() {
        Calendar min = Calendar.getInstance(); min.add(Calendar.WEEK_OF_YEAR, -44);
        Calendar max = Calendar.getInstance();
        showNepaliDatePicker(
                R.string.select_lmp_date,  // add this string resource if missing
                gregStringToBs(mLmpDate),
                new DateConstraint(min, max),
                (y, m, d) -> {
                    Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                    String gregStr = toGregFmt(greg);
                    if (!validateLMPGreg(gregStr)) return;
                    mLmpDate = gregStr;
                    mLmpDateTextView.setText(formatBsDate(y, m, d));
                    tvErrorLmpDate.setVisibility(View.GONE);
                    calculateEDDFromLMP(mLmpDate);
                });
    }

    /**
     * EDD – min: today−3 weeks, max: today+3 weeks.
     * Default is auto-filled by Naegele's rule on LMP entry.
     */
    private void pickEddDate() {
        Calendar min = Calendar.getInstance(); min.add(Calendar.WEEK_OF_YEAR, -3);
        Calendar max = Calendar.getInstance(); max.add(Calendar.WEEK_OF_YEAR, 3);
        showNepaliDatePicker(
                R.string.select_edd_date,   // add this string resource if missing
                gregStringToBs(mEDD),
                new DateConstraint(min, max),
                (y, m, d) -> {
                    Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                    String gregStr = toGregFmt(greg);
                    if (!validateEDDGreg(gregStr)) return;
                    mEDD = gregStr;
                    mEDDTextView.setText(formatBsDate(y, m, d));
                    tvErrorEDD.setVisibility(View.GONE);
                });
    }

    /**
     * Active Labour Diagnosed Date – no lower BS limit (time validation handles ≤15 h);
     * max: today (no future).
     */
    private void pickActiveLaborDate() {
        Calendar max = Calendar.getInstance();
        showNepaliDatePicker(
                R.string.select_labor_diagnosed_date, // add string res
                gregStringToBs(mActiveLaborDiagnosedDate),
                new DateConstraint(null, max),
                (y, m, d) -> {
                    Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                    mActiveLaborDiagnosedDate = toGregFmt(greg);
                    mActiveLaborDiagnosedDateTextView.setText(formatBsDate(y, m, d));
                    tvErrorLabourDiagnosedDate.setVisibility(View.GONE);
                    cardDiagnosedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                    if (!mActiveLaborDiagnosedTime.isEmpty())
                        validateNotFutureDateTime(mActiveLaborDiagnosedDate,
                                mActiveLaborDiagnosedTime, "laborOnsetString");
                });
    }

    /**
     * Amniotic Sac Ruptured Date – no future; no lower limit.
     */
    private void pickSacRupturedDate() {
        Calendar max = Calendar.getInstance();
        showNepaliDatePicker(
                R.string.select_sac_ruptured_date,
                gregStringToBs(mMembraneRupturedDate),
                new DateConstraint(null, max),
                (y, m, d) -> {
                    Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                    mMembraneRupturedDate = toGregFmt(greg);
                    mMembraneRupturedDateTextView.setText(formatBsDate(y, m, d));
                    tvErrorSacRupturedDate.setVisibility(View.GONE);
                    cardSacRupturedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                });
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Click listeners
    // ═════════════════════════════════════════════════════════════════════

    private void handleAllClickListeners() {
        TextInputLayout etLayoutAdmissionDate     = view.findViewById(R.id.etLayout_admission_date);
        TextInputLayout etLayoutAdmissionTime     = view.findViewById(R.id.etLayout_admission_time);
        TextInputLayout etLabourDiagnosedDate     = view.findViewById(R.id.etLayout_labor_diagnosed_date);
        TextInputLayout etLabourDiagnosedTime     = view.findViewById(R.id.etLayout_labor_diagnosed_time);
        TextInputLayout etLayoutSacRupturedDate   = view.findViewById(R.id.etLayout_sac_ruptured_date);
        TextInputLayout etLayoutSacRupturedTime   = view.findViewById(R.id.etLayout_sac_ruptured_time);
        TextInputLayout etLayoutRiskFactors       = view.findViewById(R.id.etLayout_risk_factors);
        TextInputLayout etLayoutPrimaryDoctor     = view.findViewById(R.id.etLayout_primary_doctor);
        TextInputLayout etLayoutSecondaryDoctor   = view.findViewById(R.id.etLayout_secondary_doctor);
        layoutSacRuptured                         = view.findViewById(R.id.card_sac_ruptured);

        View layoutLmpEdd  = view.findViewById(R.id.view_lmp_edd_layout);
        TextInputLayout etLayoutLmp = layoutLmpEdd.findViewById(R.id.etLayout_lmp);
        TextInputLayout etLayoutEdd = layoutLmpEdd.findViewById(R.id.etLayout_edd);

        // Admission Date
        etLayoutAdmissionDate.setEndIconOnClickListener(v -> pickAdmissionDate());
        mAdmissionDateTextView.setOnClickListener(v -> pickAdmissionDate());

        // Admission Time
        etLayoutAdmissionTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("admissionTimeString"));
        mAdmissionTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("admissionTimeString"));

        // Active Labour Date
        etLabourDiagnosedDate.setEndIconOnClickListener(v -> pickActiveLaborDate());
        mActiveLaborDiagnosedDateTextView.setOnClickListener(v -> pickActiveLaborDate());

        // Active Labour Time
        etLabourDiagnosedTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("laborOnsetString"));
        mActiveLaborDiagnosedTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("laborOnsetString"));

        // Sac Ruptured Date
        etLayoutSacRupturedDate.setEndIconOnClickListener(v -> pickSacRupturedDate());
        mMembraneRupturedDateTextView.setOnClickListener(v -> pickSacRupturedDate());

        // Sac Ruptured Time
        etLayoutSacRupturedTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("membraneRupturedTime"));
        mMembraneRupturedTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("membraneRupturedTime"));

        // LMP
        etLayoutLmp.setEndIconOnClickListener(v -> pickLmpDate());
        mLmpDateTextView.setOnClickListener(v -> pickLmpDate());

        // EDD
        etLayoutEdd.setEndIconOnClickListener(v -> pickEddDate());
        mEDDTextView.setOnClickListener(v -> pickEddDate());

        // Risk Factors
        etLayoutRiskFactors.setEndIconOnClickListener(v -> showRiskFactorSelectionDialog());
        mRiskFactorsTextView.setOnClickListener(v -> showRiskFactorSelectionDialog());

        // Doctors
        etLayoutPrimaryDoctor.setEndIconOnClickListener(v -> selectPrimaryDoctor());
        mPrimaryDoctorTextView.setOnClickListener(v -> selectPrimaryDoctor());
        etLayoutSecondaryDoctor.setEndIconOnClickListener(v -> selectSecondaryDoctor());
        mSecondaryDoctorTextView.setOnClickListener(v -> selectSecondaryDoctor());

        // Unknown membrane checkbox
        mUnknownMembraneRupturedCheckBox.setOnCheckedChangeListener((btn, checked) -> {
            isUnknownChecked = checked;
            if (checked) {
                layoutSacRuptured.setVisibility(View.GONE);
                mMembraneRupturedDateTextView.setEnabled(false);
                mMembraneRupturedTimeTextView.setEnabled(false);
                mMembraneRupturedDateTextView.setText("");
                mMembraneRupturedTimeTextView.setText("");
                tvErrorSacRupturedDate.setVisibility(View.GONE);
                tvErrorSacRupturedTime.setVisibility(View.GONE);
            } else {
                layoutSacRuptured.setVisibility(View.VISIBLE);
                mMembraneRupturedDateTextView.setEnabled(true);
                mMembraneRupturedTimeTextView.setEnabled(true);
                cardSacRupturedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                cardSacRupturedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
            }
        });

        i_privacy     = getActivity().getIntent();
        privacy_value = i_privacy.getStringExtra("privacy");

        if (!sessionManager.getLicenseKey().isEmpty()) hasLicense = true;
        try {
            JSONObject obj = hasLicense
                    ? new JSONObject(Objects.requireNonNullElse(
                    FileUtils.readFileRoot(AppConstants.CONFIG_FILE_NAME, mContext),
                    String.valueOf(FileUtils.encodeJSON(mContext, AppConstants.CONFIG_FILE_NAME))))
                    : new JSONObject(String.valueOf(FileUtils.encodeJSON(mContext, AppConstants.CONFIG_FILE_NAME)));
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(mContext, "JsonException" + e, Toast.LENGTH_LONG).show();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Validation helpers (Gregorian dd/MM/yyyy input)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Validates LMP expressed as Gregorian dd/MM/yyyy.
     * Rules: not null, not future, within last 44 weeks.
     */
    private boolean validateLMPGreg(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            tvErrorLmpDate.setText(getString(R.string.lmp_required));
            tvErrorLmpDate.setVisibility(View.VISIBLE);
            return false;
        }
        try {
            Date sel = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).parse(dateStr);
            Calendar now = Calendar.getInstance();
            if (sel.after(now.getTime())) {
                tvErrorLmpDate.setText(getString(R.string.lmp_future_not_allowed));
                tvErrorLmpDate.setVisibility(View.VISIBLE);
                return false;
            }
            Calendar min = Calendar.getInstance(); min.add(Calendar.WEEK_OF_YEAR, -44);
            if (sel.before(min.getTime())) {
                tvErrorLmpDate.setText(getString(R.string.lmp_range_invalid));
                tvErrorLmpDate.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (Exception e) { e.printStackTrace(); return false; }
        tvErrorLmpDate.setVisibility(View.GONE);
        return true;
    }

    /**
     * Validates EDD expressed as Gregorian dd/MM/yyyy.
     * Rule: within ±3 weeks of today.
     */
    private boolean validateEDDGreg(String eddStr) {
        try {
            Date sel = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).parse(eddStr);
            Calendar min = Calendar.getInstance(); min.add(Calendar.WEEK_OF_YEAR, -3);
            Calendar max = Calendar.getInstance(); max.add(Calendar.WEEK_OF_YEAR, 3);
            if (sel.before(min.getTime()) || sel.after(max.getTime())) {
                tvErrorEDD.setText(getString(R.string.edd_range_invalid)); // add this string
                tvErrorEDD.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (Exception e) { e.printStackTrace(); return false; }
        tvErrorEDD.setVisibility(View.GONE);
        return true;
    }

    /**
     * Auto-calculates EDD from LMP using Naegele's Rule:
     *   EDD = LMP + 7 days − 3 months + 1 year
     * and populates the EDD field in BS display.
     */

    private void calculateEDDFromLMP(String lmpGregStr) {
        try {
            Date lmp = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH).parse(lmpGregStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(lmp);
            // Naegele's Rule: +7 days, −3 months, +1 year
            cal.add(Calendar.DAY_OF_MONTH, 7);
            cal.add(Calendar.MONTH, -3);
            cal.add(Calendar.YEAR, 1);
            Date eddGreg = cal.getTime();

            mEDD = toGregFmt(eddGreg);
            int[] bsEdd = NepaliDateConverter.gregorianToBs(eddGreg);
            mEDDTextView.setText(formatBsDate(bsEdd[0], bsEdd[1], bsEdd[2]));
            tvErrorEDD.setVisibility(View.GONE);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Validates that a date+time combination is not in the future.
     * Time string expected as "hh:mm AM/PM".
     */
    private boolean validateNotFutureDateTime(String dateStr, String timeStr, String param) {
        if (dateStr == null || timeStr == null
                || dateStr.trim().isEmpty() || timeStr.trim().isEmpty()) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
            Date selected = sdf.parse(dateStr + " " + timeStr);
            if (selected != null && selected.after(new Date())) {
                switch (param) {
                    case "admissionTimeString":
                        mAdmissionTimeString = null;
                        mAdmissionTimeTextView.setText("");
                        tvErrorAdmissionTime.setText(getString(R.string.select_valid_date));
                        tvErrorAdmissionTime.setVisibility(View.VISIBLE);
                        break;
                    case "laborOnsetString":
                        mActiveLaborDiagnosedTime = null;
                        mActiveLaborDiagnosedTimeTextView.setText("");
                        tvErrorLabourDiagnosedTime.setText(getString(R.string.active_labour_diagnosis));
                        tvErrorLabourDiagnosedTime.setVisibility(View.VISIBLE);
                        break;
                }
                return false;
            }
        } catch (ParseException e) { e.printStackTrace(); }
        return true;
    }

    /**
     * Validates that active-labour date+time is within the last 15 hours.
     */
    private boolean validateActiveLabourDateTime(String date, String time) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date selected = sdf.parse(date + " " + time);
            Calendar now = Calendar.getInstance();
            Calendar min15h = Calendar.getInstance(); min15h.add(Calendar.HOUR_OF_DAY, -15);
            if (selected.before(min15h.getTime()) || selected.after(now.getTime())) {
                tvErrorLabourDiagnosedTime.setVisibility(View.VISIBLE);
                tvErrorLabourDiagnosedTime.setText(getString(R.string.active_labour_diagnosis));
                cardDiagnosedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                return false;
            } else {
                tvErrorLabourDiagnosedTime.setVisibility(View.GONE);
                cardDiagnosedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Time picker (unchanged)
    // ═════════════════════════════════════════════════════════════════════

    private void selectTimeForAllParameters(String forWhichParameter) {
        ThemeTimePickerDialog dialog = new ThemeTimePickerDialog.Builder(mContext)
                .title(R.string.current_time).positiveButtonLabel(R.string.ok).build();
        dialog.setListener((hours, minutes, amPm, value) -> {
            String timeString = String.format("%02d:%02d %s", hours, minutes, amPm);
            switch (forWhichParameter) {
                case "admissionTimeString":
                    mAdmissionTimeString = timeString;
                    mAdmissionTimeTextView.setText(timeString);
                    validateNotFutureDateTime(mAdmissionDateString, timeString, "admissionTimeString");
                    break;
                case "laborOnsetString":
                    boolean valid = validateActiveLabourDateTime(mActiveLaborDiagnosedDate, timeString);
                    if (!valid) {
                        mActiveLaborDiagnosedTimeTextView.setText("");
                        mActiveLaborDiagnosedTime = null;
                    } else {
                        mActiveLaborDiagnosedTime = timeString;
                        mActiveLaborDiagnosedTimeTextView.setText(timeString);
                    }
                    validateNotFutureDateTime(mActiveLaborDiagnosedDate, timeString, "laborOnsetString");
                    break;
                case "membraneRupturedTime":
                    mMembraneRupturedTime = timeString;
                    mMembraneRupturedTimeTextView.setText(timeString);
                    break;
            }
        });
        dialog.show(getChildFragmentManager(), "ThemeTimePickerDialog");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  updateUIForUserFromAddressTab – show stored Gregorian as BS display
    // ═════════════════════════════════════════════════════════════════════

    private void updateUIForUserFromAddressTab() {
        // Dates: stored in model as dd/MM/yyyy → show as BS
        String admDate  = patientAttributesModel.getAdmissionDate();
        String labDate  = patientAttributesModel.getActiveLabourDiagnosedDate();
        String sacDate  = patientAttributesModel.getSacRupturedDate();
        String lmpStr   = patientAttributesModel.getLmp();
        String eddStr   = patientAttributesModel.getEdd();

        mAdmissionDateString = admDate;
        mAdmissionDateTextView.setText(gregToDisplay(admDate));

        mAdmissionTimeString = patientAttributesModel.getAdmissionTime();
        mAdmissionTimeTextView.setText(mAdmissionTimeString);

        mTotalBirthCount = patientAttributesModel.getTotalBirthCount();
        mTotalMiscarriageCount = patientAttributesModel.getTotalMiscarriageCount();
        mTotalBirthEditText.setText(mTotalBirthCount);
        mTotalMiscarriageEditText.setText(mTotalMiscarriageCount);

        mActiveLaborDiagnosedDate = labDate;
        mActiveLaborDiagnosedDateTextView.setText(gregToDisplay(labDate));
        mActiveLaborDiagnosedTime = patientAttributesModel.getActiveLabourDiagnosedTime();
        mActiveLaborDiagnosedTimeTextView.setText(mActiveLaborDiagnosedTime);

        mMembraneRupturedDate = sacDate;
        mMembraneRupturedDateTextView.setText(gregToDisplay(sacDate));
        mMembraneRupturedTime = patientAttributesModel.getSacRupturedTime();
        mMembraneRupturedTimeTextView.setText(mMembraneRupturedTime);

        mRiskFactorsString = patientAttributesModel.getRiskFactors();
        mRiskFactorsTextView.setText(mRiskFactorsString);
        mPrimaryDoctorTextView.setText(patientAttributesModel.getPrimaryDoctor());
        mSecondaryDoctorTextView.setText(patientAttributesModel.getSecondaryDoctor());
        etBedNumber.setText(patientAttributesModel.getBedNumber());

        mLaborOnsetString = patientAttributesModel.getLabourOnset();
        mHospitalMaternityString = patientAttributesModel.getHospitalMaternity();
        isUnknownChecked = patientAttributesModel.isMembraneCheckboxChecked();
        etHospitalOther.setText(patientAttributesModel.getOtherHospitalString());

        mGravidaEdittext.setText(patientAttributesModel.getGravida());

        mLmpDate = lmpStr;
        mLmpDateTextView.setText(gregToDisplay(lmpStr));

        mEDD = eddStr;
        mEDDTextView.setText(gregToDisplay(eddStr));

        mHospitalId.setText(patientAttributesModel.getHospitalId());

        mUnknownMembraneRupturedCheckBox.setChecked(isUnknownChecked);
        getHospitalMaternityValue(mHospitalMaternityString);
        getLabourOnsetValue(mLaborOnsetString);
        if (!mHospitalMaternityString.isEmpty() && mHospitalMaternityString.equalsIgnoreCase("other"))
            etHospitalOther.setText(patientAttributesModel.getOtherHospitalString());

        hideAllErrorFields();
    }

    // ═════════════════════════════════════════════════════════════════════
    //  updateUI (edit-mode from DB) – stored Gregorian dd/MM/yyyy → BS display
    // ═════════════════════════════════════════════════════════════════════

    private void updateUI(Patient patient) {
        if (patient.getAdmissionDate() != null) {
            mAdmissionDateString = patient.getAdmissionDate();
            mAdmissionDateTextView.setText(gregToDisplay(mAdmissionDateString));
        }
        if (patient.getAdmissionTime() != null) {
            mAdmissionTimeString = patient.getAdmissionTime();
            mAdmissionTimeTextView.setText(mAdmissionTimeString);
        }
        if (patient.getParity() != null) {
            mTotalBirthCount       = patient.getParity().split(",")[0];
            mTotalMiscarriageCount = patient.getParity().split(",")[1];
            mTotalBirthEditText.setText(mTotalBirthCount);
            mTotalMiscarriageEditText.setText(mTotalMiscarriageCount);
        }
        if (patient.getLaborOnset() != null) {
            mLaborOnsetString = patient.getLaborOnset();
            getLabourOnsetValue(mLaborOnsetString);
        }
        if (patient.getActiveLaborDiagnosed() != null) {
            String[] parts = patient.getActiveLaborDiagnosed().split(" ");
            mActiveLaborDiagnosedDate = parts[0];
            mActiveLaborDiagnosedTime = parts.length > 1 ? parts[1] : "";
            mActiveLaborDiagnosedDateTextView.setText(gregToDisplay(mActiveLaborDiagnosedDate));
            mActiveLaborDiagnosedTimeTextView.setText(mActiveLaborDiagnosedTime);
        }
        if (patient.getMembraneRupturedTimestamp() != null) {
            if (patient.getMembraneRupturedTimestamp().equalsIgnoreCase("U")) {
                mUnknownMembraneRupturedCheckBox.setChecked(true);
            } else {
                String[] parts = patient.getMembraneRupturedTimestamp().split(" ");
                mMembraneRupturedDate = parts[0];
                mMembraneRupturedTime = parts.length > 1 ? parts[1] : "";
                mMembraneRupturedDateTextView.setText(gregToDisplay(mMembraneRupturedDate));
                mMembraneRupturedTimeTextView.setText(mMembraneRupturedTime);
            }
        }
        if (patient.getRiskFactors() != null) {
            mRiskFactorsString = patient.getRiskFactors();
            mRiskFactorsTextView.setText(mRiskFactorsString);
        }
        if (patient.getHospitalMaternity() != null) {
            mHospitalMaternityString = patient.getHospitalMaternity();
            getHospitalMaternityValue(mHospitalMaternityString);
        }
        if (patient.getPrimaryDoctor() != null) {
            mPrimaryDoctorUUIDString = patient.getPrimaryDoctor().split("@#@")[0];
            mPrimaryDoctorTextView.setText(patient.getPrimaryDoctor().split("@#@")[1]);
        }
        if (patient.getPrimaryDoctor() != null && patient.getSecondaryDoctor() != null) {
            mSecondaryDoctorUUIDString = patient.getSecondaryDoctor().split("@#@")[0];
            mSecondaryDoctorTextView.setText(patient.getSecondaryDoctor().split("@#@")[1]);
        }
        try { etBedNumber.setText(getBedNumber(patient.getUuid())); } catch (DAOException e) { e.printStackTrace(); }
        if (patient.getGravida() != null)    mGravidaEdittext.setText(patient.getGravida());
        if (patient.getLmp() != null) {
            mLmpDate = patient.getLmp();
            mLmpDateTextView.setText(gregToDisplay(mLmpDate));
        }
        if (patient.getEdd() != null) {
            mEDD = patient.getEdd();
            mEDDTextView.setText(gregToDisplay(mEDD));
        }
        if (patient.getHospitalId() != null) mHospitalId.setText(patient.getHospitalId());
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Everything below this line is unchanged from original
    // ═════════════════════════════════════════════════════════════════════

    private void hideAllErrorFields() {
        tvErrorAdmissionDate.setVisibility(View.GONE);
        tvErrorAdmissionTime.setVisibility(View.GONE);
        tvErrorSacRupturedDate.setVisibility(View.GONE);
        tvErrorSacRupturedTime.setVisibility(View.GONE);
        tvErrorLabourDiagnosedDate.setVisibility(View.GONE);
        tvErrorLabourDiagnosedTime.setVisibility(View.GONE);
        tvErrorHospital.setVisibility(View.GONE);
        tvErrorBedNumber.setVisibility(View.GONE);
        tvErrorPrimaryDoctor.setVisibility(View.GONE);
        tvErrorSecondaryDoctor.setVisibility(View.GONE);
        tvErrorLabourOnset.setVisibility(View.GONE);
        tvErrorRiskFactor.setVisibility(View.GONE);
        tvErrorHospitalOther.setVisibility(View.GONE);
        tvErrorGravida.setVisibility(View.GONE);
        tvErrorLmpDate.setVisibility(View.GONE);
        tvErrorEDD.setVisibility(View.GONE);
    }

    private void handleValidations() {
        tvErrorAdmissionDate     = view.findViewById(R.id.tv_admission_date_error);
        tvErrorAdmissionTime     = view.findViewById(R.id.tv_admission_time_error);
        tvErrorTotalBirth        = view.findViewById(R.id.tv_parity_date_error);
        tvErrorTotalMiscarriage  = view.findViewById(R.id.tv_parity_time_error);
        tvErrorLabourOnset       = view.findViewById(R.id.tv_error_labour_onset);
        tvErrorSacRupturedDate   = view.findViewById(R.id.tv_sac_ruptured_date_error);
        tvErrorSacRupturedTime   = view.findViewById(R.id.tv_sac_ruptured_time_error);
        tvErrorPrimaryDoctor     = view.findViewById(R.id.tv_error_primary_doctor);
        tvErrorSecondaryDoctor   = view.findViewById(R.id.tv_error_secondary_doctor);
        tvErrorBedNumber         = view.findViewById(R.id.tv_error_bed_number);
        tvErrorLabourDiagnosedDate = view.findViewById(R.id.tv_labour_diagnosed_date_error);
        tvErrorLabourDiagnosedTime = view.findViewById(R.id.tv_labour_diagnosed_time_error);
        tvErrorRiskFactor        = view.findViewById(R.id.tv_error_risk_factor);
        tvErrorHospital          = view.findViewById(R.id.tv_error_hospital);
        tvErrorHospitalOther     = view.findViewById(R.id.tv_error_hospital_other);
        tvErrorGravida           = view.findViewById(R.id.tv_gravida_error);

        cardAdmissionDate    = view.findViewById(R.id.card_date_admission);
        cardAdmissionTime    = view.findViewById(R.id.card_time_admission);
        cardTotalBirth       = view.findViewById(R.id.card_total_birth);
        cardTotalMiscarraige = view.findViewById(R.id.card_total_miscarraige);
        cardSacRupturedDate  = view.findViewById(R.id.card_sac_ruptured_date);
        cardSacRupturedTime  = view.findViewById(R.id.card_sac_ruptured_time);
        cardPrimaryDoctor    = view.findViewById(R.id.dropdown_primary_doctor);
        cardSecondaryDoctor  = view.findViewById(R.id.dropdown_secondary_doctor);
        cardBedNumber        = view.findViewById(R.id.card_bed_no);
        cardDiagnosedDate    = view.findViewById(R.id.card_diagnosed_date);
        cardDiagnosedTime    = view.findViewById(R.id.card_diagnosed_time);
        layoutErrorLabourOnset = view.findViewById(R.id.card_labour_onset);
        cardOptions          = view.findViewById(R.id.card_options);
        cardHospitalOther    = view.findViewById(R.id.card_hospital_other);

        mAdmissionDateTextView.addTextChangedListener(new MyTextWatcher(mAdmissionDateTextView));
        mAdmissionTimeTextView.addTextChangedListener(new MyTextWatcher(mAdmissionTimeTextView));
        mTotalBirthEditText.addTextChangedListener(new MyTextWatcher(mTotalBirthEditText));
        mTotalMiscarriageEditText.addTextChangedListener(new MyTextWatcher(mTotalMiscarriageEditText));
        mActiveLaborDiagnosedDateTextView.addTextChangedListener(new MyTextWatcher(mActiveLaborDiagnosedDateTextView));
        mActiveLaborDiagnosedTimeTextView.addTextChangedListener(new MyTextWatcher(mActiveLaborDiagnosedTimeTextView));
        mMembraneRupturedDateTextView.addTextChangedListener(new MyTextWatcher(mMembraneRupturedDateTextView));
        mMembraneRupturedTimeTextView.addTextChangedListener(new MyTextWatcher(mMembraneRupturedTimeTextView));
        mRiskFactorsTextView.addTextChangedListener(new MyTextWatcher(mRiskFactorsTextView));
        etHighRisk.addTextChangedListener(new MyTextWatcher(etHighRisk));
        mPrimaryDoctorTextView.addTextChangedListener(new MyTextWatcher(mPrimaryDoctorTextView));
        mSecondaryDoctorTextView.addTextChangedListener(new MyTextWatcher(mSecondaryDoctorTextView));
        mGravidaEdittext.addTextChangedListener(new MyTextWatcher(mGravidaEdittext));

        etHospitalOther.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { tvErrorHospital.setVisibility(View.GONE); }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                tvErrorHospital.setVisibility(View.GONE);
                if (s.length() > 0) { tvErrorHospitalOther.setVisibility(View.GONE); cardHospitalOther.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }
            }
            @Override public void afterTextChanged(Editable s) { tvErrorHospital.setVisibility(View.GONE); }
        });
    }

    private void handleOptionsForMaternity() {
        mHospitalMaternityString = "";
        optionHospital.setOnClickListener(v -> {
            setOptionSelected(optionHospital); setOptionUnselected(optionMaternity); setOptionUnselected(optionOther);
            mHospitalMaternityString = optionHospital.getText().toString();
            cardHospitalOther.setVisibility(View.GONE); tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE); etHospitalOther.setText("");
        });
        optionMaternity.setOnClickListener(v -> {
            setOptionUnselected(optionHospital); setOptionSelected(optionMaternity); setOptionUnselected(optionOther);
            mHospitalMaternityString = optionMaternity.getText().toString();
            cardHospitalOther.setVisibility(View.GONE); tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE); etHospitalOther.setText("");
        });
        optionOther.setOnClickListener(v -> {
            setOptionUnselected(optionHospital); setOptionUnselected(optionMaternity); setOptionSelected(optionOther);
            mHospitalMaternityString = optionOther.getText().toString();
            if (mHospitalMaternityString.equalsIgnoreCase("other")) {
                cardHospitalOther.setVisibility(View.VISIBLE); etHospitalOther.setVisibility(View.VISIBLE);
            }
            tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE);
        });
        tvSpontaneous.setOnClickListener(v -> {
            tvSpontaneous.setBackground(getResources().getDrawable(R.drawable.button_primary_rounded));
            tvInduced.setBackground(getResources().getDrawable(R.drawable.button_bg_rounded_corners));
            tvSpontaneous.setTextColor(getResources().getColor(R.color.white));
            tvInduced.setTextColor(getResources().getColor(R.color.darkGray));
            mLaborOnsetString = tvSpontaneous.getText().toString();
            tvErrorLabourOnset.setVisibility(View.GONE);
        });
        tvInduced.setOnClickListener(v -> {
            tvSpontaneous.setBackground(getResources().getDrawable(R.drawable.button_bg_rounded_corners));
            tvInduced.setBackground(getResources().getDrawable(R.drawable.button_primary_rounded));
            tvSpontaneous.setTextColor(getResources().getColor(R.color.darkGray));
            tvInduced.setTextColor(getResources().getColor(R.color.white));
            mLaborOnsetString = tvInduced.getText().toString();
            tvErrorLabourOnset.setVisibility(View.GONE);
        });
    }

    private void setOptionSelected(TextView tv) {
        tv.setBackground(getResources().getDrawable(R.drawable.button_primary_rounded));
        tv.setTextColor(getResources().getColor(R.color.white));
    }
    private void setOptionUnselected(TextView tv) {
        tv.setBackground(getResources().getDrawable(R.drawable.button_bg_rounded_corners));
        tv.setTextColor(getResources().getColor(R.color.darkGray));
    }

    private void showRiskFactorSelectionDialog() {
        MultiChoiceDialogFragment<String> dialog1 = new MultiChoiceDialogFragment.Builder<String>(mContext)
                .title(R.string.select_risk_factors).positiveButtonLabel(R.string.save_button).build();
        dialog1.isSearchable(true);
        List<String> items = Arrays.asList(getResources().getStringArray(R.array.risk_factors));
        dialog1.setAdapter(new RiskFactorMultiChoiceAdapter(mContext, new ArrayList<>(items)));
        dialog1.setListener(selectedItems -> {
            if (selectedItems.size() > 0) {
                View other = view.findViewById(R.id.llViewOtherRiskFactor);
                StringBuilder sb = new StringBuilder();
                other.setVisibility(View.GONE);
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (!sb.toString().isEmpty()) sb.append(", ");
                    sb.append(selectedItems.get(i));
                    if (selectedItems.get(i).equals(getString(R.string.other_risk))) other.setVisibility(View.VISIBLE);
                }
                mRiskFactorsString = sb.toString();
                mRiskFactorsTextView.setText(mRiskFactorsString);
            }
        });
        dialog1.show(getChildFragmentManager(), MultiChoiceDialogFragment.class.getCanonicalName());
    }

    public void generateUuid() { patientUuid = uuidGenerator.UuidGenerator(); }

    private void setScrollToFocusedItem() {
        if (requireView().findFocus() != null) {
            Point scroll = getLocationOnScreen(scrollviewOtherInfo);
            Point point  = getLocationOnScreen(requireView().findFocus());
            int coord = point.y - scroll.y;
            if (coord <= 0) scrollviewOtherInfo.smoothScrollTo(0, 0);
            else if (scroll.y > coord) coord = point.y;
            scrollviewOtherInfo.smoothScrollTo(0, coord);
        }
    }

    public static Point getLocationOnScreen(View v) {
        int[] loc = new int[2]; v.getLocationOnScreen(loc);
        return new Point(loc[0], loc[1]);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPatientCreateClicked() {
        if (!etHospitalOther.getText().toString().isEmpty()) mHospitalMaternityString = "other";
        if (!areValidFields()) { setScrollToFocusedItem(); return; }

        mTotalBirthCount       = mTotalBirthEditText.getText().toString().trim();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString().trim();
        int total   = Integer.parseInt(mTotalBirthCount) + Integer.parseInt(mTotalMiscarriageCount);
        int age     = DateAndTimeUtils.getAgeInYearsOnly(patientDTO.getDateofbirth());
        int allowed = age - 12;

        if (total > allowed) { isParityWarningDialogShown = true; showParityWarningDialog(); }
        else if (validateGravida()) { savePatientsDataInDb(); }
    }

    public void setSelectedDob(Context context, String dob) {
        context.getApplicationContext().getSharedPreferences("dobPatient", 0)
                .edit().putString("dobPatient", dob).apply();
    }

    private void setFocus(View v) { if (requireView().findFocus() == null) v.requestFocus(); }

    private boolean areValidFields() {
        errorDetailsList = new ArrayList<>();
        if (requireView().findFocus() != null) requireView().clearFocus();

        if (TextUtils.isEmpty(mAdmissionDateTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mAdmissionDateTextView, tvErrorAdmissionDate, getString(R.string.select_admission_date), cardAdmissionDate));
        } else { tvErrorAdmissionDate.setVisibility(View.GONE); cardAdmissionDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (TextUtils.isEmpty(mAdmissionTimeTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mAdmissionTimeTextView, tvErrorAdmissionTime, getString(R.string.select_admission_time), cardAdmissionTime));
        } else { tvErrorAdmissionDate.setVisibility(View.GONE); tvErrorAdmissionTime.setVisibility(View.GONE); cardAdmissionTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        String birthStr = mTotalBirthEditText.getText().toString();
        if (TextUtils.isEmpty(birthStr)) {
            errorDetailsList.add(new ErrorManagerModel(mTotalBirthEditText, tvErrorTotalBirth, getString(R.string.total_birth_count_val_txt), cardTotalBirth));
        } else if (Integer.parseInt(birthStr) > 15) {
            errorDetailsList.add(new ErrorManagerModel(mTotalBirthEditText, tvErrorTotalBirth, getString(R.string.total_birth_count_limit), cardTotalBirth));
        } else { tvErrorTotalBirth.setVisibility(View.GONE); cardTotalBirth.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        String misStr = mTotalMiscarriageEditText.getText().toString();
        if (TextUtils.isEmpty(misStr)) {
            errorDetailsList.add(new ErrorManagerModel(mTotalMiscarriageEditText, tvErrorTotalMiscarriage, getString(R.string.total_miscarriage_count_val_txt), cardTotalMiscarraige));
        } else if (Integer.parseInt(misStr) > 8) {
            errorDetailsList.add(new ErrorManagerModel(mTotalMiscarriageEditText, tvErrorTotalMiscarriage, getString(R.string.miscarriage_count_limit), cardTotalMiscarraige));
        } else { tvErrorTotalMiscarriage.setVisibility(View.GONE); cardTotalMiscarraige.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (mLaborOnsetString.isEmpty()) {
            tvErrorLabourOnset.setVisibility(View.VISIBLE);
            tvErrorLabourOnset.setText(getString(R.string.labor_onset_val_txt));
            tvSpontaneous.setBackground(ContextCompat.getDrawable(mContext, R.drawable.error_bg_et));
            tvInduced.setBackground(ContextCompat.getDrawable(mContext, R.drawable.error_bg_et));
        } else { tvErrorLabourOnset.setVisibility(View.GONE); getLabourOnsetValue(mLaborOnsetString); }

        if (TextUtils.isEmpty(mActiveLaborDiagnosedDateTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mActiveLaborDiagnosedDateTextView, tvErrorLabourDiagnosedDate, getString(R.string.active_labor_diagnosed_date_val_txt), cardDiagnosedDate));
        } else { tvErrorLabourDiagnosedDate.setVisibility(View.GONE); cardDiagnosedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (TextUtils.isEmpty(mActiveLaborDiagnosedTimeTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mActiveLaborDiagnosedTimeTextView, tvErrorLabourDiagnosedTime, getString(R.string.active_labor_diagnosed_time_val_txt), cardDiagnosedTime));
        } else { tvErrorLabourDiagnosedDate.setVisibility(View.GONE); tvErrorLabourDiagnosedTime.setVisibility(View.GONE); cardDiagnosedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (!isUnknownChecked) {
            if (TextUtils.isEmpty(mMembraneRupturedDateTextView.getText().toString())) {
                errorDetailsList.add(new ErrorManagerModel(mMembraneRupturedDateTextView, tvErrorSacRupturedDate, getString(R.string.select_sac_ruptured_date), cardSacRupturedDate));
            } else { tvErrorSacRupturedDate.setVisibility(View.GONE); cardSacRupturedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

            if (TextUtils.isEmpty(mMembraneRupturedTimeTextView.getText().toString())) {
                tvErrorSacRupturedTime.setVisibility(View.VISIBLE);
                tvErrorSacRupturedTime.setText(getString(R.string.select_sac_ruptured_time));
                cardSacRupturedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
            } else { tvErrorSacRupturedDate.setVisibility(View.GONE); tvErrorSacRupturedTime.setVisibility(View.GONE); cardSacRupturedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }
        }

        View otherRF = view.findViewById(R.id.llViewOtherRiskFactor);
        if (TextUtils.isEmpty(mRiskFactorsTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mRiskFactorsTextView, tvErrorRiskFactor, getString(R.string.please_select_risk_factor), dropdownRiskFactors));
        } else if (otherRF.getVisibility() == View.VISIBLE && TextUtils.isEmpty(etHighRisk.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(etHighRisk, tvErrorHighRisk, getString(R.string.error_other_risk), cardOtherRisk));
        } else { tvErrorRiskFactor.setVisibility(View.GONE); dropdownRiskFactors.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (mHospitalMaternityString.isEmpty()) {
            tvErrorHospital.setVisibility(View.VISIBLE);
            tvErrorHospital.setText(getString(R.string.hospital_matermnity_val_txt));
            errorDetailsList.add(new ErrorManagerModel(etHospitalOther, tvErrorHospital, getString(R.string.hospital_matermnity_val_txt), null));
        } else if (mHospitalMaternityString.equalsIgnoreCase("hospital") || mHospitalMaternityString.equalsIgnoreCase("maternity")) {
            etHospitalOther.setVisibility(View.GONE); cardHospitalOther.setVisibility(View.GONE);
            tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE);
        } else {
            cardHospitalOther.setVisibility(View.VISIBLE); etHospitalOther.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(etHospitalOther.getText().toString())) {
                errorDetailsList.add(new ErrorManagerModel(etHospitalOther, tvErrorHospitalOther, getString(R.string.enter_hospital_other_error), cardHospitalOther));
            } else {
                mHospitalMaternityString = etHospitalOther.getText().toString();
                cardHospitalOther.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE);
            }
        }

        if (TextUtils.isEmpty(mPrimaryDoctorTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mPrimaryDoctorTextView, tvErrorPrimaryDoctor, getString(R.string.select_primary_doctor), cardPrimaryDoctor));
        } else { tvErrorPrimaryDoctor.setVisibility(View.GONE); cardPrimaryDoctor.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (TextUtils.isEmpty(mSecondaryDoctorTextView.getText().toString())) {
            errorDetailsList.add(new ErrorManagerModel(mSecondaryDoctorTextView, tvErrorSecondaryDoctor, getString(R.string.select_secondary_doctor), cardSecondaryDoctor));
        } else { tvErrorSecondaryDoctor.setVisibility(View.GONE); cardSecondaryDoctor.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar)); }

        if (!errorDetailsList.isEmpty()) {
            for (int i = 0; i < errorDetailsList.size(); i++) {
                ErrorManagerModel em = errorDetailsList.get(i);
                em.tvError.setVisibility(View.VISIBLE);
                em.tvError.setText(em.getErrorMessage());
                if (em.cardView != null) em.cardView.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
            }
            return false;
        }
        return true;
    }

    private void onBackInsertIntopatientDTO() {
        PatientAttributesModel attrs = getPatientAttributes();
        Bundle bundle = new Bundle();
        bundle.putSerializable("patientDTO", (Serializable) patientDTO);
        bundle.putBoolean("fromThirdScreen", true);
        bundle.putBoolean("patient_detail", patient_detail);
        bundle.putBoolean("editDetails", true);
        bundle.putString("mAlternateNumberString", mAlternateNumberString);
        bundle.putBoolean("fromSummary", fromSummary);
        bundle.putString("patientUuidUpdate", patientUuidUpdate);
        bundle.putSerializable("patientAttributes", (Serializable) attrs);
        secondScreen.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_add_patient, secondScreen).commit();
        ((AddNewPatientActivity) requireActivity()).changeCurrentPage(AddNewPatientActivity.PAGE_ADDRESS);
    }

    private PatientAttributesModel getPatientAttributes() {
        PatientAttributesModel m = new PatientAttributesModel();
        mTotalBirthCount       = mTotalBirthEditText.getText().toString();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString();
        m.setAdmissionDate(mAdmissionDateString);          // Gregorian dd/MM/yyyy
        m.setAdmissionTime(mAdmissionTimeString);
        m.setActiveLabourDiagnosedDate(mActiveLaborDiagnosedDate);
        m.setActiveLabourDiagnosedTime(mActiveLaborDiagnosedTime);
        m.setTotalBirthCount(mTotalBirthCount);
        m.setTotalMiscarriageCount(mTotalMiscarriageCount);
        m.setLabourOnset(mLaborOnsetString);
        m.setHospitalMaternity(mHospitalMaternityString);
        m.setPrimaryDoctor(mPrimaryDoctorTextView.getText().toString());
        if (mSecondaryDoctorTextView.getText().length() > 0)
            m.setSecondaryDoctor(mSecondaryDoctorTextView.getText().toString());
        m.setRiskFactors(mRiskFactorsString);
        m.setBedNumber(!TextUtils.isEmpty(etBedNumber.getText().toString())
                ? etBedNumber.getText().toString() : AppConstants.NOT_APPLICABLE);
        m.setMembraneCheckboxChecked(mUnknownMembraneRupturedCheckBox.isChecked());
        if (mUnknownMembraneRupturedCheckBox.isChecked()) {
            mMembraneRupturedDate = ""; mMembraneRupturedTime = "";
            mMembraneRupturedDateTextView.setText(""); mMembraneRupturedTimeTextView.setText("");
        }
        m.setSacRupturedDate(mMembraneRupturedDate);
        m.setSacRupturedTime(mMembraneRupturedTime);
        m.setOtherHospitalString(etHospitalOther.getText().toString());
        m.setGravida(mGravidaEdittext.getText().toString());
        m.setLmp(mLmpDate);     // Gregorian dd/MM/yyyy
        m.setEdd(mEDD);         // Gregorian dd/MM/yyyy
        m.setHospitalId(mHospitalId.getText().toString());
        return m;
    }

    private void selectPrimaryDoctor() {
        List<ProviderDTO> list = new ArrayList<>();
        for (ProviderDTO p : mProviderDoctorList)
            if (!mSecondaryDoctorUUIDString.equals(p.getUserUuid())) list.add(p);
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        int selId = 0;
        for (int i = 0; i < list.size(); i++) {
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(list.get(i).getGivenName() + " " + list.get(i).getFamilyName());
            item.setItemId(list.get(i).getUserUuid()); item.setItemIndex(i); items.add(item);
            if (mPrimaryDoctorUUIDString.equals(list.get(i).getUserUuid())) selId = i;
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(mContext)
                .title(R.string.select_primary_doctor).positiveButtonLabel(R.string.save_button).content(items).build();
        dialog.isSearchable(true);
        dialog.setListener(item -> { mPrimaryDoctorUUIDString = item.getItemId(); mPrimaryDoctorTextView.setText(item.getItem()); });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void selectSecondaryDoctor() {
        if (mPrimaryDoctorUUIDString.isEmpty()) { Toast.makeText(mContext, "Please select the primary doctor", Toast.LENGTH_SHORT).show(); return; }
        List<ProviderDTO> list = new ArrayList<>();
        for (ProviderDTO p : mProviderDoctorList)
            if (!mPrimaryDoctorUUIDString.equals(p.getUserUuid())) list.add(p);
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        SingChoiceItem na = new SingChoiceItem(); na.setItem(AppConstants.NOT_APPLICABLE_FULL_TEXT); na.setItemId(AppConstants.NOT_APPLICABLE); na.setItemIndex(0); items.add(na);
        for (int i = 0; i < list.size(); i++) {
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(list.get(i).getGivenName() + " " + list.get(i).getFamilyName());
            item.setItemId(list.get(i).getUserUuid()); item.setItemIndex(i + 1);
            item.setSelected(mSecondaryDoctorUUIDString.equals(list.get(i).getUserUuid())); items.add(item);
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(mContext)
                .title(R.string.select_secondary_doctor).positiveButtonLabel(R.string.save_button).content(items).build();
        dialog.isSearchable(true);
        dialog.setListener(item -> { mSecondaryDoctorUUIDString = item.getItemId(); mSecondaryDoctorTextView.setText(item.getItem()); });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void setscreen(String patientUID) {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        String[] cols = {"uuid","first_name","middle_name","last_name","date_of_birth","address1","address2","city_village","state_province","postal_code","country","phone_number","gender","sdw","occupation","patient_photo","economic_status","education_status","caste"};
        Cursor c = db.query("tbl_patient", cols, "uuid=?", new String[]{patientUID}, null, null, null);
        if (c.moveToFirst()) {
            patient1.setUuid(c.getString(c.getColumnIndexOrThrow("uuid")));
            patient1.setFirst_name(c.getString(c.getColumnIndexOrThrow("first_name")));
            patient1.setMiddle_name(c.getString(c.getColumnIndexOrThrow("middle_name")));
            patient1.setLast_name(c.getString(c.getColumnIndexOrThrow("last_name")));
            patient1.setDate_of_birth(c.getString(c.getColumnIndexOrThrow("date_of_birth")));
            patient1.setPhone_number(c.getString(c.getColumnIndexOrThrow("phone_number")));
            patient1.setPatient_photo(c.getString(c.getColumnIndexOrThrow("patient_photo")));
        }
        c.close();
        Cursor ca = db.query("tbl_patient_attribute", new String[]{"value","person_attribute_type_uuid"}, "patientuuid = ?", new String[]{patientUID}, null, null, null);
        if (ca.moveToFirst()) {
            do {
                String name = "";
                try { name = patientsDAO.getAttributesName(ca.getString(ca.getColumnIndexOrThrow("person_attribute_type_uuid"))); }
                catch (DAOException e) { FirebaseCrashlytics.getInstance().recordException(e); }
                String val = ca.getString(ca.getColumnIndexOrThrow("value"));
                switch (name.toLowerCase()) {
                    case "admission_date": patient1.setAdmissionDate(val); break;
                    case "admission_time": patient1.setAdmissionTime(val); break;
                    case "parity": patient1.setParity(val); break;
                    case "labor onset": patient1.setLaborOnset(val); break;
                    case "active labor diagnosed": patient1.setActiveLaborDiagnosed(val); break;
                    case "membrane ruptured timestamp": patient1.setMembraneRupturedTimestamp(val); break;
                    case "risk factors": patient1.setRiskFactors(val); break;
                    case "hospital_maternity": patient1.setHospitalMaternity(val); break;
                    case "primarydoctor": patient1.setPrimaryDoctor(val); break;
                    case "secondarydoctor": patient1.setSecondaryDoctor(val); break;
                    case "ezazi registration number": patient1.seteZaziRegNumber(val); break;
                    case "gravida": patient1.setGravida(val); break;
                    case "last menstrual period (lmp)": patient1.setLmp(val); break;
                    case "estimated date of delivery (edd)": patient1.setEdd(val); break;
                    case "hospital id": patient1.setHospitalId(val); break;
                    case "alternateno": patient1.setAlternateNo(val); break;
                }
            } while (ca.moveToNext());
        }
        ca.close();
    }

    private void getHospitalMaternityValue(String s) {
        if (s.equalsIgnoreCase("Hospital")) {
            setOptionSelected(optionHospital); setOptionUnselected(optionMaternity); setOptionUnselected(optionOther);
            cardHospitalOther.setVisibility(View.GONE); etHospitalOther.setVisibility(View.GONE);
        } else if (s.equalsIgnoreCase("Maternity")) {
            setOptionUnselected(optionHospital); setOptionSelected(optionMaternity); setOptionUnselected(optionOther);
            cardHospitalOther.setVisibility(View.GONE); etHospitalOther.setVisibility(View.GONE);
        } else {
            setOptionUnselected(optionHospital); setOptionUnselected(optionMaternity); setOptionSelected(optionOther);
            cardHospitalOther = view.findViewById(R.id.card_hospital_other);
            cardHospitalOther.setVisibility(View.VISIBLE); etHospitalOther.setVisibility(View.VISIBLE);
            etHospitalOther.setText(s);
        }
    }

    private void getLabourOnsetValue(String s) {
        if (s.equalsIgnoreCase("Spontaneous")) {
            setOptionSelected(tvSpontaneous); setOptionUnselected(tvInduced);
            mLaborOnsetString = tvSpontaneous.getText().toString();
        } else if (s.equalsIgnoreCase("Induced")) {
            setOptionUnselected(tvSpontaneous); setOptionSelected(tvInduced);
            mLaborOnsetString = tvInduced.getText().toString();
        }
    }

    private int parseSafe(String v) { try { return (v == null || v.isEmpty()) ? 0 : Integer.parseInt(v); } catch (Exception e) { return 0; } }
    private void updateGravida() { if (isGravidaEdited) return; mGravidaEdittext.setText(String.valueOf(parseSafe(mTotalBirthCount) + parseSafe(mTotalMiscarriageCount) + 1)); }

    private boolean validateGravida() {
        String val = mGravidaEdittext.getText().toString().trim();
        if (val.isEmpty()) { tvErrorGravida.setText(getString(R.string.error_gravida_required)); tvErrorGravida.setVisibility(View.VISIBLE); return false; }
        int g = Integer.parseInt(val);
        if (g < 0) { tvErrorGravida.setText(getString(R.string.error_gravida_negative)); tvErrorGravida.setVisibility(View.VISIBLE); return false; }
        if (g > 20) { tvErrorGravida.setText(getString(R.string.error_gravida_max_limit)); tvErrorGravida.setVisibility(View.VISIBLE); return false; }
        tvErrorGravida.setVisibility(View.GONE); return true;
    }

    private String getBedNumber(String patientuuid) throws DAOException {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        String bedNumber = null;
        Cursor c = db.rawQuery("SELECT value FROM tbl_patient_attribute where patientuuid = ? AND person_attribute_type_uuid='d0786817-68d9-4226-b311-3de68d534b9e'", new String[]{patientuuid});
        try { while (c.moveToNext()) bedNumber = c.getString(c.getColumnIndexOrThrow("value")); }
        catch (SQLException s) { FirebaseCrashlytics.getInstance().recordException(s); }
        c.close(); return bedNumber;
    }

    private void showParityWarningDialog() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(requireActivity())
                .title(R.string.parity_dialog_warning)
                .positiveButtonLabel(R.string.confirm_and_submit)
                .negativeButtonLabel(R.string.review_details)
                .content(getString(R.string.parity_dialog_message)).build();
        dialog.setListener(new ConfirmationDialogFragment.OnConfirmationActionListener() {
            @Override public void onAccept() { savePatientsDataInDb(); dialog.dismiss(); }
            @Override public void onDecline() { dialog.dismiss(); }
        });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void savePatientsDataInDb() {
        mTotalBirthCount       = mTotalBirthEditText.getText().toString().trim();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString().trim();
        if (mHospitalMaternityString.trim().equalsIgnoreCase("other")) {
            mHospitalMaternityString = etHospitalOther.getText().toString();
            cardHospitalOther.setVisibility(View.VISIBLE); etHospitalOther.setVisibility(View.VISIBLE);
            tvErrorHospital.setVisibility(View.GONE); tvErrorHospitalOther.setVisibility(View.GONE);
        }

        PatientsDAO patientsDAO = new PatientsDAO();
        List<PatientAttributesDTO> attrList = new ArrayList<>();

        if (fromSummary && patientUuidUpdate != null && !patientUuidUpdate.isEmpty()) uuid = patientUuidUpdate;
        else uuid = UUID.randomUUID().toString();

        patientDTO.setUuid(uuid);
        patientDTO.setCreatorUuid(sessionManager.getCreatorID());

        // Helper lambda
        java.util.function.BiFunction<String, String, PatientAttributesDTO> mkAttr = (colKey, value) -> {
            PatientAttributesDTO a = new PatientAttributesDTO();
            a.setUuid(UUID.randomUUID().toString()); a.setPatientuuid(uuid);
            a.setPersonAttributeTypeUuid(patientsDAO.getUuidForAttribute(colKey));
            a.setValue(value); return a;
        };

        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ADMISSION_DATE.value, StringUtils.getValue(mAdmissionDateString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ADMISSION_TIME.value, StringUtils.getValue(mAdmissionTimeString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PARITY.value, StringUtils.getValue(mTotalBirthCount + "," + mTotalMiscarriageCount)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.LABOR_ONSET.value, StringUtils.getValue(mLaborOnsetString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ACTIVE_LABOR_DIAGNOSED.value, StringUtils.getValue(mActiveLaborDiagnosedDate + " " + mActiveLaborDiagnosedTime)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.MEMBRANE_RUPTURED_TIMESTAMP.value,
                mUnknownMembraneRupturedCheckBox.isChecked() ? "U" : StringUtils.getValue(mMembraneRupturedDate + " " + mMembraneRupturedTime)));

        if (mRiskFactorsString.contains(getString(R.string.other_risk)))
            mRiskFactorsString = mRiskFactorsString.replace(getString(R.string.other_risk), etHighRisk.getText().toString());
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.RISK_FACTORS.value, StringUtils.getValue(mRiskFactorsString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.HOSPITAL_MATERNITY.value, StringUtils.getValue(mHospitalMaternityString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PRIMARY_DOCTOR.value, StringUtils.getValue(mPrimaryDoctorUUIDString) + "@#@" + mPrimaryDoctorTextView.getText()));
        if (mSecondaryDoctorTextView.getText().length() > 0)
            attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.SECONDARY_DOCTOR.value, StringUtils.getValue(mSecondaryDoctorUUIDString) + "@#@" + mSecondaryDoctorTextView.getText()));

        int num = (int)(Math.random() * (99999999 - 100 + 1) + 100);
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.REGISTRATION_NUMBER.value,
                patientDTO.getCountry().substring(0, 2) + "/" + patientDTO.getStateprovince().substring(0, 2) + "/" + patientDTO.getCityvillage().substring(0, 2) + "/" + num));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.BED_NUMBER.value,
                !TextUtils.isEmpty(etBedNumber.getText().toString()) ? StringUtils.getValue(etBedNumber.getText().toString()) : StringUtils.getValue(AppConstants.NOT_APPLICABLE)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ALTERNATE_NO.value, StringUtils.getValue(mAlternateNumberString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PROFILE_IMG_TIMESTAMP.value, AppConstants.dateAndTimeUtils.currentDateTime()));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.GRAVIDA.value, mGravidaEdittext.getText().toString()));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.lmp.value, mLmpDate));   // Gregorian dd/MM/yyyy
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.EDD.value, mEDD));       // Gregorian dd/MM/yyyy
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.HOSPITAL_ID.value, mHospitalId.getText().toString()));

        patientDTO.setPatientAttributesDTOList(attrList);
        patientDTO.setSyncd(false);

        try {
            if (fromSummary) {
                boolean upd = patientsDAO.updatePatientToDBNew(patientDTO, uuid, attrList);
                boolean img = imagesDAO.updatePatientProfileImages(patientDTO.getPatientPhoto(), uuid);
                if (NetworkConnection.isOnline(getActivity().getApplication())) { new SyncDAO().pushDataApi(); new ImagesPushDAO().patientProfileImagesPush(); }
                if (upd && img) {
                    Intent i = new Intent(getActivity().getApplication(), PatientDetailActivity.class);
                    i.putExtra("patientUuid", uuid); i.putExtra("patientName", patientDTO.getFirstname() + " " + patientDTO.getLastname());
                    i.putExtra("tag", "newPatient"); i.putExtra("hasPrescription", "false");
                    getActivity().startActivity(i); getActivity().finish();
                }
            } else {
                patientDTO.setCreatedAt(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
                boolean ins = patientsDAO.insertPatientToDB(patientDTO, uuid);
                imagesDAO.insertPatientProfileImages(patientDTO.getPatientPhoto(), uuid);
                if (NetworkConnection.isOnline(mContext)) { new SyncDAO().pushDataApi(); new ImagesPushDAO().patientProfileImagesPush(); }
                if (ins) {
                    Intent i = new Intent(mContext, PatientDetailActivity.class);
                    i.putExtra("patientUuid", uuid); i.putExtra("patientName", patientDTO.getFirstname() + " " + patientDTO.getLastname());
                    i.putExtra("tag", "newPatient"); i.putExtra("privacy", privacy_value); i.putExtra("hasPrescription", "false");
                    setSelectedDob(requireContext(), "");
                    mContext.startActivity(i); getActivity().finish();
                } else { Toast.makeText(mContext, "Error adding data", Toast.LENGTH_SHORT).show(); }
            }
        } catch (DAOException e) { FirebaseCrashlytics.getInstance().recordException(e); }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  TextWatcher (unchanged logic, date fields read-only via picker)
    // ═════════════════════════════════════════════════════════════════════

    class MyTextWatcher implements TextWatcher {
        final EditText editText;
        MyTextWatcher(EditText et) { this.editText = et; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

        @Override
        public void afterTextChanged(Editable editable) {
            String val = editable.toString().trim();
            if (val.length() > 0) {
                int id = editText.getId();
                if (id == R.id.et_admission_date) {
                    tvErrorAdmissionDate.setVisibility(View.GONE);
                    cardAdmissionDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.et_admission_time) {
                    tvErrorAdmissionTime.setVisibility(View.GONE);
                    cardAdmissionTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.et_total_birth) {
                    if (Integer.parseInt(val) > 15) {
                        tvErrorTotalBirth.setVisibility(View.VISIBLE); tvErrorTotalBirth.setText(getString(R.string.total_birth_count_limit));
                        cardTotalBirth.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                    } else {
                        tvErrorTotalBirth.setVisibility(View.GONE); cardTotalBirth.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                        mTotalBirthCount = val; updateGravida();
                    }
                } else if (id == R.id.et_total_miscarriage) {
                    if (Integer.parseInt(val) > 8) {
                        tvErrorTotalMiscarriage.setVisibility(View.VISIBLE); tvErrorTotalMiscarriage.setText(getString(R.string.miscarriage_count_limit));
                        cardTotalMiscarraige.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                    } else {
                        tvErrorTotalMiscarriage.setVisibility(View.GONE); cardTotalMiscarraige.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                        mTotalMiscarriageCount = val; updateGravida();
                    }
                } else if (id == R.id.et_labor_diagnosed_date) {
                    tvErrorLabourDiagnosedDate.setVisibility(View.GONE); cardDiagnosedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.et_labor_diagnosed_time) {
                    tvErrorLabourDiagnosedTime.setVisibility(View.GONE); cardDiagnosedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.et_sac_ruptured_date && !isUnknownChecked) {
                    tvErrorSacRupturedDate.setVisibility(View.GONE); cardSacRupturedDate.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.et_sac_ruptured_time && !isUnknownChecked) {
                    tvErrorSacRupturedTime.setVisibility(View.GONE); cardSacRupturedTime.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.autotv_risk_factors) {
                    tvErrorRiskFactor.setVisibility(View.GONE); dropdownRiskFactors.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.etOtherRiskFactor) {
                    tvErrorHighRisk.setVisibility(View.GONE); cardOtherRisk.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.autotv_primary_doctor) {
                    tvErrorPrimaryDoctor.setVisibility(View.GONE); cardPrimaryDoctor.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                } else if (id == R.id.autotv_secondary_doctor) {
                    tvErrorSecondaryDoctor.setVisibility(View.GONE); cardSecondaryDoctor.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                }
            } else if (editText.getId() == R.id.et_total_birth) {
                isGravidaEdited = true;
            }
        }
    }
}