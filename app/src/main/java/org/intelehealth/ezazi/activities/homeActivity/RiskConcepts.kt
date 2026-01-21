package org.intelehealth.ezazi.activities.homeActivity

import org.intelehealth.ezazi.partogram.PartogramConstants
import java.util.Collections

object RiskConcepts {

    @JvmField
    val RISK_CONCEPTS: Set<String> = Collections.unmodifiableSet(
        hashSetOf(
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
    )
}