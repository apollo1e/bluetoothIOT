package com.example.bluetoothapp.di

import android.content.Context
import com.example.bluetoothapp.network.retrofit.RetrofitCosmo
import com.example.bluetoothapp.repository.ConnectedDevicesRepository
import com.example.bluetoothapp.repository.CosmoRepository
import com.example.bluetoothapp.repository.CrashAlertRepository
import com.example.bluetoothapp.repository.DefaultCosmoRepository
import com.example.bluetoothapp.repository.FakeCosmoRepository
import com.example.bluetoothapp.repository.MqttRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideCosmoRepository(retrofitCosmo: RetrofitCosmo): CosmoRepository {
        // Use the fake repository to avoid API errors
        return FakeCosmoRepository()
        // For production, use:
        // return DefaultCosmoRepository(retrofitCosmo)
    }
    
    @Provides
    @Singleton
    fun provideCrashAlertRepository(): CrashAlertRepository {
        return CrashAlertRepository()
    }
    
    @Provides
    @Singleton
    fun provideConnectedDevicesRepository(
        @ApplicationContext context: Context
    ): ConnectedDevicesRepository {
        return ConnectedDevicesRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideMqttRepository(
        @ApplicationContext context: Context
    ): MqttRepository {
        return MqttRepository(context)
    }
}