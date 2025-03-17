package com.example.bluetoothapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothapp.bluetooth.ConnectGATT
import com.example.bluetoothapp.compose.CrashAlertsScreen
import com.example.bluetoothapp.compose.DashboardScreen
import com.example.bluetoothapp.compose.DevicesScreen
import com.example.bluetoothapp.compose.MqttAlertsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavigationItem.DASHBOARD.route
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.DASHBOARD.route) {
            DashboardScreen(
                onNavigateToDevices = {
                    navController.navigate(NavigationItem.DEVICES.route)
                },
                onNavigateToBluetoothDevices = {
                    navController.navigate(NavigationItem.BLUETOOTH.route)
                },
                onNavigateToCrashAlerts = {
                    navController.navigate("crash_alerts")
                },
                onNavigateToMqttAlerts = {
                    navController.navigate(NavigationItem.MQTT.route)
                }
            )
        }
        
        composable(NavigationItem.DEVICES.route) {
            DevicesScreen()
        }
        
        composable(NavigationItem.BLUETOOTH.route) {
            ConnectGATT()
        }
        
        composable("crash_alerts") {
            CrashAlertsScreen()
        }
        
        composable(NavigationItem.MQTT.route) {
            MqttAlertsScreen()
        }
    }
}