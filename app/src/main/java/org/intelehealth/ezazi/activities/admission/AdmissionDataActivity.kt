package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.database.dao.PatientsDAO
import org.intelehealth.ezazi.databinding.ActivityAdmissionDataBinding
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.utilities.GregorianDateUtils.eddFromLmp
import org.intelehealth.ezazi.utilities.GregorianDateUtils.gregToDisplay
import org.intelehealth.ezazi.utilities.ObstetricDatePicker
import org.intelehealth.ezazi.utilities.ObstetricTimePicker

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding
    private var patientUuid: String = ""
    private var dateOfBirth: String = ""
    private var patientContextLoaded = false

    private var admissionDate: String = ""
    private var activeLabourDiagnosedDate: String = ""
    private var admissionTime: String = ""
    private var activeLabourDiagnosedTime: String = ""
    private var membraneRupturedDate: String = ""
    private var lmpDate: String = ""
    private var edd: String = ""
    private var totalBirthCount: String = "0"
    private var totalMiscarriageCount: String = "0"
    private var labourOnset: String = ""

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
        setupLabourDiagnosedDateField()
        setupAdmissionTimeField()
        setupLabourDiagnosedTimeField()
        setupLmpField()
        setupParityFields()
        setupLabourOnsetToggle()
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

    private fun setupParityFields() {
        form.etTotalBirth.afterTextChanged { value ->
            if (value.isEmpty()) {
                totalBirthCount = "0"
                form.etGravida.text = null
            } else {
                totalBirthCount = value
                updateGravida()
            }
        }
        form.etTotalMiscarriage.afterTextChanged { value ->
            if (value.isEmpty()) {
                totalMiscarriageCount = "0"
                form.etGravida.text = null
            } else {
                totalMiscarriageCount = value
                updateGravida()
            }
        }
    }

    private fun updateGravida() {
        val gravida = parseSafe(totalBirthCount) + parseSafe(totalMiscarriageCount) + 1
        form.etGravida.setText(gravida.toString())
    }

    private fun parseSafe(value: String): Int = value.toIntOrNull() ?: 0

    private fun EditText.afterTextChanged(action: (String) -> Unit) {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = action(s.toString().trim())
        })
    }

    private fun setupLabourOnsetToggle() {
        form.etSpontaneous.setOnClickListener { selectLabourOnset(form.etSpontaneous, form.etInduced) }
        form.etInduced.setOnClickListener { selectLabourOnset(form.etInduced, form.etSpontaneous) }
    }

    private fun selectLabourOnset(selected: TextView, unselected: TextView) {
        setOptionSelected(selected)
        setOptionUnselected(unselected)
        labourOnset = selected.text.toString()
    }

    private fun setOptionSelected(option: TextView) {
        option.setBackgroundResource(R.drawable.button_primary_rounded)
        option.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun setOptionUnselected(option: TextView) {
        option.setBackgroundResource(R.drawable.button_bg_rounded_corners)
        option.setTextColor(ContextCompat.getColor(this, R.color.darkGray))
    }

    private fun disableSoftInput(vararg fields: EditText) {
        fields.forEach { it.showSoftInputOnFocus = false }
    }

    private fun setupAdmissionDateField() {
        disableSoftInput(form.etAdmissionDate)
        form.etLayoutAdmissionDate.setEndIconOnClickListener { pickAdmissionDate() }
        form.etAdmissionDate.setOnClickListener { pickAdmissionDate() }
    }

    private fun pickAdmissionDate() {
        ObstetricDatePicker.show(this, R.string.select_admission_date, admissionDate) { greg ->
            admissionDate = greg
            form.etAdmissionDate.setText(gregToDisplay(greg))
        }
    }

    private fun setupLabourDiagnosedDateField() {
        disableSoftInput(form.etLaborDiagnosedDate)
        form.etLayoutLaborDiagnosedDate.setEndIconOnClickListener { pickActiveLabourDate() }
        form.etLaborDiagnosedDate.setOnClickListener { pickActiveLabourDate() }
    }

    private fun pickActiveLabourDate() {
        ObstetricDatePicker.show(
            this, R.string.select_labor_diagnosed_date, activeLabourDiagnosedDate
        ) { greg ->
            activeLabourDiagnosedDate = greg
            form.etLaborDiagnosedDate.setText(gregToDisplay(greg))
        }
    }

    private fun setupAdmissionTimeField() {
        form.etLayoutAdmissionTime.setEndIconOnClickListener { pickAdmissionTime() }
        form.etAdmissionTime.setOnClickListener { pickAdmissionTime() }
    }

    private fun pickAdmissionTime() {
        ObstetricTimePicker.show(this) { time ->
            admissionTime = time
            form.etAdmissionTime.setText(time)
        }
    }

    private fun setupLabourDiagnosedTimeField() {
        form.etLayoutLaborDiagnosedTime.setEndIconOnClickListener { pickActiveLabourTime() }
        form.etLaborDiagnosedTime.setOnClickListener { pickActiveLabourTime() }
    }

    private fun pickActiveLabourTime() {
        ObstetricTimePicker.show(this) { time ->
            activeLabourDiagnosedTime = time
            form.etLaborDiagnosedTime.setText(time)
        }
    }

    private fun setupLmpField() {
        disableSoftInput(lmpEdd.etLmp, lmpEdd.etEdd)
        lmpEdd.etLayoutLmp.setEndIconOnClickListener { pickLmpDate() }
        lmpEdd.etLmp.setOnClickListener { pickLmpDate() }
    }

    private fun pickLmpDate() {
        ObstetricDatePicker.show(this, R.string.select_lmp_date, lmpDate) { greg ->
            lmpDate = greg
            lmpEdd.etLmp.setText(gregToDisplay(greg))
            edd = eddFromLmp(greg)
            lmpEdd.etEdd.setText(gregToDisplay(edd))
        }
    }

    override fun getScreenTitle(): Int = R.string.title_activity_admission_data

    companion object {
        private const val EXTRA_PATIENT_UUID = "admission_patient_uuid"
        private const val TAG_BACK_CONFIRMATION = "back_confirmation"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
