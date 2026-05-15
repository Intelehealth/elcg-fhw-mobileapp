package org.intelehealth.ezazi.stage3.Utils

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.activities.addNewPatient.PatientOtherInfoFragment
import org.intelehealth.ezazi.databinding.ActivityWomenDeliveryDetailsBinding
import org.intelehealth.ezazi.stage3.Utils.NepaliDateUtils.BS_MONTH_NAMES
import org.intelehealth.ezazi.stage3.Utils.NepaliDateUtils.toGregFmt
import org.intelehealth.ezazi.stage3.models.DeliveryDetails
import org.intelehealth.ezazi.ui.dialog.CalendarDialog
import org.intelehealth.ezazi.ui.dialog.MultiChoiceDialogFragment
import org.intelehealth.ezazi.ui.dialog.ThemeTimePickerDialog
import org.intelehealth.ezazi.utilities.NepaliDateConverter
import org.intelehealth.ezazi.utilities.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DeliveryDetailsUIController(
    private val binding: ActivityWomenDeliveryDetailsBinding,
    private val context: Context,
    private val fragmentManager: FragmentManager
) {
    private lateinit var deliveryDetails: DeliveryDetails;
    private  val GREG_FMT = "dd/MM/yyyy"
    private var dateOfDelivery: String? = ""
    fun initialize() {
         deliveryDetails = DeliveryDetails()

        setDropdownsData()
        setupPerinealTearRadio()
        setupPlacentalAbnormalityRadio()
        setupCongenitalRadio()
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
        setupApgar1MinDropdown()
        setupApgar5MinDropdown()
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
        //binding.etApgar1.addTextChangedListener(ClearErrorTextWatcher(binding.etlApgar1))
        //binding.etApgar5.addTextChangedListener(ClearErrorTextWatcher(binding.etlApgar5))
        binding.etBirthWeightGrams.addTextChangedListener(ClearErrorTextWatcher(binding.etlBirthWeightGrams))
        binding.etModeOfDeliveryOtherOption.addTextChangedListener(ClearErrorTextWatcher(binding.etlModeOfDeliveryOtherOption))
        binding.etAmtslOtherOption.addTextChangedListener(ClearErrorTextWatcher(binding.etlAmtslOtherOption))
    }

    private fun handleClickListeners() {

        // ---- End Icon Clicks ----
        binding.etlDateOfDelivery.setEndIconOnClickListener {
            showNepaliDatePicker(
                R.string.select_date_of_delivery,
                NepaliDateUtils.gregStringToBs(dateOfDelivery)
            ) { y, m, d ->
                val selectedGreg = toGregFmt(NepaliDateConverter.bsToGregorian(y, m, d))

                // ← FIX: reject future dates right here in the picker callback,
                //   same as PatientOtherInfoFragment's areValidFields() date check.
                //   isAfterToday uses local-TZ parseGregDate — consistent with Calendar.
                /*if (NepaliDateUtils.isAfterToday(selectedGreg)) {
                    setFieldError(
                        binding.etlDateOfDelivery,
                        context.getString(R.string.date_of_delivery_future_not_allowed)
                    )
                    return@showNepaliDatePicker
                }*/

                dateOfDelivery = selectedGreg
                binding.etDateOfDelivery.setText(NepaliDateUtils.formatBsDate(y, m, d))
                clearTextInputError(binding.etlDateOfDelivery)
                deliveryDetails.dateOfDelivery = dateOfDelivery
            }
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
        binding.etDateOfDelivery.setOnClickListener {
        //selectDeliveryDate()
            showNepaliDatePicker(
                R.string.select_date_of_delivery,
                NepaliDateUtils.gregStringToBs(dateOfDelivery)
            ) { y, m, d ->
                val selectedGreg = toGregFmt(NepaliDateConverter.bsToGregorian(y, m, d))

                /*if (NepaliDateUtils.isAfterToday(selectedGreg)) {
                    setFieldError(
                        binding.etlDateOfDelivery,
                        context.getString(R.string.date_of_delivery_future_not_allowed)
                    )
                    return@showNepaliDatePicker
                }*/

                dateOfDelivery = selectedGreg
                binding.etDateOfDelivery.setText(NepaliDateUtils.formatBsDate(y, m, d))
                clearTextInputError(binding.etlDateOfDelivery)
                deliveryDetails.dateOfDelivery = dateOfDelivery
                Log.d("datedd", "handleClickListeners: dateOfDeliveryview : " + binding.etDateOfDelivery.text.toString())
                Log.d("datedd", "handleClickListeners: dateOfDelivery : " + dateOfDelivery)
            }
        }

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
            .use24Hour(true)
            .build()

        dialog.setListener { hours, minutes, amPm, value ->

            //val timeString = String.format("%02d:%02d %s", hours, minutes, amPm)
            val timeString = value

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

        val items = context.resources
            .getStringArray(R.array.amtsl_options)
            .toList()

        val adapter = GenericMultiChoiceAdapter(
            context,
            ArrayList(items),
            context.getString(R.string.none_option)
        )

        //  Restore previously selected values
        val alreadySelectedText = binding.actvAmtsl.text?.toString()?.trim()
        val preSelected = if (alreadySelectedText.isNullOrEmpty())
            emptyList()
        else
            alreadySelectedText.split(",").map { it.trim() }

        items.forEachIndexed { index, item ->
            if (preSelected.contains(item)) {
                adapter.selectItem(index)
            }
        }

        dialog.setAdapter(adapter)

        dialog.setListener { selectedItems ->

            val isOtherSelected =
                selectedItems.contains(context.getString(R.string.other))

            val selectedText = selectedItems.joinToString(", ")
            binding.actvAmtsl.setText(selectedText, false)

            if (isOtherSelected) {
                binding.etlAmtslOtherOption.visibility = View.VISIBLE
            } else {
                binding.etlAmtslOtherOption.visibility = View.GONE
                binding.etAmtslOtherOption.setText("")   // clear stale value
            }
        }

        dialog.show(fragmentManager,
            MultiChoiceDialogFragment::class.java.canonicalName)
    }
    private fun clearLiveBirthFields() {
        binding.autotvApgar1.text?.clear()
        binding.autotvApgar5.text?.clear()
        //binding.etBirthWeightGrams.text?.clear()
        binding.autotvResuscitation.text?.clear()
        binding.autotvSkinToSkinContact.text?.clear()
        binding.autotvBreastfeedWithin1Hour.text?.clear()
        deliveryDetails.apgarScore1Min =null
        deliveryDetails.apgarScore5Min =null
        deliveryDetails.skinToSkinContact =null
        deliveryDetails.resuscitation =null
        deliveryDetails.breastfeedWithin1Hour =null
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
            binding.autotvDegreeOfPerinealTear.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
            binding.etlDegreeOfPerinealTear.error = null
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
            deliveryDetails.resuscitation = parent.getItemAtPosition(position).toString()
            binding.autotvResuscitation.tag = deliveryDetails.resuscitation
            clearTextInputError(binding.etlResuscitation)
        }
    }
    private fun setupPerinealTearRadio() {
        binding.layoutPernealTearRadio.radioYesNoGroupCommon.setOnCheckedChangeListener { _, checkedId ->

            binding.tvPerinealTearValidationError.visibility = View.GONE

            when (checkedId) {

                R.id.radioYesCommon -> {
                    // Update model
                    deliveryDetails.perinealTear = "Yes"

                    // Show degree dropdown
                    binding.etlDegreeOfPerinealTear.visibility = View.VISIBLE
                    setupDegreeOfPerinealTearDropdown()
                }

                R.id.radioNoCommon -> {
                    // Update model
                    deliveryDetails.perinealTear = "No"

                    // Hide degree dropdown
                    binding.etlDegreeOfPerinealTear.visibility = View.GONE
                    deliveryDetails.degreeOfPerinealTear = null
                }
            }
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
    private fun setupPlacentalAbnormalityRadio() {

        binding.layoutPlacentalAbnormalityRadio.radioYesNoGroupCommon
            .setOnCheckedChangeListener { _, checkedId ->

                binding.tvCordAbnormalityValidationError.visibility = View.GONE

                when (checkedId) {

                    R.id.radioYesCommon -> {
                        deliveryDetails.placentalOrCordAbnormality = "Yes"
                        binding.etlPlacentalOrCordAbnormalityOtherOption.visibility = View.VISIBLE
                    }

                    R.id.radioNoCommon -> {
                        deliveryDetails.placentalOrCordAbnormality = "No"
                        binding.etlPlacentalOrCordAbnormalityOtherOption.visibility = View.GONE
                        deliveryDetails.placentalOrCordAbnormalityOther = null
                    }
                }
            }
    }

    private fun setupCongenitalRadio() {

        binding.layoutCongenitalAnomaliesRadio.radioYesNoGroupCommon
            .setOnCheckedChangeListener { _, checkedId ->

                clearCongenitalErrors()

                when (checkedId) {

                    R.id.radioYesCommon -> {
                        deliveryDetails.congenitalAnomalies = "Yes"

                        //  dependent views
                        binding.tvCongenitalYes.visibility = View.VISIBLE
                        binding.etlCongenitalYesOptions.visibility = View.VISIBLE
                    }

                    R.id.radioNoCommon -> {
                        deliveryDetails.congenitalAnomalies = "No"

                        // Hide dependent views
                        binding.tvCongenitalYes.visibility = View.GONE
                        binding.etlCongenitalYesOptions.visibility = View.GONE
                        binding.etlCongenitalYesOtherOption.visibility = View.GONE

                        // Clear model values
                        deliveryDetails.congenitalAnomalySpecification = null
                        binding.autotvCongenitalYesOptions.setText("", false)

                    }
                }
            }
    }
     fun getYesNoValue(radioGroup: RadioGroup): String? {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.radioYesCommon -> "Yes"
            R.id.radioNoCommon -> "No"
            else -> null
        }
    }
    private fun setupCongenitalAnomaliesDropdown() {
        // Make field clickable to open multi-select dialog
        binding.autotvCongenitalYesOptions.setOnClickListener {
            showCongenitalDialog()
        }
        binding.etlCongenitalYesOptions.setEndIconOnClickListener {
            showCongenitalDialog()
        }
    }

    private fun showCongenitalDialog() {
        val dialog = MultiChoiceDialogFragment.Builder<String>(context)
            .title(R.string.select)
            .positiveButtonLabel(R.string.save_button)
            .build()
        dialog.isSearchable(true)

        val items = context.resources.getStringArray(R.array.congenital_anomalies_options).toList()
        val adapter = GenericMultiChoiceAdapter(context, ArrayList(items), null)
        dialog.setAdapter(adapter)
        dialog.setListener { selectedItems ->
            binding.etlCongenitalYesOtherOption.visibility = View.GONE
            if (selectedItems.isNotEmpty()) {
                val selectedText = selectedItems.joinToString(", ")
                binding.etlCongenitalYesOptions.error = null
                binding.etlCongenitalYesOptions.isErrorEnabled = false

                if (selectedItems.contains(context.getString(R.string.other))) {
                    binding.etlCongenitalYesOtherOption.visibility = View.VISIBLE
                }
                binding.autotvCongenitalYesOptions.setText(selectedText, false)
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
        //binding.textViewBirthWeightGrams.visibility = visibility
        //binding.etlBirthWeightGrams.visibility = visibility

        // Skin to skin
        binding.textViewSkinToSkinContact.visibility = visibility
        binding.etlSkinToSkinContact.visibility = visibility

        // Breastfeeding
        binding.textViewLabelBreastfeedWithin1Hour.visibility = visibility
        binding.etlBreastfeedWithin1Hour.visibility = visibility

        if(!isLiveBirth)
            clearLiveBirthFields()

    }
    private fun setupApgar1MinDropdown() {
        val apgarScoresList = context.resources.getStringArray(R.array.apgar_scores)
        val apgarScoreAdapter = ArrayAdapter(context, R.layout.spinner_textview, apgarScoresList)

        binding.autotvApgar1.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvApgar1.setAdapter(apgarScoreAdapter)

        binding.autotvApgar1.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.apgarScore1Min = parent.getItemAtPosition(position).toString()
            binding.autotvApgar1.tag = deliveryDetails.apgarScore1Min
            clearTextInputError(binding.etlApgar1)
        }
    }
    private fun setupApgar5MinDropdown() {
        val apgarScoresList = context.resources.getStringArray(R.array.apgar_scores)
        val apgarScoreAdapter= ArrayAdapter(context, R.layout.spinner_textview, apgarScoresList)

        binding.autotvApgar5.setDropDownBackgroundResource(R.drawable.rounded_corner_white_with_gray_stroke)
        binding.autotvApgar5.setAdapter(apgarScoreAdapter)

        binding.autotvApgar5.setOnItemClickListener { parent, _, position, _ ->
            Utils.hideKeyboard(context as AppCompatActivity)
            deliveryDetails.apgarScore5Min = parent.getItemAtPosition(position).toString()
            binding.autotvApgar5.tag = deliveryDetails.apgarScore5Min
            clearTextInputError(binding.etlApgar5)
        }
    }


    private fun showNepaliDatePicker(
        titleRes: Int,
        currentBsDate: IntArray?,
        listener: OnBsDateSelectedListener
    ) {
        val (initY, initM, initD) = if (currentBsDate != null && currentBsDate[0] > 0) {
            Triple(currentBsDate[0], currentBsDate[1], currentBsDate[2])
        } else {
            val today = NepaliDateConverter.getCurrentBsDate()
            Triple(today[0], today[1], today[2])
        }

        val yearPicker = NumberPicker(context)
        val monthPicker = NumberPicker(context)
        val dayPicker = NumberPicker(context)

        yearPicker.minValue = 2000
        yearPicker.maxValue = 2090
        yearPicker.value = initY

        // min/max MUST be set before displayedValues
        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.displayedValues = BS_MONTH_NAMES
        monthPicker.value = initM

        NepaliDateUtils.refreshDayPicker(dayPicker, initY, initM)
        dayPicker.value = minOf(initD, dayPicker.maxValue)

        val onChange =
            NumberPicker.OnValueChangeListener { _, _, _ ->
                NepaliDateUtils.refreshDayPicker(
                    dayPicker,
                    yearPicker.value,
                    monthPicker.value
                )
            }

        yearPicker.setOnValueChangedListener(onChange)
        monthPicker.setOnValueChangedListener(onChange)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 24)
        }

        val lp = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        layout.addView(yearPicker, lp)
        layout.addView(monthPicker, lp)
        layout.addView(dayPicker, lp)

        MaterialAlertDialogBuilder(context)
            .setTitle("${context.getString(titleRes)} (BS)")
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                listener.onSelected(
                    yearPicker.value,
                    monthPicker.value,
                    dayPicker.value
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun interface OnBsDateSelectedListener {
        fun onSelected(year: Int, month: Int, day: Int)
    }

     fun getDateOfDelivery(): String?{
        return dateOfDelivery;
    }
}