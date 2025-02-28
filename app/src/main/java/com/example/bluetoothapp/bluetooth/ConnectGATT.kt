package com.example.bluetoothapp.bluetooth


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.bluetoothapp.MainApplication
import com.example.bluetoothapp.bluetooth.server.GATTServerService.Companion.CHARACTERISTIC_UUID
import com.example.bluetoothapp.bluetooth.server.GATTServerService.Companion.SERVICE_UUID
import com.example.bluetoothapp.data.CrashAlert
import com.example.bluetoothapp.data.CrashSeverity
import com.example.bluetoothapp.notifications.CrashNotificationManager
import com.example.bluetoothapp.repository.ConnectedDevicesRepository
import com.example.bluetoothapp.viewmodels.CrashAlertViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConnectGATT(
    crashAlertViewModel: CrashAlertViewModel = hiltViewModel()
) {
    // Get the application context for repositories and services
    val appContext = LocalContext.current
    val connectedDevicesRepository = remember { 
        (appContext.applicationContext as MainApplication).connectedDevicesRepository 
    }
    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }
    
    // Create notification manager
    val crashNotificationManager = remember { CrashNotificationManager(appContext) }
    
    // Check that BT permissions and that BT is available and enabled
    BluetoothSampleBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                // Scans for BT devices and handles clicks (see FindDeviceSample)
                FindDevicesScreen {
                    selectedDevice = it
                }
            } else {
                // Once a device is selected show the UI and try to connect device
                ConnectDeviceScreen(
                    device = device,
                    onMessageReceived = { message -> 
                        // Process crash data if received from M5Stick
                        processCrashData(
                            device = device,
                            message = message,
                            crashAlertViewModel = crashAlertViewModel,
                            crashNotificationManager = crashNotificationManager
                        )
                    },
                    onDeviceConnected = { services ->
                        // Record connected device in repository
                        connectedDevicesRepository.addConnectedDevice(device, services)
                    },
                    onDeviceDisconnected = {
                        // Update device connection status
                        connectedDevicesRepository.markDeviceDisconnected(device.address)
                    },
                    onClose = {
                        // Mark device as disconnected when closing the screen
                        connectedDevicesRepository.markDeviceDisconnected(device.address)
                        selectedDevice = null
                    }
                )
            }
        }
    }
}

/**
 * M5Stick service UUIDs for crash detection
 */
private object M5StickUUIDs {
    const val SERVICE_CRASH_UUID = "00002222-0000-1000-8000-00805f9b34fb"  // Crash Status
    const val SERVICE_TIME_UUID = "22345678-1234-5678-1234-56789abcdef0"   // Timestamp
    const val SERVICE_LOCATION_UUID = "32345678-1234-5678-1234-56789abcdef0" // Location
    const val SERVICE_CRASH_TYPE_UUID = "42345678-1234-5678-1234-56789abcdef0" // Crash Type

    const val CHARACTERISTIC_CRASH_UUID = "abcdef12-3456-7890-1234-56789abcdef1"
    const val CHARACTERISTIC_TIME_UUID = "bcdef123-4567-8901-2345-6789abcdef12"
    const val CHARACTERISTIC_LOCATION_UUID = "cdef1234-5678-9012-3456-789abcdef123"
    const val CHARACTERISTIC_CRASH_TYPE_UUID = "def12345-6789-0123-4567-89abcdef1234"
    
    // Map to convert UUID to service type
    val serviceMap = mapOf(
        CHARACTERISTIC_CRASH_UUID to "crash",
        CHARACTERISTIC_TIME_UUID to "timestamp",
        CHARACTERISTIC_LOCATION_UUID to "location",
        CHARACTERISTIC_CRASH_TYPE_UUID to "crashType"
    )
}

/**
 * Process incoming data from a BLE device, looking for crash alerts from any service
 */
private fun processCrashData(
    device: BluetoothDevice,
    message: String,
    crashAlertViewModel: CrashAlertViewModel,
    crashNotificationManager: CrashNotificationManager
) {
    val TAG = "ConnectGATT"
    try {
        val deviceName = device.name ?: "Unknown Device"
        val deviceAddress = device.address
        
        // Process according to the message content and format
        if (message.startsWith("CRASH:")) {
            // Legacy format - handle directly
            Log.d(TAG, "Received legacy crash data: $message from $deviceName")
            val crashAlert = processCrashAlertDirectMessage(deviceAddress, deviceName, message)
            if (crashAlert != null) {
                // Legacy alert handling - show notification directly
                crashNotificationManager.showCrashNotification(crashAlert)
            }
        } else if (message.startsWith("CRASH_CONFIRMED")) {
            // M5Stick crash status service
            Log.d(TAG, "Received crash status: $message from $deviceName")
            processCrashServiceData(
                deviceAddress, 
                deviceName, 
                M5StickUUIDs.CHARACTERISTIC_CRASH_UUID, 
                message, 
                crashAlertViewModel
            )
        } else if (message.contains("Lat:") && message.contains("Lon:")) {
            // M5Stick location service
            Log.d(TAG, "Received location data: $message from $deviceName")
            processCrashServiceData(
                deviceAddress, 
                deviceName, 
                M5StickUUIDs.CHARACTERISTIC_LOCATION_UUID, 
                message, 
                crashAlertViewModel
            )
        } else if (message.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
            // M5Stick timestamp service
            Log.d(TAG, "Received timestamp: $message from $deviceName")
            processCrashServiceData(
                deviceAddress, 
                deviceName, 
                M5StickUUIDs.CHARACTERISTIC_TIME_UUID, 
                message, 
                crashAlertViewModel
            )
        } else if (message.contains("Crash") || message.contains("Impact") || message.contains("Fall") || message.contains("Tilt")) {
            // M5Stick crash type service
            Log.d(TAG, "Received crash type: $message from $deviceName")
            processCrashServiceData(
                deviceAddress, 
                deviceName, 
                M5StickUUIDs.CHARACTERISTIC_CRASH_TYPE_UUID, 
                message, 
                crashAlertViewModel
            )
        } else {
            // Unknown format - try to process with a generic service
            Log.d(TAG, "Received unknown data format: $message from $deviceName")
            processCrashServiceData(
                deviceAddress, 
                deviceName, 
                "unknown", 
                message, 
                crashAlertViewModel
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error processing crash data", e)
    }
}

/**
 * Process data from a specific M5Stick service
 */
private fun processCrashServiceData(
    deviceAddress: String,
    deviceName: String,
    serviceUUID: String,
    message: String,
    crashAlertViewModel: CrashAlertViewModel
) {
    // Forward to the ViewModel for processing - it will accumulate data from multiple services
    crashAlertViewModel.processCrashData(
        deviceAddress = deviceAddress,
        deviceName = deviceName,
        serviceUUID = serviceUUID,
        data = message.toByteArray()
    )
}

/**
 * Process a direct crash alert message (legacy format)
 */
private fun processCrashAlertDirectMessage(
    deviceAddress: String,
    deviceName: String,
    message: String
): CrashAlert? {
    try {
        // Example format: "CRASH:9.8:HIGH"
        val parts = message.removePrefix("CRASH:").split(":")
        if (parts.isNotEmpty()) {
            val acceleration = parts[0].toFloatOrNull() ?: 0f
            val severity = when {
                parts.size >= 2 -> parseSeverity(parts[1])
                acceleration > 15f -> CrashSeverity.CRITICAL
                acceleration > 10f -> CrashSeverity.HIGH
                acceleration > 5f -> CrashSeverity.MEDIUM
                else -> CrashSeverity.LOW
            }
            
            return CrashAlert(
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                accelerationValue = acceleration,
                severity = severity,
                crashType = if (parts.size >= 2) parts[1] + " Crash" else "Impact Crash",
                crashStatus = "CRASH_CONFIRMED"
            )
        }
    } catch (e: Exception) {
        Log.e("ConnectGATT", "Error parsing crash alert", e)
    }
    return null
}

private fun parseSeverity(severityString: String): CrashSeverity {
    return when (severityString.uppercase()) {
        "LOW" -> CrashSeverity.LOW
        "MEDIUM" -> CrashSeverity.MEDIUM
        "HIGH" -> CrashSeverity.HIGH
        "CRITICAL" -> CrashSeverity.CRITICAL
        else -> CrashSeverity.MEDIUM
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(
    device: BluetoothDevice, 
    onMessageReceived: (String) -> Unit = {},
    onDeviceConnected: (List<BluetoothGattService>) -> Unit = {},
    onDeviceDisconnected: () -> Unit = {},
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Keeps track of the last connection state with the device
    var state by remember(device) {
        mutableStateOf<DeviceConnectionState?>(null)
    }
    
    // Track previous connection state to detect changes
    var previousConnectionState by remember { mutableStateOf(-1) }
    
    // Once the device services are discovered find the GATTServerSample service
    val service by remember(state?.services) {
        mutableStateOf(state?.services?.find { it.uuid == SERVICE_UUID })
    }
    // If the GATTServerSample service is found, get the characteristic
    val characteristic by remember(service) {
        mutableStateOf(service?.getCharacteristic(CHARACTERISTIC_UUID))
    }

    // Track if the message received has changed to trigger the callback
    val messageReceived by remember(state?.messageReceived) {
        mutableStateOf(state?.messageReceived)
    }
    
    // Call the onMessageReceived callback when new messages arrive
    DisposableEffect(messageReceived) {
        if (!messageReceived.isNullOrEmpty()) {
            onMessageReceived(messageReceived!!)
        }
        onDispose { }
    }
    
    // Track connection state changes
    LaunchedEffect(state?.connectionState) {
        val currentState = state?.connectionState ?: -1
        
        // Check for connection established
        if (previousConnectionState != BluetoothProfile.STATE_CONNECTED && 
            currentState == BluetoothProfile.STATE_CONNECTED) {
            // Just connected
            Log.d("ConnectGATT", "Device ${device.address} connected")
        }
        
        // Check for disconnection
        if (previousConnectionState == BluetoothProfile.STATE_CONNECTED && 
            currentState != BluetoothProfile.STATE_CONNECTED) {
            // Just disconnected
            Log.d("ConnectGATT", "Device ${device.address} disconnected")
            onDeviceDisconnected()
        }
        
        previousConnectionState = currentState
    }
    
    // Track services discovery
    LaunchedEffect(state?.services) {
        if (!state?.services.isNullOrEmpty()) {
            Log.d("ConnectGATT", "Device ${device.address} services discovered: ${state?.services?.size}")
            onDeviceConnected(state?.services ?: emptyList())
        }
    }

    // This effect will handle the connection and notify when the state changes
    BLEConnectEffect(device = device) {
        // update our state to recompose the UI
        state = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Name: ${device.name} (${device.address})")
        
        // Connection status with indicator color
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            val connectionState = state?.connectionState ?: -1
            val isConnected = connectionState == BluetoothProfile.STATE_CONNECTED
            
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionState) {
                            BluetoothProfile.STATE_CONNECTING -> Color.Yellow
                            BluetoothProfile.STATE_CONNECTED -> Color.Green
                            BluetoothProfile.STATE_DISCONNECTING -> Color.Red.copy(alpha = 0.5f)
                            else -> Color.Red
                        }
                    )
                    .border(1.dp, Color.White, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(8.dp).height(8.dp))
            
            Text(text = "Status: ${state?.connectionState?.toConnectionStateString()}")
        }
        
        Text(text = "MTU: ${state?.mtu}")
        
        // Improved services display
        if (!state?.services.isNullOrEmpty()) {
            Text(text = "Services:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            state?.services?.forEach { service ->
                Text(
                    text = "• ${service.uuid}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        } else {
            Text(text = "Services: None discovered yet")
        }
        
        Text(text = "Message sent: ${state?.messageSent}")
        Text(text = "Message received: ${state?.messageReceived}")
        
        // Connection controls
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    if (state?.connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                        state?.gatt?.connect()
                    }
                    // Example on how to request specific MTUs
                    // Note that from Android 14 onwards the system will define a default MTU and
                    // it will only be sent once to the peripheral device
                    state?.gatt?.requestMtu(Random.nextInt(27, 190))
                }
            },
        ) {
            Text(text = "Request MTU")
        }
        Button(
            enabled = state?.gatt != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Once we have the connection discover the peripheral services
                    state?.gatt?.discoverServices()
                }
            },
        ) {
            Text(text = "Discover")
        }
        Button(
            enabled = state?.gatt != null && characteristic != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    sendData(state?.gatt!!, characteristic!!)
                }
            },
        ) {
            Text(text = "Write to server")
        }
        Button(
            enabled = state?.gatt != null && characteristic != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    state?.gatt?.readCharacteristic(characteristic)
                }
            },
        ) {
            Text(text = "Read characteristic")
        }
        
        // Send test crash for demo purposes
        Button(
            onClick = {
                // First test message - Crash Status
                onMessageReceived("CRASH_CONFIRMED")
                
                // Delay simulation for readability
                scope.launch {
                    delay(500)
                    
                    // Second test message - Timestamp from Arduino RTC
                    val timestampMessage = "2023-02-28 14:35:42"
                    onMessageReceived(timestampMessage)
                    
                    delay(500)
                    
                    // Third test message - Location
                    val locationMessage = "Lat: 1.3521, Lon: 103.8198"
                    onMessageReceived(locationMessage)
                    
                    delay(500)
                    
                    // Fourth test message - Crash Type
                    val crashTypeMessage = "High Impact Crash"
                    onMessageReceived(crashTypeMessage)
                }
            },
        ) {
            Text(text = "Simulate M5Stick Crash Alert")
        }
        
        // Legacy crash format for testing
        Button(
            onClick = {
                // Create a legacy test crash message
                val testMessage = "CRASH:12.5:HIGH"
                onMessageReceived(testMessage)
            },
        ) {
            Text(text = "Simulate Legacy Crash Alert")
        }
        
        Button(onClick = onClose) {
            Text(text = "Close")
        }
    }
}

/**
 * Writes "hello world" to the server characteristic
 */
@SuppressLint("MissingPermission")
private fun sendData(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
) {
    val data = "Hello world!".toByteArray()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            characteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        )
    } else {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @Suppress("DEPRECATION")
        characteristic.value = data
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }
}

internal fun Int.toConnectionStateString() = when (this) {
    BluetoothProfile.STATE_CONNECTED -> "Connected"
    BluetoothProfile.STATE_CONNECTING -> "Connecting"
    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
    else -> "N/A"
}

private data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    val services: List<BluetoothGattService> = emptyList(),
    val messageSent: Boolean = false,
    val messageReceived: String = "",
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
private fun BLEConnectEffect(
    device: BluetoothDevice,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStateChange: (DeviceConnectionState) -> Unit,
) {
    val context = LocalContext.current
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    // Keep the current connection state
    var state by remember {
        mutableStateOf(DeviceConnectionState.None)
    }

    DisposableEffect(lifecycleOwner, device) {
        // This callback will notify us when things change in the GATT connection so we can update
        // our state
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                super.onConnectionStateChange(gatt, status, newState)
                state = state.copy(gatt = gatt, connectionState = newState)
                currentOnStateChange(state)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    // Here you should handle the error returned in status based on the constants
                    // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
                    // For example for GATT_INSUFFICIENT_ENCRYPTION or
                    // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
                    // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
                    Log.e("BLEConnectEffect", "An error happened: $status")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                state = state.copy(gatt = gatt, mtu = mtu)
                currentOnStateChange(state)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                state = state.copy(services = gatt.services)
                currentOnStateChange(state)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int,
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                state = state.copy(messageSent = status == BluetoothGatt.GATT_SUCCESS)
                currentOnStateChange(state)
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    doOnRead(characteristic.value)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                doOnRead(value)
            }

            private fun doOnRead(value: ByteArray) {
                state = state.copy(messageReceived = value.decodeToString())
                currentOnStateChange(state)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (state.gatt != null) {
                    // If we previously had a GATT connection let's reestablish it
                    state.gatt?.connect()
                } else {
                    // Otherwise create a new GATT connection
                    state = state.copy(gatt = device.connectGatt(context, false, callback))
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Unless you have a reason to keep connected while in the bg you should disconnect
                state.gatt?.disconnect()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and close the connection
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.gatt?.close()
            state = DeviceConnectionState.None
        }
    }
}