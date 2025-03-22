package com.example.bluetoothapp.viewmodels

import androidx.lifecycle.ViewModel
import com.example.bluetoothapp.data.MqttCrashAlert
import com.example.bluetoothapp.mqtt.MqttClientManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MqttViewModel @Inject constructor(
    private val mqttClientManager: MqttClientManager
) : ViewModel() {

    /**
     * Initializes the MQTT client
     */
    fun initialize() {
        mqttClientManager.initialize()
    }

    /**
     * Connects to the MQTT broker
     */
    fun connect() {
        mqttClientManager.connect()
    }

    /**
     * Disconnects from the MQTT broker
     */
    fun disconnect() {
        mqttClientManager.disconnect()
    }

    /**
     * Sets a callback to receive MQTT messages
     */
    fun setMessageCallback(callback: (MqttCrashAlert) -> Unit) {
        mqttClientManager.setMessageCallback(callback)
    }

    /**
     * Sets a callback to receive connection status updates
     */
    fun setConnectionCallback(callback: (Boolean) -> Unit) {
        mqttClientManager.setConnectionCallback(callback)
    }

    /**
     * Clean up resources when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        mqttClientManager.disconnect()
    }
}