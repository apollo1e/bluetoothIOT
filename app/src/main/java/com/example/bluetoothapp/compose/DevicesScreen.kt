package com.example.bluetoothapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.bluetoothapp.data.ConnectedDevice
import com.example.bluetoothapp.data.Device
import com.example.bluetoothapp.viewmodels.DevicesViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val devices: List<Device> by viewModel.devicesFlow.collectAsState()
    val connectedDevices: List<ConnectedDevice> by viewModel.connectedDevices.collectAsState()
    
    var showRemoveDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            DevicesTopBar()
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Connected devices section
            if (connectedDevices.isNotEmpty()) {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(connectedDevices) { device ->
                        ConnectedDeviceItem(
                            connectedDevice = device,
                            isConnected = viewModel.isDeviceConnected(device.address),
                            onRemoveDevice = { showRemoveDialog = device.address }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Show other devices from API if any
                    if (devices.any { device -> 
                            connectedDevices.none { it.address == device.macAddress }
                        }) {
                        item {
                            Text(
                                text = "Available Devices",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp)
                            )
                        }
                        
                        items(devices.filter { device -> 
                            connectedDevices.none { it.address == device.macAddress }
                        }) { device ->
                            DeviceListItem(device = device)
                        }
                    }
                }
            } else {
                // No connected devices, just show regular device list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "No connected devices. Connect to devices using the Bluetooth tab.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Available Devices",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp)
                        )
                    }
                    
                    items(devices) { device ->
                        DeviceListItem(device = device)
                    }
                }
            }
        }
    }
    
    // Dialog to confirm device removal
    if (showRemoveDialog != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("Remove Device") },
            text = { Text("Are you sure you want to remove this device from your saved devices list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog?.let { address ->
                            viewModel.removeConnectedDevice(address)
                        }
                        showRemoveDialog = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevicesTopBar() {
    TopAppBar(
        title = {
            Text(text = "Device List")
        }
    )
}

@Composable
private fun ConnectedDeviceItem(
    connectedDevice: ConnectedDevice,
    isConnected: Boolean,
    onRemoveDevice: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection status indicator
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color.Green else Color.Red)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        
                        Spacer(modifier = Modifier.size(8.dp))
                        
                        Text(
                            text = connectedDevice.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = connectedDevice.deviceType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = connectedDevice.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = if (isConnected) 
                            "Connected since ${dateFormat.format(connectedDevice.connectionTime)}" 
                        else 
                            "Last seen: ${dateFormat.format(connectedDevice.lastSeen)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(
                    onClick = onRemoveDevice
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Device",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Display supported services
            if (connectedDevice.supportedServices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Supported Services:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                connectedDevice.supportedServices.take(3).forEach { service ->
                    Text(
                        text = "• ${service.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (connectedDevice.supportedServices.size > 3) {
                    Text(
                        text = "• ${connectedDevice.supportedServices.size - 3} more...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(device: Device) {
    // Create a coroutine scope
    val coroutineScope = rememberCoroutineScope()
    // Add a clickable modifier to the Text
    var showDialog by remember { mutableStateOf(false) }

    // Dialog content
    val dialogContent = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Model: ")
        }
        append("${device.model}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Product: ")
        }
        append("${device.product}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Serial: ")
        }
        append("${device.serial}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Firmware Version: ")
        }
        append("${device.firmwareVersion}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Installation Mode: ")
        }
        append("${device.installationMode}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Brake Light: ")
        }
        append("${device.brakeLight}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Light Mode: ")
        }
        append("${device.lightMode}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Light Auto: ")
        }
        append("${device.lightAuto}\n")

        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Light Value: ")
        }
        append("${device.lightValue}\n")
    }

    // Show the AlertDialog if showDialog is true
    if (showDialog) {
        DeviceDetailsDialog(
            title = "Device Details",
            content = dialogContent,
            onDismiss = {
                showDialog = false
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                // Use the coroutine scope to launch a coroutine
                coroutineScope.launch {
                    // Set showDialog to true to show the AlertDialog
                    showDialog = true
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Product: ${device.product}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "MAC: ${device.macAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailsDialog(
    title: String,
    content: AnnotatedString,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss.invoke()
        },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = content)
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss.invoke()
                }
            ) {
                Text(text = "OK")
            }
        }
    )
}