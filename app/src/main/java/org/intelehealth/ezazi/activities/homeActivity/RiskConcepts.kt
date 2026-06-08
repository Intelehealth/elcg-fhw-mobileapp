package org.intelehealth.ezazi.activities.homeActivity

import org.intelehealth.ezazi.partogram.PartogramConstants

object RiskConcepts {
    // Stage 1 & 2 concepts
    @JvmField
    val STAGE_1_2_CONCEPTS: Set<String> = setOf(
        PartogramConstants.Params.COMPANION.conceptId,
        PartogramConstants.Params.PAIN_RELIEF.conceptId,
        PartogramConstants.Params.ORAL_FLUID.conceptId,
        PartogramConstants.Params.POSTURE.conceptId,
        PartogramConstants.Params.BASELINE_FHR.conceptId,
        PartogramConstants.Params.FHR_DEC.conceptId,
        PartogramConstants.Params.AMNIOTIC_FLUID.conceptId,
        PartogramConstants.Params.FETAL_POSITION.conceptId,
        PartogramConstants.Params.CAPUT.conceptId,
        PartogramConstants.Params.MOULDING.conceptId,
        PartogramConstants.Params.PULSE.conceptId,
        PartogramConstants.Params.SYSTOLIC_BP.conceptId,
        PartogramConstants.Params.DIASTOLIC_BP.conceptId,
        PartogramConstants.Params.TEMPERATURE.conceptId,
        PartogramConstants.Params.URINE_PROTEIN.conceptId,
        PartogramConstants.Params.URINE_ACETONE.conceptId,
        PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId,
        PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId
    )

    // Stage 3 - Woman
    @JvmField
    val STAGE_3_WOMAN: Set<String> = setOf(
        PartogramConstants.Params.PULSE.conceptId,
        PartogramConstants.Params.SYSTOLIC_BP.conceptId,
        PartogramConstants.Params.DIASTOLIC_BP.conceptId,
        PartogramConstants.Params.TEMPERATURE.conceptId,
        PartogramConstants.Params.RESPIRATORY_RATE_MOTHER.conceptId,
        PartogramConstants.Params.BLOOD_LOSS_MOTHER.conceptId,
        PartogramConstants.Params.UTERUS_CONTRACTED_MOTHER.conceptId,
        PartogramConstants.Params.URINE_PASSED_MOTHER.conceptId,
        PartogramConstants.Params.HEMATOMA_MOTHER.conceptId,
        PartogramConstants.Params.ONGOING_COMPLICATIONS_MOTHER.conceptId,
        PartogramConstants.Params.ASSESSMENT_MOTHER.conceptId,
        PartogramConstants.Params.PLAN_MOTHER.conceptId
    )

    // Stage 3 - Newborn
    @JvmField
    val STAGE_3_NEWBORN: Set<String> = setOf(
        PartogramConstants.Params.GRUNTING_NEWBORN.conceptId,
        PartogramConstants.Params.CHEST_INDRAWING_NEWBORN.conceptId,
        PartogramConstants.Params.FAST_BREATHING_NEWBORN.conceptId,
        PartogramConstants.Params.RESPIRATORY_RATE_NEWBORN.conceptId,
        PartogramConstants.Params.SPO2_NEWBORN.conceptId,
        PartogramConstants.Params.FEET_WARM_NEWBORN.conceptId,
        PartogramConstants.Params.TEMPERATURE_NEWBORN.conceptId,
        PartogramConstants.Params.SKIN_COLOR_NEWBORN.conceptId,
        PartogramConstants.Params.UC_OOZING_NEWBORN.conceptId,
        PartogramConstants.Params.SUCKING_FEEDING_NEWBORN.conceptId,
        PartogramConstants.Params.ONGOING_COMPLICATIONS_NEWBORN.conceptId,
        PartogramConstants.Params.ASSESSMENT_NEWBORN.conceptId,
        PartogramConstants.Params.PLAN_NEWBORN.conceptId
    )

    // All concepts combined
    @JvmField
    val ALL_RISK_CONCEPTS: Set<String> = STAGE_1_2_CONCEPTS + STAGE_3_WOMAN + STAGE_3_NEWBORN

    // ── Display units per concept ─────────────────────────────────────────────
    // Only numeric params get a unit. Dropdown/radio params are left out.

    private val unitMap: Map<String, String> = mapOf(
        PartogramConstants.Params.BASELINE_FHR.conceptId             to "bpm",
        PartogramConstants.Params.PULSE.conceptId                    to "bpm",
        PartogramConstants.Params.SYSTOLIC_BP.conceptId              to "mmHg",
        PartogramConstants.Params.DIASTOLIC_BP.conceptId             to "mmHg",
        PartogramConstants.Params.TEMPERATURE.conceptId              to "°F",
        PartogramConstants.Params.TEMPERATURE_NEWBORN.conceptId      to "°F",
        PartogramConstants.Params.RESPIRATORY_RATE_MOTHER.conceptId  to "/min",
        PartogramConstants.Params.RESPIRATORY_RATE_NEWBORN.conceptId to "/min",
        PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId   to "min",
        PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId  to "",
        PartogramConstants.Params.BLOOD_LOSS_MOTHER.conceptId        to "ml",
        PartogramConstants.Params.SPO2_NEWBORN.conceptId             to "%"
    )

    private val alertThresholdMap: Map<String, String> = mapOf(

        // Supportive care
        PartogramConstants.Params.COMPANION.conceptId
                to "No",

        PartogramConstants.Params.PAIN_RELIEF.conceptId
                to "No",

        PartogramConstants.Params.ORAL_FLUID.conceptId
                to "No",

        PartogramConstants.Params.POSTURE.conceptId
                to "Supine",

        // Fetal condition
        PartogramConstants.Params.BASELINE_FHR.conceptId
                to "<90 or >220 bpm",

        PartogramConstants.Params.FHR_DEC.conceptId
                to "Late",

        PartogramConstants.Params.AMNIOTIC_FLUID.conceptId
                to "M++ / M+++ / B",

        PartogramConstants.Params.FETAL_POSITION.conceptId
                to "P (Occiput anterior) / T (Occiput transverse)",

        PartogramConstants.Params.CAPUT.conceptId
                to "+++",

        PartogramConstants.Params.MOULDING.conceptId
                to "+++",

        // Maternal condition
        PartogramConstants.Params.PULSE.conceptId
                to "<30 or >250 bpm",

        PartogramConstants.Params.SYSTOLIC_BP.conceptId
                to "<30 or >300 mmHg",

        PartogramConstants.Params.DIASTOLIC_BP.conceptId
                to "<20 or >180 mmHg",

        PartogramConstants.Params.TEMPERATURE.conceptId
                to "<89.6 °F or >109.0 °F",

        PartogramConstants.Params.RESPIRATORY_RATE_MOTHER.conceptId
                to "<10 or >80 breaths/min",

        PartogramConstants.Params.URINE_PROTEIN.conceptId
                to "P2+ / P3+ / P4+",

        PartogramConstants.Params.URINE_ACETONE.conceptId
                to "A2+ / A3+ / A4+",

        PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId
                to "≤2 or >5",

        PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId
                to "<20 or >60 sec"
    )
    // ── Public API ────────────────────────────────────────────────────────────

    fun unitFor(conceptId: String): String = unitMap[conceptId] ?: ""

    fun formatValue(conceptId: String, rawValue: String): String {
        val unit = unitFor(conceptId)
        return if (unit.isBlank()) rawValue else "$rawValue $unit"
    }

    fun alertThresholdFor(conceptId: String): String =
        alertThresholdMap[conceptId] ?: ""

    fun hasAlertThreshold(conceptId: String): Boolean =
        alertThresholdMap.containsKey(conceptId)
}