package com.example.bluetoothapp.data

import java.util.Date
import java.util.UUID

/**
 * Data class representing a crash alert received from an M5Stick
 * 
 * This data model handles the four services sent by the M5Stick:
 * - Crash status (severity derived from crash type)
 * - Timestamp
 * - Location
 * - Crash type
 */
data class CrashAlert(
    val id: String = UUID.randomUUID().toString(),
    val deviceAddress: String,
    val deviceName: String,
    
    // From M5Stick timestamp service
    val timestamp: String = "",
    val receivedTime: Date = Date(), // Local time when notification was received
    
    // From M5Stick crash type service
    val crashType: String = "",
    
    // From M5Stick location service
    val location: String = "",
    
    // GPS coordinates for map integration
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    
    // From M5Stick crash service
    val crashStatus: String = "",
    
    // Derived properties
    val accelerationValue: Float = 0f, // Can be parsed from crash type if available
    val severity: CrashSeverity = CrashSeverity.MEDIUM,
    
    val isAcknowledged: Boolean = false,
    val isAttended: Boolean = false
)

enum class CrashSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}