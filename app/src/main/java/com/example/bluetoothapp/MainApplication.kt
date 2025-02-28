package com.example.bluetoothapp

import android.app.Application
import com.example.bluetoothapp.repository.ConnectedDevicesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {
    
    @Inject
    lateinit var connectedDevicesRepository: ConnectedDevicesRepository
}