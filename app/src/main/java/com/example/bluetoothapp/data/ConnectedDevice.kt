package com.example.bluetoothapp.data

import java.util.Date

/**
 * Represents a Bluetooth device that is connected to the phone.
 * This model stores essential device information and connection state.
 */
data class ConnectedDevice(
    val address: String,                  // MAC address
    val name: String,                     // Device name as reported by Bluetooth
    val deviceType: String = "Unknown",   // Device type (M5Stick, etc)
    val connectionTime: Date = Date(),    // When the device was connected
    val lastSeen: Date = Date(),          // Last time data was received from device
    val isConnected: Boolean = true,      // Current connection state
    val supportedServices: List<String> = emptyList() // List of service UUIDs supported
) {
    fun toDevice(): Device {
        // Convert to the app's Device model for compatibility with existing UI
        return Device(
            macAddress = address,
            model = name,
            product = deviceType,
            firmwareVersion = "Unknown",
            serial = address,
            installationMode = if (isConnected) "Connected" else "Disconnected",
            brakeLight = false,
            lightMode = "N/A",
            lightAuto = false,
            lightValue = 0
        )
    }
    
    companion object {
        // Create from a Bluetooth device
        fun fromBluetoothDevice(device: android.bluetooth.BluetoothDevice, services: List<String> = emptyList()): ConnectedDevice {
            return ConnectedDevice(
                address = device.address,
                name = device.name ?: "Unknown Device",
                deviceType = when {
                    device.name?.contains("M5Stick", ignoreCase = true) == true -> "M5Stick"
                    services.any { it.contains("crash", ignoreCase = true) } -> "Crash Sensor"
                    else -> "Bluetooth Device"
                },
                supportedServices = services
            )
        }
    }
}