package org.intelehealth.ezazi.partogram.utils

import android.content.Context
import androidx.annotation.StringRes
import org.intelehealth.ezazi.R
import org.intelehealth.ezazi.app.AppConstants.MAXIMUM_BP_DSYS
import org.intelehealth.ezazi.app.AppConstants.MAXIMUM_BP_SYS
import org.intelehealth.ezazi.app.AppConstants.MAXIMUM_PULSE
import org.intelehealth.ezazi.app.AppConstants.MAXIMUM_TEMPERATURE_CELSIUS
import org.intelehealth.ezazi.app.AppConstants.MINIMUM_BP_DSYS
import org.intelehealth.ezazi.app.AppConstants.MINIMUM_BP_SYS
import org.intelehealth.ezazi.app.AppConstants.MINIMUM_PULSE
import org.intelehealth.ezazi.app.AppConstants.MINIMUM_TEMPERATURE_CELSIUS
import org.intelehealth.ezazi.partogram.model.PartogramItemData

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
                    "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> // STAGE3_WOMAN_PULSE
                        values[KEY_WOMAN_PULSE] = info.capturedValue ?: ""
                    "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> // STAGE3_WOMAN_SYSTOLIC_BP
                        values[KEY_WOMAN_SYS_BP] = info.capturedValue ?: ""
                    "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> // STAGE3_WOMAN_DIASTOLIC_BP
                        values[KEY_WOMAN_DIA_BP] = info.capturedValue ?: ""
                    "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> // STAGE3_WOMAN_TEMPERATURE
                        values[KEY_WOMAN_TEMP] = info.capturedValue ?: ""
                    "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> // STAGE3_RESPIRATORY_RATE_MOTHER
                        values[KEY_WOMAN_RR] = info.capturedValue ?: ""
                    "585c2f4a-16d3-4fd7-8b50-2927e14ca379" -> // STAGE3_BLOOD_LOSS_MOTHER
                        values[KEY_BLOOD_LOSS] = info.capturedValue ?: ""

                    // ===============================
                    // WOMAN MONITORING - Clinical Checks
                    // ===============================
                    "13de11c4-76cc-4851-8b37-513cee310c93" -> // STAGE3_UTERUS_CONTRACTED_MOTHER
                        values[KEY_UTERUS_CONTRACTED] = info.capturedValue ?: ""
                    "cb6ee6f9-6476-4b44-adac-8e85798a716c" -> // STAGE3_URINE_PASSED_MOTHER
                        values[KEY_URINE_PASSED] = info.capturedValue ?: ""
                    "afd25b45-cb3f-4e77-8c3d-8b8f6cc83944" -> // STAGE3_HEMATOMA_MOTHER
                        values[KEY_HEMATOMA] = info.capturedValue ?: ""
                    "caa174fc-5889-443e-b53d-701fb2e85c23" -> // STAGE3_ONGOING_COMPLICATIONS_MOTHER
                        values[KEY_COMPLICATIONS_MOTHER] = info.capturedValue ?: ""

                    // ===============================
                    // WOMAN MONITORING - Documentation
                    // ===============================
                    "67a050c1-35e5-451c-a4ab-fff9d57b0db1" -> // STAGE3_ASSESSMENT_MOTHER
                        values[KEY_ASSESSMENT_MOTHER] = info.capturedValue ?: ""
                    "5991aeaf-c4c3-457d-ada1-334d90c545fc" -> // STAGE3_PLAN_MOTHER
                        values[KEY_PLAN_MOTHER] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Breathing & Vitals
                    // ===============================
                    "694d18a3-8163-43ba-9ac8-cf07a8c84b6b" -> // STAGE3_GRUNTING_NEWBORN
                        values[KEY_GRUNTING] = info.capturedValue ?: ""
                    "5d9c44df-eb2b-435b-aac9-ee6b83ee6d27" -> // STAGE3_CHEST_INDRAWING_NEWBORN
                        values[KEY_CHEST_INDRAWING] = info.capturedValue ?: ""
                    "9d2ebbfd-538f-11e6-9cfe-86f436325720" -> // STAGE3_FAST_BREATHING_NEWBORN
                        values[KEY_FAST_BREATHING] = info.capturedValue ?: ""
                    "e642e4a6-2799-488f-9f95-eec27a3bb756" -> // STAGE3_RESPIRATORY_RATE_NEWBORN
                        values[KEY_NEWBORN_RR] = info.capturedValue ?: ""
                    "ef9490bc-b039-45bc-98a8-34d55b750e04" -> // STAGE3_SPO2_NEWBORN
                        values[KEY_NEWBORN_SPO2] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Thermoregulation & Circulation
                    // ===============================
                    "d16058b1-8756-4db2-bf71-834466e1f436" -> // STAGE3_FEET_WARM_NEWBORN
                        values[KEY_FEET_WARM] = info.capturedValue ?: ""
                    "5c74ba33-b194-4a1a-b6e8-cfca07d2a593" -> // STAGE3_TEMPERATURE_NEWBORN
                        values[KEY_NEWBORN_TEMP] = info.capturedValue ?: ""
                    "641cf6ac-ce38-42ca-9672-d30dce1c56de" -> // STAGE3_SKIN_COLOR_NEWBORN
                        values[KEY_SKIN_COLOR_CYANOSIS] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Umbilical & Feeding
                    // ===============================
                    "995ab5c8-76a0-4e3c-ac70-2270fdbc14a8" -> // STAGE3_UC_OOZING_NEWBORN
                        values[KEY_UMBILICAL_CORD_OOZING] = info.capturedValue ?: ""
                    "1a49a4ef-ad20-4cd8-b176-06eb6fbf9971" -> // STAGE3_SUCKING_FEEDING_NEWBORN
                        values[KEY_SUCKLING_FEEDING] = info.capturedValue ?: ""

                    // ===============================
                    // NEWBORN MONITORING - Complications & Documentation
                    // ===============================
                    "820ae093-d3ab-402a-9a81-279963cf9dc8" -> // STAGE3_ONGOING_COMPLICATIONS_NEWBORN
                        values[KEY_COMPLICATIONS_NEWBORN] = info.capturedValue ?: ""
                    "15fc4eca-7635-4e7c-baa4-20c250cfe62a" -> // STAGE3_ASSESSMENT_NEWBORN
                        values[KEY_ASSESSMENT_NEWBORN] = info.capturedValue ?: ""
                    "a79853d9-e45e-41b2-8473-b19e5cae66cb" -> // STAGE3_PLAN_NEWBORN
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

        // REQUIRED: Check at least one field has value
        if (pulse.isEmpty() && sysBP.isEmpty() && diaBP.isEmpty() &&
            temp.isEmpty() && rr.isEmpty() && bloodLoss.isEmpty()) {
            return null
        }

        // Pulse: 40-200 bpm
        // Pulse: MINIMUM_PULSE - MAXIMUM_PULSE bpm
        if (pulse.isNotEmpty()) {
            val minPulse = MINIMUM_PULSE.toInt()
            val maxPulse = MAXIMUM_PULSE.toInt()
            validateIntRange(pulse, minPulse, maxPulse)?.let {
                return ValidationResult(false, R.string.err_pulse_range, arrayOf(minPulse, maxPulse))
            }
        }

        // Systolic BP: 70-200 mmHg
        // Systolic BP: MINIMUM_BP_SYS - MAXIMUM_BP_SYS mmHg
        if (sysBP.isNotEmpty()) {
            val minSys = MINIMUM_BP_SYS.toInt()
            val maxSys = MAXIMUM_BP_SYS.toInt()
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

            val minDia = MINIMUM_BP_DSYS.toInt()
            val maxDia = MAXIMUM_BP_DSYS.toInt()

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
            val minTemp = MINIMUM_TEMPERATURE_CELSIUS.toDouble()
            val maxTemp = MAXIMUM_TEMPERATURE_CELSIUS.toDouble()

            validateDoubleRange(temp, minTemp, maxTemp)?.let {
                return ValidationResult(
                    false,
                    R.string.err_temperature_range,
                    arrayOf(minTemp, maxTemp)
                )
            }
        }

        /*// Respiratory Rate: 10-60 breaths/min
        if (rr.isNotEmpty()) {
            validateIntRange(rr, 10, 60)?.let {
                return ValidationResult(false, R.string.err_rr_range, arrayOf(10, 60))
            }
        }

        // Blood Loss: 0-5000 ml
        if (bloodLoss.isNotEmpty()) {
            validateIntRange(bloodLoss, 0, 5000)?.let {
                return ValidationResult(false, R.string.err_blood_loss_range, arrayOf(0, 5000))
            }
        }*/

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

        //  REQUIRED: Check at least one field has value
        if (rr.isEmpty() && spo2.isEmpty() && temp.isEmpty()) {
            return null
        }

        // Respiratory Rate: 30-90 breaths/min
        if (rr.isNotEmpty()) {
            validateIntRange(rr, 30, 90)?.let {
                return ValidationResult(false, R.string.err_nb_rr_range, arrayOf(30, 90))
            }
        }

        // SPO2: 50-100%
        if (spo2.isNotEmpty()) {
            validateIntRange(spo2, 50, 100)?.let {
                return ValidationResult(false, R.string.err_nb_spo2_range, arrayOf(50, 100))
            }
        }

        // Temperature: 95.0-107.6 °F
        if (temp.isNotEmpty()) {
            validateDoubleRange(temp, 95.0, 107.6)?.let {
                return ValidationResult(false, R.string.err_nb_temp_range, arrayOf(95.0, 107.6))
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