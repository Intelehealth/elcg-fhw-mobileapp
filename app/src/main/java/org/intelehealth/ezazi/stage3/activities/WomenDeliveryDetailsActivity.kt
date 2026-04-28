package org.intelehealth.ezazi.stage3.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.database.dao.VisitsDAO
import org.intelehealth.ezazi.databinding.ActivityWomenDeliveryDetailsBinding
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.stage3.Utils.DeliveryDetailsConcept
import org.intelehealth.ezazi.stage3.Utils.DeliveryDetailsUIController
import org.intelehealth.ezazi.stage3.db.DeliveryDetailsLocalDataSource
import org.intelehealth.ezazi.stage3.db.DeliveryDetailsObsMapper
import org.intelehealth.ezazi.stage3.db.DeliveryDetailsRepository
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.factory.DeliveryDetailsViewModelFactory
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import org.intelehealth.ezazi.stage3.viewmodel.DeliveryDetailsViewModel
import org.intelehealth.ezazi.syncModule.SyncUtils
import org.intelehealth.ezazi.ui.dialog.ConfirmationDialogFragment
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.UuidDictionary
import org.intelehealth.klivekit.utils.DateTimeUtils
import java.util.UUID

class WomenDeliveryDetailsActivity : AppCompatActivity() {
    private val TAG = "WomenDeliveryDetailsAct"
    private lateinit var binding: ActivityWomenDeliveryDetailsBinding
    private lateinit var viewModel: DeliveryDetailsViewModel
    private lateinit var uiHandler: DeliveryDetailsUIController
    private var visitUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWomenDeliveryDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomSheetAppBar.toolbar.setTitle(R.string.final_delivery_outcome_form)

        visitUuid = intent?.getStringExtra("visitUuid")

        binding.bottomSheetAppBar.toolbar.setNavigationOnClickListener { v ->
            showBackConfirmationDialog()
        }


        val encounterDto = createEncounterDto()

        val repository = DeliveryDetailsRepository(DeliveryDetailsLocalDataSource(ObsDAO(), EncounterDAO(), VisitsDAO()), DeliveryDetailsObsMapper(), SyncUtils())
        val useCase = SaveDeliveryDetailsUseCase(repository)

        val factory = DeliveryDetailsViewModelFactory(useCase)

        viewModel = ViewModelProvider(this, factory)
            .get(DeliveryDetailsViewModel::class.java)
        binding.btnSave.setOnClickListener {
            val deliveryDetails = collectFormData()
            if (validateFields(deliveryDetails)) {
                clearErrors()
                viewModel.saveDelivery(encounterDto, deliveryDetails, SessionManager(this).creatorID, "Stage3_Hour1_1")
            }
        }

        setupUIHandler()
        observeViewModel()

        onBackPressedDispatcher.addCallback(
            this, object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    showBackConfirmationDialog()
                }
            }
        )
    }

    private fun setFieldError(layout: TextInputLayout, message: String) {
        layout.isFocusable = true
        layout.isFocusableInTouchMode = true
        layout.requestFocus()
        layout.error = message
    }

    private fun validateFields(deliveryDetails: DeliveryDetails): Boolean {
        // Clear previous errors first
        clearErrors()

        if (binding.etDateOfDelivery.text.isNullOrEmpty()) {
            setFieldError(binding.etlDateOfDelivery, getString(R.string.this_field_is_mandatory))
            return false
        }

        if (binding.etTimeOfDelivery.text.isNullOrEmpty()) {
            setFieldError(binding.etlTimeOfDelivery, getString(R.string.this_field_is_mandatory))
            return false
        }

        val selectedMode = binding.autotvModeOfDelivery.text?.toString()?.trim()
        val otherText = binding.etModeOfDeliveryOtherOption.text?.toString()?.trim()

        if (selectedMode.isNullOrEmpty()) {
            setFieldError(binding.etlModeOfDelivery, getString(R.string.this_field_is_mandatory))
            return false
        }
        if (selectedMode.equals(getString(R.string.other), ignoreCase = true)) {
            if (otherText.isNullOrEmpty()) {
                setFieldError(binding.etlModeOfDeliveryOtherOption, getString(R.string.this_field_is_mandatory))
                return false
            }
        }
        if (!validatePerinealTear()) return false

        if (deliveryDetails.placentaMembraneStatus.isNullOrEmpty()) {
            setFieldError(binding.etlPlacentaMembraneDelivery, getString(R.string.this_field_is_mandatory))
            return false
        }

        if (binding.etTimeOfPlacentaDelivery.text.isNullOrEmpty()) {
            setFieldError(binding.etlTimeOfPlacentaDelivery, getString(R.string.this_field_is_mandatory))
            return false
        }
        // ADD after line 116:
        val deliveryTime = binding.etTimeOfDelivery.text.toString().trim()
        val placentaTime = binding.etTimeOfPlacentaDelivery.text.toString().trim()
        if (deliveryTime.isNotEmpty() && placentaTime.isNotEmpty()) {
            if (!isTimeAfterOrEqual(placentaTime, deliveryTime)) {
                setFieldError(binding.etlTimeOfPlacentaDelivery,
                    getString(R.string.error_placenta_time_after_delivery))
                return false
            }
        }

        if (!validatePlacentalAbnormality()) return false


        if (binding.actvAmtsl.text.isNullOrEmpty()) {
            setFieldError(binding.etlAmtsl, getString(R.string.this_field_is_mandatory))
            return false
        }
        if (binding.actvAmtsl.text.toString().contains(getString(R.string.other), true)
            && binding.etAmtslOtherOption.text.isNullOrEmpty()) {
            setFieldError(binding.etlAmtslOtherOption, getString(R.string.this_field_is_mandatory))
            return false
        }

        if (deliveryDetails.typeOfBirth.isNullOrEmpty()) {
            setFieldError(binding.etlTypeOfBirth, getString(R.string.this_field_is_mandatory))
            return false
        }

        if (deliveryDetails.babyGender.isNullOrEmpty()) {
            setFieldError(binding.etlSex, getString(R.string.this_field_is_mandatory))
            return false
        }

        //  LIVE BIRTH VALIDATION
        if (deliveryDetails.typeOfBirth.equals("Live Birth", true)) {
            val apgar1 = binding.autotvApgar1.text.toString()
            val apgar5 = binding.autotvApgar5.text.toString()

            if (apgar1.isEmpty()) {
                setFieldError(binding.etlApgar1, getString(R.string.this_field_is_mandatory))
                return false
            }

            if (apgar5.isEmpty()) {
                setFieldError(binding.etlApgar5, getString(R.string.this_field_is_mandatory))
                return false
            }

            if (apgar1.toInt() !in 0..10) {
                setFieldError(binding.etlApgar1, getString(R.string.error_invalid_apgar_range))
                return false
            }
            if (apgar5.toInt() !in 0..10) {
                setFieldError(binding.etlApgar5, getString(R.string.error_invalid_apgar_range))
                return false
            }
            if (binding.autotvResuscitation.text.isNullOrEmpty()) {
                setFieldError(binding.etlResuscitation, getString(R.string.this_field_is_mandatory))
                return false
            }
            if (binding.etBirthWeightGrams.text.isNullOrEmpty()) {
                setFieldError(binding.etlBirthWeightGrams, getString(R.string.this_field_is_mandatory))
                return false
            }


            if (binding.autotvSkinToSkinContact.text.isNullOrEmpty()) {
                setFieldError(binding.etlSkinToSkinContact, getString(R.string.this_field_is_mandatory))
                return false
            }

            if (binding.autotvBreastfeedWithin1Hour.text.isNullOrEmpty()) {
                setFieldError(binding.etlBreastfeedWithin1Hour, getString(R.string.this_field_is_mandatory))
                return false
            }

            if (binding.autotvGestation.text.isNullOrEmpty()) {
                setFieldError(binding.etlGestation, getString(R.string.this_field_is_mandatory))
                return false
            }
        }
        if (!validateCongenital()) return false

        return true
    }

    private fun clearErrors() {
        binding.etlDateOfDelivery.error = null
        binding.etlTimeOfDelivery.error = null
        binding.etlModeOfDelivery.error = null
        binding.etlDegreeOfPerinealTear.error = null
        binding.etlPlacentaMembraneDelivery.error = null
        binding.etlTimeOfPlacentaDelivery.error = null
        binding.etlAmtsl.error = null
        binding.etlTypeOfBirth.error = null
        binding.etlSex.error = null
        binding.etlApgar1.error = null
        binding.etlApgar5.error = null
        binding.etlBirthWeightGrams.error = null
        binding.etlCongenitalYesOptions.error = null
        binding.tvPerinealTearValidationError.visibility =View.GONE
        binding.tvCordAbnormalityValidationError.visibility =View.GONE
    }

    private fun observeViewModel() {

        viewModel.saveResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.delivery_outcome_msg), Toast.LENGTH_SHORT).show()
                val syncUtils = SyncUtils()
                syncUtils.syncForeground("visitSummary")
                finish()
            } else {
                Toast.makeText(this, getString(R.string.failed_to_save_details), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupUIHandler() {
        uiHandler = DeliveryDetailsUIController(binding = binding, context = this, fragmentManager = supportFragmentManager)
        uiHandler.initialize()
    }
    private fun collectFormData(): DeliveryDetails {
        return DeliveryDetails().apply {
            //Section 1: Woman Delivery Details
            dateOfDelivery = binding.etDateOfDelivery.text.toString().trim()
            timeOfDelivery = binding.etTimeOfDelivery.text.toString().trim()
            modeOfDelivery = uiHandler.handleOtherField(
                keyName = DeliveryDetailsConcept.MODE_OF_DELIVERY.name,
                mainValue = binding.autotvModeOfDelivery.text.toString(),
                otherValue = binding.etModeOfDeliveryOtherOption.text.toString()
            )
            perinealTear = uiHandler.getYesNoValue(binding.layoutPernealTearRadio.radioYesNoGroupCommon)
            degreeOfPerinealTear = binding.autotvDegreeOfPerinealTear.text.toString().trim()

           // Section 2: Placenta &amp; Membrane Delivery Details
            placentaMembraneStatus = binding.autotvPlacentaMembraneDelivery.text.toString().trim()
            timeOfPlacentaDelivery = binding.etTimeOfPlacentaDelivery.text.toString().trim()
            placentalOrCordAbnormality = uiHandler.getYesNoValue(binding.layoutPlacentalAbnormalityRadio.radioYesNoGroupCommon)
            modeOfDelivery = uiHandler.handleOtherField(
                keyName = DeliveryDetailsConcept.PLACENTA_CORD_ABNORMALITY.name,
                mainValue = placentalOrCordAbnormality,
                otherValue = binding.etPlacentalOrCordAbnormalityOtherOption.text.toString()
            )

            amtsl= handleConditionalField(DeliveryDetailsConcept.MEDICATIONS_AMTSL.name, binding.actvAmtsl.text.toString(),  binding.actvAmtsl.text.toString(), binding.actvAmtsl.text.toString())

            // SECTION 3: Newborn Details

            typeOfBirth = binding.actvTypeOfBirth.text.toString().trim()
            babyGender = binding.actvSex.text.toString().trim()
            apgarScore1Min = binding.autotvApgar1.text.toString().trim()
            apgarScore5Min = binding.autotvApgar5.text.toString().trim()
            resuscitation = binding.autotvResuscitation.text.toString().trim()
            birthWeightGrams = binding.etBirthWeightGrams.text.toString().trim()
            skinToSkinContact = binding.autotvSkinToSkinContact.text.toString().trim()
            breastfeedWithin1Hour = binding.autotvBreastfeedWithin1Hour.text.toString().trim()
            gestationWeeks = binding.autotvGestation.text.toString().trim()
            congenitalAnomalies= handleConditionalField(DeliveryDetailsConcept.CONGENITAL_ANOMALY.name,
                uiHandler.getYesNoValue(binding.layoutCongenitalAnomaliesRadio.radioYesNoGroupCommon),  binding.autotvCongenitalYesOptions.text.toString(),
                binding.etCongenitalYesOtherOption.text.toString())

        }
    }
    private fun handleConditionalField(
        keyName: String,              // e.g. "complications"
        yesNoValue: String?,          // "Yes" / "No"
        selectedOptionsString: String?, // comma-separated values
        otherValue: String?           // free text
    ): String {

        val cleanYesNo = yesNoValue?.trim()
        if (cleanYesNo.isNullOrEmpty()) return ""

        val jsonParts = mutableListOf<String>()

        // Add Yes/No
        jsonParts.add(""""$keyName": "$cleanYesNo"""")

        if (cleanYesNo.equals("Yes", ignoreCase = true)) {

            // Convert comma string to list
            val selectedOptions = selectedOptionsString
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }

            // Add list if exists
            if (!selectedOptions.isNullOrEmpty()) {
                val formattedList = selectedOptions.joinToString(", ") {
                    """"$it""""
                }
                jsonParts.add(""""$keyName": [$formattedList]""")
            }

            // Add other text if exists
            val cleanOther = otherValue?.trim()
            if (!cleanOther.isNullOrEmpty()) {
                jsonParts.add(""""other_text": "$cleanOther"""")
            }
        }

        return "{ ${jsonParts.joinToString(", ")} }"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showBackConfirmationDialog()
    }

    private fun createEncounterDto(): EncounterDTO {
        return EncounterDTO().apply {
            uuid = UUID.randomUUID().toString()
            visituuid = visitUuid
            encounterTime = DateTimeUtils.getCurrentDateInUTC(AppConstants.UTC_FORMAT)
            provideruuid = SessionManager(this@WomenDeliveryDetailsActivity).providerID
            encounterTypeUuid = UuidDictionary.DELIVERY_OUTCOME_STAGE3
            syncd = false
            voided = 0
            privacynotice_value = "true"
        }
    }
    private fun validatePerinealTear(): Boolean {

        // 1️⃣ Yes/No selected?
        if (binding.layoutPernealTearRadio.radioYesNoGroupCommon.checkedRadioButtonId == -1) {
            binding.tvPerinealTearValidationError.visibility = View.VISIBLE
            binding.tvPerinealTearValidationError.text =
                getString(R.string.this_field_is_mandatory)
            return false
        }

        // Clear error if selected
        binding.tvPerinealTearValidationError.visibility = View.GONE

        // 2️⃣ If YES selected → Degree mandatory
        if (binding.layoutPernealTearRadio.radioYesCommon.isChecked) {

            if (binding.autotvDegreeOfPerinealTear.text.isNullOrEmpty()) {
                setFieldError(
                    binding.etlDegreeOfPerinealTear,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

            binding.etlDegreeOfPerinealTear.error = null
        }

        // 3️⃣ If everything valid
        return true
    }
    private fun validatePlacentalAbnormality(): Boolean {
        val radioGroup = binding.layoutPlacentalAbnormalityRadio.radioYesNoGroupCommon
        val radioYes = binding.layoutPlacentalAbnormalityRadio.radioYesCommon

        // Optional — skip if nothing selected
        if (radioGroup.checkedRadioButtonId == -1) return true

        binding.tvCordAbnormalityValidationError.visibility = View.GONE

        // If YES → free text mandatory
        if (radioYes.isChecked &&
            binding.etPlacentalOrCordAbnormalityOtherOption.text.isNullOrEmpty()) {
            setFieldError(binding.etlPlacentalOrCordAbnormalityOtherOption,
                getString(R.string.this_field_is_mandatory))
            return false
        }
        return true
    }

    private fun validateCongenital(): Boolean {
        val radioGroup = binding.layoutCongenitalAnomaliesRadio.radioYesNoGroupCommon
        // yes/no
        if (radioGroup.checkedRadioButtonId == -1) {
            uiHandler.showCongenitalSelectionError()
            return false
        }

        // yes- dropdown
        if (binding.layoutCongenitalAnomaliesRadio.radioYesCommon.isChecked) {
            if (binding.autotvCongenitalYesOptions.text.isNullOrEmpty()) {
                setFieldError(
                    binding.etlCongenitalYesOptions,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

            // dropdown - Other option - text mandatory
            if (binding.autotvCongenitalYesOptions.text.toString()
                    .equals(getString(R.string.other), true)
                && binding.etCongenitalYesOtherOption.text.isNullOrEmpty()
            ) {
                setFieldError(
                    binding.etlCongenitalYesOtherOption,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }
        }
        return true
    }
    private fun isTimeAfterOrEqual(laterTime: String, earlierTime: String): Boolean {
        return try {
            val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val t1 = format.parse(earlierTime)
            val t2 = format.parse(laterTime)
            t2 != null && t1 != null && !t2.before(t1)
        } catch (e: Exception) { true } // don't block if parsing fails
    }
    private fun navigateToTimeline() {
        val patientUuid = intent?.getStringExtra("patientUuid")
        val patientName = intent?.getStringExtra("patientName")
        val providerID = intent?.getStringExtra("providerID")
        //val tag = intent?.getStringExtra("deliveryOutcome")

        val intent = Intent(this, TimelineVisitSummaryActivity::class.java).apply {
            putExtra("patientNameTimeline", patientName)
            putExtra("patientUuid", patientUuid)
            putExtra("visitUuid", visitUuid)
            putExtra("providerID", providerID)
            putExtra("tag", "timline")

            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(intent)
        finish()
    }
    private fun showBackConfirmationDialog() {
        val dialog = ConfirmationDialogFragment.Builder(this)
            .content(getString(R.string.are_you_want_go_back))
            .positiveButtonLabel(R.string.yes)
            .build().apply {
                setListener {
                    navigateToTimeline()
                }
            }
        dialog.show(supportFragmentManager, dialog::class.java.canonicalName)
    }
}