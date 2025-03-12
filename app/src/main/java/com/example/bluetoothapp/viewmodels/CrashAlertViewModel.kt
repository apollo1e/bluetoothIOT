package com.example.bluetoothapp.viewmodels

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothapp.data.CrashAlert
import com.example.bluetoothapp.data.CrashSeverity
import com.example.bluetoothapp.repository.CrashAlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import java.util.HashMap
import javax.inject.Inject

@HiltViewModel
class CrashAlertViewModel @Inject constructor(
    private val crashAlertRepository: CrashAlertRepository
) : ViewModel() {

    val crashAlerts: StateFlow<List<CrashAlert>> = crashAlertRepository.crashAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unacknowledgedAlerts: StateFlow<List<CrashAlert>> = crashAlertRepository.unacknowledgedAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasUnacknowledgedAlerts: StateFlow<Boolean> = crashAlertRepository.hasUnacknowledgedAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val unattendedAlerts: StateFlow<List<CrashAlert>> = crashAlertRepository.unattendedAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val hasUnattendedAlerts: StateFlow<Boolean> = crashAlertRepository.hasUnattendedAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Temporary storage for accumulating data from multiple BLE services
    private val pendingAlerts = HashMap<String, MutableMap<String, String>>()
    
    // UUIDs from M5Stick - service/characteristic UUIDs
    companion object {
        private const val CRASH_STATUS_UUID = "abcdef12-3456-7890-1234-56789abcdef1"
        private const val TIMESTAMP_UUID = "bcdef123-4567-8901-2345-6789abcdef12"
        private const val LOCATION_UUID = "cdef1234-5678-9012-3456-789abcdef123"
        private const val CRASH_TYPE_UUID = "def12345-6789-0123-4567-89abcdef1234"
        
        private const val TAG = "CrashAlertViewModel"
    }

    fun acknowledgeCrashAlert(crashAlertId: String) {
        crashAlertRepository.acknowledgeCrashAlert(crashAlertId)
    }
    
    fun markAsAttended(crashAlertId: String) {
        crashAlertRepository.updateAttendanceStatus(crashAlertId, true)
    }
    
    fun markAsUnattended(crashAlertId: String) {
        crashAlertRepository.updateAttendanceStatus(crashAlertId, false)
    }

    fun deleteCrashAlert(crashAlertId: String) {
        crashAlertRepository.deleteCrashAlert(crashAlertId)
    }

    fun clearAllCrashAlerts() {
        crashAlertRepository.clearAllCrashAlerts()
    }
    
    fun getLocationUri(latitude: Double, longitude: Double): String {
        // Create a more reliable Google Maps URI with label
        // This format works better with Google Maps and other map applications
        return "geo:$latitude,$longitude?q=$latitude,$longitude(Crash+Location)"
    }

    /**
     * Process crash data received from an M5Stick over BLE.
     * 
     * This handles data from all four M5Stick services:
     * - Crash status
     * - Timestamp
     * - Location
     * - Crash type
     */
    fun processCrashData(deviceAddress: String, deviceName: String, serviceUUID: String, data: ByteArray) {
        try {
            val dataString = data.decodeToString()
            Log.d(TAG, "Received data from $deviceName ($deviceAddress): $dataString for service $serviceUUID")
            
            // Create key for this device if not exists
            val deviceKey = "$deviceAddress-$deviceName"
            if (!pendingAlerts.containsKey(deviceKey)) {
                pendingAlerts[deviceKey] = mutableMapOf()
            }
            
            // Store data based on service UUID
            when (serviceUUID) {
                CRASH_STATUS_UUID -> pendingAlerts[deviceKey]?.put("status", dataString)
                TIMESTAMP_UUID -> pendingAlerts[deviceKey]?.put("timestamp", dataString)
                LOCATION_UUID -> pendingAlerts[deviceKey]?.put("location", dataString)
                CRASH_TYPE_UUID -> pendingAlerts[deviceKey]?.put("crashType", dataString)
                else -> {
                    // For other formats or direct service UUIDs
                    if (dataString.startsWith("CRASH:")) {
                        processCrashString(deviceAddress, deviceName, dataString)
                    } else {
                        Log.d(TAG, "Unknown data format or service UUID: $serviceUUID, data: $dataString")
                    }
                }
            }
            
            // Check if we have all needed data to create a crash alert
            val pendingData = pendingAlerts[deviceKey]
            if (pendingData != null && 
                (pendingData.containsKey("status") || pendingData.containsKey("crashType"))) {
                
                // Extract available data
                val status = pendingData["status"] ?: "CRASH_CONFIRMED"
                val timestamp = pendingData["timestamp"] ?: Date().toString()
                val location = pendingData["location"] ?: "Unknown Location"
                val crashType = pendingData["crashType"] ?: "Unknown Crash Type"
                
                // Derive severity from crash type
                val severity = deriveSeverityFromCrashType(crashType)
                
                // Extract acceleration value if possible
                val acceleration = extractAccelerationValue(crashType)
                
                // Create and add the crash alert
                val crashAlert = CrashAlert(
                    deviceAddress = deviceAddress,
                    deviceName = deviceName,
                    timestamp = timestamp,
                    receivedTime = Date(),
                    crashType = crashType,
                    location = location,
                    crashStatus = status,
                    accelerationValue = acceleration,
                    severity = severity
                )
                
                Log.d(TAG, "Creating crash alert: $crashAlert")
                crashAlertRepository.addCrashAlert(crashAlert)
                
                // Clear pending data for this device
                pendingAlerts.remove(deviceKey)
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            Log.e(TAG, "Error processing crash data", e)
        }
    }
    
    /**
     * Process legacy format "CRASH:acceleration:severity"
     */
    private fun processCrashString(deviceAddress: String, deviceName: String, dataString: String) {
        if (dataString.startsWith("CRASH:")) {
            val parts = dataString.removePrefix("CRASH:").split(":")
            if (parts.size >= 1) {
                val acceleration = parts[0].toFloatOrNull() ?: 0f
                val severity = when {
                    parts.size >= 2 -> parseSeverity(parts[1])
                    acceleration > 15f -> CrashSeverity.CRITICAL
                    acceleration > 10f -> CrashSeverity.HIGH
                    acceleration > 5f -> CrashSeverity.MEDIUM
                    else -> CrashSeverity.LOW
                }
                
                val crashAlert = CrashAlert(
                    deviceAddress = deviceAddress,
                    deviceName = deviceName,
                    crashType = if (parts.size >= 2) parts[1] else "Impact Crash",
                    accelerationValue = acceleration,
                    severity = severity,
                    crashStatus = "CRASH_CONFIRMED",
                    location = "Unknown Location",
                    timestamp = Date().toString()
                )
                
                crashAlertRepository.addCrashAlert(crashAlert)
            }
        }
    }
    
    /**
     * Extract acceleration value from crash type if possible
     */
    private fun extractAccelerationValue(crashType: String): Float {
        val impactRegex = ".*Impact.*?(\\d+\\.?\\d*).*".toRegex()
        val match = impactRegex.find(crashType)
        return match?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
    }
    
    /**
     * Derive crash severity from crash type string
     */
    private fun deriveSeverityFromCrashType(crashType: String): CrashSeverity {
        return when {
            crashType.contains("High Impact", ignoreCase = true) -> CrashSeverity.HIGH
            crashType.contains("Free Fall", ignoreCase = true) -> CrashSeverity.CRITICAL
            crashType.contains("Severe Tilt", ignoreCase = true) -> CrashSeverity.MEDIUM
            else -> CrashSeverity.MEDIUM
        }
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
}