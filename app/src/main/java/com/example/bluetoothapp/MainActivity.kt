package com.example.bluetoothapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bluetoothapp.navigation.AppNavHost
import com.example.bluetoothapp.navigation.NavigationItem
import com.example.bluetoothapp.ui.theme.BluetoothAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we should open to crash alerts from notification
        val openCrashTab = intent?.getBooleanExtra("OPEN_CRASH_TAB", false) ?: false
        val startDestination = if (openCrashTab) "crash_alerts" else NavigationItem.DASHBOARD.route
        
        setContent {
            BluetoothAppTheme {
                MainScreen(startDestination = startDestination)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    startDestination: String = NavigationItem.DASHBOARD.route
) {
    val navController = rememberNavController()
    
    // Get the current route for the screen title
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = remember { mutableStateOf(startDestination) }
    
    // Update current route whenever nav changes
    currentRoute.value = navBackStackEntry?.destination?.route ?: startDestination
    
    // Calculate the screen title based on current route
    val currentScreen = when(currentRoute.value) {
        NavigationItem.DASHBOARD.route -> stringResource(id = R.string.dashboard)
        NavigationItem.DEVICES.route -> stringResource(id = R.string.devices_title)
        NavigationItem.BLUETOOTH.route -> stringResource(id = R.string.bluetooth)
        "crash_alerts" -> stringResource(id = R.string.crash_alerts)
        else -> stringResource(id = R.string.app_name)
    }
    
    // Main content with app bar and nav host
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentScreen) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    if (currentRoute.value != NavigationItem.DASHBOARD.route) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_motorcycle_24),
                                contentDescription = "Back to Dashboard",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        // Show app logo for dashboard
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_motorcycle_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Navigation host for switching between screens
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}