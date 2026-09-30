package org.intelehealth.ezazi.activities.addNewPatient

import org.intelehealth.ezazi.models.dto.PatientAttributesModel
import org.intelehealth.ezazi.models.dto.PatientDTO
import java.io.Serializable

/**
 * Everything one registration needs, owned by AddNewPatientActivity instead of ferried between the
 * three step fragments. A plain holder with no logic: the moment it gains a method the fragments
 * start calling it instead of the host.
 */
class PatientRegistrationDraft : Serializable {
    var patient: PatientDTO = PatientDTO()
    var obstetric: PatientAttributesModel? = null
    var alternateNumber: String? = null
    var dobToDb: String? = null
    var primaryDoctorUuid: String? = null
    var secondaryDoctorUuid: String? = null
    var fromSummary: Boolean = false
    var editingPatientUuid: String? = null
    var newPatientUuid: String? = null
    var privacyValue: String? = null
}
