package com.example.bluetoothapp.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import com.example.bluetoothapp.data.ConnectedDevice
import com.example.bluetoothapp.data.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import javax.inject.Inject

/**
 * Mock implementation of ConnectedDevicesRepository for testing
 */
class MockConnectedDevicesRepository @Inject constructor() {
    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: Flow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()
    
    // Add some mock data for testing
    init {
        val mockDevices = listOf(
            ConnectedDevice(
                address = "11:22:33:44:55:66",
                name = "Test M5Stick",
                deviceType = "M5Stick",
                connectionTime = Date(),
                lastSeen = Date(),
                isConnected = true,
                supportedServices = listOf(
                    "00002222-0000-1000-8000-00805f9b34fb",  // Crash Status
                    "22345678-1234-5678-1234-56789abcdef0"   // Timestamp
                )
            )
        )
        _connectedDevices.value = mockDevices
    }
    
    fun addConnectedDevice(device: BluetoothDevice, services: List<BluetoothGattService> = emptyList()) {
        // Mock implementation, do nothing for tests
    }
    
    fun markDeviceDisconnected(address: String) {
        // Mock implementation, do nothing for tests
    }
    
    fun removeDevice(address: String) {
        // Mock implementation, do nothing for tests
    }
    
    fun getDevicesForUi(): List<Device> {
        return _connectedDevices.value.map { it.toDevice() }
    }
    
    fun isDeviceConnected(address: String): Boolean {
        return _connectedDevices.value.any { it.address == address && it.isConnected }
    }
    
    fun getConnectedDevice(address: String): ConnectedDevice? {
        return _connectedDevices.value.find { it.address == address }
    }
}