package com.example.bluetoothapp.mqtt

import android.content.Context
import android.util.Log
import com.example.bluetoothapp.data.MqttCrashAlert
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.MqttClientSslConfig
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.UUID
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.TrustManagerFactory
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages MQTT client connections with TLS support
 * Provides methods to connect, subscribe, and receive messages from the MQTT broker
 */
@Singleton
class MqttClientManager @Inject constructor(private val context: Context) {
    companion object {
        private const val TAG = "MqttClientManager"
        private const val CLIENT_ID_PREFIX = "android-mqtt-client-"
        private const val MQTT_BROKER_HOST = "192.168.1.2" // Mosquitto broker IP
        private const val MQTT_BROKER_PORT = 8883 // TLS port
        private const val MQTT_TOPIC = "iot/crashalerts" // Topic to subscribe to
    }

    // Callback to deliver messages to UI
    private var messageCallback: ((MqttCrashAlert) -> Unit)? = null
    private var connectionCallback: ((Boolean) -> Unit)? = null
    
    // HiveMQ MQTT client instances
    private var mqttClient: Mqtt3AsyncClient? = null
    
    // Certificate and key file names
    private val rootCaFile = "certs/rootCA.crt"
    private val clientCertFile = "certs/client.crt"
    private val clientKeyFile = "certs/client.key"

    /**
     * Initializes the MQTT client with TLS configuration
     */
    fun initialize() {
        try {
            val clientId = CLIENT_ID_PREFIX + UUID.randomUUID().toString()
            
            // Create SSL/TLS configuration
            val sslConfig = createSslConfig()
            
            // Build and configure the MQTT client with TLS support
            mqttClient = MqttClient.builder()
                .identifier(clientId)
                .serverHost(MQTT_BROKER_HOST)
                .serverPort(MQTT_BROKER_PORT)
                .sslConfig(sslConfig)
                .useMqttVersion3()
                .buildAsync()
            
            Log.d(TAG, "MQTT Client initialized with client ID: $clientId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MQTT client: ${e.message}", e)
            connectionCallback?.invoke(false)
        }
    }

    /**
     * Creates SSL configuration using the certificates and keys
     */
    private fun createSslConfig(): MqttClientSslConfig {
        // Load the CA certificate
        val caInputStream = context.assets.open(rootCaFile)
        val caCert = loadCertificate(caInputStream)
        
        // Load the client certificate
        val clientCertInputStream = context.assets.open(clientCertFile)
        val clientCert = loadCertificate(clientCertInputStream)
        
        // Load the client private key
        val clientKeyInputStream = context.assets.open(clientKeyFile)
        val clientKey = loadPrivateKey(clientKeyInputStream)
        
        // Create a KeyStore and add the client certificate and private key
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        
        // Add the client cert and key to the keystore
        keyStore.setKeyEntry("client", clientKey, "".toCharArray(), arrayOf(clientCert))
        
        // Create a KeyManagerFactory with the keystore
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "".toCharArray())
        
        // Create a TrustManagerFactory with the CA certificate
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        trustStore.load(null, null)
        trustStore.setCertificateEntry("ca", caCert)
        
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        
        // Build and return the SSL config
        return MqttClientSslConfig.builder()
            .keyManagerFactory(keyManagerFactory)
            .trustManagerFactory(trustManagerFactory)
            .build()
    }

    /**
     * Loads a certificate from an input stream
     */
    private fun loadCertificate(inputStream: InputStream): Certificate {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(inputStream)
    }

    /**
     * Loads a private key from an input stream
     */
    private fun loadPrivateKey(inputStream: InputStream): PrivateKey {
        val keyBytes = cleanPrivateKey(inputStream.readBytes().toString(Charsets.UTF_8))
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBytes))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    /**
     * Cleans private key content by removing headers and whitespace
     */
    private fun cleanPrivateKey(privateKey: String): String {
        return privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
    }

    /**
     * Connects to the MQTT broker
     */
    fun connect() {
        if (mqttClient == null) {
            initialize()
        }
        
        try {
            mqttClient?.connectWith()
                ?.cleanSession(true)
                ?.keepAlive(60)
                ?.send()
                ?.whenComplete { connAck, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Connection failed: ${throwable.message}", throwable)
                        connectionCallback?.invoke(false)
                    } else {
                        Log.d(TAG, "Connected successfully: $connAck")
                        connectionCallback?.invoke(true)
                        
                        // Subscribe to topic after successful connection
                        subscribe()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during connection: ${e.message}", e)
            connectionCallback?.invoke(false)
        }
    }

    /**
     * Subscribes to the MQTT topic
     */
    private fun subscribe() {
        try {
            mqttClient?.subscribeWith()
                ?.topicFilter(MQTT_TOPIC)
                ?.qos(MqttQos.AT_LEAST_ONCE)
                ?.callback { publish -> processMessage(publish) }
                ?.send()
                ?.whenComplete { subAck, throwable ->
                    if (throwable != null) {
                        Log.e(TAG, "Subscription failed: ${throwable.message}", throwable)
                    } else {
                        Log.d(TAG, "Subscribed successfully: $subAck")
                    }
                }
                
            // Also add a global callback to catch any other messages
            mqttClient?.publishes(MqttGlobalPublishFilter.ALL, Consumer { publish ->
                processMessage(publish)
            })
            
            Log.d(TAG, "Subscribed to topic: $MQTT_TOPIC")
        } catch (e: Exception) {
            Log.e(TAG, "Error during subscription: ${e.message}", e)
        }
    }

    /**
     * Processes received MQTT messages
     */
    private fun processMessage(publish: com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish) {
        try {
            val topic = publish.topic.toString()
            val payload = String(publish.payloadAsBytes)
            
            Log.d(TAG, "Received message on topic '$topic': $payload")
            
            // Parse the message if it's from our expected topic
            if (topic == MQTT_TOPIC) {
                val gson = Gson()
                val crashAlert = gson.fromJson(payload, MqttCrashAlert::class.java)
                
                // Extra validation to ensure we have a valid object
                if (crashAlert != null) {
                    Log.d(TAG, "Successfully parsed crash alert: ${crashAlert.crashId}, type: ${crashAlert.crashType}")
                    
                    // Notify UI about the new message
                    messageCallback?.invoke(crashAlert)
                } else {
                    Log.e(TAG, "Failed to parse crash alert: null object returned")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: ${e.message}", e)
            
            // Try to handle specific parsing errors and provide more helpful debugging
            if (e.message?.contains("Expected a string but was BEGIN_OBJECT") == true) {
                Log.e(TAG, "JSON structure mismatch. Check if the data class matches the JSON structure in the message.")
            }
        }
    }

    /**
     * Disconnects from the MQTT broker
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            Log.d(TAG, "Disconnected from MQTT broker")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Set callback for receiving messages
     */
    fun setMessageCallback(callback: (MqttCrashAlert) -> Unit) {
        this.messageCallback = callback
    }

    /**
     * Set callback for connection status
     */
    fun setConnectionCallback(callback: (Boolean) -> Unit) {
        this.connectionCallback = callback
    }
}