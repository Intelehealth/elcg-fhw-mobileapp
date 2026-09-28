package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.ActivityAdmissionDataBinding
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAdmissionDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    override fun getScreenTitle(): Int = R.string.title_activity_admission_data

    companion object {
        private const val EXTRA_PATIENT_UUID = "patientUuid"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
