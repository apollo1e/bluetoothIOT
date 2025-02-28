package com.example.bluetoothapp.viewmodel

import com.example.bluetoothapp.MainCoroutineRule
import com.example.bluetoothapp.di.RepositoryModule
import com.example.bluetoothapp.domain.GetDevicesUseCase
import com.example.bluetoothapp.repository.FakeCosmoRepository
import com.example.bluetoothapp.repository.MockConnectedDevicesRepository
import com.example.bluetoothapp.runBlockingTest
import com.example.bluetoothapp.viewmodel.TestDevicesViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
class DevicesViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeCosmoRepository: FakeCosmoRepository
    
    //@Inject
    //lateinit var mockConnectedDevicesRepository: MockConnectedDevicesRepository

    private val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: TestDevicesViewModel

    @Before
    fun setUp() {
        hiltRule.inject()

        // Use the FakeCosmoRepository in the TestViewModel
        viewModel = TestDevicesViewModel(
            getDevicesUseCase = GetDevicesUseCase(fakeCosmoRepository)
        )
    }

    @Test
    fun testDevicesFlow() = coroutineRule.runBlockingTest {
        // Observe the first emitted value from devicesFlow
        val devicesList = viewModel.devicesFlow.first()
        
        // Verify that we have our test device
        assertTrue("Device list should contain test device",
            devicesList.any { device -> device.macAddress == "00:11:22:33:44:55" }
        )
    }
}

