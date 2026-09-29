package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.database.dao.PatientsDAO
import org.intelehealth.ezazi.databinding.ActivityAdmissionDataBinding
import org.intelehealth.ezazi.ui.dialog.CalendarDialog
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.utilities.GregorianDateUtils.GREG_FMT
import org.intelehealth.ezazi.utilities.GregorianDateUtils.gregStringToMillis

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding
    private var patientUuid: String = ""
    private var dateOfBirth: String = ""
    private var patientContextLoaded = false

    private var admissionDate: String = ""
    private var activeLabourDiagnosedDate: String = ""
    private var membraneRupturedDate: String = ""
    private var lmpDate: String = ""
    private var edd: String = ""

    // get() and not a plain val: binding is lateinit and assigned in onCreate, so an initializer runs too early.
    private val form get() = binding.admissionForm
    private val lmpEdd get() = form.viewLmpEddLayout
    private val common get() = form.includeOtherCommonComponent
    private val actions get() = form.includeOtherActionView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdmissionDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.initializeNetworkBannerComponents()

        setupActionBar()
        readIntentExtras()
        loadPatientContext()
        setupSaveButton()
        setupBackButton()
        setupBackConfirmation()
        setupAdmissionDateField()
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
            dateOfBirth = withContext(Dispatchers.IO) {
                PatientsDAO.getDateOfBirth(patientUuid).orEmpty()
            }
            patientContextLoaded = true
        }
    }

    private fun setupSaveButton() {
        actions.btnNextAddress.apply {
            text = getString(R.string.save_button)
            icon = null
            setOnClickListener { onSaveClicked() }
        }
    }

    private fun onSaveClicked() {
        Toast.makeText(this, "Save clicked | dob=$dateOfBirth", Toast.LENGTH_SHORT).show()
    }

    private fun setupBackButton() {
        actions.btnBackAddress.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBackConfirmation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showBackConfirmationDialog()
            }
        })
    }

    private fun showBackConfirmationDialog() {
        if (supportFragmentManager.findFragmentByTag(TAG_BACK_CONFIRMATION) != null) return

        val dialog = ConfirmationDialogFragment.Builder(this)
            .content(getString(R.string.admission_discard_message))
            .positiveButtonLabel(R.string.confirm)
            .negativeButtonLabel(R.string.cancel)
            .build()
        dialog.setListener { finish() }
        dialog.show(supportFragmentManager, TAG_BACK_CONFIRMATION)
    }

    private fun disableSoftInput(vararg fields: EditText) {
        fields.forEach { it.showSoftInputOnFocus = false }
    }

    private fun showDatePicker(titleRes: Int, currentGreg: String, onPicked: (String) -> Unit) {
        val dialog = CalendarDialog.Builder(this)
            .title(getString(titleRes))
            .positiveButtonLabel(R.string.ok)
            .build()
        dialog.setDateFormat(GREG_FMT)
        gregStringToMillis(currentGreg)?.let { dialog.setDefaultDate(it) }
        dialog.setListener { _, _, _, value -> onPicked(value) }
        dialog.show(supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun setupAdmissionDateField() {
        disableSoftInput(form.etAdmissionDate)
        form.etLayoutAdmissionDate.setEndIconOnClickListener { pickAdmissionDate() }
        form.etAdmissionDate.setOnClickListener { pickAdmissionDate() }
    }

    private fun pickAdmissionDate() {
        showDatePicker(R.string.select_admission_date, admissionDate) { greg ->
            admissionDate = greg
            form.etAdmissionDate.setText(greg)
        }
    }

    override fun getScreenTitle(): Int = R.string.title_activity_admission_data

    companion object {
        private const val EXTRA_PATIENT_UUID = "admission_patient_uuid"
        private const val TAG_BACK_CONFIRMATION = "back_confirmation"
        private const val TAG_DATE_PICKER = "date_picker"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
