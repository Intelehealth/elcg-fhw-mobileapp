package org.intelehealth.ezazi.activities.visitCreation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.app.AppConstants;
import org.intelehealth.ezazi.database.dao.ProviderDAO;
import org.intelehealth.ezazi.models.dto.ProviderDTO;
import org.intelehealth.ezazi.ui.dialog.CalendarDialog;
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment;
import org.intelehealth.ezazi.ui.dialog.SingleChoiceDialogFragment;
import org.intelehealth.ezazi.ui.dialog.ThemeTimePickerDialog;
import org.intelehealth.ezazi.ui.dialog.adapter.RiskFactorMultiChoiceAdapter;
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem;
import org.intelehealth.ezazi.utilities.exception.DAOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Collects the obstetric intake for a new visit. This step only gathers and validates
 * the input; creating the visit and persisting the values as visit attributes is wired
 * in a later step of the visit flow.
 */
public class ObstetricIntakeFragment extends Fragment {

    private TextInputEditText etGravida, etTotalBirths, etTotalMiscarriages, etLmp, etEdd,
            etAdmissionDate, etAdmissionTime, etDiagnosedDate, etDiagnosedTime,
            etMembraneDate, etMembraneTime, etRiskFactors, etHospitalOther, etHospitalId,
            etPrimaryDoctor, etSecondaryDoctor, etBedNumber;
    private TextInputLayout layoutLmp, layoutEdd, layoutAdmissionDate, layoutAdmissionTime,
            layoutDiagnosedDate, layoutDiagnosedTime, layoutMembraneDate, layoutMembraneTime;
    private TextView tvSpontaneous, tvInduced, tvHospital, tvMaternity, tvOther;
    private MaterialCheckBox cbMembraneUnknown;
    private MaterialCardView cardMembraneDate, cardMembraneTime, cardHospitalOther;
    private MaterialButton btnSave;

    private String laborOnset = "";
    private String hospitalMaternity = "";
    private String primaryDoctorUuid = "";
    private String secondaryDoctorUuid = "";
    private final List<ProviderDTO> doctorList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_obstetric_intake, container, false);
        bindViews(root);
        loadDoctors();
        setupDateTimePickers();
        setupToggles();
        setupChoiceFields();
        setupMembraneUnknown();
        btnSave.setOnClickListener(v -> onSaveClicked());
        return root;
    }

    private void bindViews(View root) {
        etGravida = root.findViewById(R.id.et_oi_gravida);
        etTotalBirths = root.findViewById(R.id.et_oi_total_births);
        etTotalMiscarriages = root.findViewById(R.id.et_oi_total_miscarriages);
        etLmp = root.findViewById(R.id.et_oi_lmp);
        etEdd = root.findViewById(R.id.et_oi_edd);
        etAdmissionDate = root.findViewById(R.id.et_oi_admission_date);
        etAdmissionTime = root.findViewById(R.id.et_oi_admission_time);
        etDiagnosedDate = root.findViewById(R.id.et_oi_diagnosed_date);
        etDiagnosedTime = root.findViewById(R.id.et_oi_diagnosed_time);
        etMembraneDate = root.findViewById(R.id.et_oi_membrane_date);
        etMembraneTime = root.findViewById(R.id.et_oi_membrane_time);
        etRiskFactors = root.findViewById(R.id.et_oi_risk_factors);
        etHospitalOther = root.findViewById(R.id.et_oi_hospital_other);
        etHospitalId = root.findViewById(R.id.et_oi_hospital_id);
        etPrimaryDoctor = root.findViewById(R.id.et_oi_primary_doctor);
        etSecondaryDoctor = root.findViewById(R.id.et_oi_secondary_doctor);
        etBedNumber = root.findViewById(R.id.et_oi_bed_number);

        layoutLmp = root.findViewById(R.id.etLayout_oi_lmp);
        layoutEdd = root.findViewById(R.id.etLayout_oi_edd);
        layoutAdmissionDate = root.findViewById(R.id.etLayout_oi_admission_date);
        layoutAdmissionTime = root.findViewById(R.id.etLayout_oi_admission_time);
        layoutDiagnosedDate = root.findViewById(R.id.etLayout_oi_diagnosed_date);
        layoutDiagnosedTime = root.findViewById(R.id.etLayout_oi_diagnosed_time);
        layoutMembraneDate = root.findViewById(R.id.etLayout_oi_membrane_date);
        layoutMembraneTime = root.findViewById(R.id.etLayout_oi_membrane_time);

        tvSpontaneous = root.findViewById(R.id.tv_oi_spontaneous);
        tvInduced = root.findViewById(R.id.tv_oi_induced);
        tvHospital = root.findViewById(R.id.tv_oi_hospital);
        tvMaternity = root.findViewById(R.id.tv_oi_maternity);
        tvOther = root.findViewById(R.id.tv_oi_other);

        cbMembraneUnknown = root.findViewById(R.id.cb_oi_membrane_unknown);
        cardMembraneDate = root.findViewById(R.id.card_oi_membrane_date);
        cardMembraneTime = root.findViewById(R.id.card_oi_membrane_time);
        cardHospitalOther = root.findViewById(R.id.card_oi_hospital_other);
        btnSave = root.findViewById(R.id.btn_oi_save);
    }

    private void loadDoctors() {
        try {
            doctorList.addAll(new ProviderDAO().getDoctorList());
        } catch (DAOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void setupDateTimePickers() {
        etLmp.setOnClickListener(v -> showDatePicker(etLmp, false));
        layoutLmp.setEndIconOnClickListener(v -> showDatePicker(etLmp, false));
        etEdd.setOnClickListener(v -> showDatePicker(etEdd, true));
        layoutEdd.setEndIconOnClickListener(v -> showDatePicker(etEdd, true));
        etAdmissionDate.setOnClickListener(v -> showDatePicker(etAdmissionDate, false));
        layoutAdmissionDate.setEndIconOnClickListener(v -> showDatePicker(etAdmissionDate, false));
        etAdmissionTime.setOnClickListener(v -> showTimePicker(etAdmissionTime));
        layoutAdmissionTime.setEndIconOnClickListener(v -> showTimePicker(etAdmissionTime));
        etDiagnosedDate.setOnClickListener(v -> showDatePicker(etDiagnosedDate, false));
        layoutDiagnosedDate.setEndIconOnClickListener(v -> showDatePicker(etDiagnosedDate, false));
        etDiagnosedTime.setOnClickListener(v -> showTimePicker(etDiagnosedTime));
        layoutDiagnosedTime.setEndIconOnClickListener(v -> showTimePicker(etDiagnosedTime));
        etMembraneDate.setOnClickListener(v -> showDatePicker(etMembraneDate, false));
        layoutMembraneDate.setEndIconOnClickListener(v -> showDatePicker(etMembraneDate, false));
        etMembraneTime.setOnClickListener(v -> showTimePicker(etMembraneTime));
        layoutMembraneTime.setEndIconOnClickListener(v -> showTimePicker(etMembraneTime));
    }

    private void setupToggles() {
        tvSpontaneous.setOnClickListener(v -> {
            laborOnset = getString(R.string.spontaneous);
            highlightToggle(tvSpontaneous, tvSpontaneous, tvInduced);
        });
        tvInduced.setOnClickListener(v -> {
            laborOnset = getString(R.string.induced);
            highlightToggle(tvInduced, tvSpontaneous, tvInduced);
        });
        tvHospital.setOnClickListener(v -> {
            hospitalMaternity = getString(R.string.hospital_option);
            highlightToggle(tvHospital, tvHospital, tvMaternity, tvOther);
            cardHospitalOther.setVisibility(View.GONE);
        });
        tvMaternity.setOnClickListener(v -> {
            hospitalMaternity = getString(R.string.maternity_option);
            highlightToggle(tvMaternity, tvHospital, tvMaternity, tvOther);
            cardHospitalOther.setVisibility(View.GONE);
        });
        tvOther.setOnClickListener(v -> {
            hospitalMaternity = getString(R.string.other_option);
            highlightToggle(tvOther, tvHospital, tvMaternity, tvOther);
            cardHospitalOther.setVisibility(View.VISIBLE);
        });
    }

    private void highlightToggle(TextView selected, TextView... all) {
        for (TextView tv : all) {
            tv.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_bg_rounded_corners));
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.darkGray));
        }
        selected.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.button_primary_rounded));
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void setupChoiceFields() {
        etRiskFactors.setOnClickListener(v -> showRiskFactorsDialog());
        etPrimaryDoctor.setOnClickListener(v -> selectPrimaryDoctor());
        etSecondaryDoctor.setOnClickListener(v -> selectSecondaryDoctor());
    }

    private void setupMembraneUnknown() {
        cbMembraneUnknown.setOnCheckedChangeListener((button, isChecked) -> {
            cardMembraneDate.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            cardMembraneTime.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            if (isChecked) {
                etMembraneDate.setText("");
                etMembraneTime.setText("");
            }
        });
    }

    private void showDatePicker(TextInputEditText target, boolean allowFuture) {
        boolean isTablet = getResources().getBoolean(R.bool.isTabletSize);
        int maxHeight = getResources().getDimensionPixelOffset(R.dimen.std_430dp);
        CalendarDialog dialog = new CalendarDialog.Builder(requireContext())
                .title("")
                .positiveButtonLabel(R.string.ok)
                .maxHeight(!isTablet ? maxHeight : 0)
                .build();
        if (!allowFuture) {
            dialog.setMaxDate(System.currentTimeMillis());
        }
        dialog.setListener((day, month, year, value) -> target.setText(value));
        dialog.show(getChildFragmentManager(), "DatePicker");
    }

    private void showTimePicker(TextInputEditText target) {
        ThemeTimePickerDialog dialog = new ThemeTimePickerDialog.Builder(requireContext())
                .title(R.string.current_time)
                .positiveButtonLabel(R.string.ok)
                .build();
        dialog.setListener((hours, minutes, amPm, value) ->
                target.setText(String.format(Locale.ENGLISH, "%02d:%02d %s", hours, minutes, amPm)));
        dialog.show(getChildFragmentManager(), "TimePicker");
    }

    private void showRiskFactorsDialog() {
        MultiChoiceDialogFragment<String> dialog = new MultiChoiceDialogFragment.Builder<String>(requireContext())
                .title(R.string.select_risk_factors)
                .positiveButtonLabel(R.string.save_button)
                .build();
        dialog.isSearchable(true);
        List<String> items = Arrays.asList(getResources().getStringArray(R.array.risk_factors));
        dialog.setAdapter(new RiskFactorMultiChoiceAdapter(requireContext(), new ArrayList<>(items)));
        dialog.setListener(selected -> etRiskFactors.setText(TextUtils.join(", ", selected)));
        dialog.show(getChildFragmentManager(), "RiskFactors");
    }

    private void selectPrimaryDoctor() {
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        for (int i = 0; i < doctorList.size(); i++) {
            ProviderDTO doctor = doctorList.get(i);
            if (doctor.getUserUuid().equals(secondaryDoctorUuid)) continue;
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(doctor.getGivenName() + " " + doctor.getFamilyName());
            item.setItemId(doctor.getUserUuid());
            item.setItemIndex(i);
            item.setSelected(doctor.getUserUuid().equals(primaryDoctorUuid));
            items.add(item);
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(requireContext())
                .title(R.string.select_primary_doctor)
                .positiveButtonLabel(R.string.save_button)
                .content(items)
                .build();
        dialog.isSearchable(true);
        dialog.setListener(item -> {
            primaryDoctorUuid = item.getItemId();
            etPrimaryDoctor.setText(item.getItem());
        });
        dialog.show(getChildFragmentManager(), "PrimaryDoctor");
    }

    private void selectSecondaryDoctor() {
        if (primaryDoctorUuid.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_primary_doctor, Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<SingChoiceItem> items = new ArrayList<>();
        SingChoiceItem notApplicable = new SingChoiceItem();
        notApplicable.setItem(AppConstants.NOT_APPLICABLE_FULL_TEXT);
        notApplicable.setItemId(AppConstants.NOT_APPLICABLE);
        notApplicable.setItemIndex(0);
        items.add(notApplicable);
        for (int i = 0; i < doctorList.size(); i++) {
            ProviderDTO doctor = doctorList.get(i);
            if (doctor.getUserUuid().equals(primaryDoctorUuid)) continue;
            SingChoiceItem item = new SingChoiceItem();
            item.setItem(doctor.getGivenName() + " " + doctor.getFamilyName());
            item.setItemId(doctor.getUserUuid());
            item.setItemIndex(i + 1);
            item.setSelected(doctor.getUserUuid().equals(secondaryDoctorUuid));
            items.add(item);
        }
        SingleChoiceDialogFragment dialog = new SingleChoiceDialogFragment.Builder(requireContext())
                .title(R.string.select_secondary_doctor)
                .positiveButtonLabel(R.string.save_button)
                .content(items)
                .build();
        dialog.isSearchable(true);
        dialog.setListener(item -> {
            secondaryDoctorUuid = item.getItemId();
            etSecondaryDoctor.setText(item.getItem());
        });
        dialog.show(getChildFragmentManager(), "SecondaryDoctor");
    }

    private void onSaveClicked() {
        String error = firstValidationError();
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            return;
        }
        onObstetricDataCollected(collect());
    }

    private String firstValidationError() {
        if (isBlank(etAdmissionDate)) return getString(R.string.select_admission_date);
        if (isBlank(etAdmissionTime)) return getString(R.string.select_admission_time);
        if (isBlank(etTotalBirths)) return getString(R.string.total_birth_count_val_txt);
        if (isBlank(etTotalMiscarriages)) return getString(R.string.total_miscarriage_count_val_txt);
        if (laborOnset.isEmpty()) return getString(R.string.labor_onset_val_txt);
        if (isBlank(etDiagnosedDate)) return getString(R.string.active_labor_diagnosed_date_val_txt);
        if (isBlank(etDiagnosedTime)) return getString(R.string.active_labor_diagnosed_time_val_txt);
        if (!cbMembraneUnknown.isChecked()) {
            if (isBlank(etMembraneDate)) return getString(R.string.select_sac_ruptured_date);
            if (isBlank(etMembraneTime)) return getString(R.string.select_sac_ruptured_time);
        }
        if (primaryDoctorUuid.isEmpty()) return getString(R.string.select_primary_doctor);
        return null;
    }

    private ObstetricVisitData collect() {
        ObstetricVisitData data = new ObstetricVisitData();
        data.gravida = text(etGravida);
        data.admissionDate = text(etAdmissionDate);
        data.admissionTime = text(etAdmissionTime);
        data.totalBirths = text(etTotalBirths);
        data.totalMiscarriages = text(etTotalMiscarriages);
        data.laborOnset = laborOnset;
        data.lastMenstrualPeriod = text(etLmp);
        data.estimatedDeliveryDate = text(etEdd);
        data.activeLaborDiagnosedDate = text(etDiagnosedDate);
        data.activeLaborDiagnosedTime = text(etDiagnosedTime);
        data.membraneRupturedUnknown = cbMembraneUnknown.isChecked();
        data.membraneRupturedDate = text(etMembraneDate);
        data.membraneRupturedTime = text(etMembraneTime);
        data.riskFactors = text(etRiskFactors);
        data.hospitalMaternity = hospitalMaternity;
        data.hospitalOther = text(etHospitalOther);
        data.hospitalId = text(etHospitalId);
        data.primaryDoctorUuid = primaryDoctorUuid;
        data.primaryDoctorName = text(etPrimaryDoctor);
        data.secondaryDoctorUuid = secondaryDoctorUuid;
        data.secondaryDoctorName = text(etSecondaryDoctor);
        data.bedNumber = text(etBedNumber);
        return data;
    }

    /**
     * Receives the validated obstetric intake. Creating the visit, writing these values as
     * visit attributes and opening the timeline is handled in a later step of the flow.
     */
    private void onObstetricDataCollected(ObstetricVisitData data) {
        Toast.makeText(requireContext(), R.string.obstetric_intake_saved, Toast.LENGTH_SHORT).show();
    }

    private boolean isBlank(TextInputEditText editText) {
        return editText.getText() == null || TextUtils.isEmpty(editText.getText().toString().trim());
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
