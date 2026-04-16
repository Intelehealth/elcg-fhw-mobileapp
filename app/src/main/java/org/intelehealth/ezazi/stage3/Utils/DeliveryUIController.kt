package org.intelehealth.ezazi.stage3.Utils

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.databinding.ActivityWomenDeliveryDetailsBinding
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import org.intelehealth.ezazi.ui.dialog.CalendarDialog
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment
import org.intelehealth.ezazi.ui.dialog.ThemeTimePickerDialog
import org.intelehealth.ezazi.utilities.Utils

class DeliveryUIController(
    private val binding: ActivityWomenDeliveryDetailsBinding,
    private val context: Context,
    private val fragmentManager: FragmentManager
) {
    private lateinit var deliveryDetails: DeliveryDetails;

    fun initialize() {
         deliveryDetails = DeliveryDetails()

        setDropdownsData()
        setupPerinealTearButtons()
        setupCongenitalButtons()
        setupPlacentalAbnormalityButtons()
        handleClickListeners()
    }

    private fun setDropdownsData() {
        deliveryDetails = DeliveryDetails()

        setupModeOfDeliveryDropdown()
        setupTypeOfBirthDropdown()
        setupBabyGenderDropdown()
        setupGestationDropdown()
        setupSkinToSkinContactDropdown()
        setupBreastFedInOurDropdown()
        setupCongenitalAnomaliesDropdown()
        setupPlacentaAndMembranesDropdown();
        setupResuscitationDropdown()

        handleTextwatcher()
    }
    private fun setupPlacentaAndMembranesDropdown() {
        val placentaAndMembranesOptions = context.resources.getStringArray(R.array.placenta_and_membranes)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, placentaAndMembranesOptions)

        binding.autotvPlacentaMembraneDelivery.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvPlacentaMembraneDelivery.setAdapter(labourCompletedAdapter)

        binding.autotvPlacentaMembraneDelivery.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.placentaMembraneStatus = parent.getItemAtPosition(position).toString()
            binding.autotvPlacentaMembraneDelivery.tag = deliveryDetails.placentaMembraneStatus
            clearTextInputError(binding.etlPlacentaMembraneDelivery)
        }
    }
    private fun setupTypeOfBirthDropdown() {
        val typeOfBirthsOptionsList = context.resources.getStringArray(R.array.types_of_birth)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, typeOfBirthsOptionsList)

        binding.actvTypeOfBirth.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.actvTypeOfBirth.setAdapter(labourCompletedAdapter)

        binding.actvTypeOfBirth.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.typeOfBirth = parent.getItemAtPosition(position).toString()
            binding.actvTypeOfBirth.tag = deliveryDetails.typeOfBirth
            handleLiveBirthUI(deliveryDetails.typeOfBirth)
            clearTextInputError(binding.etlTypeOfBirth)
        }
    }
    private fun setupBabyGenderDropdown() {
        val babyGenderList = context.resources.getStringArray(R.array.baby_gender)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, babyGenderList)

        binding.actvSex.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.actvSex.setAdapter(labourCompletedAdapter)

        binding.actvSex.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.babyGender = parent.getItemAtPosition(position).toString()
            binding.actvSex.tag = deliveryDetails.babyGender
            clearTextInputError(binding.etlSex)
        }
    }
    private fun setupGestationDropdown() {
        val gestationWeeksList = context.resources.getStringArray(R.array.gestation_options)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, gestationWeeksList)

        binding.autotvGestation.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvGestation.setAdapter(labourCompletedAdapter)

        binding.autotvGestation.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.gestationWeeks = parent.getItemAtPosition(position).toString()
            binding.autotvGestation.tag = deliveryDetails.gestationWeeks
            clearTextInputError(binding.etlGestation)
        }
    }

    private fun setupSkinToSkinContactDropdown() {
        val skinToSkinOptionsList = context.resources.getStringArray(R.array.yes_no_options_array)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, skinToSkinOptionsList)

        binding.autotvSkinToSkinContact.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvSkinToSkinContact.setAdapter(labourCompletedAdapter)

        binding.autotvSkinToSkinContact.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.skinToSkinContact = parent.getItemAtPosition(position).toString()
            binding.autotvSkinToSkinContact.tag = deliveryDetails.skinToSkinContact
            clearTextInputError(binding.etlSkinToSkinContact)
        }
    }
    private fun setupBreastFedInOurDropdown() {
        val breastfedWithinHourOptions = context.resources.getStringArray(R.array.yes_no_options_array)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, breastfedWithinHourOptions)

        binding.autotvBreastfeedWithin1Hour.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvBreastfeedWithin1Hour.setAdapter(labourCompletedAdapter)

        binding.autotvBreastfeedWithin1Hour.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.breastfeedWithin1Hour = parent.getItemAtPosition(position).toString()
            binding.autotvBreastfeedWithin1Hour.tag = deliveryDetails.breastfeedWithin1Hour
            clearTextInputError(binding.etlBreastfeedWithin1Hour)
        }
    }
    private fun setupCongenitalAnomaliesDropdown() {
        val congenitalAnomaliesOptionsList = context.resources.getStringArray(R.array.congenital_anomalies_options)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, congenitalAnomaliesOptionsList)

        binding.autotvCongenitalYesOptions.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvCongenitalYesOptions.setAdapter(labourCompletedAdapter)

        binding.autotvCongenitalYesOptions.setOnItemClickListener { parent, _, position, _ ->

            Utils.hideKeyboard(context as AppCompatActivity)

            deliveryDetails.congenitalAnomalies = parent.getItemAtPosition(position).toString()
            val otherString = context.getString(R.string.other).lowercase()
            val mode = deliveryDetails.congenitalAnomalies

            if (!mode.isNullOrEmpty() &&
                mode.equals(otherString, ignoreCase = true)
            ) {
                // enableAndDisableAllFields(false)
                changeOtherInputEnableStatus(binding.etlCongenitalYesOptions, true, null, false)
            } else {
                //enableAndDisableAllFields(true)
                changeOtherInputEnableStatus(binding.etlCongenitalYesOptions, false, null, false)
            }
            binding.autotvCongenitalYesOptions.tag = deliveryDetails.congenitalAnomalies
            binding.etlCongenitalYesOptions.error = null
        }
    }
    private fun setupModeOfDeliveryDropdown() {
        val modeOfDeliveryList = context.resources.getStringArray(R.array.mode_of_delivery_array)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, modeOfDeliveryList)

        binding.autotvModeOfDelivery.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvModeOfDelivery.setAdapter(labourCompletedAdapter)

        binding.autotvModeOfDelivery.setOnItemClickListener { parent, _, position, _ ->

            Utils.hideKeyboard(context as AppCompatActivity)

            deliveryDetails.modeOfDelivery = parent.getItemAtPosition(position).toString()
            val otherString =  context.getString(R.string.other).lowercase()
            val mode = deliveryDetails.modeOfDelivery

            if (!mode.isNullOrEmpty() && mode.equals(otherString, ignoreCase = true)) {
                // enableAndDisableAllFields(false)
                changeOtherInputEnableStatus(binding.etlModeOfDeliveryOtherOption, true, null, false)
            } else {
                clearTextInputError(binding.etlModeOfDelivery)
                //enableAndDisableAllFields(true)
                changeOtherInputEnableStatus(binding.etlModeOfDeliveryOtherOption, false, null, false)
            }
            binding.autotvModeOfDelivery.tag = deliveryDetails.modeOfDelivery
        }
    }
    private fun changeOtherInputEnableStatus(
        textInputLayout: TextInputLayout,
        enable: Boolean,
        textView: MaterialTextView? = null,
        showClearIcon: Boolean = true
    ) {
        // Handle TextInputLayout visibility
        textInputLayout.visibility = if (enable) View.VISIBLE else View.GONE

        // Handle optional TextView visibility
        textView?.visibility = if (enable) View.VISIBLE else View.GONE

        // Handle clear icon
        if (showClearIcon) {
            textInputLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            textInputLayout.isEndIconCheckable = true
        } else {
            textInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    private fun clearTextInputError(textInputLayout: TextInputLayout) {
        textInputLayout.error = null
    }
    private fun handleTextwatcher() {
        binding.etDateOfDelivery.addTextChangedListener(ClearErrorTextWatcher(binding.etlDateOfDelivery))
        binding.etTimeOfDelivery.addTextChangedListener(ClearErrorTextWatcher(binding.etlTimeOfDelivery))
        binding.etTimeOfPlacentaDelivery.addTextChangedListener(ClearErrorTextWatcher(binding.etlTimeOfPlacentaDelivery))
        binding.etApgar1.addTextChangedListener(ClearErrorTextWatcher(binding.etlApgar1))
        binding.etApgar5.addTextChangedListener(ClearErrorTextWatcher(binding.etlApgar5))
        binding.etBirthWeightGrams.addTextChangedListener(ClearErrorTextWatcher(binding.etlBirthWeightGrams))
        binding.etModeOfDeliveryOtherOption.addTextChangedListener(ClearErrorTextWatcher(binding.etlModeOfDeliveryOtherOption))
    }

    private fun handleClickListeners() {

        // ---- End Icon Clicks ----
        binding.etlDateOfDelivery.setEndIconOnClickListener {
            selectDeliveryDate()
        }

        binding.etlTimeOfDelivery.setEndIconOnClickListener {
            selectTimeForAllParameters("timeOfDelivery")
        }

        binding.etlTimeOfPlacentaDelivery.setEndIconOnClickListener {
            selectTimeForAllParameters("timeOfPlacentaDelivery")
        }

        binding.etlAmtsl.setEndIconOnClickListener {
            showAmtslDialog()
        }

        // ---- Field Clicks ----
        binding.etDateOfDelivery.setOnClickListener { selectDeliveryDate() }

        binding.etTimeOfDelivery.setOnClickListener { selectTimeForAllParameters("timeOfDelivery") }

        binding.etTimeOfPlacentaDelivery.setOnClickListener { selectTimeForAllParameters("timeOfPlacentaDelivery") }

        binding.actvAmtsl.setOnClickListener { showAmtslDialog() }
    }
    private fun selectDeliveryDate() {
        val isTablet = context.resources.getBoolean(R.bool.isTabletSize)
        val maxHeight = context.resources.getDimensionPixelOffset(R.dimen.std_430dp)

        val dialog = CalendarDialog.Builder(context)
            .title("")
            .positiveButtonLabel(R.string.ok)
            .maxHeight(if (!isTablet) maxHeight else 0)
            .build()

        dialog.setListener { day, month, year, value ->
            val selectedDate = value
            binding.etDateOfDelivery.setText(selectedDate)
            deliveryDetails.dateOfDelivery = selectedDate
        }
        dialog.show(fragmentManager, "DatePicker")
    }


    private fun selectTimeForAllParameters(forWhichParameter: String) {

        val dialog = ThemeTimePickerDialog.Builder(context)
            .title(R.string.current_time)
            .positiveButtonLabel(R.string.ok)
            .build()

        dialog.setListener { hours, minutes, amPm, value ->

            val timeString = String.format("%02d:%02d %s", hours, minutes, amPm)

            when (forWhichParameter) {

                "timeOfDelivery" -> {
                    binding.etTimeOfDelivery.setText(timeString)
                    deliveryDetails.timeOfDelivery = timeString
                }

                "timeOfPlacentaDelivery" -> {
                    binding.etTimeOfPlacentaDelivery.setText(timeString)
                    deliveryDetails.timeOfPlacentaDelivery = timeString
                }
            }
        }

        dialog.show(fragmentManager, "ThemeTimePickerDialog")
    }
    private fun showAmtslDialog() {
        val dialog = MultiChoiceDialogFragment.Builder<String>(context)
            .title(R.string.select)
            .positiveButtonLabel(R.string.save_button)
            .build()
        dialog.isSearchable(true)

        val items = context.resources.getStringArray(R.array.amtsl_options).toList()
        val adapter = GenericMultiChoiceAdapter(context, ArrayList(items),  context.getString(R.string.none_option))
        dialog.setAdapter(adapter)
        dialog.setListener { selectedItems ->
            binding.etlAmtslOtherOption.visibility = View.GONE
            if (selectedItems.isNotEmpty()) {
                val selectedText = selectedItems.joinToString(", ")
                // Show "Other" layout if selected
                if (selectedItems.contains(context.getString(R.string.other))) {
                    binding.etlAmtslOtherOption.visibility = View.VISIBLE
                }
                binding.actvAmtsl.setText(selectedText, false)
            }
        }
        dialog.show(fragmentManager, MultiChoiceDialogFragment::class.java.canonicalName)
    }

    private fun handleLiveBirthUI(type: String?) {

        val isLiveBirth = type.equals("Live Birth", ignoreCase = true)
        val visibility = if (isLiveBirth) View.VISIBLE else View.GONE

        // APGAR
        binding.textViewApgar1.visibility = visibility
        binding.etlApgar1.visibility = visibility
        binding.textViewApgar5.visibility = visibility
        binding.etlApgar5.visibility = visibility

        // Resuscitation
        binding.textViewResuscitation.visibility = visibility
        binding.etlResuscitation.visibility = visibility

        // Birth weight
        binding.textViewBirthWeightGrams.visibility = visibility
        binding.etlBirthWeightGrams.visibility = visibility

        // Skin to skin
        binding.textViewSkinToSkinContact.visibility = visibility
        binding.etlSkinToSkinContact.visibility = visibility

        // Breastfeeding
        binding.textViewLabelBreastfeedWithin1Hour.visibility = visibility
        binding.etlBreastfeedWithin1Hour.visibility = visibility

        // Congenital anomaly section
        binding.btnCongenitalYes.visibility = visibility
        binding.btnCongenitalNo.visibility = visibility
        binding.etlCongenitalYesOptions.visibility = View.GONE

        if (!isLiveBirth) {
            clearLiveBirthFields()
        }
    }

    private fun clearLiveBirthFields() {
        binding.etApgar1.text?.clear()
        binding.etApgar5.text?.clear()
        binding.etBirthWeightGrams.text?.clear()
    }
    private fun setupDegreeOfPerinealTearDropdown() {
        val modeOfDeliveryList = context.resources.getStringArray(R.array.perineal_tear_degree)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, modeOfDeliveryList)

        binding.autotvDegreeOfPerinealTear.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvDegreeOfPerinealTear.setAdapter(labourCompletedAdapter)

        binding.autotvDegreeOfPerinealTear.setOnItemClickListener { parent, _, position, _ ->

            Utils.hideKeyboard(context as AppCompatActivity)

            deliveryDetails.degreeOfPerinealTear = parent.getItemAtPosition(position).toString()

            binding.autotvDegreeOfPerinealTear.tag = deliveryDetails.degreeOfPerinealTear
        }
    }

     fun handleOtherField(
        keyName: String,        // e.g., "Mode of Delivery"
        mainValue: String?,     // value selected in dropdown
        otherValue: String?     // free text if "Other"
    ): String {
        return if (mainValue.equals("Other", ignoreCase = true)) {
            """{ "$keyName": "Other", "other_text": "${otherValue?.trim() ?: ""}" }"""
        } else {
            mainValue?.trim() ?: ""
        }
    }

    private fun setupResuscitationDropdown() {
        val resuscitationList = context.resources.getStringArray(R.array.yes_no_options_array)
        val labourCompletedAdapter = ArrayAdapter(context, R.layout.spinner_textview, resuscitationList)

        binding.autotvResuscitation.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvResuscitation.setAdapter(labourCompletedAdapter)

        binding.autotvResuscitation.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.babyGender = parent.getItemAtPosition(position).toString()
            binding.autotvResuscitation.tag = deliveryDetails.resuscitation
            clearTextInputError(binding.etlResuscitation)
        }
    }
    private fun setupPerinealTearButtons() {

        binding.btnPerinealTearYes.setOnClickListener {
        binding.tvPerinealTearValidationError.visibility = View.GONE
            // Toggle UI
            //binding.btnPerinealTearYes.isSelected = true
            //binding.btnPerinealTearNo.isSelected = false
            binding.btnPerinealTearYes.isChecked = true
            binding.btnPerinealTearNo.isChecked = false

            // Update model
            deliveryDetails.perinealTear = "Yes"

            binding.etlDegreeOfPerinealTear.visibility = View.VISIBLE
            setupDegreeOfPerinealTearDropdown()
        }

        binding.btnPerinealTearNo.setOnClickListener {
            binding.tvPerinealTearValidationError.visibility = View.GONE

            // Toggle UI
            //binding.btnPerinealTearYes.isSelected = false
            //binding.btnPerinealTearNo.isSelected = true
            binding.btnPerinealTearYes.isChecked = false
            binding.btnPerinealTearNo.isChecked = true

            // Update model
            deliveryDetails.perinealTear = "No"

            binding.etlDegreeOfPerinealTear.visibility = View.GONE
            deliveryDetails.degreeOfPerinealTear = null
        }
    }
    fun getPerinealTearValue(): String? {
        return when {
            binding.btnPerinealTearYes.isSelected -> "Yes"
            binding.btnPerinealTearNo.isSelected -> "No"
            else -> null
        }
    }
    private fun setupPlacentalAbnormalityButtons() {

        binding.btnPlacentalOrCordAbnormalityYes.setOnClickListener {

            // Toggle UI
            binding.btnPlacentalOrCordAbnormalityYes.isSelected = true
            binding.btnPlacentalOrCordAbnormalityNo.isSelected = false

            // Update model
            deliveryDetails.placentalOrCordAbnormality = "Yes"
            binding.etlPlacentalOrCordAbnormalityOtherOption.visibility = View.VISIBLE

            // Clear error if any
            clearPlacentalCordError()
        }

        binding.btnPlacentalOrCordAbnormalityNo.setOnClickListener {

            // Toggle UI
            binding.btnPlacentalOrCordAbnormalityYes.isSelected = false
            binding.btnPlacentalOrCordAbnormalityNo.isSelected = true

            // Update model
            deliveryDetails.placentalOrCordAbnormality = "No"
            binding.etlPlacentalOrCordAbnormalityOtherOption.visibility = View.GONE
            deliveryDetails.placentalOrCordAbnormalityOther = null

            // Clear error if any
            clearPlacentalCordError()
        }
    }
     fun showPlacentalCordError() {
        binding.tvCordAbnormalityValidationError.visibility = View.VISIBLE
    }

     fun clearPlacentalCordError() {
        binding.tvCordAbnormalityValidationError.visibility = View.GONE
    }

    private fun setupCongenitalButtons() {

        binding.btnCongenitalYes.setOnClickListener {

            // Toggle UI
            binding.btnCongenitalYes.isSelected = true
            binding.btnCongenitalNo.isSelected = false

            deliveryDetails.congenitalAnomalies = "Yes"

            // Show dependent views
            binding.tvCongenitalYes.visibility = View.VISIBLE
            binding.etlCongenitalYesOptions.visibility = View.VISIBLE

            clearCongenitalErrors()
        }

        binding.btnCongenitalNo.setOnClickListener {

            // Toggle UI
            binding.btnCongenitalYes.isSelected = false
            binding.btnCongenitalNo.isSelected = true

            deliveryDetails.congenitalAnomalies = "No"

            // Hide dependent views
            binding.tvCongenitalYes.visibility = View.GONE
            binding.etlCongenitalYesOptions.visibility = View.GONE
            binding.etlCongenitalYesOtherOption.visibility = View.GONE

            // Clear model values
            deliveryDetails.congenitalAnomalySpecification = null

            clearCongenitalErrors()
        }
    }
     fun showCongenitalSelectionError() {
        binding.tvCongenitalValidationError.visibility = View.VISIBLE
    }

    private fun clearCongenitalErrors() {
        binding.tvCongenitalValidationError.visibility = View.GONE
        binding.etlCongenitalYesOptions.error = null
        binding.etlCongenitalYesOtherOption.error = null
    }
    fun getPlacentalCordAbnormalityValue(): String? {
        return when {
            binding.btnPlacentalOrCordAbnormalityYes.isSelected -> "Yes"
            binding.btnPlacentalOrCordAbnormalityNo.isSelected -> "No"
            else -> null
        }
    }
    fun getCongenitalAnomalyValue(): String? {
        return when {
            binding.btnCongenitalYes.isSelected -> "Yes"
            binding.btnCongenitalNo.isSelected -> "No"
            else -> null
        }
    }

}