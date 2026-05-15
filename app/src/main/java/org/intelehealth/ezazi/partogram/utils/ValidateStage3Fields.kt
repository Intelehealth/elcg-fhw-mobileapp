package org.intelehealth.ezazi.partogram.utils

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.partogram.PartogramConstants
import org.intelehealth.ezazi.partogram.model.PartogramItemData
import org.json.JSONObject

/**
 * ============================================================
 * EARLY POSTPARTUM MONITORING VALIDATION (Stage 3)
 * ============================================================
 *
 * MONITORING SCHEDULE (CRITICAL):
 * - 0–1 hour: Every 15 minutes
 * - 1–2 hours: Every 30 minutes
 * - 2–4 hours: Every 1 hour
 *
 * REQUIREMENT: All fields are OPTIONAL
 * But if user adds input, validations must be applied
 */

object ValidateStage3Fields {

    data class ValidationResult(
        val isValid: Boolean,
        @StringRes val errorMessageRes: Int = 0,
        val errorArgs: Array<Any> = arrayOf()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ValidationResult
            if (isValid != other.isValid) return false
            if (errorMessageRes != other.errorMessageRes) return false
            if (!errorArgs.contentEquals(other.errorArgs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isValid.hashCode()
            result = 31 * result + errorMessageRes
            result = 31 * result + errorArgs.contentHashCode()
            return result
        }
    }

    fun validatePostpartumMonitoringFromParams(
        context: Context,
        itemList: List<PartogramItemData>,
        isNewbornLiveBirth: Boolean = true  //  REQUIRED: Hide newborn section if NOT live birth
    ): ValidationResult {

        val values = extractValues(itemList)

        // Validate Woman Monitoring (Always required if any data provided)
        validateWomanMonitoring(values)?.let { return it }

        // Validate Newborn Monitoring (Only if Live Birth)
        if (isNewbornLiveBirth) {
            validateNewbornMonitoring(values)?.let { return it }
        }

        return ValidationResult(true)
    }

    private fun extractValues(itemList: List<PartogramItemData>): Map<String, String> {
        val values = mutableMapOf<String, String>()

        itemList.forEach { item ->
            item.paramInfoList.forEach { info ->
                when (info.conceptUUID) {
                    // ===============================
                    // WOMAN MONITORING - Vital Signs
                    // ===============================
                    PartogramConstants.Params.PULSE.conceptId -> // STAGE3_WOMAN_PULSE
                        values[KEY_WOMAN_PULSE] = info.capturedValue ?: ""
                    PartogramConstants.Params.SYSTOLIC_BP.conceptId -> // STAGE3_WOMAN_SYSTOLIC_BP
                        values[KEY_WOMAN_SYS_BP] = info.capturedValue ?: ""
                    PartogramConstants.Params.DIASTOLIC_BP.conceptId -> // STAGE3_WOMAN_DIASTOLIC_BP
                        values[KEY_WOMAN_DIA_BP] = info.capturedValue ?: ""
                    PartogramConstants.Params.TEMPERATURE.conceptId -> // STAGE3_WOMAN_TEMPERATURE
                        values[KEY_WOMAN_TEMP] = info.capturedValue ?: ""
                    PartogramConstants.Params.RESPIRATORY_RATE_MOTHER.conceptId -> // STAGE3_RESPIRATORY_RATE_MOTHER
                        values[KEY_WOMAN_RR] = info.capturedValue ?: ""
                    PartogramConstants.Params.BLOOD_LOSS_MOTHER.conceptId -> // STAGE3_BLOOD_LOSS_MOTHER
                        values[KEY_BLOOD_LOSS] = info.capturedValue ?: ""

                    // ===============================
                    // WOMAN MONITORING - Clinical Checks
                    // ===============================
                    PartogramConstants.Params.UTERUS_CONTRACTED_MOTHER.conceptId -> // STAGE3_UTERUS_CONTRACTED_MOTHER
                        values[KEY_UTERUS_CONTRACTED] = info.capturedValue ?: ""
                    PartogramConstants.Params.URINE_PASSED_MOTHER.conceptId -> // STAGE3_URINE_PASSED_MOTHER
                        values[KEY_URINE_PASSED] = info.capturedValue ?: ""
                    PartogramConstants.Params.HEMATOMA_MOTHER.conceptId -> // STAGE3_HEMATOMA_MOTHER
                        values[KEY_HEMATOMA] = info.capturedValue ?: ""
                    PartogramConstants.Params.ONGOING_COMPLICATIONS_MOTHER.conceptId -> // STAGE3_ONGOING_COMPLICATIONS_MOTHER
                        values[KEY_COMPLICATIONS_MOTHER] = info.capturedValue ?: ""

                    // ===============================
                    // WOMAN MONITORING - Documentation
                    // ===============================
                    PartogramConstants.Params.ASSESSMENT_MOTHER.conceptId-> // STAGE3_ASSESSMENT_MOTHER
                        values[KEY_ASSESSMENT_MOTHER] = info.capturedValue ?: ""
                    PartogramConstants.Params.PLAN_MOTHER.conceptId -> // STAGE3_PLAN_MOTHER
                        values[KEY_PLAN_MOTHER] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Breathing & Vitals
                    // ===============================
                    PartogramConstants.Params.GRUNTING_NEWBORN.conceptId -> // STAGE3_GRUNTING_NEWBORN
                        values[KEY_GRUNTING] = info.capturedValue ?: ""
                    PartogramConstants.Params.CHEST_INDRAWING_NEWBORN.conceptId -> // STAGE3_CHEST_INDRAWING_NEWBORN
                        values[KEY_CHEST_INDRAWING] = info.capturedValue ?: ""
                    PartogramConstants.Params.FAST_BREATHING_NEWBORN.conceptId -> // STAGE3_FAST_BREATHING_NEWBORN
                        values[KEY_FAST_BREATHING] = info.capturedValue ?: ""
                    PartogramConstants.Params.RESPIRATORY_RATE_NEWBORN.conceptId -> // STAGE3_RESPIRATORY_RATE_NEWBORN
                        values[KEY_NEWBORN_RR] = info.capturedValue ?: ""
                    PartogramConstants.Params.SPO2_NEWBORN.conceptId -> // STAGE3_SPO2_NEWBORN
                        values[KEY_NEWBORN_SPO2] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Thermoregulation & Circulation
                    // ===============================
                    PartogramConstants.Params.FEET_WARM_NEWBORN.conceptId -> // STAGE3_FEET_WARM_NEWBORN
                        values[KEY_FEET_WARM] = info.capturedValue ?: ""
                    PartogramConstants.Params.TEMPERATURE_NEWBORN.conceptId -> // STAGE3_TEMPERATURE_NEWBORN
                        values[KEY_NEWBORN_TEMP] = info.capturedValue ?: ""
                    PartogramConstants.Params.SKIN_COLOR_NEWBORN.conceptId -> // STAGE3_SKIN_COLOR_NEWBORN
                        values[KEY_SKIN_COLOR_CYANOSIS] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Umbilical & Feeding
                    // ===============================
                    PartogramConstants.Params.UC_OOZING_NEWBORN.conceptId -> // STAGE3_UC_OOZING_NEWBORN
                        values[KEY_UMBILICAL_CORD_OOZING] = info.capturedValue ?: ""
                    PartogramConstants.Params.SUCKING_FEEDING_NEWBORN.conceptId -> // STAGE3_SUCKING_FEEDING_NEWBORN
                        values[KEY_SUCKLING_FEEDING] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Complications & Documentation
                    // ===============================
                    PartogramConstants.Params.ONGOING_COMPLICATIONS_NEWBORN.conceptId -> // STAGE3_ONGOING_COMPLICATIONS_NEWBORN
                        values[KEY_COMPLICATIONS_NEWBORN] = info.capturedValue ?: ""
                    PartogramConstants.Params.ASSESSMENT_NEWBORN.conceptId -> // STAGE3_ASSESSMENT_NEWBORN
                        values[KEY_ASSESSMENT_NEWBORN] = info.capturedValue ?: ""
                    PartogramConstants.Params.PLAN_NEWBORN.conceptId -> // STAGE3_PLAN_NEWBORN
                        values[KEY_PLAN_NEWBORN] = info.capturedValue ?: ""
                }
            }
        }
        return values
    }

    // ============================================================
    // WOMAN MONITORING VALIDATION
    // ============================================================

    private fun validateWomanMonitoring(values: Map<String, String>): ValidationResult? {
        val pulse = values[KEY_WOMAN_PULSE] ?: ""
        val sysBP = values[KEY_WOMAN_SYS_BP] ?: ""
        val diaBP = values[KEY_WOMAN_DIA_BP] ?: ""
        val temp = values[KEY_WOMAN_TEMP] ?: ""
        val rr = values[KEY_WOMAN_RR] ?: ""
        val bloodLoss = values[KEY_BLOOD_LOSS] ?: ""

        val motherComplications =
            values[KEY_COMPLICATIONS_MOTHER] ?: ""

        // Validate complications first
        validateComplications(
            motherComplications,
            R.string.select_mother_complication_error
        )?.let {
            return it
        }
        // REQUIRED: Check at least one field has value
        if (pulse.isEmpty() && sysBP.isEmpty() && diaBP.isEmpty() &&
            temp.isEmpty() && rr.isEmpty() && bloodLoss.isEmpty()) {
            return null
        }

        // Pulse: 40-200 bpm
        // Pulse: MINIMUM_PULSE - MAXIMUM_PULSE bpm
        if (pulse.isNotEmpty()) {
            val minPulse = ValidationConstants.WOMAN_PULSE_MIN
            val maxPulse = ValidationConstants.WOMAN_PULSE_MAX
            validateIntRange(pulse, minPulse, maxPulse)?.let {
                return ValidationResult(false, R.string.err_pulse_range, arrayOf(minPulse, maxPulse))
            }
        }

        // Systolic BP: 70-200 mmHg
        // Systolic BP: MINIMUM_BP_SYS - MAXIMUM_BP_SYS mmHg
        if (sysBP.isNotEmpty()) {
            val minSys = ValidationConstants.WOMAN_SYSTOLIC_BP_MIN
            val maxSys = ValidationConstants.WOMAN_SYSTOLIC_BP_MAX
            validateIntRange(sysBP, minSys, maxSys)?.let {
                return ValidationResult(false, R.string.err_systolic_range, arrayOf(minSys, maxSys))
            }
        }

        // Diastolic BP: 40-120 mmHg and < Systolic
        // Diastolic BP: MINIMUM_BP_DSYS - MAXIMUM_BP_DSYS mmHg and < Systolic
        if (diaBP.isNotEmpty()) {

            if (sysBP.isEmpty()) {
                return ValidationResult(false, R.string.err_enter_systolic_first)
            }

            val minDia = ValidationConstants.WOMAN_DIASTOLIC_BP_MIN
            val maxDia = ValidationConstants.WOMAN_DIASTOLIC_BP_MAX

            validateIntRange(diaBP, minDia, maxDia)?.let {
                return ValidationResult(false, R.string.err_diastolic_range, arrayOf(minDia, maxDia))
            }

            try {
                if (diaBP.toInt() >= sysBP.toInt()) {
                    return ValidationResult(false, R.string.err_diastolic_less_than_sys)
                }
            } catch (e: Exception) {
                return ValidationResult(false, R.string.err_invalid_number)
            }
        }

        // Temperature: MINIMUM_TEMPERATURE_CELSIUS - MAXIMUM_TEMPERATURE_CELSIUS °C
      if (temp.isNotEmpty()) {
            val minTemp = ValidationConstants.WOMAN_TEMPERATURE_MIN
            val maxTemp = ValidationConstants.WOMAN_TEMPERATURE_MAX

            validateDoubleRange(temp, minTemp, maxTemp)?.let {
                return ValidationResult(
                    false,
                    R.string.err_temperature_range,
                    arrayOf(minTemp, maxTemp)
                )
            }
        }

        // Respiratory Rate: 10-60 breaths/min
        if (rr.isNotEmpty()) {
            validateIntRange(rr, ValidationConstants.WOMAN_RESPIRATORY_RATE_MIN, ValidationConstants.WOMAN_RESPIRATORY_RATE_MAX)?.let {
                return ValidationResult(false, R.string.err_rr_range, arrayOf(ValidationConstants.WOMAN_RESPIRATORY_RATE_MIN,  ValidationConstants.WOMAN_RESPIRATORY_RATE_MAX))
            }
        }

        // Blood Loss: 0-5000 ml
        if (bloodLoss.isNotEmpty()) {
            validateIntRange(bloodLoss, ValidationConstants.WOMAN_BLOOD_LOSS_MIN, ValidationConstants.WOMAN_BLOOD_LOSS_MAX)?.let {
                return ValidationResult(false, R.string.err_blood_loss_range, arrayOf(ValidationConstants.WOMAN_BLOOD_LOSS_MIN, ValidationConstants.WOMAN_BLOOD_LOSS_MAX))
            }
        }

        // Yes/No fields - no validation needed
        // Free text fields (Assessment, Plan) - no validation needed

        return null
    }

    // ============================================================
    // NEWBORN MONITORING VALIDATION (Only if Live Birth)
    // ============================================================

    private fun validateNewbornMonitoring(values: Map<String, String>): ValidationResult? {
        val rr = values[KEY_NEWBORN_RR] ?: ""
        val spo2 = values[KEY_NEWBORN_SPO2] ?: ""
        val temp = values[KEY_NEWBORN_TEMP] ?: ""

        val newbornComplications =
            values[KEY_COMPLICATIONS_NEWBORN] ?: ""

        // Validate complications first
        validateComplications(newbornComplications, R.string.select_newborn_complication_error)?.let {
            return it
        }
        //  REQUIRED: Check at least one field has value
        if (rr.isEmpty() && spo2.isEmpty() && temp.isEmpty()) {
            return null
        }

        // Respiratory Rate: 30-90 breaths/min
        if (rr.isNotEmpty()) {
            validateIntRange(rr, ValidationConstants.NEWBORN_RESPIRATORY_RATE_MIN, ValidationConstants.NEWBORN_RESPIRATORY_RATE_MAX)?.let {
                return ValidationResult(false, R.string.err_nb_rr_range, arrayOf(ValidationConstants.NEWBORN_RESPIRATORY_RATE_MIN, ValidationConstants.NEWBORN_RESPIRATORY_RATE_MAX))
            }
        }

        // SPO2: 50-100%
        if (spo2.isNotEmpty()) {
            validateIntRange(spo2,  ValidationConstants.NEWBORN_SPO2_MIN, ValidationConstants.NEWBORN_SPO2_MAX)?.let {
                return ValidationResult(false, R.string.err_nb_spo2_range, arrayOf(ValidationConstants.NEWBORN_SPO2_MIN, ValidationConstants.NEWBORN_SPO2_MAX))
            }
        }

        // Temperature: 95.0-107.6 °F
        if (temp.isNotEmpty()) {
            validateDoubleRange(temp,  ValidationConstants.NEWBORN_TEMPERATURE_MIN, ValidationConstants.NEWBORN_TEMPERATURE_MAX)?.let {
                return ValidationResult(false, R.string.err_nb_temp_range, arrayOf(ValidationConstants.NEWBORN_TEMPERATURE_MIN, ValidationConstants.NEWBORN_TEMPERATURE_MAX))
            }
        }

        // Yes/No fields - no validation needed
        // Free text fields (Assessment, Plan) - no validation needed

        return null
    }

    // ============================================================
    // HELPER VALIDATION FUNCTIONS
    // ============================================================

    private fun validateIntRange(value: String, min: Int, max: Int): Boolean? {
        return try {
            val v = value.trim().toInt()
            if (v !in min..max) true else null
        } catch (e: Exception) {
            true
        }
    }

    private fun validateDoubleRange(value: String, min: Double, max: Double): Boolean? {
        return try {
            val v = value.trim().toDouble()
            if (v !in min..max) true else null
        } catch (e: Exception) {
            true
        }
    }
    private val radioTypeConcepts = setOf(
        PartogramConstants.Params.UTERUS_CONTRACTED_MOTHER.conceptId,
        PartogramConstants.Params.URINE_PASSED_MOTHER.conceptId,
        PartogramConstants.Params.HEMATOMA_MOTHER.conceptId,
        PartogramConstants.Params.GRUNTING_NEWBORN.conceptId,
        PartogramConstants.Params.CHEST_INDRAWING_NEWBORN.conceptId,
        PartogramConstants.Params.FAST_BREATHING_NEWBORN.conceptId,
        PartogramConstants.Params.FEET_WARM_NEWBORN.conceptId,
        PartogramConstants.Params.SKIN_COLOR_NEWBORN.conceptId,
        PartogramConstants.Params.UC_OOZING_NEWBORN.conceptId,
    )

    fun isRadioSelectField(conceptId: String?): Boolean {
        return conceptId != null && radioTypeConcepts.contains(conceptId)
    }

    private fun validateComplications(
        jsonValue: String?,
        @StringRes errorRes: Int
    ): ValidationResult? {

        if (jsonValue.isNullOrEmpty()) {
            return null
        }

        return try {

            Log.d("ComplicationValidation", jsonValue)

            val jsonObject = JSONObject(jsonValue)

            val answer = jsonObject.optString(
                "any ongoing complication"
            ).trim()

            val complications = jsonObject.optString(
                "complications"
            ).trim()

            Log.d("ComplicationValidation",
                "answer=$answer complications=$complications")

            if (answer.equals("yes", ignoreCase = true)
                && complications.isBlank()
            ) {

                ValidationResult(
                    false,
                    errorRes
                )

            } else {
                null
            }

        } catch (e: Exception) {

            e.printStackTrace()

            ValidationResult(
                false,
                errorRes
            )
        }
    }
}

// ============================================================
// CONSTANTS FOR FIELD MAPPING
// ============================================================

private const val KEY_WOMAN_PULSE = "woman_pulse"
private const val KEY_WOMAN_SYS_BP = "woman_sys_bp"
private const val KEY_WOMAN_DIA_BP = "woman_dia_bp"
private const val KEY_WOMAN_TEMP = "woman_temp"
private const val KEY_WOMAN_RR = "woman_rr"
private const val KEY_BLOOD_LOSS = "blood_loss"
private const val KEY_UTERUS_CONTRACTED = "uterus_contracted"
private const val KEY_URINE_PASSED = "urine_passed"
private const val KEY_HEMATOMA = "hematoma"
private const val KEY_COMPLICATIONS_MOTHER = "complications_mother"
private const val KEY_ASSESSMENT_MOTHER = "assessment_mother"
private const val KEY_PLAN_MOTHER = "plan_mother"

private const val KEY_GRUNTING = "grunting"
private const val KEY_CHEST_INDRAWING = "chest_indrawing"
private const val KEY_FAST_BREATHING = "fast_breathing"
private const val KEY_NEWBORN_RR = "newborn_rr"
private const val KEY_NEWBORN_SPO2 = "newborn_spo2"
private const val KEY_FEET_WARM = "feet_warm"
private const val KEY_NEWBORN_TEMP = "newborn_temp"
private const val KEY_SKIN_COLOR_CYANOSIS = "skin_color_cyanosis"
private const val KEY_UMBILICAL_CORD_OOZING = "umbilical_cord_oozing"
private const val KEY_SUCKLING_FEEDING = "suckling_feeding"
private const val KEY_COMPLICATIONS_NEWBORN = "complications_newborn"
private const val KEY_ASSESSMENT_NEWBORN = "assessment_newborn"
private const val KEY_PLAN_NEWBORN = "plan_newborn"

