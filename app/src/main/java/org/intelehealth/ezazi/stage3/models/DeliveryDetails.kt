package org.intelehealth.ezazi.stage3.models


class DeliveryDetails {

    // ===============================
    // SECTION 1: Women Delivery Details
    // ===============================

    var dateOfDelivery: String? = null
    var timeOfDelivery: String? = null

    var modeOfDelivery: String? = null
    var modeOfDeliveryOtherOption: String? = null

    var perinealTear: String? = null
    var degreeOfPerinealTear: String? = null


    // ===============================
    // SECTION 2: Placenta & Membrane Delivery
    // ===============================

    var placentaMembraneStatus: String? = null
    var timeOfPlacentaDelivery: String? = null

    var placentalOrCordAbnormality: String? = null
    var placentalOrCordAbnormalityOther: String? = null

    var amtsl: String? = null
    var amtslOtherOption: String? = null


    // ===============================
    // SECTION 3: Newborn Details
    // ===============================

    var typeOfBirth: String? = null
    var babyGender: String? = null

    var apgarScore1Min: String? = null
    var apgarScore5Min: String? = null

    var resuscitation: String? = null

    var birthWeightGrams: String? = null

    var skinToSkinContact: String? = null
    var breastfeedWithin1Hour: String? = null

    var gestationWeeks: String? = null

    var congenitalAnomalies: String? = null
    var congenitalAnomalySpecification: String? = null
}