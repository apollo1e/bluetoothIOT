package com.example.bluetoothapp.di

import com.example.bluetoothapp.repository.ConnectedDevicesRepository
import com.example.bluetoothapp.repository.CosmoRepository
import com.example.bluetoothapp.repository.FakeCosmoRepository
import com.example.bluetoothapp.repository.MockConnectedDevicesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TestModule {

    @Provides
    @Singleton
    fun provideCosmoRepository(): CosmoRepository {
        return FakeCosmoRepository()
    }
    
    @Provides
    @Singleton
    fun provideConnectedDevicesRepository(): MockConnectedDevicesRepository {
        return MockConnectedDevicesRepository()
    }

}