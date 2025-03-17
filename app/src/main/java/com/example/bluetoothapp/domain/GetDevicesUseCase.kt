package com.example.bluetoothapp.domain

import android.util.Log
import com.example.bluetoothapp.data.DeviceResponse
import com.example.bluetoothapp.repository.CosmoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetDevicesUseCase @Inject constructor(private val cosmoRepository: CosmoRepository) {

    suspend operator fun invoke(): Flow<DeviceResponse> = flow {
        try {
            // Call the repository to get devices
            val devices = cosmoRepository.getDevicesRepository()

            // Emit the devices into the flow
            emit(devices)
        } catch (e: Exception) {
            // Log the error but don't crash the app
            Log.e(TAG, "Error in GetDevicesUseCase: ${e.message}", e)
            
            // Emit an empty device response instead of crashing
            emit(DeviceResponse(emptyList()))
        }
    }

    companion object {
        private const val TAG = "TESTDEBUG"
    }
}
