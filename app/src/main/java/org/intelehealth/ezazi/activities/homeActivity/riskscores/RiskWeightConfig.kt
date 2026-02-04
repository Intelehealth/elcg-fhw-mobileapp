package org.intelehealth.ezazi.activities.homeActivity.riskscores

import org.intelehealth.ezazi.partogram.PartogramConstants

object RiskWeightConfig {

    const val YELLOW_WEIGHT = 0.5

    val redWeights: Map<RedKey, Double> = mapOf(

        // ---------------- Supportive care ----------------
        // Oral fluid: No / Declines = Red (1)
        RedKey(PartogramConstants.Params.ORAL_FLUID.conceptId, "N") to 1.0,

        RedKey(PartogramConstants.Params.BASELINE_FHR.conceptId, "<110") to 1.0,
        RedKey(PartogramConstants.Params.BASELINE_FHR.conceptId, ">160") to 1.0,

        // ---------------- Baby ----------------
        // FHR deceleration
        RedKey(PartogramConstants.Params.FHR_DEC.conceptId, "L") to 1.0,
        // Amniotic fluid
        RedKey(PartogramConstants.Params.AMNIOTIC_FLUID.conceptId, "M+++") to 2.0,
        RedKey(PartogramConstants.Params.AMNIOTIC_FLUID.conceptId, "B") to 2.0,

        //Fetal Position
        RedKey(PartogramConstants.Params.FETAL_POSITION.conceptId, "P") to 1.0,
        RedKey(PartogramConstants.Params.FETAL_POSITION.conceptId, "T") to 1.0,

        //Caput
        RedKey(PartogramConstants.Params.CAPUT.conceptId, "+++") to 1.0,
        //Moulding
        RedKey(PartogramConstants.Params.MOULDING.conceptId, "+++") to 1.0,

        // ---------------- Woman ----------------
        // Pulse
        RedKey(PartogramConstants.Params.PULSE.conceptId, ">120") to 1.0,
        RedKey(PartogramConstants.Params.PULSE.conceptId, "<60") to 1.0,

        // Systolic BP
        RedKey(PartogramConstants.Params.SYSTOLIC_BP.conceptId, "<80") to 1.0,
        RedKey(PartogramConstants.Params.SYSTOLIC_BP.conceptId, ">=140") to 1.0,

        // Diastolic BP
        RedKey(PartogramConstants.Params.DIASTOLIC_BP.conceptId, ">=90") to 1.0,

        // Temperature
        RedKey(PartogramConstants.Params.TEMPERATURE.conceptId, "<35") to 1.0,
        RedKey(PartogramConstants.Params.TEMPERATURE.conceptId, ">=37.5") to 1.0,

        // Urine protein
        RedKey(PartogramConstants.Params.URINE_PROTEIN.conceptId, "P2+") to 1.0,
        RedKey(PartogramConstants.Params.URINE_PROTEIN.conceptId, "P3+") to 1.5,
        RedKey(PartogramConstants.Params.URINE_PROTEIN.conceptId, "P4+") to 2.0,

        // Urine Acetone
        RedKey(PartogramConstants.Params.URINE_ACETONE.conceptId, "A2+") to 1.0,
        RedKey(PartogramConstants.Params.URINE_ACETONE.conceptId, "A3+") to 1.5,
        RedKey(PartogramConstants.Params.URINE_ACETONE.conceptId, "A4+") to 2.0,

        // ---------------- Labour progress ----------------
        // Contractions per minute
        RedKey(PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId, "<=2") to 1.0,
        RedKey(PartogramConstants.Params.CONTRACTION_PER_10_MIN.conceptId, ">5") to 1.0,

        // Duration of contractions
        RedKey(PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId, "<20") to 1.0,
        RedKey(PartogramConstants.Params.DURATION_OF_CONTRACTION.conceptId, ">60") to 1.0,

        // Cervix plot delays
        ///RedKey(PartogramConstants.Params.CERVIX_PLOT.conceptId, "<6H") to 2.0
    )
}
