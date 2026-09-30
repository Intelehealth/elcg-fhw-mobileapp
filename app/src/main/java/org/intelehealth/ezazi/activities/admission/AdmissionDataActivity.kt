package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.graphics.Point
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.activities.admission.persistence.AdmissionRecord
import org.intelehealth.ezazi.activities.admission.persistence.AdmissionValues
import org.intelehealth.ezazi.activities.admission.persistence.AdmissionWriter
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity
import org.intelehealth.ezazi.activities.admission.validation.AdmissionField
import org.intelehealth.ezazi.activities.admission.validation.AdmissionForm
import org.intelehealth.ezazi.activities.admission.validation.AdmissionValidator
import org.intelehealth.ezazi.activities.admission.validation.Failure
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
import org.intelehealth.ezazi.optimized_sync.OptimizedSyncWorker
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.DateAndTimeUtils
import org.intelehealth.ezazi.utilities.GregorianDateUtils.eddFromLmp
import org.intelehealth.ezazi.utilities.GregorianDateUtils.gregToDisplay
import org.intelehealth.ezazi.utilities.ObstetricDatePicker
import org.intelehealth.ezazi.utilities.ObstetricTimePicker

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding
    private var patientUuid: String = ""
    private var dateOfBirth: String = ""
    private var patientContextLoaded = false
    private var patientContextJob: Job? = null
    private var isWriting = false
    private var patientName: String = ""

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
        setupClearErrorWatchers()
    }

    private fun readIntentExtras() {
        patientUuid = intent.getStringExtra(EXTRA_PATIENT_UUID).orEmpty()
    }

    /**
     * Loads the patient context this screen needs but does not display. The date of birth drives the
     * parity-against-age rule on Save, which must not run while patientContextLoaded is false.
     */
    private fun loadPatientContext() {
        patientContextJob = lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dateOfBirth = PatientsDAO.getDateOfBirth(patientUuid).orEmpty()
                    patientName = PatientsDAO.getFullNameWithMiddle(patientUuid).orEmpty()
                } catch (e: Exception) {
                    Log.e(TAG_ADMISSION, "patient context load failed", e)
                }
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
        if (common.etHospitalOther.text.toString().isNotEmpty()) hospitalMaternity = "other"

        lifecycleScope.launch {
            patientContextJob?.join()

            val snapshot = snapshotForm()

            val failures = validateForm(snapshot)
            if (failures.isNotEmpty()) {
                scrollToFailures(failures)
                return@launch
            }

            totalBirthCount = form.etTotalBirth.text.toString().trim()
            totalMiscarriageCount = form.etTotalMiscarriage.text.toString().trim()
            val total = parseSafe(totalBirthCount) + parseSafe(totalMiscarriageCount)
            val allowed = DateAndTimeUtils.getAgeInYearsOnly(dateOfBirth) - 12

            if (total > allowed) {
                showParityWarningDialog(snapshot)
            } else if (validateGravida(snapshot)) {
                onValidated()
            }
        }
    }

    private fun onValidated() {
        if (isWriting) return
        isWriting = true
        actions.btnNextAddress.isEnabled = false

        val record = buildRecord()
        val values = AdmissionValues.pack(record, getString(R.string.other_risk))

        lifecycleScope.launch {
            val visitUuid = withContext(Dispatchers.IO) {
                try {
                    AdmissionWriter.write(record, values)
                } catch (e: Exception) {
                    Log.e(TAG_ADMISSION, "admission write failed", e)
                    null
                }
            }
            if (visitUuid == null) {
                isWriting = false
                actions.btnNextAddress.isEnabled = true
            } else {
                OptimizedSyncWorker.tempOneTimeWorkRequest(this@AdmissionDataActivity)
                openTimeline(visitUuid)
            }
        }
    }

    private fun openTimeline(visitUuid: String) {
        val intent = Intent(this, TimelineVisitSummaryActivity::class.java).apply {
            putExtra("patientUuid", patientUuid)
            putExtra("visitUuid", visitUuid)
            putExtra("patientNameTimeline", patientName)
            putExtra("providerID", SessionManager(this@AdmissionDataActivity).providerID)
            putExtra("tag", "new")
        }
        startActivity(intent)
        finish()
    }

    private fun buildRecord(): AdmissionRecord {
        val session = SessionManager(this)
        return AdmissionRecord(
            patientUuid = patientUuid,
            providerUuid = session.providerID,
            creatorUuid = session.creatorID,
            locationUuid = session.locationUuid,
            admissionDate = admissionDate,
            admissionTime = admissionTime,
            totalBirthCount = totalBirthCount,
            totalMiscarriageCount = totalMiscarriageCount,
            gravida = form.etGravida.text.toString(),
            labourOnset = labourOnset,
            activeLabourDiagnosedDate = activeLabourDiagnosedDate,
            activeLabourDiagnosedTime = activeLabourDiagnosedTime,
            selectedRuptureMembrane = selectedRuptureMembrane,
            membraneRupturedDate = membraneRupturedDate,
            membraneRupturedTime = membraneRupturedTime,
            riskFactors = riskFactors,
            otherRiskFactorText = common.etOtherRiskFactor.text.toString(),
            hospitalMaternity = hospitalMaternity,
            hospitalOtherText = common.etHospitalOther.text.toString(),
            hospitalId = common.etHospitalId.text.toString(),
            bedNumber = common.etBedNumber.text.toString(),
            primaryDoctorUuid = primaryDoctorUuid,
            primaryDoctorName = common.autotvPrimaryDoctor.text.toString(),
            secondaryDoctorUuid = secondaryDoctorUuid,
            secondaryDoctorName = common.autotvSecondaryDoctor.text.toString(),
            lmpDate = lmpDate,
            edd = edd
        )
    }

    /** Confirm is a second route into the save, so it runs the same Gravida check the normal route runs. */
    private fun showParityWarningDialog(snapshot: AdmissionForm) {
        val dialog = ConfirmationDialogFragment.Builder(this)
            .title(R.string.parity_dialog_warning)
            .content(getString(R.string.parity_dialog_message))
            .positiveButtonLabel(R.string.confirm_and_submit)
            .negativeButtonLabel(R.string.review_details)
            .build()
        dialog.setListener { if (validateGravida(snapshot)) onValidated() }
        dialog.show(supportFragmentManager, TAG_PARITY_WARNING)
    }


    private fun snapshotForm(): AdmissionForm = AdmissionForm(
        admissionDate = admissionDate,
        admissionTime = admissionTime,
        selectedRuptureMembrane = selectedRuptureMembrane,
        membraneRupturedDate = membraneRupturedDate,
        membraneRupturedTime = membraneRupturedTime,
        totalBirth = form.etTotalBirth.text.toString().trim(),
        totalMiscarriage = form.etTotalMiscarriage.text.toString().trim(),
        labourOnset = labourOnset,
        activeLabourDiagnosedDate = activeLabourDiagnosedDate,
        activeLabourDiagnosedTime = activeLabourDiagnosedTime,
        riskFactorsText = common.autotvRiskFactors.text.toString(),
        isOtherRiskFactorVisible = common.llViewOtherRiskFactor.visibility == View.VISIBLE,
        otherRiskFactorText = common.etOtherRiskFactor.text.toString(),
        hospitalMaternity = hospitalMaternity,
        hospitalOtherText = common.etHospitalOther.text.toString(),
        lmpDate = lmpDate,
        primaryDoctorText = common.autotvPrimaryDoctor.text.toString(),
        ruptureMembraneText = form.autotvSacRupturedOptions.text.toString(),
        gravida = form.etGravida.text.toString().trim()
    )

    /** Paints every failure and returns them, so the caller can scroll to one. Empty means valid. */
    private fun validateForm(snapshot: AdmissionForm): List<Failure> {
        hideAllErrorFields()
        resetAllCardStrokes()
        val failures = AdmissionValidator.validate(snapshot)
        failures.forEach { paintFailure(it) }
        return failures
    }

    private fun validateGravida(snapshot: AdmissionForm): Boolean {
        val failure = AdmissionValidator.validateGravida(snapshot)
        if (failure == null) {
            form.tvGravidaError.visibility = View.GONE
            return true
        }
        paintFailure(failure)
        scrollToFailures(listOf(failure))
        return false
    }

    private fun paintFailure(failure: Failure) {
        val message = getString(failure.message) + failure.suffix
        when (failure.field) {
            AdmissionField.ADMISSION_DATE ->
                showError(form.tvAdmissionDateError, form.cardDateAdmission, message)

            AdmissionField.ADMISSION_TIME ->
                showError(form.tvAdmissionTimeError, form.cardTimeAdmission, message)

            AdmissionField.SAC_RUPTURED_DATE ->
                showError(form.tvSacRupturedDateError, form.cardSacRupturedDate, message)

            AdmissionField.SAC_RUPTURED_TIME ->
                showError(form.tvSacRupturedTimeError, form.cardSacRupturedTime, message)

            AdmissionField.TOTAL_BIRTH ->
                showError(form.tvParityDateError, form.cardTotalBirth, message)

            AdmissionField.TOTAL_MISCARRIAGE ->
                showError(form.tvParityTimeError, form.cardTotalMiscarraige, message)

            AdmissionField.LABOUR_DIAGNOSED_DATE ->
                showError(form.tvLabourDiagnosedDateError, form.cardDiagnosedDate, message)

            AdmissionField.LABOUR_DIAGNOSED_TIME ->
                showError(form.tvLabourDiagnosedTimeError, form.cardDiagnosedTime, message)

            AdmissionField.RISK_FACTORS ->
                showError(common.tvErrorRiskFactor, common.dropdownRiskFactors, message)

            AdmissionField.OTHER_RISK_FACTOR ->
                showError(common.tvErrorRiskFactorOther, common.cardOtherRiskFactor, message)

            AdmissionField.HOSPITAL_OTHER ->
                showError(common.tvErrorHospitalOther, common.cardHospitalOther, message)

            AdmissionField.PRIMARY_DOCTOR ->
                showError(common.tvErrorPrimaryDoctor, common.dropdownPrimaryDoctor, message)

            AdmissionField.RUPTURE_MEMBRANE ->
                showError(form.tvErrorSacRupturedMembrane, form.dropdownSacRupturedOptions, message)

            AdmissionField.GRAVIDA -> showError(form.tvGravidaError, null, message)

            AdmissionField.LABOUR_ONSET -> {
                form.tvErrorLabourOnset.visibility = View.VISIBLE
                form.tvErrorLabourOnset.text = message
                form.etSpontaneous.setBackgroundResource(R.drawable.error_bg_et)
                form.etInduced.setBackgroundResource(R.drawable.error_bg_et)
            }

            AdmissionField.HOSPITAL_MATERNITY -> {
                common.tvErrorHospital.visibility = View.VISIBLE
                common.tvErrorHospital.text = message
            }

            AdmissionField.LMP -> {
                lmpEdd.tvLmpError.text = message
                lmpEdd.tvLmpError.visibility = View.VISIBLE
            }
        }
    }

    /** Scrolls to the failing field highest on the form. Rule order is not screen order, so position decides. */
    private fun scrollToFailures(failures: List<Failure>) {
        if (failures.isEmpty()) return
        form.scrollOtherInfo.post {
            val anchor = failures.map { anchorFor(it.field) }
                .minByOrNull { locationOnScreen(it).y } ?: return@post
            val margin = (SCROLL_MARGIN_DP * resources.displayMetrics.density).toInt()
            val top = locationOnScreen(form.scrollOtherInfo).y + form.scrollOtherInfo.paddingTop
            form.scrollOtherInfo.smoothScrollBy(0, locationOnScreen(anchor).y - top - margin)
        }
    }

    /** The card, never the error text: anchoring on the message scrolls the field itself off the top. */
    private fun anchorFor(field: AdmissionField): View = when (field) {
        AdmissionField.ADMISSION_DATE -> form.cardDateAdmission
        AdmissionField.ADMISSION_TIME -> form.cardTimeAdmission
        AdmissionField.TOTAL_BIRTH -> form.cardTotalBirth
        AdmissionField.TOTAL_MISCARRIAGE -> form.cardTotalMiscarraige
        AdmissionField.GRAVIDA -> form.cardGravida
        AdmissionField.LMP -> lmpEdd.root
        AdmissionField.LABOUR_ONSET -> form.cardLabourOnset
        AdmissionField.LABOUR_DIAGNOSED_DATE -> form.cardDiagnosedDate
        AdmissionField.LABOUR_DIAGNOSED_TIME -> form.cardDiagnosedTime
        AdmissionField.RUPTURE_MEMBRANE -> form.textView8
        AdmissionField.SAC_RUPTURED_DATE -> form.cardSacRupturedDate
        AdmissionField.SAC_RUPTURED_TIME -> form.cardSacRupturedTime
        AdmissionField.RISK_FACTORS -> common.dropdownRiskFactors
        AdmissionField.OTHER_RISK_FACTOR -> common.cardOtherRiskFactor
        AdmissionField.HOSPITAL_MATERNITY -> common.cardOptions
        AdmissionField.HOSPITAL_OTHER -> common.cardHospitalOther
        AdmissionField.PRIMARY_DOCTOR -> common.dropdownPrimaryDoctor
    }

    private fun locationOnScreen(view: View): Point {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Point(loc[0], loc[1])
    }

    private fun hideAllErrorFields() {
        listOf(
            form.tvAdmissionDateError,
            form.tvAdmissionTimeError,
            form.tvParityDateError,
            form.tvParityTimeError,
            form.tvErrorLabourOnset,
            form.tvSacRupturedDateError,
            form.tvSacRupturedTimeError,
            form.tvLabourDiagnosedDateError,
            form.tvLabourDiagnosedTimeError,
            common.tvErrorRiskFactor,
            common.tvErrorRiskFactorOther,
            common.tvErrorHospital,
            common.tvErrorHospitalOther,
            common.tvErrorPrimaryDoctor,
            common.tvErrorSecondaryDoctor,
            common.tvErrorBedNumber,
            form.tvGravidaError,
            lmpEdd.tvLmpError,
            lmpEdd.tvEddError
        ).forEach { it.visibility = View.GONE }
    }

    private fun resetAllCardStrokes() {
        val normal = ContextCompat.getColor(this, R.color.colorScrollbar)
        listOf(
            form.cardDateAdmission,
            form.cardTimeAdmission,
            form.cardTotalBirth,
            form.cardTotalMiscarraige,
            form.cardSacRupturedDate,
            form.cardSacRupturedTime,
            common.dropdownPrimaryDoctor,
            common.dropdownSecondaryDoctor,
            form.cardDiagnosedDate,
            form.cardDiagnosedTime,
            common.dropdownRiskFactors,
            common.cardOtherRiskFactor,
            common.cardHospitalOther
        ).forEach { it.setStrokeColor(normal) }
    }

    private fun setupBackButton() {
        actions.btnBackAddress.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBackConfirmation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isWriting) return
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
        labourOnset = if (selected === form.etSpontaneous) ONSET_SPONTANEOUS else ONSET_INDUCED
        form.tvErrorLabourOnset.visibility = View.GONE
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
        hospitalMaternity = when {
            selected === common.optionHospital -> HOSPITAL
            selected === common.optionMaternity -> MATERNITY
            else -> OTHER
        }
        common.tvErrorHospital.visibility = View.GONE
        common.tvErrorHospitalOther.visibility = View.GONE

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
        clearError(form.tvErrorSacRupturedMembrane, form.dropdownSacRupturedOptions)
        if (choice.equals("Known", ignoreCase = true)) {
            form.cardSacRuptured.visibility = View.VISIBLE
        } else {
            form.cardSacRuptured.visibility = View.GONE
            membraneRupturedDate = ""
            membraneRupturedTime = ""
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
            clearError(form.tvSacRupturedDateError, form.cardSacRupturedDate)
        }
    }

    private fun pickSacRupturedTime() {
        ObstetricTimePicker.show(this) { time ->
            membraneRupturedTime = time
            form.etSacRupturedTime.setText(time)
            clearError(form.tvSacRupturedTimeError, form.cardSacRupturedTime)
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
        clearError(common.tvErrorRiskFactor, common.dropdownRiskFactors)
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
            clearError(common.tvErrorPrimaryDoctor, common.dropdownPrimaryDoctor)
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
            clearError(common.tvErrorSecondaryDoctor, common.dropdownSecondaryDoctor)
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

    private fun setupClearErrorWatchers() {
        form.etTotalBirth.afterTextChanged { clearError(form.tvParityDateError, form.cardTotalBirth) }
        form.etTotalMiscarriage.afterTextChanged {
            clearError(form.tvParityTimeError, form.cardTotalMiscarraige)
        }
        common.autotvPrimaryDoctor.afterTextChanged {
            clearError(common.tvErrorPrimaryDoctor, common.dropdownPrimaryDoctor)
        }
        common.autotvSecondaryDoctor.afterTextChanged {
            clearError(common.tvErrorSecondaryDoctor, common.dropdownSecondaryDoctor)
        }
        common.etOtherRiskFactor.afterTextChanged {
            clearError(common.tvErrorRiskFactorOther, common.cardOtherRiskFactor)
        }
        form.etGravida.afterTextChanged { clearError(form.tvGravidaError, null) }
        common.etHospitalOther.afterTextChanged {
            clearError(common.tvErrorHospitalOther, common.cardHospitalOther)
        }
    }

    private fun showError(errorView: TextView?, card: MaterialCardView?, message: String) {
        errorView?.let {
            it.text = message
            it.visibility = View.VISIBLE
        }
        card?.setStrokeColor(ContextCompat.getColor(this, R.color.error_red))
    }

    private fun clearError(errorView: TextView?, card: MaterialCardView?) {
        errorView?.visibility = View.GONE
        card?.setStrokeColor(ContextCompat.getColor(this, R.color.colorScrollbar))
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
            clearError(form.tvAdmissionDateError, form.cardDateAdmission)
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
            clearError(form.tvLabourDiagnosedDateError, form.cardDiagnosedDate)
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
            clearError(form.tvAdmissionTimeError, form.cardTimeAdmission)
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
            clearError(form.tvLabourDiagnosedTimeError, form.cardDiagnosedTime)
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
            clearError(lmpEdd.tvLmpError, null)
            clearError(lmpEdd.tvEddError, null)
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
        private const val TAG_PARITY_WARNING = "parity_warning"
        private const val TAG_ADMISSION = "AdmissionData"

        private const val HOSPITAL = "Hospital"
        private const val MATERNITY = "Maternity"
        private const val OTHER = "Other"
        private const val SCROLL_MARGIN_DP = 16
        private const val ONSET_SPONTANEOUS = "Spontaneous"
        private const val ONSET_INDUCED = "Induced"

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
