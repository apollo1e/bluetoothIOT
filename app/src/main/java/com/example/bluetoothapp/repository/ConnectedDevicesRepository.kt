package com.example.bluetoothapp.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.bluetoothapp.data.ConnectedDevice
import com.example.bluetoothapp.data.Device
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Repository to manage and persist connected Bluetooth devices
 */
@Singleton
class ConnectedDevicesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ConnectedDevicesRepo"
    
    // SharedPreferences for storing device info
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // In-memory cache of connected devices
    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: Flow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()
    
    init {
        // Load saved devices when repository is created
        loadSavedDevices()
    }
    
    /**
     * Add or update a connected device
     */
    fun addConnectedDevice(device: BluetoothDevice, services: List<BluetoothGattService> = emptyList()) {
        Log.d(TAG, "Adding connected device: ${device.name ?: "Unknown"} (${device.address})")
        
        val serviceUuids = services.map { it.uuid.toString() }
        
        val existingDeviceIndex = _connectedDevices.value.indexOfFirst { it.address == device.address }
        val connectedDevice = ConnectedDevice.fromBluetoothDevice(device, serviceUuids)
        
        val updatedList = _connectedDevices.value.toMutableList()
        
        if (existingDeviceIndex >= 0) {
            // Update existing device
            updatedList[existingDeviceIndex] = connectedDevice.copy(
                lastSeen = Date(),
                isConnected = true,
                supportedServices = serviceUuids
            )
        } else {
            // Add new device
            updatedList.add(connectedDevice)
        }
        
        _connectedDevices.value = updatedList
        saveDevices()
    }
    
    /**
     * Mark device as disconnected
     */
    fun markDeviceDisconnected(address: String) {
        val existingDeviceIndex = _connectedDevices.value.indexOfFirst { it.address == address }
        
        if (existingDeviceIndex >= 0) {
            val updatedList = _connectedDevices.value.toMutableList()
            val device = updatedList[existingDeviceIndex]
            
            updatedList[existingDeviceIndex] = device.copy(
                isConnected = false,
                lastSeen = Date()
            )
            
            _connectedDevices.value = updatedList
            saveDevices()
        }
    }
    
    /**
     * Remove a device from the repository
     */
    fun removeDevice(address: String) {
        val updatedList = _connectedDevices.value.filter { it.address != address }
        _connectedDevices.value = updatedList
        saveDevices()
    }
    
    /**
     * Get all devices as Device objects for the UI
     */
    fun getDevicesForUi(): List<Device> {
        return _connectedDevices.value.map { it.toDevice() }
    }
    
    /**
     * Check if a device is already connected
     */
    fun isDeviceConnected(address: String): Boolean {
        return _connectedDevices.value.any { it.address == address && it.isConnected }
    }
    
    /**
     * Get a specific connected device
     */
    fun getConnectedDevice(address: String): ConnectedDevice? {
        return _connectedDevices.value.find { it.address == address }
    }
    
    /**
     * Save devices to persistent storage
     */
    private fun saveDevices() {
        try {
            val deviceListJson = Gson().toJson(_connectedDevices.value)
            prefs.edit().putString(KEY_CONNECTED_DEVICES, deviceListJson).apply()
            Log.d(TAG, "Saved ${_connectedDevices.value.size} devices to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving connected devices", e)
        }
    }
    
    /**
     * Load saved devices from persistent storage
     */
    private fun loadSavedDevices() {
        try {
            val deviceListJson = prefs.getString(KEY_CONNECTED_DEVICES, null)
            if (deviceListJson != null) {
                val type = object : TypeToken<List<ConnectedDevice>>() {}.type
                val savedDevices: List<ConnectedDevice> = Gson().fromJson(deviceListJson, type)
                
                // Mark all as not connected initially (they'll be updated if they reconnect)
                val devices = savedDevices.map { it.copy(isConnected = false) }
                _connectedDevices.value = devices
                
                Log.d(TAG, "Loaded ${devices.size} devices from preferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading connected devices", e)
        }
    }
    
    companion object {
        private const val PREFS_NAME = "bluetooth_connected_devices"
        private const val KEY_CONNECTED_DEVICES = "connected_devices"
    }
}