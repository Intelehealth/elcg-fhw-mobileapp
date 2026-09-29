package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.PatientsDAO
import org.intelehealth.ezazi.database.dao.ProviderDAO
import org.intelehealth.ezazi.models.dto.ProviderDTO
import org.intelehealth.ezazi.databinding.ActivityAdmissionDataBinding
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment
import org.intelehealth.ezazi.ui.dialog.SingleChoiceDialogFragment
import org.intelehealth.ezazi.ui.dialog.model.SingChoiceItem
import org.intelehealth.ezazi.ui.shared.BaseActionBarActivity
import org.intelehealth.ezazi.ui.validation.FirstLetterUpperCaseInputFilter
import org.intelehealth.ezazi.ui.dialog.adapter.RiskFactorMultiChoiceAdapter
import org.intelehealth.ezazi.utilities.SessionManager
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
    private var hospitalMaternity: String = ""
    private var selectedRuptureMembrane: String = ""
    private var membraneRupturedTime: String = ""
    private var riskFactors: String = ""
    private var providerDoctorList: List<ProviderDTO> = emptyList()
    private var primaryDoctorUuid: String = ""
    private var secondaryDoctorUuid: String = ""

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
        setupHospitalMaternityToggle()
        setupHospitalOtherInput()
        setupRuptureMembraneField()
        setupSacRupturedFields()
        setupRiskFactorsField()
        loadDoctorList()
        setupPrimaryDoctorField()
        setupSecondaryDoctorField()
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

    private fun setupHospitalMaternityToggle() {
        common.optionHospital.setOnClickListener { selectHospitalMaternity(common.optionHospital) }
        common.optionMaternity.setOnClickListener { selectHospitalMaternity(common.optionMaternity) }
        common.optionOther.setOnClickListener { selectHospitalMaternity(common.optionOther) }
    }

    private fun selectHospitalMaternity(selected: TextView) {
        listOf(common.optionHospital, common.optionMaternity, common.optionOther).forEach {
            if (it === selected) setOptionSelected(it) else setOptionUnselected(it)
        }
        hospitalMaternity = selected.text.toString()

        if (selected === common.optionOther) {
            common.cardHospitalOther.visibility = View.VISIBLE
            common.etHospitalOther.visibility = View.VISIBLE
        } else {
            common.cardHospitalOther.visibility = View.GONE
            common.etHospitalOther.setText("")
        }
    }

    private fun setupRuptureMembraneField() {
        form.etLayoutSacRupturedOptions.setEndIconOnClickListener { selectRuptureMembrane() }
        form.autotvSacRupturedOptions.setOnClickListener { selectRuptureMembrane() }
    }

    private fun selectRuptureMembrane() {
        val items = ArrayList<SingChoiceItem>()
        resources.getStringArray(R.array.rupture_membrane_options).forEachIndexed { index, option ->
            items.add(SingChoiceItem().apply {
                item = option
                itemId = index.toString()
                itemIndex = index
            })
        }
        val dialog = SingleChoiceDialogFragment.Builder(this)
            .title(R.string.select_rupture_membrane)
            .positiveButtonLabel(R.string.save_button)
            .content(items)
            .build()
        dialog.isSearchable(false)
        dialog.setListener { chosen -> applyRuptureMembrane(chosen.item) }
        dialog.show(supportFragmentManager, TAG_RUPTURE_MEMBRANE)
    }

    private fun applyRuptureMembrane(choice: String) {
        selectedRuptureMembrane = choice
        form.autotvSacRupturedOptions.setText(choice)
        if (choice.equals("Known", ignoreCase = true)) {
            form.cardSacRuptured.visibility = View.VISIBLE
        } else {
            form.cardSacRuptured.visibility = View.GONE
            form.etSacRupturedDate.setText("")
            form.etSacRupturedTime.setText("")
        }
    }

    private fun setupSacRupturedFields() {
        disableSoftInput(form.etSacRupturedDate)
        form.etLayoutSacRupturedDate.setEndIconOnClickListener { pickSacRupturedDate() }
        form.etSacRupturedDate.setOnClickListener { pickSacRupturedDate() }
        form.etLayoutSacRupturedTime.setEndIconOnClickListener { pickSacRupturedTime() }
        form.etSacRupturedTime.setOnClickListener { pickSacRupturedTime() }
    }

    private fun pickSacRupturedDate() {
        ObstetricDatePicker.show(
            this, R.string.select_sac_ruptured_date, membraneRupturedDate
        ) { greg ->
            membraneRupturedDate = greg
            form.etSacRupturedDate.setText(gregToDisplay(greg))
        }
    }

    private fun pickSacRupturedTime() {
        ObstetricTimePicker.show(this) { time ->
            membraneRupturedTime = time
            form.etSacRupturedTime.setText(time)
        }
    }

    private fun setupRiskFactorsField() {
        common.etLayoutRiskFactors.setEndIconOnClickListener { selectRiskFactors() }
        common.autotvRiskFactors.setOnClickListener { selectRiskFactors() }
    }

    private fun selectRiskFactors() {
        val dialog = MultiChoiceDialogFragment.Builder<String>(this)
            .title(R.string.select_risk_factors)
            .positiveButtonLabel(R.string.save_button)
            .build()
        dialog.isSearchable(true)
        val options = resources.getStringArray(R.array.risk_factors).toCollection(ArrayList())
        dialog.setAdapter(RiskFactorMultiChoiceAdapter(this, options))
        dialog.setListener { selected -> applyRiskFactors(selected) }
        dialog.show(supportFragmentManager, TAG_RISK_FACTORS)
    }

    private fun applyRiskFactors(selected: List<String>) {
        if (selected.isEmpty()) return
        val other = getString(R.string.other_risk)
        common.llViewOtherRiskFactor.visibility =
            if (selected.contains(other)) View.VISIBLE else View.GONE
        riskFactors = selected.joinToString(", ")
        common.autotvRiskFactors.setText(riskFactors)
    }

    private fun loadDoctorList() {
        val locationUuid = SessionManager(this).locationUuid
        lifecycleScope.launch {
            providerDoctorList = withContext(Dispatchers.IO) {
                try {
                    ProviderDAO().getDoctorList(locationUuid)
                } catch (e: Exception) {
                    emptyList<ProviderDTO>()
                }
            }
        }
    }

    private fun setupPrimaryDoctorField() {
        common.etLayoutPrimaryDoctor.setEndIconOnClickListener { selectPrimaryDoctor() }
        common.autotvPrimaryDoctor.setOnClickListener { selectPrimaryDoctor() }
    }

    private fun selectPrimaryDoctor() {
        val items = ArrayList<SingChoiceItem>()
        providerDoctorList.filter { it.userUuid != secondaryDoctorUuid }
            .forEachIndexed { index, provider ->
                items.add(SingChoiceItem().apply {
                    item = provider.givenName + " " + provider.familyName
                    itemId = provider.userUuid
                    itemIndex = index
                })
            }
        val dialog = SingleChoiceDialogFragment.Builder(this)
            .title(R.string.select_primary_doctor)
            .positiveButtonLabel(R.string.save_button)
            .content(items)
            .build()
        dialog.isSearchable(true)
        dialog.setListener { chosen ->
            primaryDoctorUuid = chosen.itemId
            common.autotvPrimaryDoctor.setText(chosen.item)
        }
        dialog.show(supportFragmentManager, TAG_PRIMARY_DOCTOR)
    }

    private fun setupSecondaryDoctorField() {
        common.etLayoutSecondaryDoctor.setEndIconOnClickListener { selectSecondaryDoctor() }
        common.autotvSecondaryDoctor.setOnClickListener { selectSecondaryDoctor() }
    }

    private fun selectSecondaryDoctor() {
        if (primaryDoctorUuid.isEmpty()) {
            Toast.makeText(this, "Please select the primary doctor", Toast.LENGTH_SHORT).show()
            return
        }
        val items = ArrayList<SingChoiceItem>()
        items.add(SingChoiceItem().apply {
            item = AppConstants.NOT_APPLICABLE_FULL_TEXT
            itemId = AppConstants.NOT_APPLICABLE
            itemIndex = 0
        })
        providerDoctorList.filter { it.userUuid != primaryDoctorUuid }
            .forEachIndexed { index, provider ->
                items.add(SingChoiceItem().apply {
                    item = provider.givenName + " " + provider.familyName
                    itemId = provider.userUuid
                    itemIndex = index + 1
                    isSelected = secondaryDoctorUuid == provider.userUuid
                })
            }
        val dialog = SingleChoiceDialogFragment.Builder(this)
            .title(R.string.select_secondary_doctor)
            .positiveButtonLabel(R.string.save_button)
            .content(items)
            .build()
        dialog.isSearchable(true)
        dialog.setListener { chosen ->
            secondaryDoctorUuid = chosen.itemId
            common.autotvSecondaryDoctor.setText(chosen.item)
        }
        dialog.show(supportFragmentManager, TAG_SECONDARY_DOCTOR)
    }

    private fun setupHospitalOtherInput() {
        common.etHospitalOther.filters = arrayOf(FirstLetterUpperCaseInputFilter())
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
        private const val TAG_RUPTURE_MEMBRANE = "rupture_membrane"
        private const val TAG_RISK_FACTORS = "risk_factors"
        private const val TAG_PRIMARY_DOCTOR = "primary_doctor"
        private const val TAG_SECONDARY_DOCTOR = "secondary_doctor"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
