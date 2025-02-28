package com.example.bluetoothapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.bluetoothapp.data.Device
import com.example.bluetoothapp.domain.GetDevicesUseCase
import com.example.bluetoothapp.viewmodels.DevicesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A simplified version of DevicesViewModel for testing that doesn't depend on ConnectedDevicesRepository
 */
class TestDevicesViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase
) : androidx.lifecycle.ViewModel() {
    private val _apiDevicesFlow = MutableStateFlow<List<Device>>(emptyList())
    
    // Simple flow for testing
    val devicesFlow: StateFlow<List<Device>> = _apiDevicesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                Device(
                    macAddress = "00:11:22:33:44:55",
                    model = "Test Model",
                    product = "Test Product",
                    firmwareVersion = "1.0.0",
                    serial = "TST12345",
                    installationMode = "Standard",
                    brakeLight = true,
                    lightMode = "Auto",
                    lightAuto = true,
                    lightValue = 50
                )
            )
        )

    init {
        // Start the use case when the ViewModel is created
        viewModelScope.launch {
            getDevicesUseCase().collect { deviceResponse ->
                // Update the StateFlow with the list of devices from API
                _apiDevicesFlow.value = deviceResponse.devices
            }
        }
    }
}