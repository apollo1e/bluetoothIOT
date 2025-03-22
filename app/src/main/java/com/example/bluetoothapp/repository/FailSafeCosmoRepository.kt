package com.example.bluetoothapp.repository

import android.util.Log
import com.example.bluetoothapp.data.DeviceResponse
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Repository implementation that tries the real repository first and falls back to the fake 
 * repository if there's an error, ensuring the app doesn't crash due to network issues.
 */
class FailSafeCosmoRepository @Inject constructor(
    private val defaultRepository: CosmoRepository,
    private val fakeRepository: CosmoRepository
) : CosmoRepository {

    companion object {
        private const val TAG = "FailSafeCosmoRepository"
    }

    override suspend fun getDevicesRepository(): DeviceResponse {
        return try {
            // Try to get data from the real repository
            defaultRepository.getDevicesRepository()
        } catch (e: HttpException) {
            // Log the error
            Log.e(TAG, "HTTP error ${e.code()}, falling back to fake data", e)
            
            // Fall back to fake data
            fakeRepository.getDevicesRepository()
        } catch (e: Exception) {
            // Log any other errors
            Log.e(TAG, "Error getting devices, falling back to fake data", e)
            
            // Fall back to fake data
            fakeRepository.getDevicesRepository()
        }
    }
}