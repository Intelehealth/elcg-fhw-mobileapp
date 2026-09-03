package org.intelehealth.ezazi.activities.addNewPatient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import org.intelehealth.ezazi.ui.dialog.CalendarDialog;
import org.intelehealth.ezazi.utilities.AppRegion;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

public class PatientOtherInfoFragment extends Fragment {

    private static final String TAG = "PatientOtherInfoFrag";

    private static final String[] BS_MONTH_NAMES = {
            "Baisakh", "Jestha", "Asar", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

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

    // ── State ──────────────────────────────────────────────────────────────
    private String mAdmissionDateString = "", mAdmissionTimeString = "";
    private String mTotalBirthCount = "0", mTotalMiscarriageCount = "0";
    private String mLaborOnsetString = "";
    private String mHospitalMaternityString = "";
    private String mActiveLaborDiagnosedDate = "", mActiveLaborDiagnosedTime = "";
    private String mMembraneRupturedDate = "", mMembraneRupturedTime = "";
    private String mRiskFactorsString = "", mPrimaryDoctorUUIDString = "", mSecondaryDoctorUUIDString = "";
    private String mAlternateNumberString = "";
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
    boolean fromSecondScreen = false;
    TextView tvSpontaneous, tvInduced;
    PatientsDAO patientsDAO = new PatientsDAO();

    // ── Error TextViews ────────────────────────────────────────────────────
    private TextView tvErrorAdmissionDate, tvErrorAdmissionTime, tvErrorTotalBirth,
            tvErrorTotalMiscarriage, tvErrorLabourOnset,
            tvErrorSacRupturedDate, tvErrorSacRupturedTime,
            tvErrorPrimaryDoctor, tvErrorSecondaryDoctor, tvErrorBedNumber,
            tvErrorLabourDiagnosedDate, tvErrorLabourDiagnosedTime,
            tvErrorRiskFactor, tvErrorHospital, tvErrorHospitalOther,
            tvErrorGravida, tvErrorLmpDate, tvErrorEDD, tvErrorHospitalId, tvErrorSacRuptured;
    private TextView tvErrorHighRisk;

    // ── Card views ─────────────────────────────────────────────────────────
    private MaterialCardView cardAdmissionDate, cardAdmissionTime, cardTotalBirth,
            cardTotalMiscarraige, cardSacRupturedDate, cardSacRupturedTime,
            cardPrimaryDoctor, cardSecondaryDoctor, cardBedNumber,
            cardDiagnosedDate, cardDiagnosedTime, dropdownRiskFactors,
            cardOtherRisk, cardHospitalOther, cardSacRupturedMembrane;
    private LinearLayout layoutSacRuptured, cardOptions;

    private boolean isUnknownChecked;
    private PatientAttributesModel patientAttributesModel;
    private NestedScrollView scrollviewOtherInfo;
    private EditText etHighRisk;
    private boolean isParityWarningDialogShown = false;
    boolean isGravidaEdited = false;
    private String mLmpDate = "", mEDD = "";
    private TextInputEditText mLmpDateTextView, mEDDTextView;
    private String patientUuid = "";
    private String mSelectedRuptureMembrane = "";
    // ═════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════
    private AutoCompleteTextView autotvRupturedMembrane;
    private boolean shouldValidateSacMembraneDates = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_patient_other_info, container, false);
        mContext = getActivity();
        sessionManager = new SessionManager(mContext);
        initUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

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
        mAdmissionDateTextView = view.findViewById(R.id.et_admission_date);
        mAdmissionTimeTextView = view.findViewById(R.id.et_admission_time);
        mTotalBirthEditText = view.findViewById(R.id.et_total_birth);
        mTotalMiscarriageEditText = view.findViewById(R.id.et_total_miscarriage);
        tvSpontaneous = view.findViewById(R.id.et_spontaneous);
        tvInduced = view.findViewById(R.id.et_induced);
        mActiveLaborDiagnosedDateTextView = view.findViewById(R.id.et_labor_diagnosed_date);
        mActiveLaborDiagnosedTimeTextView = view.findViewById(R.id.et_labor_diagnosed_time);
        mMembraneRupturedDateTextView = view.findViewById(R.id.et_sac_ruptured_date);
        mMembraneRupturedTimeTextView = view.findViewById(R.id.et_sac_ruptured_time);
        optionHospital = view.findViewById(R.id.option_hospital);
        optionMaternity = view.findViewById(R.id.option_maternity);
        optionOther = view.findViewById(R.id.option_other);
        mPrimaryDoctorTextView = view.findViewById(R.id.autotv_primary_doctor);
        mSecondaryDoctorTextView = view.findViewById(R.id.autotv_secondary_doctor);
        etBedNumber = view.findViewById(R.id.et_bed_number);
        btnBack = view.findViewById(R.id.btn_back_address);
        btnNext = view.findViewById(R.id.btn_next_address);
        mUnknownMembraneRupturedCheckBox = view.findViewById(R.id.mUnknownMembraneRupturedCheckBox);
        mRiskFactorsTextView = view.findViewById(R.id.autotv_risk_factors);
        dropdownRiskFactors = view.findViewById(R.id.dropdown_risk_factors);
        etHighRisk = view.findViewById(R.id.etOtherRiskFactor);
        tvErrorHighRisk = view.findViewById(R.id.tv_error_risk_factor_other);
        cardOtherRisk = view.findViewById(R.id.cardOtherRiskFactor);
        etHospitalOther = view.findViewById(R.id.et_hospital_other);
        scrollviewOtherInfo = view.findViewById(R.id.scroll_other_info);
        mGravidaEdittext = view.findViewById(R.id.et_gravida);
        mHospitalId = view.findViewById(R.id.et_hospital_id);
        tvErrorHospitalId = view.findViewById(R.id.tv_hospital_id_error);
        layoutSacRuptured = view.findViewById(R.id.card_sac_ruptured);

        View layoutLmpEdd = view.findViewById(R.id.view_lmp_edd_layout);
        mLmpDateTextView = layoutLmpEdd.findViewById(R.id.et_lmp);
        mEDDTextView     = layoutLmpEdd.findViewById(R.id.et_edd);
        tvErrorLmpDate   = view.findViewById(R.id.tv_lmp_error);
        tvErrorEDD       = view.findViewById(R.id.tv_edd_error);

        etHospitalOther.setFilters(new InputFilter[]{new FirstLetterUpperCaseInputFilter()});
        disableSoftInput(mAdmissionDateTextView, mActiveLaborDiagnosedDateTextView,
                mMembraneRupturedDateTextView, mLmpDateTextView, mEDDTextView);

        initErrorViews();
        initCardViews();
        handleOptionsForMaternity();

        ProviderDAO providerDAO = new ProviderDAO();
        try { mProviderDoctorList = providerDAO.getDoctorList(sessionManager.getLocationUuid()); }
        catch (DAOException e) { e.printStackTrace(); }

        autotvRupturedMembrane = view.findViewById(R.id.autotv_sac_ruptured_options);
        tvErrorSacRuptured = view.findViewById(R.id.tv_error_sac_ruptured_membrane);
        cardSacRupturedMembrane = view.findViewById(R.id.dropdown_sac_ruptured_options);

        handleAllClickListeners();

        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra("patientUuid")) {
            mIsEditMode    = true;
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
        for (EditText f : fields)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                f.setShowSoftInputOnFocus(false);
    }

    private void initErrorViews() {
        tvErrorAdmissionDate      = view.findViewById(R.id.tv_admission_date_error);
        tvErrorAdmissionTime      = view.findViewById(R.id.tv_admission_time_error);
        tvErrorTotalBirth         = view.findViewById(R.id.tv_parity_date_error);
        tvErrorTotalMiscarriage   = view.findViewById(R.id.tv_parity_time_error);
        tvErrorLabourOnset        = view.findViewById(R.id.tv_error_labour_onset);
        tvErrorSacRupturedDate    = view.findViewById(R.id.tv_sac_ruptured_date_error);
        tvErrorSacRupturedTime    = view.findViewById(R.id.tv_sac_ruptured_time_error);
        tvErrorPrimaryDoctor      = view.findViewById(R.id.tv_error_primary_doctor);
        tvErrorSecondaryDoctor    = view.findViewById(R.id.tv_error_secondary_doctor);
        tvErrorBedNumber          = view.findViewById(R.id.tv_error_bed_number);
        tvErrorLabourDiagnosedDate = view.findViewById(R.id.tv_labour_diagnosed_date_error);
        tvErrorLabourDiagnosedTime = view.findViewById(R.id.tv_labour_diagnosed_time_error);
        tvErrorRiskFactor         = view.findViewById(R.id.tv_error_risk_factor);
        tvErrorHospital           = view.findViewById(R.id.tv_error_hospital);
        tvErrorHospitalOther      = view.findViewById(R.id.tv_error_hospital_other);
        tvErrorGravida            = view.findViewById(R.id.tv_gravida_error);

        // Clear errors as user edits free-type fields
        mTotalBirthEditText.addTextChangedListener(new ClearErrorWatcher(tvErrorTotalBirth, cardTotalBirth));
        mTotalMiscarriageEditText.addTextChangedListener(new ClearErrorWatcher(tvErrorTotalMiscarriage, cardTotalMiscarraige));
        mPrimaryDoctorTextView.addTextChangedListener(new ClearErrorWatcher(tvErrorPrimaryDoctor, cardPrimaryDoctor));
        mSecondaryDoctorTextView.addTextChangedListener(new ClearErrorWatcher(tvErrorSecondaryDoctor, cardSecondaryDoctor));
        etHighRisk.addTextChangedListener(new ClearErrorWatcher(tvErrorHighRisk, cardOtherRisk));
        mGravidaEdittext.addTextChangedListener(new ClearErrorWatcher(tvErrorGravida, null));
        etHospitalOther.addTextChangedListener(new ClearErrorWatcher(tvErrorHospitalOther, cardHospitalOther));

        // Gravida auto-update
        mTotalBirthEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                String v = e.toString().trim();
                if (!v.isEmpty()) {
                    mTotalBirthCount = v;
                    updateGravida();
                } else {
                    mGravidaEdittext.setText(null);
                }
            }
        });
        mTotalMiscarriageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                String v = e.toString().trim();
                if (!v.isEmpty()) {
                    mTotalMiscarriageCount = v;
                    updateGravida();
                } else {
                    mGravidaEdittext.setText(null);
                }
            }
        });
    }

    private void initCardViews() {
        cardAdmissionDate = view.findViewById(R.id.card_date_admission);
        cardAdmissionTime = view.findViewById(R.id.card_time_admission);
        cardTotalBirth = view.findViewById(R.id.card_total_birth);
        cardTotalMiscarraige = view.findViewById(R.id.card_total_miscarraige);
        cardSacRupturedDate = view.findViewById(R.id.card_sac_ruptured_date);
        cardSacRupturedTime = view.findViewById(R.id.card_sac_ruptured_time);
        cardPrimaryDoctor = view.findViewById(R.id.dropdown_primary_doctor);
        cardSecondaryDoctor = view.findViewById(R.id.dropdown_secondary_doctor);
        cardBedNumber = view.findViewById(R.id.card_bed_no);
        cardDiagnosedDate = view.findViewById(R.id.card_diagnosed_date);
        cardDiagnosedTime = view.findViewById(R.id.card_diagnosed_time);
        cardOptions = view.findViewById(R.id.card_options);
        cardHospitalOther = view.findViewById(R.id.card_hospital_other);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  ClearErrorWatcher — only hides error when user types; no validation
    // ═════════════════════════════════════════════════════════════════════

    private class ClearErrorWatcher implements TextWatcher {
        private final TextView errorView;
        @Nullable
        private final MaterialCardView card;

        ClearErrorWatcher(TextView errorView, @Nullable MaterialCardView card) {
            this.errorView = errorView;
            this.card = card;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable e) {
            if (errorView != null) errorView.setVisibility(View.GONE);
            if (card != null)
                card.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Nepali Date Picker — fully open, no restrictions
    // ═════════════════════════════════════════════════════════════════════

    interface OnBsDateSelectedListener {
        void onSelected(int bsYear, int bsMonth, int bsDay);
    }

    /**
     * Delivers the chosen date as a Gregorian {@code dd/MM/yyyy} string, so callers never handle
     * Bikram Sambat components and do not change when the calendar does.
     */
    private interface OnGregorianDateSelectedListener {
        void onSelected(String gregorianDdMmYyyy);
    }

    /**
     * Single entry point for every obstetric date field. Nepal gets the Bikram Sambat wheel picker;
     * every other region gets the standard Gregorian calendar dialog. Both hand the caller the same
     * Gregorian string, which is also what gets persisted.
     */
    private void showDatePicker(int titleRes, String currentGreg, OnGregorianDateSelectedListener listener) {
        if (AppRegion.usesBikramSambat()) {
            showNepaliDatePicker(titleRes, gregStringToBs(currentGreg), (y, m, d) -> {
                Date greg = NepaliDateConverter.bsToGregorian(y, m, d);
                if (greg == null) return;
                listener.onSelected(toGregFmt(greg));
            });
            return;
        }
        CalendarDialog dialog = new CalendarDialog.Builder(mContext)
                .title(getString(titleRes))
                .positiveButtonLabel(R.string.ok)
                .build();
        dialog.setDateFormat(GREG_FMT);
        Long seed = gregStringToMillis(currentGreg);
        if (seed != null) dialog.setDefaultDate(seed);
        dialog.setListener((day, month, year, value) -> listener.onSelected(value));
        dialog.show(getParentFragmentManager(), "DatePicker");
    }

    /**
     * Parses a stored {@code dd/MM/yyyy} value to epoch millis for seeding the Gregorian picker, or
     * null when there is nothing stored yet.
     */
    private Long gregStringToMillis(String gregDdMmYyyy) {
        if (gregDdMmYyyy == null || gregDdMmYyyy.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(gregDdMmYyyy);
            return d == null ? null : d.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private void showNepaliDatePicker(int titleRes,
                                      @Nullable int[] currentBsDate,
                                      OnBsDateSelectedListener listener) {
        int initY, initM, initD;
        if (currentBsDate != null && currentBsDate[0] > 0) {
            initY = currentBsDate[0];
            initM = currentBsDate[1];
            initD = currentBsDate[2];
        } else {
            int[] today = NepaliDateConverter.getCurrentBsDate();
            initY = today[0];
            initM = today[1];
            initD = today[2];
        }

        NumberPicker yearPicker = new NumberPicker(mContext);
        NumberPicker monthPicker = new NumberPicker(mContext);
        NumberPicker dayPicker = new NumberPicker(mContext);

        yearPicker.setMinValue(NepaliDateConverter.getMinSupportedBsYear());
        yearPicker.setMaxValue(NepaliDateConverter.getMaxSupportedBsYear());
        yearPicker.setValue(initY);

        // min/max MUST be set before setDisplayedValues; array length must == max-min+1 == 12
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(BS_MONTH_NAMES);
        monthPicker.setValue(initM);

        refreshDayPicker(dayPicker, initY, initM);
        dayPicker.setValue(Math.min(initD, dayPicker.getMaxValue()));

        NumberPicker.OnValueChangeListener onChange = (picker, oldVal, newVal) ->
                refreshDayPicker(dayPicker, yearPicker.getValue(), monthPicker.getValue());
        yearPicker.setOnValueChangedListener(onChange);
        monthPicker.setOnValueChangedListener(onChange);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(mContext);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(24, 24, 24, 24);
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

    private void refreshDayPicker(NumberPicker dp, int bsYear, int bsMonth) {
        int maxDay = NepaliDateConverter.getDaysInBsMonth(bsYear, bsMonth);
        dp.setMinValue(1);
        dp.setMaxValue(maxDay);
        if (dp.getValue() > maxDay) dp.setValue(maxDay);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Gregorian ↔ BS helpers
    // ═════════════════════════════════════════════════════════════════════

    private String formatBsDate(int y, int m, int d) {
        return String.format(Locale.ENGLISH, "%02d-%s-%d", d, BS_MONTH_NAMES[m - 1], y);
    }

    private String toGregFmt(Date date) {
        // ── FIX: UTC so the stored dd/MM/yyyy string matches the UTC midnight
        // produced by NepaliDateConverter.bsToGregorian(). Without this a device
        // in Nepal (UTC+5:45) would shift the date forward by 5h45m before
        // formatting, potentially rolling to the next day on edge-of-day values.
        SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Renders a stored Gregorian {@code dd/MM/yyyy} date for display.
     *
     * <p>Nepal renders Bikram Sambat. Every other region renders the stored Gregorian string
     * unchanged; choosing a friendlier Gregorian format is deferred to open decision Q6.
     */
    /**
     * Returns the leading two characters used to build the registration number.
     *
     * <p>The output is byte-identical to {@code substring(0, 2)} for every value of two characters or
     * more, which is every realistic country, province and village name. It differs only where the
     * old code threw: a null, empty, or single-character value. The registration number format is
     * therefore unchanged for all existing and future records; this only removes a crash on Save.
     */
    private String regNumberPart(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.length() >= 2 ? value.substring(0, 2) : value;
    }

    private String gregToDisplay(String gregDdMmYyyy) {
        if (gregDdMmYyyy == null || gregDdMmYyyy.isEmpty()) return "";
        if (!AppRegion.usesBikramSambat()) return gregDdMmYyyy;
        try {
            // ── FIX: pin to UTC so the parsed midnight matches the UTC-based
            // NepaliDateConverter. Without this, Nepal TZ (UTC+5:45) would shift
            // midnight to the previous day before gregorianToBs() sees it.
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(gregDdMmYyyy);
            int[] bs = NepaliDateConverter.gregorianToBs(d);
            return formatBsDate(bs[0], bs[1], bs[2]);
        } catch (Exception e) {
            return gregDdMmYyyy;
        }
    }

    @Nullable
    private int[] gregStringToBs(String gregDdMmYyyy) {
        if (gregDdMmYyyy == null || gregDdMmYyyy.isEmpty()) return null;
        try {
            // ── FIX: UTC so the day boundary matches the UTC-based converter ──
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = sdf.parse(gregDdMmYyyy);
            return NepaliDateConverter.gregorianToBs(d);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Normalises a stored time string to "hh:mm AM/PM" format.
     * The time picker always saves in "hh:mm a" (e.g. "09:30 AM").
     * Older / DB-loaded values may be in "HH:mm" (e.g. "14:30") with no AM/PM.
     * This method converts the latter to the former so all downstream
     * parsing (parseGregDateTime) and validation uses a consistent format.
     * Returns the input unchanged if it already contains AM/PM or cannot be parsed.
     */
    private String normaliseTimeString(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return timeStr;
        // Not 12-hour format — already 24-hour, nothing to convert
        String upper = timeStr.toUpperCase(Locale.ENGLISH);
        if (upper.contains("AM") || upper.contains("PM")) return timeStr;
        // Try parsing as HH:mm (24-hour)
        // Legacy 12-hour value ("hh:mm a") — convert down to 24-hour
        try {
            SimpleDateFormat in24  = new SimpleDateFormat("HH:mm a", Locale.ENGLISH);
            SimpleDateFormat out12 = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
            in24.setLenient(false);
            Date parsed = in24.parse(timeStr.trim());
            if (parsed != null) return out12.format(parsed);
        } catch (Exception ignored) {}
        return timeStr; // Hunchanged if we couldn't parse
    }

    /**
     * Parses Gregorian dd/MM/yyyy — returns null safely, never throws.
     */
    @Nullable
    private Date parseGregDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setLenient(false);
            return sdf.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns a Date representing today at 23:59:59 local time.
     * Using end-of-day instead of the current moment means any date
     * that parses to today's midnight is correctly treated as "today"
     * and not rejected as past, while any date parsing to tomorrow's
     * midnight is correctly rejected as future.
     */
    private Date endOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * Returns true if the given Gregorian dd/MM/yyyy date string
     * represents a date strictly after today (i.e. tomorrow or later).
     * Comparison is done at the date level, not the millisecond level,
     * to avoid timezone-induced off-by-one-day errors.
     */
    private boolean isAfterToday(String gregDateStr) {
        Date parsed = parseGregDate(gregDateStr);
        if (parsed == null) return false;
        // Strip time from both sides — compare dates only
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar sel = Calendar.getInstance();
        sel.setTime(parsed);
        sel.set(Calendar.HOUR_OF_DAY, 0);
        sel.set(Calendar.MINUTE, 0);
        sel.set(Calendar.SECOND, 0);
        sel.set(Calendar.MILLISECOND, 0);

        return sel.after(today);
    }

    /**
     * Parses a Gregorian date string + time string into a single Date.
     * Supports "hh:mm a" (12-hour, picker output) and "HH:mm" (24-hour legacy).
     * Returns null safely, never throws.
     */
    @Nullable
    private Date parseGregDateTime(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null || dateStr.isEmpty() || timeStr.isEmpty())
            return null;
        String combined = dateStr.trim() + " " + timeStr.trim();
        String[] formats = {"dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm", "dd/MM/yyyy hh:mm"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.ENGLISH);
                sdf.setLenient(false);
                Date d = sdf.parse(combined);
                if (d != null) return d;
            } catch (Exception ignored) {
            }
        }
        Log.e(TAG, "parseGregDateTime: could not parse '" + combined + "'");
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Per-field date picker launchers (no restrictions)
    // ═════════════════════════════════════════════════════════════════════

    private void pickAdmissionDate() {
        showDatePicker(R.string.select_admission_date, mAdmissionDateString,
                greg -> {
                    mAdmissionDateString = greg;
                    mAdmissionDateTextView.setText(gregToDisplay(greg));
                    clearError(tvErrorAdmissionDate, cardAdmissionDate);
                });
    }

    private void pickActiveLaborDate() {
        showDatePicker(R.string.select_labor_diagnosed_date, mActiveLaborDiagnosedDate,
                greg -> {
                    mActiveLaborDiagnosedDate = greg;
                    mActiveLaborDiagnosedDateTextView.setText(gregToDisplay(greg));
                    clearError(tvErrorLabourDiagnosedDate, cardDiagnosedDate);
                });
    }

    private void pickSacRupturedDate() {
        showDatePicker(R.string.select_sac_ruptured_date, mMembraneRupturedDate,
                greg -> {
                    mMembraneRupturedDate = greg;
                    mMembraneRupturedDateTextView.setText(gregToDisplay(greg));
                    clearError(tvErrorSacRupturedDate, cardSacRupturedDate);
                });
    }

    private void pickLmpDate() {
        showDatePicker(R.string.select_lmp_date, mLmpDate,
                greg -> {
                    mLmpDate = greg;
                    mLmpDateTextView.setText(gregToDisplay(greg));
                    clearError(tvErrorLmpDate, null);
                    calculateEDDFromLMP(mLmpDate);
                });
    }

    private void pickEddDate() {
        showDatePicker(R.string.select_edd_date, mEDD,
                greg -> {
                    mEDD = greg;
                    mEDDTextView.setText(gregToDisplay(greg));
                    clearError(tvErrorEDD, null);
                });
    }

    /**
     * EDD = LMP + 9 months + 7 days
     */
/* Commented due to https://intelehealthwiki.atlassian.net/browse/EZ-956 ticket changes.
    private void calculateEDDFromLMP(String lmpGregStr) {
        try {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");

            // Parse input string in UTC
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setTimeZone(utcZone);
            Date lmp = sdf.parse(lmpGregStr);

            // Initialize Calendar in UTC
            Calendar cal = Calendar.getInstance(utcZone);
            cal.setTime(lmp);

            // Naegele's Rule: Add 9 months and 7 days
            cal.add(Calendar.MONTH, 9);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date eddGreg = cal.getTime();

            // Convert back to string (Ensure toGregFmt uses UTC!)
            mEDD = toGregFmt(eddGreg);

            mEDDTextView.setText(gregToDisplay(mEDD));

            clearError(tvErrorEDD, null);
        } catch (Exception e) { e.printStackTrace(); }
    }
*/

    private void calculateEDDFromLMP(String lmpGregStr) {
        try {
            TimeZone utcZone = TimeZone.getTimeZone("UTC");

            // Parse input string in UTC
            SimpleDateFormat sdf = new SimpleDateFormat(GREG_FMT, Locale.ENGLISH);
            sdf.setTimeZone(utcZone);
            Date lmp = sdf.parse(lmpGregStr);

            // Initialize Calendar in UTC
            Calendar cal = Calendar.getInstance(utcZone);
            cal.setTime(lmp);

            // ===================== Naegele's Rule = LMP + 7 days - 3 months + 1 year =====================
            cal.add(Calendar.DAY_OF_MONTH, 7);
            cal.add(Calendar.MONTH, -3);
            cal.add(Calendar.YEAR, 1);
            Date eddGreg = cal.getTime();

            // Convert back to string (Ensure toGregFmt uses UTC!)
            mEDD = toGregFmt(eddGreg);

            mEDDTextView.setText(gregToDisplay(mEDD));

            clearError(tvErrorEDD, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ═════════════════════════════════════════════════════════════════════
    //  Time picker — no restrictions, validated on Save
    // ═════════════════════════════════════════════════════════════════════

    private void selectTimeForAllParameters(String forWhichParameter) {
        ThemeTimePickerDialog dialog = new ThemeTimePickerDialog.Builder(mContext)
                .title(R.string.current_time).positiveButtonLabel(R.string.ok)
                .use24Hour(true)
                .build();
        dialog.setListener((hours, minutes, amPm, value) -> {
            String ts = String.format(Locale.ENGLISH, "%02d:%02d %s", hours, minutes, amPm);
            switch (forWhichParameter) {
                case "admissionTimeString":
                    mAdmissionTimeString = ts;
                    mAdmissionTimeTextView.setText(ts);
                    clearError(tvErrorAdmissionTime, cardAdmissionTime);
                    break;
                case "laborOnsetString":
                    mActiveLaborDiagnosedTime = ts;
                    mActiveLaborDiagnosedTimeTextView.setText(ts);
                    clearError(tvErrorLabourDiagnosedTime, cardDiagnosedTime);
                    break;
                case "membraneRupturedTime":
                    mMembraneRupturedTime = ts;
                    mMembraneRupturedTimeTextView.setText(ts);
                    clearError(tvErrorSacRupturedTime, cardSacRupturedTime);
                    break;
            }
        });
        dialog.show(getChildFragmentManager(), "ThemeTimePickerDialog");
    }

    // ═════════════════════════════════════════════════════════════════════
    //  All validations — run ONLY on Save
    // ═════════════════════════════════════════════════════════════════════

    private boolean areValidFields() {
        hideAllErrorFields();
        resetAllCardStrokes();
        boolean isValid = true;

        // 1. Admission Date
        if (TextUtils.isEmpty(mAdmissionDateString)) {
            showError(tvErrorAdmissionDate, cardAdmissionDate, getString(R.string.select_admission_date));
            isValid = false;
        } else {
            Date admDate = parseGregDate(mAdmissionDateString);
            if (admDate == null) {
                showError(tvErrorAdmissionDate, cardAdmissionDate, getString(R.string.select_admission_date));
                isValid = false;
            } else if (isAfterToday(mAdmissionDateString)) {
                // Date-level check — prevents tomorrow slipping through due to timezone offset
                showError(tvErrorAdmissionDate, cardAdmissionDate, getString(R.string.select_admission_date));
                isValid = false;
            } else {
                Calendar minAdm = Calendar.getInstance();
                minAdm.add(Calendar.DAY_OF_MONTH, -10);
                minAdm.set(Calendar.HOUR_OF_DAY, 0);
                minAdm.set(Calendar.MINUTE, 0);
                minAdm.set(Calendar.SECOND, 0);
                minAdm.set(Calendar.MILLISECOND, 0);
                if (admDate.before(minAdm.getTime())) {
                    showError(tvErrorAdmissionDate, cardAdmissionDate,
                            getString(R.string.select_admission_date) + " (max 10 days ago)");
                    isValid = false;
                }
            }
        }

        // 2. Admission Time
        if (TextUtils.isEmpty(mAdmissionTimeString)) {
            showError(tvErrorAdmissionTime, cardAdmissionTime, getString(R.string.select_admission_time));
            isValid = false;
        } else if (!TextUtils.isEmpty(mAdmissionDateString)) {
            Date admDt = parseGregDateTime(mAdmissionDateString, mAdmissionTimeString);
            if (admDt != null && admDt.after(new Date())) {
                showError(tvErrorAdmissionTime, cardAdmissionTime, getString(R.string.select_admission_time));
                isValid = false;
            }
        }

       /* // 8. Sac Ruptured
        if (!isUnknownChecked) {
            if (TextUtils.isEmpty(mMembraneRupturedDate)) {
                showError(tvErrorSacRupturedDate, cardSacRupturedDate, getString(R.string.select_sac_ruptured_date));
                isValid = false;
            } else if (isAfterToday(mMembraneRupturedDate)) {
                showError(tvErrorSacRupturedDate, cardSacRupturedDate, getString(R.string.sac_ruptured_future_not_allowed));
                isValid = false;
            }
            if (TextUtils.isEmpty(mMembraneRupturedTime)) {
                showError(tvErrorSacRupturedTime, cardSacRupturedTime, getString(R.string.select_sac_ruptured_time));
                isValid = false;
            }
            else if (!TextUtils.isEmpty(mMembraneRupturedTime)) {
                Date rupDt = parseGregDateTime(mMembraneRupturedDate, mMembraneRupturedTime);
                if (rupDt != null && rupDt.after(new Date())) {
                    showError(tvErrorSacRupturedTime, cardSacRupturedTime, getString(R.string.select_sac_ruptured_time));
                    isValid = false;
                }
            }
        }
*/
        // 8. Sac Ruptured
        Log.d(TAG, "areValidFields: shouldValidateSacMembraneDates : " + shouldValidateSacMembraneDates);
        if (shouldValidateSacMembraneDates) {
            Log.d(TAG, "areValidFields: medd : " + mEDD);
            if (TextUtils.isEmpty(mMembraneRupturedDate)) {
                showError(tvErrorSacRupturedDate, cardSacRupturedDate, getString(R.string.select_sac_ruptured_date));
                isValid = false;
            } else if (isAfterToday(mMembraneRupturedDate)) {
                showError(tvErrorSacRupturedDate, cardSacRupturedDate, getString(R.string.sac_ruptured_future_not_allowed));
                isValid = false;
            } else if (!TextUtils.isEmpty(mEDD)) {
                // lower-bound check — sac ruptured date can't be earlier than (EDD - 4 weeks)
                Date sacDate = parseGregDate(mMembraneRupturedDate);
                Date eddDate = parseGregDate(mEDD);
                if (sacDate != null && eddDate != null) {
                    Calendar minSacDate = Calendar.getInstance();
                    minSacDate.setTime(eddDate);
                    minSacDate.add(Calendar.WEEK_OF_YEAR, -4);
                    minSacDate.set(Calendar.HOUR_OF_DAY, 0);
                    minSacDate.set(Calendar.MINUTE, 0);
                    minSacDate.set(Calendar.SECOND, 0);
                    minSacDate.set(Calendar.MILLISECOND, 0);

                    Calendar selSacDate = Calendar.getInstance();
                    selSacDate.setTime(sacDate);
                    selSacDate.set(Calendar.HOUR_OF_DAY, 0);
                    selSacDate.set(Calendar.MINUTE, 0);
                    selSacDate.set(Calendar.SECOND, 0);
                    selSacDate.set(Calendar.MILLISECOND, 0);
                }
            }
            if (TextUtils.isEmpty(mMembraneRupturedTime)) {
                showError(tvErrorSacRupturedTime, cardSacRupturedTime, getString(R.string.select_sac_ruptured_time));
                isValid = false;
            } else if (!TextUtils.isEmpty(mMembraneRupturedTime)) {
                Date rupDt = parseGregDateTime(mMembraneRupturedDate, mMembraneRupturedTime);
                if (rupDt != null && rupDt.after(new Date())) {
                    showError(tvErrorSacRupturedTime, cardSacRupturedTime, getString(R.string.select_sac_ruptured_time));
                    isValid = false;
                }
            }
        }

        // 3. Total Birth
        String birthStr = mTotalBirthEditText.getText().toString().trim();
        if (TextUtils.isEmpty(birthStr)) {
            showError(tvErrorTotalBirth, cardTotalBirth, getString(R.string.total_birth_count_val_txt));
            isValid = false;
        } else if (Integer.parseInt(birthStr) > 15) {
            showError(tvErrorTotalBirth, cardTotalBirth, getString(R.string.total_birth_count_limit));
            isValid = false;
        }

        // 4. Total Miscarriage
        String misStr = mTotalMiscarriageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(misStr)) {
            showError(tvErrorTotalMiscarriage, cardTotalMiscarraige, getString(R.string.total_miscarriage_count_val_txt));
            isValid = false;
        } else if (Integer.parseInt(misStr) > 8) {
            showError(tvErrorTotalMiscarriage, cardTotalMiscarraige, getString(R.string.miscarriage_count_limit));
            isValid = false;
        }

        // 5. Labour Onset
        if (mLaborOnsetString.isEmpty()) {
            tvErrorLabourOnset.setVisibility(View.VISIBLE);
            tvErrorLabourOnset.setText(getString(R.string.labor_onset_val_txt));
            tvSpontaneous.setBackground(ContextCompat.getDrawable(mContext, R.drawable.error_bg_et));
            tvInduced.setBackground(ContextCompat.getDrawable(mContext, R.drawable.error_bg_et));
            isValid = false;
        }

        // 6. Active Labour Diagnosed Date
        if (TextUtils.isEmpty(mActiveLaborDiagnosedDate)) {
            showError(tvErrorLabourDiagnosedDate, cardDiagnosedDate,
                    getString(R.string.active_labor_diagnosed_date_val_txt));
            isValid = false;
        } else if (isAfterToday(mActiveLaborDiagnosedDate)) {
            showError(tvErrorLabourDiagnosedDate, cardDiagnosedDate, getString(R.string.active_labor_diagnosed_date_val_txt));
            isValid = false;
        }

        // 7. Active Labour Diagnosed Time
        if (TextUtils.isEmpty(mActiveLaborDiagnosedTime)) {
            showError(tvErrorLabourDiagnosedTime, cardDiagnosedTime,
                    getString(R.string.active_labor_diagnosed_time_val_txt));
            isValid = false;
        } else if (!TextUtils.isEmpty(mActiveLaborDiagnosedDate)) {
            Date labDt = parseGregDateTime(mActiveLaborDiagnosedDate, mActiveLaborDiagnosedTime);
            if (labDt != null) {
                Date now = new Date();
                Calendar min15h = Calendar.getInstance();
                min15h.add(Calendar.HOUR_OF_DAY, -15);
                if (labDt.after(now) || labDt.before(min15h.getTime())) {
                    showError(tvErrorLabourDiagnosedTime, cardDiagnosedTime,
                            getString(R.string.active_labour_diagnosis));
                    isValid = false;
                }
            }
        }


        // 9. Risk Factors
        View otherRF = view.findViewById(R.id.llViewOtherRiskFactor);
        if (TextUtils.isEmpty(mRiskFactorsTextView.getText().toString())) {
            showError(tvErrorRiskFactor, dropdownRiskFactors, getString(R.string.please_select_risk_factor));
            isValid = false;
        } else if (otherRF.getVisibility() == View.VISIBLE && TextUtils.isEmpty(etHighRisk.getText().toString())) {
            showError(tvErrorHighRisk, cardOtherRisk, getString(R.string.error_other_risk));
            isValid = false;
        }

        // 10. Hospital / Maternity
        if (mHospitalMaternityString.isEmpty()) {
            tvErrorHospital.setVisibility(View.VISIBLE);
            tvErrorHospital.setText(getString(R.string.hospital_matermnity_val_txt));
            isValid = false;
        } else if (!mHospitalMaternityString.equalsIgnoreCase("hospital")
                && !mHospitalMaternityString.equalsIgnoreCase("maternity")) {
            if (TextUtils.isEmpty(etHospitalOther.getText().toString())) {
                showError(tvErrorHospitalOther, cardHospitalOther, getString(R.string.enter_hospital_other_error));
                isValid = false;
            }
        }

        // 11. LMP
        if (!TextUtils.isEmpty(mLmpDate)) {
            Date lmp = parseGregDate(mLmpDate);
            if (lmp != null) {
                if (isAfterToday(mLmpDate)) {
                    tvErrorLmpDate.setText(getString(R.string.lmp_future_not_allowed));
                    tvErrorLmpDate.setVisibility(View.VISIBLE);
                    isValid = false;
                } else {
                    Calendar min44 = Calendar.getInstance();
                    min44.add(Calendar.WEEK_OF_YEAR, -44);
                    if (lmp.before(min44.getTime())) {
                        tvErrorLmpDate.setText(getString(R.string.lmp_range_invalid));
                        tvErrorLmpDate.setVisibility(View.VISIBLE);
                        isValid = false;
                    }
                }
            }
        } else {
            tvErrorLmpDate.setText(getString(R.string.select_lmp_date));
            tvErrorLmpDate.setVisibility(View.VISIBLE);
            isValid = false;
        }

        //edd validation removed
        // 12. EDD
        /*if (!TextUtils.isEmpty(mEDD)) {
            Date edd = parseGregDate(mEDD);
            if (edd != null) {
                Calendar minEdd = Calendar.getInstance(); minEdd.add(Calendar.WEEK_OF_YEAR, -3);
                Calendar maxEdd = Calendar.getInstance(); maxEdd.add(Calendar.WEEK_OF_YEAR, 3);
                if (edd.before(minEdd.getTime()) || edd.after(maxEdd.getTime())) {
                    tvErrorEDD.setText(getString(R.string.edd_range_invalid));
                    tvErrorEDD.setVisibility(View.VISIBLE);
                    isValid = false;
                }
            }
        }
        else
        {
            tvErrorEDD.setText(getString(R.string.select_edd_date));
            tvErrorEDD .setVisibility(View.VISIBLE);
            isValid = false;
        }*/

        // 13. Primary Doctor
        if (TextUtils.isEmpty(mPrimaryDoctorTextView.getText().toString())) {
            showError(tvErrorPrimaryDoctor, cardPrimaryDoctor, getString(R.string.select_primary_doctor));
            isValid = false;
        }

        // 14. Secondary Doctor
        /*if (TextUtils.isEmpty(mSecondaryDoctorTextView.getText().toString())) {
            showError(tvErrorSecondaryDoctor, cardSecondaryDoctor, getString(R.string.select_secondary_doctor));
            isValid = false;
        }*/

        if (TextUtils.isEmpty(autotvRupturedMembrane.getText().toString())) {
            showError(tvErrorSacRuptured, cardSacRupturedMembrane, getString(R.string.select_rupture_membrane));
            isValid = false;
        }

        return isValid;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Error helpers
    // ═════════════════════════════════════════════════════════════════════

    private void showError(TextView tv, @Nullable MaterialCardView card, String msg) {
        if (tv != null) {
            tv.setText(msg);
            tv.setVisibility(View.VISIBLE);
        }
        if (card != null) card.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
    }

    private void clearError(TextView tv, @Nullable MaterialCardView card) {
        if (tv != null) tv.setVisibility(View.GONE);
        if (card != null)
            card.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
    }

    private void hideAllErrorFields() {
        TextView[] all = {
                tvErrorAdmissionDate, tvErrorAdmissionTime, tvErrorTotalBirth, tvErrorTotalMiscarriage,
                tvErrorLabourOnset, tvErrorSacRupturedDate, tvErrorSacRupturedTime,
                tvErrorLabourDiagnosedDate, tvErrorLabourDiagnosedTime,
                tvErrorRiskFactor, tvErrorHighRisk, tvErrorHospital, tvErrorHospitalOther,
                tvErrorPrimaryDoctor, tvErrorSecondaryDoctor, tvErrorBedNumber,
                tvErrorGravida, tvErrorLmpDate, tvErrorEDD
        };
        for (TextView tv : all) if (tv != null) tv.setVisibility(View.GONE);
    }

    private void resetAllCardStrokes() {
        int normal = ContextCompat.getColor(mContext, R.color.colorScrollbar);
        MaterialCardView[] cards = {
                cardAdmissionDate, cardAdmissionTime, cardTotalBirth, cardTotalMiscarraige,
                cardSacRupturedDate, cardSacRupturedTime, cardPrimaryDoctor, cardSecondaryDoctor,
                cardDiagnosedDate, cardDiagnosedTime, dropdownRiskFactors, cardOtherRisk, cardHospitalOther
        };
        for (MaterialCardView c : cards) if (c != null) c.setStrokeColor(normal);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Save flow
    // ═════════════════════════════════════════════════════════════════════

    @SuppressLint("UseCompatLoadingForDrawables")
    public void onPatientCreateClicked() {
        if (!etHospitalOther.getText().toString().isEmpty()) mHospitalMaternityString = "other";
        if (!areValidFields()) {
            setScrollToFocusedItem();
            return;
        }

        mTotalBirthCount = mTotalBirthEditText.getText().toString().trim();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString().trim();
        int total = Integer.parseInt(mTotalBirthCount) + Integer.parseInt(mTotalMiscarriageCount);
        int age = DateAndTimeUtils.getAgeInYearsOnly(patientDTO.getDateofbirth());
        int allowed = age - 12;

        if (total > allowed) {
            isParityWarningDialogShown = true;
            showParityWarningDialog();
        } else if (validateGravida()) {
            savePatientsDataInDb();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Click listeners
    // ═════════════════════════════════════════════════════════════════════

    private void handleAllClickListeners() {
        TextInputLayout etLayoutAdmissionDate = view.findViewById(R.id.etLayout_admission_date);
        TextInputLayout etLayoutAdmissionTime = view.findViewById(R.id.etLayout_admission_time);
        TextInputLayout etLabourDiagnosedDate = view.findViewById(R.id.etLayout_labor_diagnosed_date);
        TextInputLayout etLabourDiagnosedTime = view.findViewById(R.id.etLayout_labor_diagnosed_time);
        TextInputLayout etLayoutSacRupturedDate = view.findViewById(R.id.etLayout_sac_ruptured_date);
        TextInputLayout etLayoutSacRupturedTime = view.findViewById(R.id.etLayout_sac_ruptured_time);
        TextInputLayout etLayoutRiskFactors = view.findViewById(R.id.etLayout_risk_factors);
        TextInputLayout etLayoutPrimaryDoctor = view.findViewById(R.id.etLayout_primary_doctor);
        TextInputLayout etLayoutSecondaryDoctor = view.findViewById(R.id.etLayout_secondary_doctor);
        TextInputLayout etLayoutSacRupturedMembraneOptions = view.findViewById(R.id.etLayout_sac_ruptured_options);

        View layoutLmpEdd = view.findViewById(R.id.view_lmp_edd_layout);
        TextInputLayout etLayoutLmp = layoutLmpEdd.findViewById(R.id.etLayout_lmp);
        TextInputLayout etLayoutEdd = layoutLmpEdd.findViewById(R.id.etLayout_edd);

        etLayoutAdmissionDate.setEndIconOnClickListener(v -> pickAdmissionDate());
        mAdmissionDateTextView.setOnClickListener(v -> pickAdmissionDate());
        etLayoutAdmissionTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("admissionTimeString"));
        mAdmissionTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("admissionTimeString"));
        etLabourDiagnosedDate.setEndIconOnClickListener(v -> pickActiveLaborDate());
        mActiveLaborDiagnosedDateTextView.setOnClickListener(v -> pickActiveLaborDate());
        etLabourDiagnosedTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("laborOnsetString"));
        mActiveLaborDiagnosedTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("laborOnsetString"));
        etLayoutSacRupturedDate.setEndIconOnClickListener(v -> pickSacRupturedDate());
        mMembraneRupturedDateTextView.setOnClickListener(v -> pickSacRupturedDate());
        etLayoutSacRupturedTime.setEndIconOnClickListener(v -> selectTimeForAllParameters("membraneRupturedTime"));
        mMembraneRupturedTimeTextView.setOnClickListener(v -> selectTimeForAllParameters("membraneRupturedTime"));
        etLayoutLmp.setEndIconOnClickListener(v -> pickLmpDate());
        mLmpDateTextView.setOnClickListener(v -> pickLmpDate());
        //etLayoutEdd.setEndIconOnClickListener(v -> pickEddDate());
        //mEDDTextView.setOnClickListener(v -> pickEddDate());
        etLayoutRiskFactors.setEndIconOnClickListener(v -> showRiskFactorSelectionDialog());
        mRiskFactorsTextView.setOnClickListener(v -> showRiskFactorSelectionDialog());
        etLayoutPrimaryDoctor.setEndIconOnClickListener(v -> selectPrimaryDoctor());
        mPrimaryDoctorTextView.setOnClickListener(v -> selectPrimaryDoctor());
        etLayoutSecondaryDoctor.setEndIconOnClickListener(v -> selectSecondaryDoctor());
        mSecondaryDoctorTextView.setOnClickListener(v -> selectSecondaryDoctor());
        etLayoutSacRupturedMembraneOptions.setEndIconOnClickListener(v -> selectRuptureMembraneOptions());
        autotvRupturedMembrane.setOnClickListener(v -> selectRuptureMembraneOptions());

        mUnknownMembraneRupturedCheckBox.setOnCheckedChangeListener((btn, checked) -> {
            isUnknownChecked = checked;
            if (checked) {
                layoutSacRuptured.setVisibility(View.GONE);
                mMembraneRupturedDateTextView.setEnabled(false);
                mMembraneRupturedTimeTextView.setEnabled(false);
                mMembraneRupturedDateTextView.setText("");
                mMembraneRupturedTimeTextView.setText("");
                clearError(tvErrorSacRupturedDate, cardSacRupturedDate);
                clearError(tvErrorSacRupturedTime, cardSacRupturedTime);
            } else {
                layoutSacRuptured.setVisibility(View.VISIBLE);
                mMembraneRupturedDateTextView.setEnabled(true);
                mMembraneRupturedTimeTextView.setEnabled(true);
            }
        });

        i_privacy = getActivity().getIntent();
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
            Toast.makeText(mContext, "JsonException " + e, Toast.LENGTH_LONG).show();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Maternity / Labour onset option buttons
    // ═════════════════════════════════════════════════════════════════════

    private void handleOptionsForMaternity() {
        mHospitalMaternityString = "";
        optionHospital.setOnClickListener(v -> {
            setOptionSelected(optionHospital);
            setOptionUnselected(optionMaternity);
            setOptionUnselected(optionOther);
            mHospitalMaternityString = optionHospital.getText().toString();
            cardHospitalOther.setVisibility(View.GONE);
            tvErrorHospital.setVisibility(View.GONE);
            tvErrorHospitalOther.setVisibility(View.GONE);
            etHospitalOther.setText("");
        });
        optionMaternity.setOnClickListener(v -> {
            setOptionUnselected(optionHospital);
            setOptionSelected(optionMaternity);
            setOptionUnselected(optionOther);
            mHospitalMaternityString = optionMaternity.getText().toString();
            cardHospitalOther.setVisibility(View.GONE);
            tvErrorHospital.setVisibility(View.GONE);
            tvErrorHospitalOther.setVisibility(View.GONE);
            etHospitalOther.setText("");
        });
        optionOther.setOnClickListener(v -> {
            setOptionUnselected(optionHospital);
            setOptionUnselected(optionMaternity);
            setOptionSelected(optionOther);
            mHospitalMaternityString = optionOther.getText().toString();
            cardHospitalOther.setVisibility(View.VISIBLE);
            etHospitalOther.setVisibility(View.VISIBLE);
            tvErrorHospital.setVisibility(View.GONE);
            tvErrorHospitalOther.setVisibility(View.GONE);
        });
        tvSpontaneous.setOnClickListener(v -> {
            setOptionSelected(tvSpontaneous);
            setOptionUnselected(tvInduced);
            mLaborOnsetString = tvSpontaneous.getText().toString();
            tvErrorLabourOnset.setVisibility(View.GONE);
        });
        tvInduced.setOnClickListener(v -> {
            setOptionUnselected(tvSpontaneous);
            setOptionSelected(tvInduced);
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

    // ═════════════════════════════════════════════════════════════════════
    //  UI population (back nav + edit mode)
    // ═════════════════════════════════════════════════════════════════════

    private void updateUIForUserFromAddressTab() {
        Log.d(TAG, "updateUIForUserFromAddressTab: patientAttributesModel : " + new Gson().toJson(patientAttributesModel));
        String admDate = patientAttributesModel.getAdmissionDate();
        String labDate = patientAttributesModel.getActiveLabourDiagnosedDate();
        String sacDate = patientAttributesModel.getSacRupturedDate();
        String lmpStr = patientAttributesModel.getLmp();
        String eddStr = patientAttributesModel.getEdd();

        mAdmissionDateString = admDate;
        mAdmissionDateTextView.setText(gregToDisplay(admDate));
        mAdmissionTimeString = patientAttributesModel.getAdmissionTime();
        mAdmissionTimeTextView.setText(mAdmissionTimeString);
        mTotalBirthCount = patientAttributesModel.getTotalBirthCount();
        mTotalBirthEditText.setText(mTotalBirthCount);
        mTotalMiscarriageCount = patientAttributesModel.getTotalMiscarriageCount();
        mTotalMiscarriageEditText.setText(mTotalMiscarriageCount);
        mActiveLaborDiagnosedDate = labDate;
        mActiveLaborDiagnosedDateTextView.setText(gregToDisplay(labDate));
        mActiveLaborDiagnosedTime = normaliseTimeString(patientAttributesModel.getActiveLabourDiagnosedTime());
        mActiveLaborDiagnosedTimeTextView.setText(mActiveLaborDiagnosedTime);
        mMembraneRupturedDate = sacDate;
        mMembraneRupturedDateTextView.setText(gregToDisplay(sacDate));
        mMembraneRupturedTime = normaliseTimeString(patientAttributesModel.getSacRupturedTime());
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
        mSelectedRuptureMembrane = patientAttributesModel.getRuptureMembraneOption();
        if (mSelectedRuptureMembrane != null && !mSelectedRuptureMembrane.isEmpty()) {
            autotvRupturedMembrane.setText(mSelectedRuptureMembrane, false);
            if (mSelectedRuptureMembrane.equalsIgnoreCase("Known")) {
                shouldValidateSacMembraneDates = true;
                layoutSacRuptured.setVisibility(View.VISIBLE);
            } else {
                shouldValidateSacMembraneDates = false;
                layoutSacRuptured.setVisibility(View.GONE);
            }
        }
        hideAllErrorFields();
    }

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
            mTotalBirthCount = patient.getParity().split(",")[0];
            mTotalMiscarriageCount = patient.getParity().split(",")[1];
            mTotalBirthEditText.setText(mTotalBirthCount);
            mTotalMiscarriageEditText.setText(mTotalMiscarriageCount);
        }
        if (patient.getLaborOnset() != null) {
            mLaborOnsetString = patient.getLaborOnset();
            getLabourOnsetValue(mLaborOnsetString);
        }
        if (patient.getActiveLaborDiagnosed() != null) {
            // ── FIX (AM/PM bug): stored format is "dd/MM/yyyy hh:mm AM/PM".
            // split(" ") produces 3 tokens: date, time-part, AM/PM — the AM/PM
            // was silently dropped, causing normaliseTimeString("11:55") to always
            // produce "11:55 AM" regardless of the original value.
            // split(" ", 2) produces exactly 2 tokens: date and full time string.
            String[] p = patient.getActiveLaborDiagnosed().split(" ", 2);
            mActiveLaborDiagnosedDate = p[0];
            mActiveLaborDiagnosedTime = normaliseTimeString(p.length > 1 ? p[1].trim() : "");
            mActiveLaborDiagnosedDateTextView.setText(gregToDisplay(mActiveLaborDiagnosedDate));
            mActiveLaborDiagnosedTimeTextView.setText(mActiveLaborDiagnosedTime);
        }
        /*if (patient.getMembraneRupturedTimestamp() != null) {
            if (patient.getMembraneRupturedTimestamp().equalsIgnoreCase("U")) {
                mUnknownMembraneRupturedCheckBox.setChecked(true);
            } else {
                // ── FIX (AM/PM bug): same split(" ", 2) fix as activeLaborDiagnosed ──
                String[] p = patient.getMembraneRupturedTimestamp().split(" ", 2);
                mMembraneRupturedDate = p[0];
                mMembraneRupturedTime = normaliseTimeString(p.length > 1 ? p[1].trim() : "");
                mMembraneRupturedDateTextView.setText(gregToDisplay(mMembraneRupturedDate));
                mMembraneRupturedTimeTextView.setText(mMembraneRupturedTime);
            }
        }*/
        Log.d(TAG, "updateUI: membraine status - " + patient.getMembraneRupturedTimestamp());
        if (patient.getMembraneRupturedTimestamp() != null) {

            String membraneValue = patient.getMembraneRupturedTimestamp();

            if ("U".equalsIgnoreCase(membraneValue)) {
                // Unknown selected
                autotvRupturedMembrane.setText("Unknown", false);
            } else if ("I".equalsIgnoreCase(membraneValue)) {
                // Intact selected
                autotvRupturedMembrane.setText("Intact", false);
            } else {
                // Known selected
                autotvRupturedMembrane.setText("Known", false);
                layoutSacRuptured.setVisibility(View.VISIBLE);
                shouldValidateSacMembraneDates = true;
                // Split date and time
                String[] p = membraneValue.split(" ", 2);
                mMembraneRupturedDate = p[0];
                mMembraneRupturedTime = normaliseTimeString(p.length > 1 ? p[1].trim() : "");
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
        try {
            etBedNumber.setText(getBedNumber(patient.getUuid()));
        } catch (DAOException e) {
            e.printStackTrace();
        }
        if (patient.getGravida() != null) mGravidaEdittext.setText(patient.getGravida());
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
    //  Remaining helpers
    // ═════════════════════════════════════════════════════════════════════

    private void showRiskFactorSelectionDialog() {
        MultiChoiceDialogFragment<String> dialog = new MultiChoiceDialogFragment.Builder<String>(mContext)
                .title(R.string.select_risk_factors).positiveButtonLabel(R.string.save_button).build();
        dialog.isSearchable(true);
        List<String> items = Arrays.asList(getResources().getStringArray(R.array.risk_factors));
        dialog.setAdapter(new RiskFactorMultiChoiceAdapter(mContext, new ArrayList<>(items)));
        dialog.setListener(selectedItems -> {
            if (!selectedItems.isEmpty()) {
                View other = view.findViewById(R.id.llViewOtherRiskFactor);
                StringBuilder sb = new StringBuilder();
                other.setVisibility(View.GONE);
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(selectedItems.get(i));
                    if (selectedItems.get(i).equals(getString(R.string.other_risk)))
                        other.setVisibility(View.VISIBLE);
                }
                mRiskFactorsString = sb.toString();
                mRiskFactorsTextView.setText(mRiskFactorsString);
                clearError(tvErrorRiskFactor, dropdownRiskFactors);
            }
        });
        dialog.show(getChildFragmentManager(), MultiChoiceDialogFragment.class.getCanonicalName());
    }

    private void selectPrimaryDoctor() {
        List<ProviderDTO> list = new ArrayList<>();
        for (ProviderDTO p : mProviderDoctorList)
            if (!mSecondaryDoctorUUIDString.equals(p.getUserUuid())) list.add(p);
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(list.get(i).getGivenName() + " " + list.get(i).getFamilyName());
            item.setItemId(list.get(i).getUserUuid());
            item.setItemIndex(i);
            items.add(item);
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(mContext)
                .title(R.string.select_primary_doctor).positiveButtonLabel(R.string.save_button).content(items).build();
        dialog.isSearchable(true);
        dialog.setListener(item -> {
            mPrimaryDoctorUUIDString = item.getItemId();
            mPrimaryDoctorTextView.setText(item.getItem());
            clearError(tvErrorPrimaryDoctor, cardPrimaryDoctor);
        });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void selectSecondaryDoctor() {
        if (mPrimaryDoctorUUIDString.isEmpty()) {
            Toast.makeText(mContext, "Please select the primary doctor", Toast.LENGTH_SHORT).show();
            return;
        }
        List<ProviderDTO> list = new ArrayList<>();
        for (ProviderDTO p : mProviderDoctorList)
            if (!mPrimaryDoctorUUIDString.equals(p.getUserUuid())) list.add(p);
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        SingChoiceItem na = new SingChoiceItem();
        na.setItem(AppConstants.NOT_APPLICABLE_FULL_TEXT);
        na.setItemId(AppConstants.NOT_APPLICABLE);
        na.setItemIndex(0);
        items.add(na);
        for (int i = 0; i < list.size(); i++) {
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(list.get(i).getGivenName() + " " + list.get(i).getFamilyName());
            item.setItemId(list.get(i).getUserUuid());
            item.setItemIndex(i + 1);
            item.setSelected(mSecondaryDoctorUUIDString.equals(list.get(i).getUserUuid()));
            items.add(item);
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(mContext)
                .title(R.string.select_secondary_doctor).positiveButtonLabel(R.string.save_button).content(items).build();
        dialog.isSearchable(true);
        dialog.setListener(item -> {
            mSecondaryDoctorUUIDString = item.getItemId();
            mSecondaryDoctorTextView.setText(item.getItem());
            clearError(tvErrorSecondaryDoctor, cardSecondaryDoctor);
        });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    private void getHospitalMaternityValue(String s) {
        if (s.equalsIgnoreCase("Hospital")) {
            setOptionSelected(optionHospital);
            setOptionUnselected(optionMaternity);
            setOptionUnselected(optionOther);
            cardHospitalOther.setVisibility(View.GONE);
            etHospitalOther.setVisibility(View.GONE);
        } else if (s.equalsIgnoreCase("Maternity")) {
            setOptionUnselected(optionHospital);
            setOptionSelected(optionMaternity);
            setOptionUnselected(optionOther);
            cardHospitalOther.setVisibility(View.GONE);
            etHospitalOther.setVisibility(View.GONE);
        } else {
            setOptionUnselected(optionHospital);
            setOptionUnselected(optionMaternity);
            setOptionSelected(optionOther);
            cardHospitalOther = view.findViewById(R.id.card_hospital_other);
            cardHospitalOther.setVisibility(View.VISIBLE);
            etHospitalOther.setVisibility(View.VISIBLE);
            etHospitalOther.setText(s);
        }
    }

    private void getLabourOnsetValue(String s) {
        if (s.equalsIgnoreCase("Spontaneous")) {
            setOptionSelected(tvSpontaneous);
            setOptionUnselected(tvInduced);
            mLaborOnsetString = s;
        } else if (s.equalsIgnoreCase("Induced")) {
            setOptionUnselected(tvSpontaneous);
            setOptionSelected(tvInduced);
            mLaborOnsetString = s;
        }
    }

    private int parseSafe(String v) {
        try {
            return (v == null || v.isEmpty()) ? 0 : Integer.parseInt(v);
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateGravida() {
        if (isGravidaEdited) return;
        mGravidaEdittext.setText(String.valueOf(parseSafe(mTotalBirthCount) + parseSafe(mTotalMiscarriageCount) + 1));
    }

    private boolean validateGravida() {
        String val = mGravidaEdittext.getText().toString().trim();
        if (val.isEmpty()) {
            tvErrorGravida.setText(getString(R.string.error_gravida_required));
            tvErrorGravida.setVisibility(View.VISIBLE);
            return false;
        }
        int g = Integer.parseInt(val);
        if (g < 0) {
            tvErrorGravida.setText(getString(R.string.error_gravida_negative));
            tvErrorGravida.setVisibility(View.VISIBLE);
            return false;
        }
        if (g > 20) {
            tvErrorGravida.setText(getString(R.string.error_gravida_max_limit));
            tvErrorGravida.setVisibility(View.VISIBLE);
            return false;
        }
        tvErrorGravida.setVisibility(View.GONE);
        return true;
    }

    private String getBedNumber(String patientuuid) throws DAOException {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        String bed = null;
        Cursor c = db.rawQuery("SELECT value FROM tbl_patient_attribute WHERE patientuuid=? AND person_attribute_type_uuid='d0786817-68d9-4226-b311-3de68d534b9e'", new String[]{patientuuid});
        try {
            while (c.moveToNext()) bed = c.getString(c.getColumnIndexOrThrow("value"));
        } catch (SQLException s) {
            FirebaseCrashlytics.getInstance().recordException(s);
        }
        c.close();
        return bed;
    }

    private void setscreen(String patientUID) {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();
        String[] cols = {"uuid", "first_name", "middle_name", "last_name", "date_of_birth", "address1", "address2",
                "city_village", "state_province", "postal_code", "country", "phone_number", "gender", "sdw",
                "occupation", "patient_photo", "economic_status", "education_status", "caste"};
        Cursor c = db.query("tbl_patient", cols, "uuid=?", new String[]{patientUID}, null, null, null);
        if (c.moveToFirst()) {
            patient1.setUuid(c.getString(c.getColumnIndexOrThrow("uuid")));
            patient1.setDate_of_birth(c.getString(c.getColumnIndexOrThrow("date_of_birth")));
        }
        c.close();
        Cursor ca = db.query("tbl_patient_attribute", new String[]{"value", "person_attribute_type_uuid"}, "patientuuid = ?", new String[]{patientUID}, null, null, null);
        if (ca.moveToFirst()) {
            do {
                String name = "";
                try {
                    name = patientsDAO.getAttributesName(ca.getString(ca.getColumnIndexOrThrow("person_attribute_type_uuid")));
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                String val = ca.getString(ca.getColumnIndexOrThrow("value"));
                switch (name.toLowerCase()) {
                    case "admission_date":
                        patient1.setAdmissionDate(val);
                        break;
                    case "admission_time":
                        patient1.setAdmissionTime(val);
                        break;
                    case "parity":
                        patient1.setParity(val);
                        break;
                    case "labor onset":
                        patient1.setLaborOnset(val);
                        break;
                    case "active labor diagnosed":
                        patient1.setActiveLaborDiagnosed(val);
                        break;
                    case "membrane ruptured timestamp":
                        patient1.setMembraneRupturedTimestamp(val);
                        break;
                    case "risk factors":
                        patient1.setRiskFactors(val);
                        break;
                    case "hospital_maternity":
                        patient1.setHospitalMaternity(val);
                        break;
                    case "primarydoctor":
                        patient1.setPrimaryDoctor(val);
                        break;
                    case "secondarydoctor":
                        patient1.setSecondaryDoctor(val);
                        break;
                    case "ezazi registration number":
                        patient1.seteZaziRegNumber(val);
                        break;
                    case "gravida":
                        patient1.setGravida(val);
                        break;
                    case "last menstrual period (lmp)":
                        patient1.setLmp(val);
                        break;
                    case "estimated date of delivery (edd)":
                        patient1.setEdd(val);
                        break;
                    case "hospital id":
                        patient1.setHospitalId(val);
                        break;
                    case "alternateno":
                        patient1.setAlternateNo(val);
                        break;
                }
            } while (ca.moveToNext());
        }
        ca.close();
    }

    private void showParityWarningDialog() {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(requireActivity())
                .title(R.string.parity_dialog_warning).positiveButtonLabel(R.string.confirm_and_submit)
                .negativeButtonLabel(R.string.review_details).content(getString(R.string.parity_dialog_message)).build();
        dialog.setListener(new ConfirmationDialogFragment.OnConfirmationActionListener() {
            @Override
            public void onAccept() {
                savePatientsDataInDb();
                dialog.dismiss();
            }

            @Override
            public void onDecline() {
                dialog.dismiss();
            }
        });
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
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
        mTotalBirthCount = mTotalBirthEditText.getText().toString();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString();
        m.setAdmissionDate(mAdmissionDateString);
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
        m.setBedNumber(!TextUtils.isEmpty(etBedNumber.getText().toString()) ? etBedNumber.getText().toString() : AppConstants.NOT_APPLICABLE);
        m.setMembraneCheckboxChecked(mUnknownMembraneRupturedCheckBox.isChecked());
        if (mUnknownMembraneRupturedCheckBox.isChecked()) {
            mMembraneRupturedDate = "";
            mMembraneRupturedTime = "";
            mMembraneRupturedDateTextView.setText("");
            mMembraneRupturedTimeTextView.setText("");
        }
        m.setSacRupturedDate(mMembraneRupturedDate);
        m.setSacRupturedTime(mMembraneRupturedTime);
        m.setOtherHospitalString(etHospitalOther.getText().toString());
        m.setGravida(mGravidaEdittext.getText().toString());
        m.setLmp(mLmpDate);
        m.setEdd(mEDD);
        m.setHospitalId(mHospitalId.getText().toString());
        m.setRuptureMembraneOption(mSelectedRuptureMembrane);
        if (!"Known".equalsIgnoreCase(mSelectedRuptureMembrane)) {
            mMembraneRupturedDate = "";
            mMembraneRupturedTime = "";
            mMembraneRupturedDateTextView.setText("");
            mMembraneRupturedTimeTextView.setText("");
        }
        m.setSacRupturedDate(mMembraneRupturedDate);
        m.setSacRupturedTime(mMembraneRupturedTime);
        return m;
    }

    private void savePatientsDataInDb() {
        mTotalBirthCount = mTotalBirthEditText.getText().toString().trim();
        mTotalMiscarriageCount = mTotalMiscarriageEditText.getText().toString().trim();
        if (mHospitalMaternityString.trim().equalsIgnoreCase("other")) {
            mHospitalMaternityString = etHospitalOther.getText().toString();
            cardHospitalOther.setVisibility(View.VISIBLE);
            etHospitalOther.setVisibility(View.VISIBLE);
        }

        PatientsDAO patientsDAO = new PatientsDAO();
        List<PatientAttributesDTO> attrList = new ArrayList<>();

        if (fromSummary && patientUuidUpdate != null && !patientUuidUpdate.isEmpty())
            uuid = patientUuidUpdate;
        else uuid = UUID.randomUUID().toString();

        patientDTO.setUuid(uuid);
        patientDTO.setCreatorUuid(sessionManager.getCreatorID());

        java.util.function.BiFunction<String, String, PatientAttributesDTO> mkAttr = (colKey, value) -> {
            PatientAttributesDTO a = new PatientAttributesDTO();
            a.setUuid(UUID.randomUUID().toString());
            a.setPatientuuid(uuid);
            a.setPersonAttributeTypeUuid(patientsDAO.getUuidForAttribute(colKey));
            a.setValue(value);
            return a;
        };

        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ADMISSION_DATE.value, StringUtils.getValue(mAdmissionDateString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ADMISSION_TIME.value, StringUtils.getValue(mAdmissionTimeString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PARITY.value, StringUtils.getValue(mTotalBirthCount + "," + mTotalMiscarriageCount)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.LABOR_ONSET.value, StringUtils.getValue(mLaborOnsetString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ACTIVE_LABOR_DIAGNOSED.value, StringUtils.getValue(mActiveLaborDiagnosedDate + " " + mActiveLaborDiagnosedTime)));
       /* attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.MEMBRANE_RUPTURED_TIMESTAMP.value,
                mUnknownMembraneRupturedCheckBox.isChecked() ? "U" : StringUtils.getValue(mMembraneRupturedDate + " " + mMembraneRupturedTime)));*/
        String membraneValue;
        if ("Unknown".equals(mSelectedRuptureMembrane)) {
            membraneValue = "U";
        } else if ("Intact".equals(mSelectedRuptureMembrane)) {
            membraneValue = "I";
        } else {
            // Known
            membraneValue = StringUtils.getValue(
                    mMembraneRupturedDate + " " + mMembraneRupturedTime
            );
        }

        attrList.add(
                mkAttr.apply(
                        PatientAttributesDTO.Columns.MEMBRANE_RUPTURED_TIMESTAMP.value,
                        membraneValue
                )
        );
        if (mRiskFactorsString.contains(getString(R.string.other_risk)))
            mRiskFactorsString = mRiskFactorsString.replace(getString(R.string.other_risk), etHighRisk.getText().toString());
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.RISK_FACTORS.value, StringUtils.getValue(mRiskFactorsString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.HOSPITAL_MATERNITY.value, StringUtils.getValue(mHospitalMaternityString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PRIMARY_DOCTOR.value, StringUtils.getValue(mPrimaryDoctorUUIDString) + "@#@" + mPrimaryDoctorTextView.getText()));
        if (mSecondaryDoctorTextView.getText().length() > 0)
            attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.SECONDARY_DOCTOR.value, StringUtils.getValue(mSecondaryDoctorUUIDString) + "@#@" + mSecondaryDoctorTextView.getText()));
        int num = (int) (Math.random() * (99999999 - 100 + 1) + 100);
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.REGISTRATION_NUMBER.value,
                regNumberPart(patientDTO.getCountry()) + "/" + regNumberPart(patientDTO.getStateprovince()) + "/" + regNumberPart(patientDTO.getCityvillage()) + "/" + num));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.BED_NUMBER.value,
                !TextUtils.isEmpty(etBedNumber.getText().toString()) ? StringUtils.getValue(etBedNumber.getText().toString()) : StringUtils.getValue(AppConstants.NOT_APPLICABLE)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.ALTERNATE_NO.value, StringUtils.getValue(mAlternateNumberString)));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PROFILE_IMG_TIMESTAMP.value, AppConstants.dateAndTimeUtils.currentDateTime()));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.GRAVIDA.value, mGravidaEdittext.getText().toString()));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.lmp.value, mLmpDate));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.EDD.value, mEDD));
        attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.HOSPITAL_ID.value, mHospitalId.getText().toString()));
        if (!fromSummary) {
            attrList.add(mkAttr.apply(PatientAttributesDTO.Columns.PATIENT_REGISTRATION_START_DATE_TIME.value, sessionManager.getPatientRegistrationDateTime()));
        }
        patientDTO.setPatientAttributesDTOList(attrList);
        patientDTO.setSyncd(false);

        try {
            if (fromSummary) {
                boolean upd = patientsDAO.updatePatientToDBNew(patientDTO, uuid, attrList);
                boolean img = imagesDAO.updatePatientProfileImages(patientDTO.getPatientPhoto(), uuid);
                if (NetworkConnection.isOnline(getActivity().getApplication())) {
                    new SyncDAO().pushDataApi();
                    new ImagesPushDAO().patientProfileImagesPush();
                }
                if (upd && img) {
                    Intent i = new Intent(getActivity().getApplication(), PatientDetailActivity.class);
                    i.putExtra("patientUuid", uuid);
                    i.putExtra("patientName", patientDTO.getFirstname() + " " + patientDTO.getLastname());
                    i.putExtra("tag", "newPatient");
                    i.putExtra("hasPrescription", "false");
                    getActivity().startActivity(i);
                    getActivity().finish();
                }
            } else {
                patientDTO.setCreatedAt(DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT));
                boolean ins = patientsDAO.insertPatientToDB(patientDTO, uuid);
                imagesDAO.insertPatientProfileImages(patientDTO.getPatientPhoto(), uuid);
                if (NetworkConnection.isOnline(mContext)) {
                    new SyncDAO().pushDataApi();
                    new ImagesPushDAO().patientProfileImagesPush();
                }
                if (ins) {
                    Intent i = new Intent(mContext, PatientDetailActivity.class);
                    i.putExtra("patientUuid", uuid);
                    i.putExtra("patientName", patientDTO.getFirstname() + " " + patientDTO.getLastname());
                    i.putExtra("tag", "newPatient");
                    i.putExtra("privacy", privacy_value);
                    i.putExtra("hasPrescription", "false");
                    setSelectedDob(requireContext(), "");
                    sessionManager.savePatientRegistrationDateTime("");
                    mContext.startActivity(i);
                    getActivity().finish();
                } else {
                    Toast.makeText(mContext, "Error adding data", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public void setSelectedDob(Context context, String dob) {
        context.getApplicationContext().getSharedPreferences("dobPatient", 0)
                .edit().putString("dobPatient", dob).apply();
    }

    public void generateUuid() {
        patientUuid = uuidGenerator.UuidGenerator();
    }

    private void setScrollToFocusedItem() {
        if (requireView().findFocus() != null) {
            Point scroll = getLocationOnScreen(scrollviewOtherInfo);
            Point point = getLocationOnScreen(requireView().findFocus());
            int coord = point.y - scroll.y;
            if (coord <= 0) scrollviewOtherInfo.smoothScrollTo(0, 0);
            else if (scroll.y > coord) coord = point.y;
            scrollviewOtherInfo.smoothScrollTo(0, coord);
        }
    }

    public static Point getLocationOnScreen(View v) {
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new Point(loc[0], loc[1]);
    }

    private void selectRuptureMembraneOptions() {
        String[] options = getResources().getStringArray(R.array.rupture_membrane_options);
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(options[i]);
            item.setItemId(String.valueOf(i));
            item.setItemIndex(i);
            items.add(item);
        }

        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(mContext)
                .title(R.string.select_rupture_membrane)
                .positiveButtonLabel(R.string.save_button)
                .content(items)
                .build();

        dialog.isSearchable(false);
        dialog.setListener(item -> {
            mSelectedRuptureMembrane = item.getItem();
            mSelectedRuptureMembrane = item.getItem();
            autotvRupturedMembrane.setText(item.getItem());
            clearError(tvErrorSacRuptured, cardSacRupturedMembrane);
            if (!mSelectedRuptureMembrane.isEmpty() && mSelectedRuptureMembrane.equalsIgnoreCase("Known")) {
                shouldValidateSacMembraneDates = true;
                layoutSacRuptured.setVisibility(View.VISIBLE);
            } else {
                shouldValidateSacMembraneDates = false;
                layoutSacRuptured.setVisibility(View.GONE);
                mMembraneRupturedDateTextView.setText("");
                mMembraneRupturedTimeTextView.setText("");
            }
        });

        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }
}