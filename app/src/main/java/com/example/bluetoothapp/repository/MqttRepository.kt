package com.example.bluetoothapp.repository

import android.content.Context
import com.example.bluetoothapp.data.MqttAlert
import com.example.bluetoothapp.service.MqttClientWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for MQTT functionality
 * Delegates actual MQTT operations to MqttClientWrapper to isolate potential issues
 */
@Singleton
class MqttRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Create wrapper which handles all MQTT client operations
    private val mqttClientWrapper = MqttClientWrapper(context)
    
    val mqttAlerts: StateFlow<List<MqttAlert>> = mqttClientWrapper.mqttAlerts
    val connectionStatus: StateFlow<Boolean> = mqttClientWrapper.connectionStatus

    // Lazy initialization - only connect when needed
    fun ensureConnected() {
        try {
            mqttClientWrapper.connect()
        } catch (e: Exception) {
            // Log but don't crash
            android.util.Log.e("MqttRepository", "Error ensuring connection: ${e.message}")
        }
    }

    fun acknowledgeAlert(alertId: String) {
        mqttClientWrapper.acknowledgeAlert(alertId)
    }

    fun disconnect() {
        mqttClientWrapper.disconnect()
    }
}