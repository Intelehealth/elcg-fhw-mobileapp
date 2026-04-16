package org.intelehealth.ezazi.stage3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.activities.visitSummaryActivity.TimelineVisitSummaryActivity
import org.intelehealth.ezazi.app.AppConstants
import org.intelehealth.ezazi.database.dao.EncounterDAO
import org.intelehealth.ezazi.database.dao.ObsDAO
import org.intelehealth.ezazi.databinding.ActivityWomenDeliveryDetailsBinding
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.stage3.Utils.DeliveryConcept
import org.intelehealth.ezazi.stage3.Utils.DeliveryUIController
import org.intelehealth.ezazi.stage3.db.DeliveryLocalDataSource
import org.intelehealth.ezazi.stage3.db.DeliveryObsMapper
import org.intelehealth.ezazi.stage3.db.DeliveryRepository
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.factory.DeliveryViewModelFactory
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import org.intelehealth.ezazi.stage3.viewmodel.DeliveryViewModel
import org.intelehealth.ezazi.utilities.SessionManager
import org.intelehealth.ezazi.utilities.UuidDictionary
import org.intelehealth.klivekit.utils.DateTimeUtils
import java.util.UUID

class WomenDeliveryDetailsActivity : AppCompatActivity() {
    private val TAG = "WomenDeliveryDetailsAct"
    private lateinit var binding: ActivityWomenDeliveryDetailsBinding
    private lateinit var viewModel: DeliveryViewModel
    private lateinit var uiHandler: DeliveryUIController
    private var visitUuid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWomenDeliveryDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bottomSheetAppBar.toolbar.setTitle(R.string.final_delivery_outcome_form)
        binding.bottomSheetAppBar.toolbar.setNavigationOnClickListener { v ->
            val intent = Intent(this, TimelineVisitSummaryActivity::class.java)////timeline
            startActivity(intent)
            finish()
        }

        visitUuid = intent?.getStringExtra("visitUuid")
        val encounterDto = createEncounterDto()

        val repository = DeliveryRepository(DeliveryLocalDataSource(ObsDAO(), EncounterDAO()), DeliveryObsMapper())
        val useCase = SaveDeliveryDetailsUseCase(repository)

        val factory = DeliveryViewModelFactory(useCase)

        viewModel = ViewModelProvider(this, factory)
            .get(DeliveryViewModel::class.java)
        binding.btnSave.setOnClickListener {
            val deliveryDetails = collectFormData()
            if (validateFields(deliveryDetails)) {
                clearErrors()
                viewModel.saveDelivery(encounterDto, deliveryDetails, SessionManager(this).creatorID)
            }
        }

        setupUIHandler()
        observeViewModel()
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
            setFieldError(
                binding.etlTimeOfDelivery,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        val selectedMode = binding.autotvModeOfDelivery.text?.toString()?.trim()
        val otherText = binding.etModeOfDeliveryOtherOption.text?.toString()?.trim()

        if (selectedMode.isNullOrEmpty()) {
            setFieldError(
                binding.etlModeOfDelivery,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }
        if (selectedMode.equals(getString(R.string.other), ignoreCase = true)) {

            if (otherText.isNullOrEmpty()) {
                setFieldError(
                    binding.etlModeOfDeliveryOtherOption,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }
        }

        if (deliveryDetails.perinealTear.isNullOrEmpty()) {
            binding.tvPerinealTearValidationError.visibility =View.VISIBLE
            return false
        }

        if (deliveryDetails.perinealTear == "Yes" &&
            deliveryDetails.degreeOfPerinealTear.isNullOrEmpty()
        ) {
            setFieldError(binding.etlDegreeOfPerinealTear, getString(R.string.this_field_is_mandatory))
            binding.tvPerinealTearValidationError.visibility =View.GONE
            //binding.tvPerinealTearValidationError.visibility =View.VISIBLE
            return false
        }

        if (deliveryDetails.placentaMembraneStatus.isNullOrEmpty()) {
            setFieldError(
                binding.etlPlacentaMembraneDelivery,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        if (binding.etTimeOfPlacentaDelivery.text.isNullOrEmpty()) {
            setFieldError(
                binding.etlTimeOfPlacentaDelivery,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        if (deliveryDetails.placentalOrCordAbnormality.isNullOrEmpty()) {

            uiHandler.showPlacentalCordError()
            return false
        }

        if (deliveryDetails.placentalOrCordAbnormality == "Yes" &&
            binding.etPlacentalOrCordAbnormalityOtherOption.text.isNullOrEmpty()
        ) {
            binding.etPlacentalOrCordAbnormalityOtherOption.error =
                getString(R.string.this_field_is_mandatory)
            return false
        }

        if (binding.actvAmtsl.text.toString().contains(getString(R.string.other), true)
            && binding.etAmtslOtherOption.text.isNullOrEmpty()
        ) {
            setFieldError(
                binding.etlAmtslOtherOption,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        if (deliveryDetails.typeOfBirth.isNullOrEmpty()) {
            setFieldError(
                binding.etlTypeOfBirth,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        if (deliveryDetails.babyGender.isNullOrEmpty()) {
            setFieldError(
                binding.etlSex,
                getString(R.string.this_field_is_mandatory)
            )
            return false
        }

        //  LIVE BIRTH VALIDATION
        if (deliveryDetails.typeOfBirth.equals("Live Birth", true)) {

            val apgar1 = binding.etApgar1.text.toString()
            val apgar5 = binding.etApgar5.text.toString()

            if (apgar1.isEmpty()) {
                setFieldError(
                    binding.etlApgar1,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

            if (apgar5.isEmpty()) {
                setFieldError(
                    binding.etlApgar5,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

            if (apgar1.toInt() !in 0..10 || apgar5.toInt() !in 0..10) {
                setFieldError(
                    binding.etlApgar1,
                    getString(R.string.error_invalid_apgar_range)
                )
                return false
            }

            if (binding.etBirthWeightGrams.text.isNullOrEmpty()) {
                setFieldError(
                    binding.etlBirthWeightGrams,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

           /* if (deliveryDetails.congenitalAnomalies == "Yes" &&
                deliveryDetails.congenitalAnomalySpecification.isNullOrEmpty()
            ) {
                setFieldError(
                    binding.etlCongenitalYesOptions,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }*/
        }

        if (deliveryDetails.congenitalAnomalies == "Yes") {

            if (binding.autotvCongenitalYesOptions.text.isNullOrEmpty()) {
                setFieldError(
                    binding.etlCongenitalYesOptions,
                    getString(R.string.this_field_is_mandatory)
                )
                return false
            }

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

        if (deliveryDetails.congenitalAnomalies.isNullOrEmpty()) {
            uiHandler.showCongenitalSelectionError()
            return false
        }

        if (deliveryDetails.congenitalAnomalies == "Yes") {

            if (binding.autotvCongenitalYesOptions.text.isNullOrEmpty()) {
                binding.etlCongenitalYesOptions.error =
                    getString(R.string.this_field_is_mandatory)
                return false
            }

            // 3️⃣ If dropdown = Other → text mandatory
            if (binding.autotvCongenitalYesOptions.text.toString()
                    .equals("Other", true)
                && binding.etCongenitalYesOtherOption.text.isNullOrEmpty()
            ) {
                binding.etlCongenitalYesOtherOption.error =
                    getString(R.string.this_field_is_mandatory)
                return false
            }
        }
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
                finish()
            } else {
                Toast.makeText(this, getString(R.string.failed_to_save_details), Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupUIHandler() {
        uiHandler = DeliveryUIController(binding = binding, context = this, fragmentManager = supportFragmentManager)
        uiHandler.initialize()
    }
    fun collectFormData(): DeliveryDetails {
        return DeliveryDetails().apply {
            //Section 1: Woman Delivery Details
            dateOfDelivery = binding.etDateOfDelivery.text.toString().trim()
            timeOfDelivery = binding.etTimeOfDelivery.text.toString().trim()
            modeOfDelivery = uiHandler.handleOtherField(
                keyName = DeliveryConcept.MODE_OF_DELIVERY.name,
                mainValue = binding.autotvModeOfDelivery.text.toString(),
                otherValue = binding.etModeOfDeliveryOtherOption.text.toString()
            )
            perinealTear = uiHandler.getPerinealTearValue()
            degreeOfPerinealTear = binding.autotvDegreeOfPerinealTear.text.toString().trim()

           // Section 2: Placenta &amp; Membrane Delivery Details
            placentaMembraneStatus = binding.autotvPlacentaMembraneDelivery.text.toString().trim()
            timeOfPlacentaDelivery = binding.etTimeOfPlacentaDelivery.text.toString().trim()
            placentalOrCordAbnormality = uiHandler.getPlacentalCordAbnormalityValue()
            modeOfDelivery = uiHandler.handleOtherField(
                keyName = DeliveryConcept.PLACENTA_CORD_ABNORMALITY.name,
                mainValue = placentalOrCordAbnormality,
                otherValue = binding.etPlacentalOrCordAbnormalityOtherOption.text.toString()
            )
            amtsl= handleConditionalField(DeliveryConcept.MEDICATIONS_AMTSL.name, binding.actvAmtsl.text.toString(),  binding.actvAmtsl.text.toString(), binding.actvAmtsl.text.toString())

            // SECTION 3: Newborn Details

            typeOfBirth = binding.actvTypeOfBirth.text.toString().trim()
            babyGender = binding.actvSex.text.toString().trim()
            apgarScore1Min = binding.etApgar1.text.toString().trim()
            apgarScore5Min = binding.etApgar5.text.toString().trim()
            resuscitation = binding.autotvResuscitation.text.toString().trim()
            birthWeightGrams = binding.etBirthWeightGrams.text.toString().trim()
            skinToSkinContact = binding.autotvSkinToSkinContact.text.toString().trim()
            breastfeedWithin1Hour = binding.autotvBreastfeedWithin1Hour.text.toString().trim()
            gestationWeeks = binding.autotvGestation.text.toString().trim()
            congenitalAnomalies= handleConditionalField(DeliveryConcept.CONGENITAL_ANOMALY.name,
                uiHandler.getCongenitalAnomalyValue(),  binding.autotvCongenitalYesOptions.text.toString(),
                binding.etCongenitalYesOtherOption.text.toString())

        }
    }
    fun handleConditionalField(
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
        val intent = Intent(this, TimelineVisitSummaryActivity::class.java) //timeline
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
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
}