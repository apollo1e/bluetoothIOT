package com.example.bluetoothapp.service

import android.content.Context
import android.util.Log
import com.example.bluetoothapp.data.MqttAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.UUID
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wrapper for MQTT client to isolate its functionality and handle errors
 */
class MqttClientWrapper(private val context: Context) {
    private val _mqttAlerts = MutableStateFlow<List<MqttAlert>>(emptyList())
    val mqttAlerts: StateFlow<List<MqttAlert>> = _mqttAlerts

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    private var mqttClient: MqttAndroidClient? = null
    private val isConnecting = AtomicBoolean(false)

    // Sample data for when connection fails
    private val sampleAlerts = listOf(
        MqttAlert(
            id = "sample1",
            crashID = 3,
            acceleration = 0.947031f,
            tiltAngle = 177.3504f,
            timestamp = "4000-02-09 04:26:30",
            crashType = "Severe Tilt",
            crashStatus = "CRASH_CONFIRMED",
            location = "Lat: 1.3521, Lon: 103.8198"
        ),
        MqttAlert(
            id = "sample2",
            crashID = 4,
            acceleration = 1.247031f,
            tiltAngle = 165.1504f,
            timestamp = "4000-02-10 14:22:10",
            crashType = "High Impact",
            crashStatus = "CRASH_CONFIRMED",
            location = "Lat: 1.4121, Lon: 103.9198"
        )
    )

    init {
        // Initialize with sample data
        _mqttAlerts.value = sampleAlerts
    }

    fun connect() {
        if (!isConnecting.getAndSet(true)) {
            try {
                val clientId = "AndroidApp_" + UUID.randomUUID().toString()
                val broker = "test.mosquitto.org"
                val port = 8883
                val serverUri = "ssl://$broker:$port"
                
                Log.d(TAG, "Creating MQTT client with URI: $serverUri")
                
                // Using try-catch to handle any issues with client creation
                try {
                    mqttClient = MqttAndroidClient(context, serverUri, clientId)
                    setupCallbacks()
                    
                    // Setup connection options
                    val options = MqttConnectOptions().apply {
                        isCleanSession = true
                        connectionTimeout = 10
                        keepAliveInterval = 60
                    }

                    Log.d(TAG, "Attempting to connect to MQTT broker")
                    mqttClient?.connect(options, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.d(TAG, "Connected to MQTT broker")
                            _connectionStatus.value = true
                            subscribeTopic("iot/crashalerts")
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e(TAG, "Failed to connect: ${exception?.message}")
                            isConnecting.set(false)
                            _connectionStatus.value = false
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating MQTT client: ${e.message}")
                    isConnecting.set(false)
                    _connectionStatus.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "MQTT Exception: ${e.message}")
                isConnecting.set(false)
                _connectionStatus.value = false
            }
        }
    }

    private fun setupCallbacks() {
        mqttClient?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.e(TAG, "Connection lost: ${cause?.message}")
                _connectionStatus.value = false
                isConnecting.set(false)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.payload?.let { payload ->
                    val messageContent = String(payload)
                    Log.d(TAG, "Message received: $messageContent")
                    
                    try {
                        val jsonObject = JSONObject(messageContent)
                        val mqttAlert = MqttAlert(
                            id = UUID.randomUUID().toString(),
                            crashID = jsonObject.optInt("crashID", 0),
                            acceleration = jsonObject.optDouble("acceleration", 0.0).toFloat(),
                            tiltAngle = jsonObject.optDouble("tiltAngle", 0.0).toFloat(),
                            timestamp = jsonObject.optString("timestamp", ""),
                            crashType = jsonObject.optString("crashType", ""),
                            crashStatus = jsonObject.optString("crashStatus", ""),
                            location = jsonObject.optString("location", "")
                        )
                        
                        _mqttAlerts.value = _mqttAlerts.value + mqttAlert
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message: ${e.message}")
                    }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Message delivered")
            }
        })
    }

    private fun subscribeTopic(topic: String) {
        try {
            mqttClient?.subscribe(topic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Failed to subscribe: ${exception?.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing: ${e.message}")
        }
    }

    fun acknowledgeAlert(alertId: String) {
        _mqttAlerts.value = _mqttAlerts.value.map { 
            if (it.id == alertId) it.copy(isAcknowledged = true) else it 
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
            _connectionStatus.value = false
            isConnecting.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MqttClientWrapper"
    }
}