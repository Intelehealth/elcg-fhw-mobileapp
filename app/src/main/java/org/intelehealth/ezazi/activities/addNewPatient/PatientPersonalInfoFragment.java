package org.intelehealth.ezazi.activities.addNewPatient;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.activities.cameraActivity.CameraActivity;
import org.intelehealth.ezazi.activities.setupActivity.SetupActivity;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.app.IntelehealthApplication;
import org.intelehealth.ezazi.database.dao.ImagesDAO;
import org.intelehealth.ezazi.database.dao.PatientsDAO;
import org.intelehealth.ezazi.database.dao.ProviderDAO;
import org.intelehealth.ezazi.models.Patient;
import org.intelehealth.ezazi.models.dto.PatientAttributesModel;
import org.intelehealth.ezazi.models.dto.PatientDTO;
import org.intelehealth.ezazi.models.dto.ProviderDTO;
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment;
import org.intelehealth.ezazi.ui.validation.UpperCaseAlphabetsInputFilter;
import org.intelehealth.ezazi.utilities.DateAndTimeUtils;
import org.intelehealth.ezazi.utilities.FileUtils;
import org.intelehealth.ezazi.utilities.NepaliDateConverter;
import org.intelehealth.ezazi.utilities.SessionManager;
import org.intelehealth.ezazi.utilities.UuidGenerator;
import org.intelehealth.ezazi.utilities.exception.DAOException;
import org.intelehealth.klivekit.utils.DateTimeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

public class PatientPersonalInfoFragment extends Fragment {

    private static final String TAG = "PatientPersonalInfoFrag";

    private static final String[] BS_MONTH_NAMES = {
            "Baisakh", "Jestha", "Asar", "Shrawan",
            "Bhadra", "Ashwin", "Kartik", "Mangsir",
            "Poush", "Magh", "Falgun", "Chaitra"
    };

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isDobFromCalendar = false;
    private int selectedBsYear  = 0;
    private int selectedBsMonth = 0;
    private int selectedBsDay   = 0;
    private String dobToDb = "";

    // ── UI fields ─────────────────────────────────────────────────────────────
    View view;
    SessionManager sessionManager;
    Context mContext;
    private List<ProviderDTO> mProviderDoctorList = new ArrayList<>();
    TextInputEditText mFirstName, mMiddleName, mLastName, mDOB, mAge, mMobileNumber, mAlternateNumber;
    private boolean mIsEditMode = false;
    String patientID_edit;
    boolean fromSummary;
    Patient patient1 = new Patient();
    private String patientUuid = "";
    UuidGenerator uuidGenerator = new UuidGenerator();
    private int mAgeYears = 0;
    Calendar today = Calendar.getInstance();
    MaterialButton btnSaveUpdate;
    PatientDTO patientDTO = new PatientDTO();
    private String mCurrentPhotoPath;
    ImagesDAO imagesDAO = new ImagesDAO();
    Intent i_privacy;
    String privacy_value;
    private String mAlternateNumberString = "";
    PatientsDAO patientsDAO = new PatientsDAO();
    boolean fromSecondScreen = false;
    private PatientAddressInfoFragment fragment_secondScreen;
    boolean patient_detail = false;
    boolean editDetails = false;
    private static final int GROUP_PERMISSION_REQUEST = 1000;
    FloatingActionButton fab;
    ImageView ivProfilePhoto;
    TextInputLayout etLayoutDob, etLayoutAge;
    int MY_REQUEST_CODE = 5555;
    private TextView tvDobForDb, tvAgeDob;
    private MaterialCardView cardFirstName, cardLastName, cardDob, cardAge, cardMobileNumber, cardAlternateMobileNumber;
    private TextView tvErrorFirstName, tvErrorLastName, tvErrorDob, tvErrorAge, tvErrorMobileNo, tvErrAlternateMobileNo;
    private PatientAttributesModel patientAttributesModel;
    private NestedScrollView scrollviewPersonalInfo;

    public static PatientPersonalInfoFragment getInstance() {
        return new PatientPersonalInfoFragment();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        updateLocale();
        view = inflater.inflate(R.layout.fragment_patient_personal_info, container, false);
        mContext = getActivity();
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
        btnSaveUpdate.setOnClickListener(v -> onPatientCreateClicked());
        fab.setOnClickListener(v -> takePicture());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Initialisation
    // ═════════════════════════════════════════════════════════════════════════

    private void updateLocale() {
        sessionManager = new SessionManager(getActivity());
        String language = sessionManager.getAppLanguage();
        if (!language.equalsIgnoreCase("")) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }
    }

    private void initUI() {
        etLayoutAge  = view.findViewById(R.id.etLayout_age);
        etLayoutDob  = view.findViewById(R.id.etLayout_dob);
        tvDobForDb   = view.findViewById(R.id.tv_selected_date_dob);
        tvAgeDob     = view.findViewById(R.id.tv_age_dob);

        cardFirstName             = view.findViewById(R.id.card_first_name);
        cardLastName              = view.findViewById(R.id.card_last_name);
        cardDob                   = view.findViewById(R.id.card_dob);
        cardAge                   = view.findViewById(R.id.card_age);
        cardMobileNumber          = view.findViewById(R.id.card_mobile_no);
        cardAlternateMobileNumber = view.findViewById(R.id.card_alternate_mobile_no);

        tvErrorFirstName       = view.findViewById(R.id.firstname_error);
        tvErrorLastName        = view.findViewById(R.id.lastname_error);
        tvErrorDob             = view.findViewById(R.id.dob_error);
        tvErrorAge             = view.findViewById(R.id.age_error);
        tvErrorMobileNo        = view.findViewById(R.id.mobile_no_error);
        tvErrAlternateMobileNo = view.findViewById(R.id.alternate_no_error);

        ProviderDAO providerDAO = new ProviderDAO();
        try {
            mProviderDoctorList = providerDAO.getDoctorList();
        } catch (DAOException e) {
            e.printStackTrace();
        }

        fab              = view.findViewById(R.id.fab_update_photo);
        ivProfilePhoto   = view.findViewById(R.id.iv_profile_photo);
        mFirstName       = view.findViewById(R.id.et_first_name);
        mMiddleName      = view.findViewById(R.id.et_middle_name);
        mLastName        = view.findViewById(R.id.et_last_name);
        mDOB             = view.findViewById(R.id.et_dob);
        mAge             = view.findViewById(R.id.et_age);
        mMobileNumber    = view.findViewById(R.id.et_mobile_no);
        mAlternateNumber = view.findViewById(R.id.et_alternate_mobile);
        btnSaveUpdate    = view.findViewById(R.id.btn_save_update_first);
        scrollviewPersonalInfo = view.findViewById(R.id.scroll_personal_info);

        i_privacy     = getActivity().getIntent();
        privacy_value = i_privacy.getStringExtra("privacy");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mDOB.setShowSoftInputOnFocus(false);
        }

        int[] todayBs = NepaliDateConverter.getCurrentBsDate();
        tvDobForDb.setText(formatBsDate(todayBs[0], todayBs[1], todayBs[2]));

        mFirstName.addTextChangedListener(new MyTextWatcher(mFirstName));
        mLastName.addTextChangedListener(new MyTextWatcher(mLastName));
        mDOB.addTextChangedListener(new MyTextWatcher(mDOB));
        mAge.addTextChangedListener(new MyTextWatcher(mAge));
        mMobileNumber.addTextChangedListener(new MyTextWatcher(mMobileNumber));
        mAlternateNumber.addTextChangedListener(new MyTextWatcher(mAlternateNumber));

        UpperCaseAlphabetsInputFilter alphabetFilter = new UpperCaseAlphabetsInputFilter();
        mFirstName.setFilters(new InputFilter[]{alphabetFilter});
        mMiddleName.setFilters(new InputFilter[]{alphabetFilter});
        mLastName.setFilters(new InputFilter[]{alphabetFilter});

        setDetailsAsPerConfigFile();
        updatePatientDetailsFromSummary();
        updatePatientDetailsFromSecondScreen();
        handleClickListeners();
    }

    private void handleClickListeners() {
        etLayoutDob.setEndIconOnClickListener(v -> showNepaliDatePicker());
        mDOB.setOnClickListener(v -> showNepaliDatePicker());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Nepali Date Picker
    // ═════════════════════════════════════════════════════════════════════════

    private void showNepaliDatePicker() {
        Calendar maxGreg = Calendar.getInstance();
        maxGreg.add(Calendar.YEAR, -13);
        int[] maxBs    = NepaliDateConverter.gregorianToBs(maxGreg.getTime());
        int maxBsYear  = maxBs[0];
        int maxBsMonth = maxBs[1];
        int maxBsDay   = maxBs[2];

        int initYear, initMonth, initDay;
        if (selectedBsYear > 0) {
            initYear  = selectedBsYear;
            initMonth = selectedBsMonth;
            initDay   = selectedBsDay;
        } else {
            initYear  = maxBsYear;
            initMonth = maxBsMonth;
            initDay   = maxBsDay;
        }

        NumberPicker yearPicker  = new NumberPicker(mContext);
        NumberPicker monthPicker = new NumberPicker(mContext);
        NumberPicker dayPicker   = new NumberPicker(mContext);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(maxBsYear);
        yearPicker.setValue(initYear);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(initYear == maxBsYear ? maxBsMonth : 12);
        monthPicker.setDisplayedValues(BS_MONTH_NAMES);
        monthPicker.setValue(initMonth);

        int daysInMonth = NepaliDateConverter.getDaysInBsMonth(initYear, initMonth);
        boolean isBoundaryMonth = (initYear == maxBsYear && initMonth == maxBsMonth);
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(isBoundaryMonth ? Math.min(daysInMonth, maxBsDay) : daysInMonth);
        dayPicker.setValue(Math.min(initDay, dayPicker.getMaxValue()));

        NumberPicker.OnValueChangeListener refreshDays = (picker, oldVal, newVal) -> {
            int y = yearPicker.getValue();
            int m = monthPicker.getValue();

            if (y == maxBsYear) {
                if (monthPicker.getMaxValue() != maxBsMonth) {
                    monthPicker.setMaxValue(maxBsMonth);
                    if (m > maxBsMonth) {
                        monthPicker.setValue(maxBsMonth);
                        m = maxBsMonth;
                    }
                }
            } else {
                monthPicker.setMaxValue(12);
            }

            int days = NepaliDateConverter.getDaysInBsMonth(y, m);
            int dayLimit = (y == maxBsYear && m == maxBsMonth) ? Math.min(days, maxBsDay) : days;
            dayPicker.setMaxValue(dayLimit);
            if (dayPicker.getValue() > dayLimit) dayPicker.setValue(dayLimit);
        };
        yearPicker.setOnValueChangedListener(refreshDays);
        monthPicker.setOnValueChangedListener(refreshDays);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(mContext);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        layout.addView(yearPicker, lp);
        layout.addView(monthPicker, lp);
        layout.addView(dayPicker, lp);

        new MaterialAlertDialogBuilder(mContext)
                .setTitle(getString(R.string.select_dob) + " (BS)")
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    int y = yearPicker.getValue();
                    int m = monthPicker.getValue();
                    int d = dayPicker.getValue();
                    onBsDateSelected(y, m, d);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Called when the user confirms a BS date in the picker.
     * Converts to Gregorian, validates age ≥ 13, populates UI fields.
     */
    private void onBsDateSelected(int bsYear, int bsMonth, int bsDay) {
        Date gregDate = NepaliDateConverter.bsToGregorian(bsYear, bsMonth, bsDay);

        int ageYears = calcAgeYears(gregDate);
        if (ageYears < 13) {
            showAgeError();
            return;
        }

        selectedBsYear    = bsYear;
        selectedBsMonth   = bsMonth;
        selectedBsDay     = bsDay;
        isDobFromCalendar = true;

        dobToDb = toGregorianDbFormat(gregDate);
        patient1.setDate_of_birth(dobToDb);
        patientDTO.setDateofbirth(dobToDb);

        String bsDisplay = formatBsDate(bsYear, bsMonth, bsDay);
        mDOB.setText(bsDisplay);
        tvDobForDb.setText(bsDisplay);
        setSelectedDob(mContext, bsDisplay);

        // ── FIX (Bug #2): age field shows only years (it is an input field).
        // The detail screen shows the full "X years - Y months - Z days" string.
        mAgeYears = ageYears;
        mAge.setText(String.valueOf(mAgeYears));

        tvErrorDob.setVisibility(View.GONE);
        tvErrorAge.setVisibility(View.GONE);
        cardDob.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        cardAge.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Age → DOB back-calculation
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Calculates an approximate DOB from an integer age and populates the DOB field.
     *
     * Strategy: subtract exactly {@code ageInYears} from today's date.
     * This gives a DOB whose calcAgeYears() will always return exactly
     * {@code ageInYears}, because Calendar.add(YEAR, -n) produces the
     * exact anniversary date n years ago.
     *
     * Example: today = 2026-04-15, age = 25 → DOB = 2001-04-15
     *   calcAgeYears(2001-04-15) on 2026-04-15 = 25 ✓
     */
    private void calculateDobFromAge(int ageInYears) {
        Calendar birthGreg = Calendar.getInstance();
        birthGreg.add(Calendar.YEAR, -ageInYears);
        Date birthDate = birthGreg.getTime();

        int[] bs = NepaliDateConverter.gregorianToBs(birthDate);
        selectedBsYear  = bs[0];
        selectedBsMonth = bs[1];
        selectedBsDay   = bs[2];

        String bsDisplay = formatBsDate(bs[0], bs[1], bs[2]);
        mDOB.setText(bsDisplay);
        tvDobForDb.setText(bsDisplay);

        dobToDb = toGregorianDbFormat(birthDate);
        patient1.setDate_of_birth(dobToDb);
        patientDTO.setDateofbirth(dobToDb);
        setSelectedDob(mContext, bsDisplay);

        Log.d(TAG, "calculateDobFromAge → BS: " + bsDisplay + " | DB: " + dobToDb);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helper utilities
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns "YYYY-MonthName-DD" e.g. "2055-Baisakh-15". */
    private String formatBsDate(int y, int m, int d) {
        return String.format(Locale.ENGLISH, "%d-%s-%02d", y, BS_MONTH_NAMES[m - 1], d);
    }

    /**
     * Full completed years of age from a Gregorian birth date to today.
     *
     * ── FIX (Bug #3 – leap-year off-by-one in calcAgeYears) ─────────────────
     * The original code used DAY_OF_YEAR for the "birthday not yet passed"
     * check. This is wrong across leap/non-leap year boundaries:
     *
     *   DOB  = 2000-03-01  →  DAY_OF_YEAR = 61  (2000 is a leap year)
     *   Today = 2001-03-01  →  DAY_OF_YEAR = 60  (2001 is not a leap year)
     *   Raw diff = 1 year.  60 < 61 → age-- → 0  ← WRONG (should be 1)
     *
     * Fix: compare MONTH + DAY_OF_MONTH instead of DAY_OF_YEAR.
     * ────────────────────────────────────────────────────────────────────────
     */
    private int calcAgeYears(Date birthDate) {
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthDate);
        Calendar now = Calendar.getInstance();

        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);

        // ── FIX: use MONTH + DAY_OF_MONTH, not DAY_OF_YEAR ───────────────────
        boolean birthdayNotYetThisYear =
                now.get(Calendar.MONTH) < birth.get(Calendar.MONTH)
                        || (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH)
                        && now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH));

        if (birthdayNotYetThisYear) age--;
        return age;
    }

    /** Formats a Date as yyyy-MM-dd (Gregorian) for DB storage. */
    private String toGregorianDbFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // consistent with NepaliDateConverter
        return sdf.format(date);
    }

    /**
     * Parses a Gregorian yyyy-MM-dd string → converts to the BS display string.
     * Used when loading existing patient data from DB.
     */
    private String gregDbDateToBsDisplay(String yyyyMMdd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(yyyyMMdd);
            if (date == null) return "";
            int[] bs = NepaliDateConverter.gregorianToBs(date);
            selectedBsYear  = bs[0];
            selectedBsMonth = bs[1];
            selectedBsDay   = bs[2];
            return formatBsDate(bs[0], bs[1], bs[2]);
        } catch (Exception e) {
            Log.e(TAG, "gregDbDateToBsDisplay failed: " + e.getMessage());
            return "";
        }
    }

    private void showAgeError() {
        mAge.setText("");
        mDOB.setText("");
        tvErrorAge.setVisibility(View.VISIBLE);
        tvErrorAge.setText(getString(R.string.patient_age_validation));
        cardAge.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
        cardDob.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
        selectedBsYear = selectedBsMonth = selectedBsDay = 0;
        dobToDb = "";
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Patient data load / bind
    // ═════════════════════════════════════════════════════════════════════════

    private void updatePatientDetailsFromSecondScreen() {
        fragment_secondScreen = new PatientAddressInfoFragment();
        if (getArguments() != null) {
            patientDTO               = (PatientDTO) getArguments().getSerializable("patientDTO");
            patient_detail           = getArguments().getBoolean("patient_detail");
            fromSecondScreen         = getArguments().getBoolean("fromSecondScreen");
            mAlternateNumberString   = getArguments().getString("mAlternateNumberString");
            editDetails              = getArguments().getBoolean("editDetails");
            patientAttributesModel   = (PatientAttributesModel) getArguments().getSerializable("patientAttributes");
            patientDTO.setAlternateNo(mAlternateNumberString);
            updateUI(patient1);

            if (fromSecondScreen) {
                mFirstName.setText(patientDTO.getFirstname());
                mMiddleName.setText(patientDTO.getMiddlename());
                mLastName.setText(patientDTO.getLastname());
                mMobileNumber.setText(patientDTO.getPhonenumber());
                mAlternateNumber.setText(mAlternateNumberString);

                String savedBsDisplay = getSelectedDob(mContext);
                if (savedBsDisplay != null && !savedBsDisplay.isEmpty()) {
                    mDOB.setText(savedBsDisplay);
                    tvDobForDb.setText(savedBsDisplay);
                }

                if (patientDTO.getDateofbirth() != null && !patientDTO.getDateofbirth().isEmpty()) {
                    dobToDb = patientDTO.getDateofbirth();
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date d = sdf.parse(dobToDb);
                        mAgeYears = calcAgeYears(d);
                        mAge.setText(String.valueOf(mAgeYears));
                    } catch (Exception ignored) {}
                }

                if (patientDTO.getPatientPhoto() != null && !patientDTO.getPatientPhoto().trim().isEmpty()) {
                    Glide.with(getActivity()).load(new File(patientDTO.getPatientPhoto()))
                            .thumbnail(0.25f).centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                            .into(ivProfilePhoto);
                }
            }

            if (patient1.getPatient_photo() != null && !patient1.getPatient_photo().trim().isEmpty()) {
                ivProfilePhoto.setImageBitmap(BitmapFactory.decodeFile(patient1.getPatient_photo()));
            }
        }
    }

    private void updatePatientDetailsFromSummary() {
        Intent intent = requireActivity().getIntent();
        if (intent != null && intent.hasExtra("fromSummary")) {
            mIsEditMode    = true;
            patientID_edit = intent.getStringExtra("patientUuid");
            fromSummary    = intent.getBooleanExtra("fromSummary", false);
            if (fromSummary) {
                patient1.setUuid(patientID_edit);
                bindDataWithUI(patientID_edit);
                updateUI(patient1);
            }
        }
    }

    private void updateUI(Patient patient) {
        if (patient.getAlternateNo() != null) {
            mAlternateNumberString = patient.getAlternateNo();
            mAlternateNumber.setText(mAlternateNumberString);
        }
    }

    private void bindDataWithUI(String patientUID) {
        SQLiteDatabase db = AppConstants.inteleHealthDatabaseHelper.getWriteDb();

        String[] patientColumns = {"uuid","first_name","middle_name","last_name","date_of_birth",
                "address1","address2","city_village","state_province","postal_code","country",
                "phone_number","gender","sdw","occupation","patient_photo",
                "economic_status","education_status","caste"};
        Cursor c = db.query("tbl_patient", patientColumns, "uuid=?", new String[]{patientUID},
                null, null, null);
        if (c.moveToFirst()) {
            patient1.setUuid(c.getString(c.getColumnIndexOrThrow("uuid")));
            patient1.setFirst_name(c.getString(c.getColumnIndexOrThrow("first_name")));
            patient1.setMiddle_name(c.getString(c.getColumnIndexOrThrow("middle_name")));
            patient1.setLast_name(c.getString(c.getColumnIndexOrThrow("last_name")));
            patient1.setDate_of_birth(c.getString(c.getColumnIndexOrThrow("date_of_birth")));
            patient1.setAddress1(c.getString(c.getColumnIndexOrThrow("address1")));
            patient1.setAddress2(c.getString(c.getColumnIndexOrThrow("address2")));
            patient1.setCity_village(c.getString(c.getColumnIndexOrThrow("city_village")));
            patient1.setState_province(c.getString(c.getColumnIndexOrThrow("state_province")));
            patient1.setPostal_code(c.getString(c.getColumnIndexOrThrow("postal_code")));
            patient1.setCountry(c.getString(c.getColumnIndexOrThrow("country")));
            patient1.setPhone_number(c.getString(c.getColumnIndexOrThrow("phone_number")));
            patient1.setGender(c.getString(c.getColumnIndexOrThrow("gender")));
            patient1.setSdw(c.getString(c.getColumnIndexOrThrow("sdw")));
            patient1.setOccupation(c.getString(c.getColumnIndexOrThrow("occupation")));
            patient1.setPatient_photo(c.getString(c.getColumnIndexOrThrow("patient_photo")));
        }
        c.close();

        Cursor ca = db.query("tbl_patient_attribute",
                new String[]{"value","person_attribute_type_uuid"},
                "patientuuid = ?", new String[]{patientUID}, null, null, null);
        if (ca.moveToFirst()) {
            do {
                String attrName = "";
                try {
                    attrName = patientsDAO.getAttributesName(
                            ca.getString(ca.getColumnIndexOrThrow("person_attribute_type_uuid")));
                } catch (DAOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                String val = ca.getString(ca.getColumnIndexOrThrow("value"));
                switch (attrName.toLowerCase()) {
                    case "alternateno":             patient1.setAlternateNo(val); break;
                    case "telephone number":        patient1.setPhone_number(val); break;
                    case "education level":         patient1.setEducation_level(val); break;
                    case "economic status":         patient1.setEconomic_status(val); break;
                    case "occupation":              patient1.setOccupation(val); break;
                    case "son/wife/daughter":       patient1.setSdw(val); break;
                    case "wife_daughter_of":        patient1.setWifeDaughterOf(val); break;
                    case "admission_date":          patient1.setAdmissionDate(val); break;
                    case "admission_time":          patient1.setAdmissionTime(val); break;
                    case "parity":                  patient1.setParity(val); break;
                    case "labor onset":             patient1.setLaborOnset(val); break;
                    case "active labor diagnosed":  patient1.setActiveLaborDiagnosed(val); break;
                    case "membrane ruptured timestamp": patient1.setMembraneRupturedTimestamp(val); break;
                    case "risk factors":            patient1.setRiskFactors(val); break;
                    case "hospital_maternity":      patient1.setHospitalMaternity(val); break;
                    case "primarydoctor":           patient1.setPrimaryDoctor(val); break;
                    case "secondarydoctor":         patient1.setSecondaryDoctor(val); break;
                    case "ezazi registration number": patient1.seteZaziRegNumber(val); break;
                }
            } while (ca.moveToNext());
        }
        ca.close();

        mFirstName.setText(patient1.getFirst_name());
        mMiddleName.setText(patient1.getMiddle_name());
        mLastName.setText(patient1.getLast_name());
        mMobileNumber.setText(patient1.getPhone_number());
        mAlternateNumber.setText(patient1.getAlternateNo());

        mCurrentPhotoPath = patient1.getPatient_photo();

        String gregDob = patient1.getDate_of_birth();
        if (gregDob != null && !gregDob.isEmpty()) {
            dobToDb = gregDob;
            String bsDisplay = gregDbDateToBsDisplay(gregDob);
            mDOB.setText(bsDisplay);
            tvDobForDb.setText(bsDisplay);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(gregDob);
                mAgeYears = calcAgeYears(d);
            } catch (Exception ignored) {}
            mAge.setText(String.valueOf(mAgeYears));
        }

        if (mCurrentPhotoPath != null && !mCurrentPhotoPath.isEmpty()) {
            Glide.with(getActivity()).load(new File(mCurrentPhotoPath))
                    .thumbnail(0.25f).centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                    .into(ivProfilePhoto);
        }

        patientDTO.setCityvillage(patient1.getCity_village());
        patientDTO.setStateprovince(patient1.getState_province());
        patientDTO.setCountry(patient1.getCountry());
        patientDTO.setAddress1(patient1.getAddress1());
        patientDTO.setAddress2(patient1.getAddress2());
        patientDTO.setPostalcode(patient1.getPostal_code());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Save / Navigate
    // ═════════════════════════════════════════════════════════════════════════

    private void onPatientCreateClicked() {
        if (!areValidFields()) {
            setScrollToFocusedItem();
            return;
        }
        patientUuid = UUID.randomUUID().toString();
        if (!patient_detail) patientDTO.setUuid(patientUuid);

        if (patientDTO != null) {
            patientDTO.setPatientPhoto(mCurrentPhotoPath != null ? mCurrentPhotoPath : patientDTO.getPatientPhoto());
            patientDTO.setFirstname(mFirstName.getText().toString());
            patientDTO.setMiddlename(mMiddleName.getText().toString());
            patientDTO.setLastname(mLastName.getText().toString());
            patientDTO.setPhonenumber(mMobileNumber.getText().toString());
            patientDTO.setDateofbirth(dobToDb);
            patientDTO.setGender(((EditText) view.findViewById(R.id.etGender)).getText().toString());

            Bundle bundle = new Bundle();
            bundle.putSerializable("patientDTO", (Serializable) patientDTO);
            bundle.putBoolean("fromFirstScreen", true);
            bundle.putBoolean("patient_detail", patient_detail);
            bundle.putString("patientUuidUpdate", patientID_edit);
            bundle.putString("mAlternateNumberString", mAlternateNumber.getText().toString());
            bundle.putBoolean("editDetails", true);
            bundle.putBoolean("fromSummary", fromSummary);
            bundle.putSerializable("patientAttributes", (Serializable) patientAttributesModel);

            fragment_secondScreen.setArguments(bundle);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_add_patient, fragment_secondScreen)
                    .commit();
            ((AddNewPatientActivity) requireActivity()).changeCurrentPage(AddNewPatientActivity.PAGE_ADDRESS);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Validation
    // ═════════════════════════════════════════════════════════════════════════

    private boolean areValidFields() {
        LinkedList<ErrorManagerModel> errors = new LinkedList<>();

        if (TextUtils.isEmpty(mFirstName.getText())) {
            errors.add(new ErrorManagerModel(mFirstName, tvErrorFirstName,
                    getString(R.string.enter_first_name), cardFirstName));
        } else {
            tvErrorFirstName.setVisibility(View.GONE);
            cardFirstName.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        if (TextUtils.isEmpty(mLastName.getText())) {
            errors.add(new ErrorManagerModel(mLastName, tvErrorLastName,
                    getString(R.string.enter_last_name), cardLastName));
        } else {
            tvErrorLastName.setVisibility(View.GONE);
            cardLastName.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        if (TextUtils.isEmpty(mDOB.getText())) {
            errors.add(new ErrorManagerModel(mDOB, tvErrorDob,
                    getString(R.string.select_dob), cardDob));
        } else {
            tvErrorDob.setVisibility(View.GONE);
            cardDob.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        String ageStr = mAge.getText() != null ? mAge.getText().toString().trim() : "";
        if (ageStr.isEmpty() || Integer.parseInt(ageStr) < 13) {
            errors.add(new ErrorManagerModel(mAge, tvErrorAge,
                    getString(R.string.patient_age_validation), cardAge));
        } else {
            tvErrorAge.setVisibility(View.GONE);
            cardAge.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        String mobile = mMobileNumber.getText().toString();
        if (!mobile.isEmpty() && mobile.length() != 10) {
            errors.add(new ErrorManagerModel(mMobileNumber, tvErrorMobileNo,
                    getString(R.string.mobile_no_length), cardMobileNumber));
        } else {
            tvErrorMobileNo.setVisibility(View.GONE);
            cardMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        String altMobile = mAlternateNumber.getText().toString();
        if (!altMobile.isEmpty() && altMobile.length() != 10) {
            errors.add(new ErrorManagerModel(mAlternateNumber, tvErrAlternateMobileNo,
                    getString(R.string.mobile_no_length), cardAlternateMobileNumber));
        } else {
            tvErrAlternateMobileNo.setVisibility(View.GONE);
            cardAlternateMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
        }

        if (!errors.isEmpty()) {
            for (int i = 0; i < errors.size(); i++) {
                ErrorManagerModel em = errors.get(i);
                if (i == 0) em.view.requestFocus();
                em.tvError.setVisibility(View.VISIBLE);
                em.tvError.setText(em.getErrorMessage());
                em.cardView.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
            }
            return false;
        }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Camera / Photo
    // ═════════════════════════════════════════════════════════════════════════

    private void checkPerm() {
        if (checkAndRequestPermissions()) takePicture();
    }

    private boolean checkAndRequestPermissions() {
        int cam  = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
        int wext = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> needed = new ArrayList<>();
        if (cam  != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CAMERA);
        if (wext != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), GROUP_PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    private void takePicture() {
        String patientTemp = patientUuid.isEmpty() ? patientDTO.getUuid() : patientUuid;
        File filePath = new File(AppConstants.IMAGE_PATH + patientTemp);
        if (!filePath.exists()) filePath.mkdir();
        Intent cam = new Intent(getActivity(), CameraActivity.class);
        cam.putExtra(CameraActivity.SET_IMAGE_NAME, patientTemp);
        cam.putExtra(CameraActivity.SET_IMAGE_PATH, filePath.toString());
        cameraLauncher.launch(cam);
    }

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    bindProfilePicture(result.getData());
                }
            });

    private void bindProfilePicture(Intent intent) {
        mCurrentPhotoPath = intent.getStringExtra("RESULT");
        RequestBuilder<Drawable> thumb = Glide.with(requireContext()).asDrawable().sizeMultiplier(0.25f);
        Glide.with(requireActivity()).load(new File(mCurrentPhotoPath))
                .thumbnail(thumb).centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                .into(ivProfilePhoto);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(requestCode, perms, grants);
        if (requestCode == GROUP_PERMISSION_REQUEST) {
            boolean all = grants.length != 0;
            for (int g : grants) if (g != PackageManager.PERMISSION_GRANTED) { all = false; break; }
            if (all) takePicture();
            else showPermissionDeniedAlert(perms);
        }
    }

    private void showPermissionDeniedAlert(String[] permissions) {
        ConfirmationDialogFragment dialog = new ConfirmationDialogFragment.Builder(requireActivity())
                .content(getString(R.string.reject_permission_results))
                .positiveButtonLabel(R.string.retry_again)
                .negativeButtonLabel(R.string.ok_close_now)
                .build();
        dialog.setListener(this::checkPerm);
        dialog.show(getChildFragmentManager(), dialog.getClass().getCanonicalName());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Config file setup
    // ═════════════════════════════════════════════════════════════════════════

    private void setDetailsAsPerConfigFile() {
        boolean hasLicense = !sessionManager.getLicenseKey().isEmpty();
        try {
            JSONObject obj = hasLicense
                    ? new JSONObject(Objects.requireNonNullElse(
                    FileUtils.readFileRoot(AppConstants.CONFIG_FILE_NAME, mContext),
                    String.valueOf(FileUtils.encodeJSON(mContext, AppConstants.CONFIG_FILE_NAME))))
                    : new JSONObject(String.valueOf(FileUtils.encodeJSON(mContext, AppConstants.CONFIG_FILE_NAME)));

            mFirstName.setVisibility(obj.getBoolean("mFirstName")  ? View.VISIBLE : View.GONE);
            mMiddleName.setVisibility(obj.getBoolean("mMiddleName") ? View.VISIBLE : View.GONE);
            mLastName.setVisibility(obj.getBoolean("mLastName")     ? View.VISIBLE : View.GONE);
            mDOB.setVisibility(obj.getBoolean("mDOB")               ? View.VISIBLE : View.GONE);
            mMobileNumber.setVisibility(obj.getBoolean("mPhoneNum") ? View.VISIBLE : View.GONE);
            mAge.setVisibility(obj.getBoolean("mAge")               ? View.VISIBLE : View.GONE);
        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(mContext, "JsonException " + e, Toast.LENGTH_LONG).show();
            showAlertDialogButtonClicked(e.toString());
        }
    }

    public void showAlertDialogButtonClicked(String errorMessage) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(mContext);
        b.setTitle("Config Error").setMessage(errorMessage)
                .setNeutralButton(R.string.generic_ok, (d, w) -> {
                    Intent i = new Intent(mContext, SetupActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                });
        AlertDialog ad = b.create();
        ad.show();
        IntelehealthApplication.setAlertDialogCustomTheme(mContext, ad);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SharedPrefs
    // ═════════════════════════════════════════════════════════════════════════

    public String getSelectedDob(Context context) {
        return context.getSharedPreferences("dobPatient", MODE_PRIVATE)
                .getString("dobPatient", "");
    }

    public void setSelectedDob(Context context, String dob) {
        context.getApplicationContext()
                .getSharedPreferences("dobPatient", 0)
                .edit().putString("dobPatient", dob).apply();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UUID / Scroll helpers
    // ═════════════════════════════════════════════════════════════════════════

    public void generateUuid() {
        patientUuid = uuidGenerator.UuidGenerator();
    }

    private void setScrollToFocusedItem() {
        if (requireView().findFocus() != null) {
            View focused = requireView().findFocus();
            if (focused.getId() == R.id.et_first_name) {
                scrollviewPersonalInfo.smoothScrollTo(0, 0);
            } else {
                scrollviewPersonalInfo.smoothScrollTo(0, getLocationOnScreen(focused).y);
            }
        }
    }

    public static Point getLocationOnScreen(View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return new Point(loc[0], loc[1]);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TextWatcher
    // ═════════════════════════════════════════════════════════════════════════

    class MyTextWatcher implements TextWatcher {
        final EditText editText;
        MyTextWatcher(EditText et) { this.editText = et; }

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

        @Override
        public void afterTextChanged(Editable editable) {
            String val = editable.toString().trim();
            int id = editText.getId();

            if (id == R.id.et_first_name) {
                boolean empty = val.isEmpty();
                tvErrorFirstName.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) tvErrorFirstName.setText(getString(R.string.enter_first_name));
                cardFirstName.setStrokeColor(ContextCompat.getColor(mContext,
                        empty ? R.color.error_red : R.color.colorScrollbar));

            } else if (id == R.id.et_last_name) {
                boolean empty = val.isEmpty();
                tvErrorLastName.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) tvErrorLastName.setText(getString(R.string.enter_last_name));
                cardLastName.setStrokeColor(ContextCompat.getColor(mContext,
                        empty ? R.color.error_red : R.color.colorScrollbar));

            } else if (id == R.id.et_dob) {
                boolean empty = val.isEmpty();
                tvErrorDob.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty) tvErrorDob.setText(getString(R.string.select_dob));
                cardDob.setStrokeColor(ContextCompat.getColor(mContext,
                        empty ? R.color.error_red : R.color.colorScrollbar));

            } else if (id == R.id.et_age) {
                if (val.isEmpty()) {
                    mDOB.setText("");
                    tvDobForDb.setText("");
                    dobToDb = "";
                    selectedBsYear = selectedBsMonth = selectedBsDay = 0;
                    isDobFromCalendar = false;
                    tvErrorAge.setVisibility(View.VISIBLE);
                    tvErrorAge.setText(getString(R.string.patient_age_validation));
                    cardAge.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                } else {
                    int age;
                    try { age = Integer.parseInt(val); } catch (NumberFormatException e) { return; }
                    if (age < 13) {
                        showAgeError();
                    } else {
                        tvErrorAge.setVisibility(View.GONE);
                        cardAge.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                        // Back-calculate DOB only when user is typing age directly (not after picker)
                        if (!isDobFromCalendar || mDOB.getText().toString().isEmpty()) {
                            calculateDobFromAge(age);
                        }
                    }
                }

            } else if (id == R.id.et_mobile_no) {
                if (!val.isEmpty() && val.length() != 10) {
                    tvErrorMobileNo.setVisibility(View.VISIBLE);
                    tvErrorMobileNo.setText(getString(R.string.mobile_no_length));
                    cardMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                } else {
                    tvErrorMobileNo.setVisibility(View.GONE);
                    cardMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                }

            } else if (id == R.id.et_alternate_mobile) {
                if (!val.isEmpty() && val.length() != 10) {
                    tvErrAlternateMobileNo.setVisibility(View.VISIBLE);
                    tvErrAlternateMobileNo.setText(getString(R.string.mobile_no_length));
                    cardAlternateMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.error_red));
                } else {
                    tvErrAlternateMobileNo.setVisibility(View.GONE);
                    cardAlternateMobileNumber.setStrokeColor(ContextCompat.getColor(mContext, R.color.colorScrollbar));
                }
            }
        }
    }
}