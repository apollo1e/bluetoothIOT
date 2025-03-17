package com.example.bluetoothapp.data

import java.util.UUID

data class MqttAlert(
    val id: String = UUID.randomUUID().toString(),
    val crashID: Int = 0,
    val acceleration: Float = 0f,
    val tiltAngle: Float = 0f,
    val timestamp: String = "",
    val crashType: String = "",
    val crashStatus: String = "",
    val location: String = "",
    val isAcknowledged: Boolean = false
)