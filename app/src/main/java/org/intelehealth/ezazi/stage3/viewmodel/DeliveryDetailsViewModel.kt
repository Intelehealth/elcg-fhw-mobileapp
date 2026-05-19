package org.intelehealth.ezazi.stage3.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intelehealth.ezazi.database.dao.VisitAttributeListDAO
import org.intelehealth.ezazi.models.dto.EncounterDTO
import org.intelehealth.ezazi.stage3.db.SaveDeliveryDetailsUseCase
import org.intelehealth.ezazi.stage3.models.DeliveryDetails

class DeliveryDetailsViewModel(
    private val saveDeliveryDetailsUseCase: SaveDeliveryDetailsUseCase
) : ViewModel() {

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun saveDelivery(
        encounterDTO: EncounterDTO,
        deliveryDetails: DeliveryDetails,
        creatorId: String,
        encounterTypeName: String,
        visitAttributeListDAO: VisitAttributeListDAO
    ) {
        viewModelScope.launch {

            val result = withContext(Dispatchers.IO) { saveDeliveryDetailsUseCase(encounterDTO, deliveryDetails, creatorId, encounterTypeName, visitAttributeListDAO)
            }

            _saveResult.value = result
        }
    }
}