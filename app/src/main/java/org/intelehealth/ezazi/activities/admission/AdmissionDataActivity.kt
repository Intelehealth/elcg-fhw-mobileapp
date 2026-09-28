package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.database.dao.PatientsDAO
import org.intelehealth.ezazi.databinding.ActivityAdmissionDataBinding
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import timber.log.Timber

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding
    private var patientUuid: String = ""
    private var dateOfBirth: String = ""
    private var patientContextLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAdmissionDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        setupActionBar()
        readIntentExtras()
        loadPatientContext()
    }

    private fun readIntentExtras() {
        patientUuid = intent.getStringExtra(EXTRA_PATIENT_UUID).orEmpty()
    }

    /**
     * Loads the patient context this screen needs but does not display. The date of birth drives the
     * parity-against-age rule on Save, which must not run while patientContextLoaded is false.
     */
    private fun loadPatientContext() {
        lifecycleScope.launch {
            dateOfBirth = withContext(Dispatchers.IO) { PatientsDAO.getDateOfBirth(patientUuid).orEmpty() }
            patientContextLoaded = true
            Timber.tag("AdmissionDataActivity").d("dob=$dateOfBirth")
        }
    }

    override fun getScreenTitle(): Int = R.string.title_activity_admission_data

    companion object {
        private const val EXTRA_PATIENT_UUID = "admission_patient_uuid"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
