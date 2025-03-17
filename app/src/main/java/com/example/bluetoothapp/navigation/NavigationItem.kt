package com.example.bluetoothapp.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.bluetoothapp.R

enum class NavigationItem(
    @StringRes val titleResId: Int,
    @DrawableRes val iconResId: Int,
    val route: String
) {
    DASHBOARD(R.string.dashboard, R.drawable.baseline_motorcycle_24, "dashboard"),
    DEVICES(R.string.devices_title, R.drawable.baseline_devices_other_24, "devices"),
    BLUETOOTH(R.string.bluetooth, R.drawable.baseline_bluetooth_24, "bluetooth"),
    MQTT(R.string.mqtt_alerts, R.drawable.baseline_mqtt_24, "mqtt_alerts")
}