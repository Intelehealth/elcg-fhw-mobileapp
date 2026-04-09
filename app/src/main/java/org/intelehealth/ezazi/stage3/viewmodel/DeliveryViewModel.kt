package org.intelehealth.ezazi.stage3.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class DeliveryViewModel(
    private val saveDeliveryDetailsUseCase: SaveDeliveryDetailsUseCase
) : ViewModel() {

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun saveDelivery(
        encounterUuid: String,
        deliveryDetails: DeliveryDetails,
        creatorId: String
    ) {
        viewModelScope.launch {

            val result = withContext(Dispatchers.IO) {
                saveDeliveryDetailsUseCase(
                    encounterUuid,
                    deliveryDetails,
                    creatorId
                )
            }

            _saveResult.value = result
        }
    }
}