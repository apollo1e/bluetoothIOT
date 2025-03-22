package com.example.bluetoothapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothapp.data.ConnectedDevice
import com.example.bluetoothapp.data.Device
import com.example.bluetoothapp.data.DeviceResponse
import com.example.bluetoothapp.domain.GetDevicesUseCase
import com.example.bluetoothapp.repository.ConnectedDevicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val connectedDevicesRepository: ConnectedDevicesRepository
) : ViewModel() {

    private val _apiDevicesFlow = MutableStateFlow<List<Device>>(emptyList())
    private val _errorState = MutableStateFlow<String?>(null)
    
    // Publicly exposed error state
    val errorState: StateFlow<String?> = _errorState
    
    // Combine API devices with connected devices
    val devicesFlow: StateFlow<List<Device>> = 
        combine(
            _apiDevicesFlow,
            connectedDevicesRepository.connectedDevices
        ) { apiDevices, connectedDevices ->
            // Convert connected devices to Device objects
            val connectedDevicesList = connectedDevices.map { it.toDevice() }
            
            // Add the connected devices to the beginning of the list
            connectedDevicesList + apiDevices.filter { api ->
                // Filter out API devices that have the same MAC address as connected devices
                // to avoid duplicates
                connectedDevices.none { it.address == api.macAddress }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    // Separate flow of only connected devices
    val connectedDevices: StateFlow<List<ConnectedDevice>> = 
        connectedDevicesRepository.connectedDevices
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        // Start the use case when the ViewModel is created
        loadDevices()
    }
    
    /**
     * Load devices from the repository with error handling
     */
    fun loadDevices() {
        viewModelScope.launch {
            try {
                // Clear any previous errors
                _errorState.value = null
                
                getDevicesUseCase().collect { deviceResponse ->
                    // Update the StateFlow with the list of devices from API
                    _apiDevicesFlow.value = deviceResponse.devices
                }
            } catch (e: Exception) {
                // In case the error still propagates to here despite our safety measures
                Log.e("DevicesViewModel", "Error loading devices", e)
                _errorState.value = "Couldn't load devices: ${e.localizedMessage}"
                
                // We'll still provide an empty list to not break the UI
                _apiDevicesFlow.value = emptyList()
            }
        }
    }
    
    /**
     * Remove a previously connected device from the list
     */
    fun removeConnectedDevice(address: String) {
        connectedDevicesRepository.removeDevice(address)
    }
    
    /**
     * Get connection status of a device
     */
    fun isDeviceConnected(address: String): Boolean {
        return connectedDevicesRepository.isDeviceConnected(address)
    }
}