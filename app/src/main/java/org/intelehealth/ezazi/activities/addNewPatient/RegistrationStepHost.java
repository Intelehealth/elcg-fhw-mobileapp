package org.intelehealth.ezazi.activities.addNewPatient;

/**
 * What a registration step may ask of the Activity hosting it. Top-level rather than nested in
 * AddNewPatientActivity, because a class cannot implement its own nested interface.
 */
public interface RegistrationStepHost {
    PatientRegistrationDraft draft();

    String resolveUuid();

    void onStepCompleted();

    void onStepBack();
}
