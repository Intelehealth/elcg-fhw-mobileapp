package org.intelehealth.ezazi.stage3.Utils

import com.google.android.material.textfield.TextInputLayout
import org.intelehealth.ezazi.databinding.ActivityWomenDeliveryDetailsBinding

class DeliveryDetailsErrorHandler(
    private val binding: ActivityWomenDeliveryDetailsBinding
) {

    fun clearErrors() {
        binding.etlDateOfDelivery.error = null
        binding.etlTimeOfDelivery.error = null
        binding.etlModeOfDeliveryOtherOption.error = null
        binding.etlDegreeOfPerinealTear.error = null
        binding.etlPlacentaMembraneDelivery.error = null
        binding.etlTimeOfPlacentaDelivery.error = null
        binding.etlAmtslOtherOption.error = null
        binding.etlTypeOfBirth.error = null
        binding.etlSex.error = null
        binding.etlApgar1.error = null
        binding.etlApgar5.error = null
        binding.etlBirthWeightGrams.error = null
        binding.etlCongenitalYesOptions.error = null
        binding.etlCongenitalYesOtherOption.error = null
    }

    fun showError(layout: TextInputLayout, message: String) {
        layout.error = message
    }
}