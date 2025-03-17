package com.example.bluetoothapp.compose

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.R
import com.example.bluetoothapp.data.CrashSeverity
import com.example.bluetoothapp.ui.theme.CriticalSeverityBackground
import com.example.bluetoothapp.ui.theme.CriticalSeverityContent
import com.example.bluetoothapp.ui.theme.ConnectedColor
import com.example.bluetoothapp.ui.theme.DisconnectedColor
import com.example.bluetoothapp.ui.theme.HighSeverityBackground
import com.example.bluetoothapp.ui.theme.HighSeverityContent
import com.example.bluetoothapp.ui.theme.LowSeverityBackground
import com.example.bluetoothapp.ui.theme.LowSeverityContent
import com.example.bluetoothapp.ui.theme.MediumSeverityBackground
import com.example.bluetoothapp.ui.theme.MediumSeverityContent
import com.example.bluetoothapp.ui.theme.WidgetBackground
import com.example.bluetoothapp.viewmodels.CrashAlertViewModel
import com.example.bluetoothapp.viewmodels.DevicesViewModel

@Composable
fun DashboardScreen(
    onNavigateToDevices: () -> Unit,
    onNavigateToBluetoothDevices: () -> Unit,
    onNavigateToCrashAlerts: () -> Unit,
    onNavigateToMqttAlerts: () -> Unit,
    devicesViewModel: DevicesViewModel = hiltViewModel(),
    crashAlertViewModel: CrashAlertViewModel = hiltViewModel()
) {
    val connectedDevices by devicesViewModel.connectedDevices.collectAsState()
    val hasUnacknowledgedAlerts by crashAlertViewModel.hasUnacknowledgedAlerts.collectAsState()
    val unacknowledgedAlerts by crashAlertViewModel.unacknowledgedAlerts.collectAsState()
    val crashAlerts by crashAlertViewModel.crashAlerts.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome header
        Text(
            text = "Motorcycle Crash Detection",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Device Status Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WidgetBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
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
                        text = "Device Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${connectedDevices.size} Connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (connectedDevices.isEmpty()) {
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
                                painter = painterResource(id = R.drawable.baseline_bluetooth_24),
                                contentDescription = null,
                                tint = DisconnectedColor,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "No devices connected",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        connectedDevices.take(3).forEach { device ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ConnectedColor)
                                )
                                
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        
                        if (connectedDevices.size > 3) {
                            Text(
                                text = "... and ${connectedDevices.size - 3} more",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 8.dp)
                        .clickable { onNavigateToDevices() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View All Devices",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // Crash Alert Summary Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WidgetBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (hasUnacknowledgedAlerts) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CriticalSeverityBackground)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${unacknowledgedAlerts.size} New",
                                style = MaterialTheme.typography.labelMedium,
                                color = CriticalSeverityContent
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (crashAlerts.isEmpty()) {
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
                                painter = painterResource(id = R.drawable.baseline_warning_24),
                                contentDescription = null,
                                tint = DisconnectedColor,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "No crash alerts detected",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Alert severity summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Critical alerts
                        val criticalCount = crashAlerts.count { it.severity == CrashSeverity.CRITICAL }
                        SeverityIndicator(
                            count = criticalCount,
                            label = "Critical",
                            backgroundColor = CriticalSeverityBackground,
                            contentColor = CriticalSeverityContent
                        )
                        
                        // High alerts
                        val highCount = crashAlerts.count { it.severity == CrashSeverity.HIGH }
                        SeverityIndicator(
                            count = highCount,
                            label = "High",
                            backgroundColor = HighSeverityBackground,
                            contentColor = HighSeverityContent
                        )
                        
                        // Medium alerts
                        val mediumCount = crashAlerts.count { it.severity == CrashSeverity.MEDIUM }
                        SeverityIndicator(
                            count = mediumCount,
                            label = "Medium",
                            backgroundColor = MediumSeverityBackground,
                            contentColor = MediumSeverityContent
                        )
                        
                        // Low alerts
                        val lowCount = crashAlerts.count { it.severity == CrashSeverity.LOW }
                        SeverityIndicator(
                            count = lowCount,
                            label = "Low",
                            backgroundColor = LowSeverityBackground,
                            contentColor = LowSeverityContent
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 8.dp)
                        .clickable { onNavigateToCrashAlerts() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View All Alerts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // Bluetooth Discovery Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WidgetBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Discover Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_bluetooth_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Text(
                        text = "Scan for nearby Bluetooth devices",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 8.dp)
                        .clickable { onNavigateToBluetoothDevices() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scan Bluetooth Devices",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // MQTT Alerts Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = WidgetBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "MQTT Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_mqtt_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Text(
                        text = "View alerts from MQTT broker",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(vertical = 8.dp)
                        .clickable { onNavigateToMqttAlerts() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View MQTT Alerts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityIndicator(
    count: Int,
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}