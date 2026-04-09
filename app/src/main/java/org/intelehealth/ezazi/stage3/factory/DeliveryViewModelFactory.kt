package org.intelehealth.ezazi.stage3.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.intelehealth.ezazi.stage3.db.DeliveryRepository
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.viewmodel.DeliveryViewModel

class DeliveryViewModelFactory(
    private val saveDeliveryDetailsUseCase: SaveDeliveryDetailsUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeliveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeliveryViewModel(saveDeliveryDetailsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}