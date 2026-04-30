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
}