package com.example.bluetoothapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.data.MqttCrashAlert
import com.example.bluetoothapp.viewmodels.MqttViewModel

@Composable
fun MqttScreen(
    viewModel: MqttViewModel = hiltViewModel()
) {
    var connectionStatus by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var crashAlerts by remember { mutableStateOf<List<MqttCrashAlert>>(emptyList()) }
    
    // Maximum number of alerts to keep in memory
    val maxAlerts = 20
    
    // Set up callbacks for MQTT client
    DisposableEffect(viewModel) {
        // Set message callback
        viewModel.setMessageCallback { alert ->
            // Add new alert to the beginning of the list (most recent first)
            // and limit the list size to maxAlerts
            crashAlerts = (listOf(alert) + crashAlerts).take(maxAlerts)
        }
        
        // Set connection callback
        viewModel.setConnectionCallback { isConnected ->
            connectionStatus = isConnected
            connecting = false
        }
        
        // Initialize MQTT client
        viewModel.initialize()
        
        onDispose {
            // Clean up when composable is disposed
            viewModel.disconnect()
        }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App title
                Text(
                    text = "MQTT TLS Client",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Connection status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Connection Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Status indicator
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (connectionStatus) Color.Green else Color.Red,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(8.dp)
                            )
                            
                            Text(
                                text = if (connectionStatus) "Connected" else "Disconnected",
                                fontWeight = FontWeight.Bold,
                                color = if (connectionStatus) Color.Green else Color.Red
                            )
                            
                            if (connecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Connect button
                        Button(
                            onClick = {
                                connecting = true
                                viewModel.connect()
                            },
                            enabled = !connectionStatus && !connecting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Connect")
                        }
                        
                        // Disconnect button
                        Button(
                            onClick = {
                                viewModel.disconnect()
                                connectionStatus = false
                            },
                            enabled = connectionStatus,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Disconnect")
                        }
                    }
                }
                
                // Crash Alerts card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Crash Alerts",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${crashAlerts.size} alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                if (crashAlerts.isNotEmpty()) {
                                    Button(
                                        onClick = { crashAlerts = emptyList() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Clear",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (crashAlerts.isEmpty()) {
                            Text(
                                text = "No crash alerts received yet",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Display list of crash alerts
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                crashAlerts.forEachIndexed { index, alert ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "Alert #${index + 1} - ${alert.crashType}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            Text(
                                                text = "Time: ${alert.timestamp}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Format and display the crash alert data
                                            MqttCrashAlertDetails(alert)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Broker info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Broker Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(text = "Host: 192.168.1.2")
                        Text(text = "Port: 8883 (TLS)")
                        Text(text = "Topic: iot/crashalerts")
                    }
                }
            }
        }
    }
}

@Composable
fun MqttCrashAlertDetails(crashAlert: MqttCrashAlert) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DataRow("Crash ID", crashAlert.crashId.toString())
        DataRow("Status", crashAlert.crashStatus)
        DataRow("Location", crashAlert.location)
        DataRow("Acceleration", String.format("%.2f", crashAlert.acceleration))
        DataRow("Tilt Angle", String.format("%.2f", crashAlert.tiltAngle))
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp
        )
    }
}