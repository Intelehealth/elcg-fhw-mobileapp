package org.intelehealth.ezazi.activities.addNewPatient;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.intelehealth.ezazi.R;
import org.intelehealth.ezazi.models.dto.PatientDTO;

/**
 * Final step of patient registration. Obstetric intake moved to the visit flow, so this
 * step now only carries optional demographic add-ons (e.g. ABHA details in the ABDM build).
 * It is shown only when {@link #hasFieldsToShow(Activity)} is true; otherwise the address
 * step creates the patient directly through {@link PatientRegistrationSaver}.
 */
public class PatientOtherInfoFragment extends Fragment {

    private PatientDTO patientDTO;
    private String alternateNumber = "";
    private boolean fromSummary = false;
    private String patientUuidUpdate = "";

    public static PatientOtherInfoFragment getInstance() {
        return new PatientOtherInfoFragment();
    }

    /**
     * Whether this step has any field to show. Returns false in this build (nothing to
     * collect). The ABDM build returns true when an AbdmResult with a profile is present,
     * matching the condition that reveals the ABHA fields.
     */
    public static boolean hasFieldsToShow(Activity activity) {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_other_info, container, false);
        readArguments();
        MaterialButton btnBack = view.findViewById(R.id.btn_back_address);
        MaterialButton btnNext = view.findViewById(R.id.btn_next_address);
        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnNext.setOnClickListener(v -> {
            if (patientDTO != null) {
                PatientRegistrationSaver.savePatient(requireActivity(), patientDTO, alternateNumber,
                        fromSummary, patientUuidUpdate);
            }
        });
        return view;
    }

    private void readArguments() {
        if (getArguments() == null) return;
        patientDTO = (PatientDTO) getArguments().getSerializable("patientDTO");
        alternateNumber = getArguments().getString("mAlternateNumberString");
        fromSummary = getArguments().getBoolean("fromSummary");
        patientUuidUpdate = getArguments().getString("patientUuidUpdate");
    }
}
