package com.example.bluetoothapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.R
import com.example.bluetoothapp.data.MqttAlert
import com.example.bluetoothapp.ui.theme.DisconnectedColor
import com.example.bluetoothapp.viewmodels.MqttAlertViewModel

@Composable
fun MqttAlertsScreen(
    viewModel: MqttAlertViewModel = hiltViewModel()
) {
    val mqttAlerts by viewModel.mqttAlerts.collectAsState()
    val isConnected by viewModel.connectionStatus.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Connection status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MQTT Alerts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier
                    .background(
                        if (isConnected) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    color = if (isConnected) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        // MQTT alerts list
        if (mqttAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_mqtt_24),
                        contentDescription = null,
                        tint = DisconnectedColor,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "No MQTT alerts received",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mqttAlerts) { alert ->
                    MqttAlertItem(alert = alert, onAcknowledge = { viewModel.acknowledgeMqttAlert(alert.id) })
                }
            }
        }
    }
}

@Composable
fun MqttAlertItem(
    alert: MqttAlert,
    onAcknowledge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isAcknowledged) 
                MaterialTheme.colorScheme.surface
            else 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Crash ID: ${alert.crashID}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = alert.timestamp,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Type: ${alert.crashType}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Status: ${alert.crashStatus}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Location: ${alert.location}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Acceleration: ${alert.acceleration}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "Tilt: ${alert.tiltAngle}°",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}