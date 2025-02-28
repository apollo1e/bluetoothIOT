package com.example.bluetoothapp.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.data.CrashAlert
import com.example.bluetoothapp.data.CrashSeverity
import com.example.bluetoothapp.viewmodels.CrashAlertViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CrashAlertsScreen(
    viewModel: CrashAlertViewModel = hiltViewModel()
) {
    val crashAlerts by viewModel.crashAlerts.collectAsState()
    val hasUnacknowledgedAlerts by viewModel.hasUnacknowledgedAlerts.collectAsState()
    var selectedCrashAlert by remember { mutableStateOf<CrashAlert?>(null) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Crash Alert Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (crashAlerts.isEmpty()) {
                EmptyCrashAlertsList()
            } else {
                CrashAlertsList(
                    crashAlerts = crashAlerts,
                    onCrashAlertClick = { selectedCrashAlert = it },
                    onAcknowledge = { viewModel.acknowledgeCrashAlert(it.id) },
                    onDelete = { viewModel.deleteCrashAlert(it.id) }
                )
            }
        }

        // Clear all button
        if (crashAlerts.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showConfirmClearDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear All Alerts"
                )
            }
        }

        // Emergency alert indicator (flashing) for unacknowledged alerts
        AnimatedVisibility(
            visible = hasUnacknowledgedAlerts,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
        }
    }

    // Show crash alert details dialog
    selectedCrashAlert?.let { alert ->
        CrashAlertDetailsDialog(
            crashAlert = alert,
            onDismiss = { selectedCrashAlert = null },
            onAcknowledge = {
                viewModel.acknowledgeCrashAlert(alert.id)
                selectedCrashAlert = null
            }
        )
    }

    // Show clear all confirmation dialog
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("Clear All Alerts?") },
            text = { Text("Are you sure you want to delete all crash alerts? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllCrashAlerts()
                        showConfirmClearDialog = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EmptyCrashAlertsList() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No crash alerts",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "When an M5Stick detects a crash, alerts will appear here",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CrashAlertsList(
    crashAlerts: List<CrashAlert>,
    onCrashAlertClick: (CrashAlert) -> Unit,
    onAcknowledge: (CrashAlert) -> Unit,
    onDelete: (CrashAlert) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(crashAlerts) { alert ->
            CrashAlertItem(
                crashAlert = alert,
                onClick = { onCrashAlertClick(alert) },
                onAcknowledge = { onAcknowledge(alert) },
                onDelete = { onDelete(alert) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashAlertItem(
    crashAlert: CrashAlert,
    onClick: () -> Unit,
    onAcknowledge: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(crashAlert.timestamp)
    
    val (backgroundColor, borderColor) = when (crashAlert.severity) {
        CrashSeverity.LOW -> Pair(Color(0xFFE3F2FD), Color(0xFF2196F3))
        CrashSeverity.MEDIUM -> Pair(Color(0xFFFFF9C4), Color(0xFFFBC02D))
        CrashSeverity.HIGH -> Pair(Color(0xFFFFEBEE), Color(0xFFF44336))
        CrashSeverity.CRITICAL -> Pair(Color(0xFFFFCDD2), Color(0xFFD32F2F))
    }
    
    val accentColor = when (crashAlert.severity) {
        CrashSeverity.LOW -> Color(0xFF2196F3)
        CrashSeverity.MEDIUM -> Color(0xFFFBC02D)
        CrashSeverity.HIGH -> Color(0xFFF44336)
        CrashSeverity.CRITICAL -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (!crashAlert.isAcknowledged) 2.dp else 0.dp,
                color = if (!crashAlert.isAcknowledged) borderColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(1.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = crashAlert.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Crash type with severity
                Text(
                    text = crashAlert.crashType.ifEmpty {
                        "${
                            when (crashAlert.severity) {
                                CrashSeverity.LOW -> "Minor"
                                CrashSeverity.MEDIUM -> "Moderate"
                                CrashSeverity.HIGH -> "Severe"
                                CrashSeverity.CRITICAL -> "Critical"
                            }
                        } crash" + if (crashAlert.accelerationValue > 0) " (${crashAlert.accelerationValue}g)" else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    maxLines = 1
                )
                
                // Location
                if (crashAlert.location.isNotEmpty() && crashAlert.location != "Unknown Location") {
                    Text(
                        text = "📍 ${crashAlert.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                
                // Timestamp from M5Stick or local timestamp
                Text(
                    text = if (crashAlert.timestamp.isNotEmpty()) crashAlert.timestamp else formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (!crashAlert.isAcknowledged) {
                    IconButton(onClick = onAcknowledge) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Acknowledge",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun CrashAlertDetailsDialog(
    crashAlert: CrashAlert,
    onDismiss: () -> Unit,
    onAcknowledge: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(crashAlert.timestamp)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crash Alert Details") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Device: ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = crashAlert.deviceName)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "MAC Address: ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = crashAlert.deviceAddress)
                }
                
                // Crash type information
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Crash Type: ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = crashAlert.crashType.ifEmpty {
                            "Impact Crash"
                        }
                    )
                }
                
                // Crash status from M5Stick
                if (crashAlert.crashStatus.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Status: ",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = crashAlert.crashStatus)
                    }
                }
                
                // Severity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Severity: ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (crashAlert.severity) {
                            CrashSeverity.LOW -> "Minor"
                            CrashSeverity.MEDIUM -> "Moderate"
                            CrashSeverity.HIGH -> "Severe"
                            CrashSeverity.CRITICAL -> "Critical"
                        }
                    )
                }
                
                // Acceleration
                if (crashAlert.accelerationValue > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Acceleration: ",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "${crashAlert.accelerationValue}g")
                    }
                }
                
                // Location information from M5Stick
                if (crashAlert.location.isNotEmpty() && crashAlert.location != "Unknown Location") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Location: ",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = crashAlert.location)
                    }
                }
                
                // Timestamp from M5Stick
                if (crashAlert.timestamp.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Timestamp (M5Stick): ",
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = crashAlert.timestamp)
                    }
                }
                
                // Local received time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Received: ",
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = formattedDate)
                }
                
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = "Status: ${if (crashAlert.isAcknowledged) "Acknowledged" else "Unacknowledged"}",
                    fontWeight = FontWeight.Bold,
                    color = if (crashAlert.isAcknowledged) MaterialTheme.colorScheme.primary else Color.Red
                )
            }
        },
        confirmButton = {
            if (!crashAlert.isAcknowledged) {
                Button(
                    onClick = onAcknowledge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Acknowledge")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!crashAlert.isAcknowledged) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}