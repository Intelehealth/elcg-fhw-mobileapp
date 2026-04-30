package org.intelehealth.ezazi.stage3.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.viewmodel.DeliveryDetailsViewModel

class DeliveryDetailsViewModelFactory(
    private val saveDeliveryDetailsUseCase: SaveDeliveryDetailsUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeliveryDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeliveryDetailsViewModel(saveDeliveryDetailsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}