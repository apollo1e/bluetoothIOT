package com.example.bluetoothapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothapp.data.MqttAlert
import com.example.bluetoothapp.repository.MqttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MqttAlertViewModel @Inject constructor(
    private val mqttRepository: MqttRepository
) : ViewModel() {
    
    // Properties directly from repository
    val mqttAlerts: StateFlow<List<MqttAlert>> = mqttRepository.mqttAlerts
    val connectionStatus: StateFlow<Boolean> = mqttRepository.connectionStatus
    
    // Derived properties
    val unacknowledgedAlerts = mqttRepository.mqttAlerts.map { alerts ->
        alerts.filter { !it.isAcknowledged }
    }
    
    val hasUnacknowledgedAlerts = unacknowledgedAlerts.map { alerts ->
        alerts.isNotEmpty()
    }
    
    // Initialize MQTT connection when viewModel is created
    init {
        // Defer initialization to avoid blocking UI thread
        viewModelScope.launch {
            try {
                mqttRepository.ensureConnected()
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.e("MqttViewModel", "Error initializing MQTT: ${e.message}")
            }
        }
    }
    
    fun acknowledgeMqttAlert(alertId: String) {
        viewModelScope.launch {
            mqttRepository.acknowledgeAlert(alertId)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Only disconnect when viewModel is cleared
        mqttRepository.disconnect()
    }
}