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

    /**
     * Add a new crash alert from an M5Stick device
     */
    fun addCrashAlert(crashAlert: CrashAlert) {
        val currentList = _crashAlerts.value.toMutableList()
        currentList.add(0, crashAlert) // Add to beginning of list (newest first)
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