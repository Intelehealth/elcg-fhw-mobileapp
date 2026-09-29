package org.intelehealth.ezazi.activities.admission

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.graphics.Point
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
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
import org.intelehealth.ezazi.utilities.DateAndTimeUtils
import org.intelehealth.ezazi.utilities.GregorianDateUtils.GREG_FMT
import org.intelehealth.ezazi.utilities.GregorianDateUtils.eddFromLmp
import org.intelehealth.ezazi.utilities.GregorianDateUtils.gregToDisplay
import org.intelehealth.ezazi.utilities.ObstetricDatePicker
import org.intelehealth.ezazi.utilities.ObstetricTimePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdmissionDataActivity : BaseActionBarActivity() {

    private lateinit var binding: ActivityAdmissionDataBinding
    private var patientUuid: String = ""
    private var dateOfBirth: String = ""
    private var patientContextLoaded = false
    private var patientContextJob: Job? = null
    private var isParityWarningDialogShown = false

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
            dateOfBirth = withContext(Dispatchers.IO) {
                try {
                    PatientsDAO.getDateOfBirth(patientUuid).orEmpty()
                } catch (e: Exception) {
                    ""
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

            if (!areValidFields()) {
                scrollToFocusedItem()
                return@launch
            }

            totalBirthCount = form.etTotalBirth.text.toString().trim()
            totalMiscarriageCount = form.etTotalMiscarriage.text.toString().trim()
            val total = parseSafe(totalBirthCount) + parseSafe(totalMiscarriageCount)
            val allowed = DateAndTimeUtils.getAgeInYearsOnly(dateOfBirth) - 12

            if (total > allowed) {
                isParityWarningDialogShown = true
                showParityWarningDialog()
            } else if (validateGravida()) {
                onValidated()
            }
        }
    }

    private fun onValidated() {
        Toast.makeText(this, "would save | dob=$dateOfBirth", Toast.LENGTH_SHORT).show()
    }

    private fun showParityWarningDialog() {
        val dialog = ConfirmationDialogFragment.Builder(this)
            .title(R.string.parity_dialog_warning)
            .content(getString(R.string.parity_dialog_message))
            .positiveButtonLabel(R.string.confirm_and_submit)
            .negativeButtonLabel(R.string.review_details)
            .build()
        dialog.setListener { onValidated() }
        dialog.show(supportFragmentManager, TAG_PARITY_WARNING)
    }

    private fun validateGravida(): Boolean {
        val value = form.etGravida.text.toString().trim()
        if (value.isEmpty()) {
            showError(form.tvGravidaError, null, getString(R.string.error_gravida_required))
            return false
        }
        val gravida = parseSafe(value)
        if (gravida < 0) {
            showError(form.tvGravidaError, null, getString(R.string.error_gravida_negative))
            return false
        }
        if (gravida > 20) {
            showError(form.tvGravidaError, null, getString(R.string.error_gravida_max_limit))
            return false
        }
        form.tvGravidaError.visibility = View.GONE
        return true
    }

    private fun areValidFields(): Boolean {
        hideAllErrorFields()
        resetAllCardStrokes()
        var isValid = true

        if (admissionDate.isEmpty()) {
            showError(form.tvAdmissionDateError, form.cardDateAdmission, getString(R.string.select_admission_date))
            isValid = false
        } else {
            val admDate = parseGregDate(admissionDate)
            if (admDate == null || isAfterToday(admissionDate)) {
                showError(form.tvAdmissionDateError, form.cardDateAdmission, getString(R.string.select_admission_date))
                isValid = false
            } else {
                val minAdm = startOfDay(Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -10) })
                if (admDate.before(minAdm.time)) {
                    showError(
                        form.tvAdmissionDateError, form.cardDateAdmission,
                        getString(R.string.select_admission_date) + " (max 10 days ago)"
                    )
                    isValid = false
                }
            }
        }

        if (admissionTime.isBlank()) {
            showError(form.tvAdmissionTimeError, form.cardTimeAdmission, getString(R.string.select_admission_time))
            isValid = false
        } else if (admissionDate.isNotEmpty()) {
            val admDt = parseGregDateTime(admissionDate, admissionTime)
            if (admDt != null && admDt.after(Date())) {
                showError(form.tvAdmissionTimeError, form.cardTimeAdmission, getString(R.string.select_admission_time))
                isValid = false
            }
        }

        if (selectedRuptureMembrane.equals("Known", ignoreCase = true)) {
            if (membraneRupturedDate.isEmpty()) {
                showError(
                    form.tvSacRupturedDateError, form.cardSacRupturedDate,
                    getString(R.string.select_sac_ruptured_date)
                )
                isValid = false
            } else if (isAfterToday(membraneRupturedDate)) {
                showError(
                    form.tvSacRupturedDateError, form.cardSacRupturedDate,
                    getString(R.string.sac_ruptured_future_not_allowed)
                )
                isValid = false
            }
            if (membraneRupturedTime.isBlank()) {
                showError(
                    form.tvSacRupturedTimeError, form.cardSacRupturedTime,
                    getString(R.string.select_sac_ruptured_time)
                )
                isValid = false
            } else {
                val rupDt = parseGregDateTime(membraneRupturedDate, membraneRupturedTime)
                if (rupDt != null && rupDt.after(Date())) {
                    showError(
                        form.tvSacRupturedTimeError, form.cardSacRupturedTime,
                        getString(R.string.select_sac_ruptured_time)
                    )
                    isValid = false
                }
            }
        }

        val birthStr = form.etTotalBirth.text.toString().trim()
        if (birthStr.isEmpty()) {
            showError(form.tvParityDateError, form.cardTotalBirth, getString(R.string.total_birth_count_val_txt))
            isValid = false
        } else if (parseSafe(birthStr) > 15) {
            showError(form.tvParityDateError, form.cardTotalBirth, getString(R.string.total_birth_count_limit))
            isValid = false
        }

        val misStr = form.etTotalMiscarriage.text.toString().trim()
        if (misStr.isEmpty()) {
            showError(
                form.tvParityTimeError, form.cardTotalMiscarraige,
                getString(R.string.total_miscarriage_count_val_txt)
            )
            isValid = false
        } else if (parseSafe(misStr) > 8) {
            showError(form.tvParityTimeError, form.cardTotalMiscarraige, getString(R.string.miscarriage_count_limit))
            isValid = false
        }

        if (labourOnset.isEmpty()) {
            form.tvErrorLabourOnset.visibility = View.VISIBLE
            form.tvErrorLabourOnset.text = getString(R.string.labor_onset_val_txt)
            form.etSpontaneous.setBackgroundResource(R.drawable.error_bg_et)
            form.etInduced.setBackgroundResource(R.drawable.error_bg_et)
            isValid = false
        }

        if (activeLabourDiagnosedDate.isEmpty() || isAfterToday(activeLabourDiagnosedDate)) {
            showError(
                form.tvLabourDiagnosedDateError, form.cardDiagnosedDate,
                getString(R.string.active_labor_diagnosed_date_val_txt)
            )
            isValid = false
        }

        if (activeLabourDiagnosedTime.isBlank()) {
            showError(
                form.tvLabourDiagnosedTimeError, form.cardDiagnosedTime,
                getString(R.string.active_labor_diagnosed_time_val_txt)
            )
            isValid = false
        } else if (activeLabourDiagnosedDate.isNotEmpty()) {
            val labDt = parseGregDateTime(activeLabourDiagnosedDate, activeLabourDiagnosedTime)
            if (labDt != null) {
                val min15h = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -15) }
                if (labDt.after(Date()) || labDt.before(min15h.time)) {
                    showError(
                        form.tvLabourDiagnosedTimeError, form.cardDiagnosedTime,
                        getString(R.string.active_labour_diagnosis)
                    )
                    isValid = false
                }
            }
        }

        if (common.autotvRiskFactors.text.toString().isEmpty()) {
            showError(
                common.tvErrorRiskFactor, common.dropdownRiskFactors,
                getString(R.string.please_select_risk_factor)
            )
            isValid = false
        } else if (common.llViewOtherRiskFactor.visibility == View.VISIBLE &&
            common.etOtherRiskFactor.text.toString().isEmpty()
        ) {
            showError(common.tvErrorRiskFactorOther, common.cardOtherRiskFactor, getString(R.string.error_other_risk))
            isValid = false
        }

        if (hospitalMaternity.isEmpty()) {
            common.tvErrorHospital.visibility = View.VISIBLE
            common.tvErrorHospital.text = getString(R.string.hospital_matermnity_val_txt)
            isValid = false
        } else if (!hospitalMaternity.equals("hospital", ignoreCase = true) &&
            !hospitalMaternity.equals("maternity", ignoreCase = true)
        ) {
            if (common.etHospitalOther.text.toString().isEmpty()) {
                showError(
                    common.tvErrorHospitalOther, common.cardHospitalOther,
                    getString(R.string.enter_hospital_other_error)
                )
                isValid = false
            }
        }

        if (lmpDate.isNotEmpty()) {
            val lmp = parseGregDate(lmpDate)
            if (lmp != null) {
                if (isAfterToday(lmpDate)) {
                    lmpEdd.tvLmpError.text = getString(R.string.lmp_future_not_allowed)
                    lmpEdd.tvLmpError.visibility = View.VISIBLE
                    isValid = false
                } else {
                    val min44 = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -44) }
                    if (lmp.before(min44.time)) {
                        lmpEdd.tvLmpError.text = getString(R.string.lmp_range_invalid)
                        lmpEdd.tvLmpError.visibility = View.VISIBLE
                        isValid = false
                    }
                }
            }
        } else {
            lmpEdd.tvLmpError.text = getString(R.string.select_lmp_date)
            lmpEdd.tvLmpError.visibility = View.VISIBLE
            isValid = false
        }

        if (common.autotvPrimaryDoctor.text.toString().isEmpty()) {
            showError(
                common.tvErrorPrimaryDoctor, common.dropdownPrimaryDoctor,
                getString(R.string.select_primary_doctor)
            )
            isValid = false
        }

        if (form.autotvSacRupturedOptions.text.toString().isEmpty()) {
            showError(
                form.tvErrorSacRupturedMembrane, form.dropdownSacRupturedOptions,
                getString(R.string.select_rupture_membrane)
            )
            isValid = false
        }

        return isValid
    }

    private fun startOfDay(cal: Calendar): Calendar = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /**
     * Device-local and lenient=false, matching the Java. Deliberately not GregorianDateUtils, which is
     * UTC-pinned: these comparisons are against a local Calendar and a UTC parse would shift the boundary.
     */
    private fun parseGregDate(dateStr: String?): Date? {
        if (dateStr.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat(GREG_FMT, Locale.ENGLISH).apply { isLenient = false }.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun isAfterToday(dateStr: String?): Boolean {
        val parsed = parseGregDate(dateStr) ?: return false
        val today = startOfDay(Calendar.getInstance())
        val sel = startOfDay(Calendar.getInstance().apply { time = parsed })
        return sel.after(today)
    }

    private fun parseGregDateTime(dateStr: String?, timeStr: String?): Date? {
        if (dateStr.isNullOrEmpty() || timeStr.isNullOrEmpty()) return null
        val combined = dateStr.trim() + " " + timeStr.trim()
        for (fmt in arrayOf("dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm", "dd/MM/yyyy hh:mm")) {
            try {
                SimpleDateFormat(fmt, Locale.ENGLISH).apply { isLenient = false }.parse(combined)
                    ?.let { return it }
            } catch (ignored: Exception) {
            }
        }
        return null
    }

    private fun scrollToFocusedItem() {
        val focused = binding.root.findFocus() ?: return
        val scroll = locationOnScreen(form.scrollOtherInfo)
        val point = locationOnScreen(focused)
        var coord = point.y - scroll.y
        if (coord <= 0) form.scrollOtherInfo.smoothScrollTo(0, 0)
        else if (scroll.y > coord) coord = point.y
        form.scrollOtherInfo.smoothScrollTo(0, coord)
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

        @JvmStatic
        fun newIntent(context: Context, patientUuid: String): Intent =
            Intent(context, AdmissionDataActivity::class.java).apply {
                putExtra(EXTRA_PATIENT_UUID, patientUuid)
            }
    }
}
