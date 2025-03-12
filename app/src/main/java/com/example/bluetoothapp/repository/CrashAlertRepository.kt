package com.example.bluetoothapp.repository

import com.example.bluetoothapp.data.CrashAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing crash alerts from M5Sticks
 */
@Singleton
class CrashAlertRepository @Inject constructor() {
    private val _crashAlerts = MutableStateFlow<List<CrashAlert>>(emptyList())
    val crashAlerts = _crashAlerts.asStateFlow()
    
    val unacknowledgedAlerts: Flow<List<CrashAlert>> = crashAlerts
        .map { alerts -> alerts.filter { !it.isAcknowledged } }
        
    val hasUnacknowledgedAlerts: Flow<Boolean> = unacknowledgedAlerts
        .map { it.isNotEmpty() }
        
    val unattendedAlerts: Flow<List<CrashAlert>> = crashAlerts
        .map { alerts -> alerts.filter { !it.isAttended } }
        
    val hasUnattendedAlerts: Flow<Boolean> = unattendedAlerts
        .map { it.isNotEmpty() }

    /**
     * Add a new crash alert from an M5Stick device
     */
    fun addCrashAlert(crashAlert: CrashAlert) {
        val currentList = _crashAlerts.value.toMutableList()
        
        // Generate sample GPS coordinates for demo purposes
        // In a real app, these would come from the M5Stick
        val crashAlertWithCoordinates = if (crashAlert.latitude == 0.0 && crashAlert.longitude == 0.0) {
            // Generate random coordinates near kuala lumpur for demo
            val baseLat = 3.1390
            val baseLong = 101.6869
            val latOffset = (-5..5).random() / 100.0
            val longOffset = (-5..5).random() / 100.0
            
            crashAlert.copy(
                latitude = baseLat + latOffset,
                longitude = baseLong + longOffset
            )
        } else {
            crashAlert
        }
        
        currentList.add(0, crashAlertWithCoordinates) // Add to beginning of list (newest first)
        _crashAlerts.value = currentList
    }

    /**
     * Mark a crash alert as acknowledged
     */
    fun acknowledgeCrashAlert(crashAlertId: String) {
        val currentList = _crashAlerts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == crashAlertId }
        
        if (index != -1) {
            val alert = currentList[index]
            currentList[index] = alert.copy(isAcknowledged = true)
            _crashAlerts.value = currentList
        }
    }
    
    /**
     * Mark a crash alert as attended or unattended
     */
    fun updateAttendanceStatus(crashAlertId: String, isAttended: Boolean) {
        val currentList = _crashAlerts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == crashAlertId }
        
        if (index != -1) {
            val alert = currentList[index]
            currentList[index] = alert.copy(isAttended = isAttended)
            _crashAlerts.value = currentList
        }
    }

    /**
     * Delete a crash alert
     */
    fun deleteCrashAlert(crashAlertId: String) {
        val currentList = _crashAlerts.value.toMutableList()
        currentList.removeAll { it.id == crashAlertId }
        _crashAlerts.value = currentList
    }

    /**
     * Clear all crash alerts
     */
    fun clearAllCrashAlerts() {
        _crashAlerts.value = emptyList()
    }
}