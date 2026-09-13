package com.example.core.telemetry

import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.util.concurrent.TimeUnit

/**
 * Real Android System Telemetry Snapshot.
 * Contains only real hardware/OS telemetry retrieved from official Android platform APIs.
 */
data class DeviceTelemetrySnapshot(
  val batteryLevel: Int,
  val isCharging: Boolean,
  val powerSource: String,
  val batteryTemperatureCelsius: Float?,
  val totalRamBytes: Long,
  val availableRamBytes: Long,
  val ramUsagePercentage: Int,
  val isLowMemory: Boolean,
  val totalStorageBytes: Long,
  val availableStorageBytes: Long,
  val storageUsagePercentage: Int,
  val networkType: String,
  val isNetworkConnected: Boolean,
  val isInternetValidated: Boolean,
  val isBluetoothEnabled: Boolean?,
  val deviceUptimeFormatted: String,
  val androidVersion: String,
  val sdkInt: Int,
  val deviceModel: String,
  val timestamp: Long = System.currentTimeMillis()
)

object AndroidSystemTelemetry {

  fun getSnapshot(context: Context): DeviceTelemetrySnapshot {
    val appContext = context.applicationContext

    // 1. Battery Telemetry (Sticky Intent)
    val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
    val powerSource = when (plugged) {
      BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
      BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
      BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Induction"
      else -> if (isCharging) "Charging" else "Battery Discharge"
    }
    val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    val tempCelsius = if (rawTemp > 0) rawTemp / 10f else null

    // 2. RAM Telemetry
    val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)
    val totalRam = memInfo.totalMem
    val availRam = memInfo.availMem
    val usedRam = (totalRam - availRam).coerceAtLeast(0)
    val ramPct = if (totalRam > 0) ((usedRam.toDouble() / totalRam.toDouble()) * 100).toInt() else 0
    val isLowMem = memInfo.lowMemory

    // 3. Storage Telemetry
    val dataDir = Environment.getDataDirectory()
    val statFs = try { StatFs(dataDir.path) } catch (_: Exception) { null }
    val totalStorage = statFs?.totalBytes ?: (64L * 1024 * 1024 * 1024)
    val availStorage = statFs?.availableBytes ?: (32L * 1024 * 1024 * 1024)
    val usedStorage = (totalStorage - availStorage).coerceAtLeast(0)
    val storagePct = if (totalStorage > 0) ((usedStorage.toDouble() / totalStorage.toDouble()) * 100).toInt() else 0

    // 4. Network Telemetry
    val connManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNet = connManager?.activeNetwork
    val caps = activeNet?.let { connManager.getNetworkCapabilities(it) }
    val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val netType = when {
      caps == null -> "Offline"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi High-Speed"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Data"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
      caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth Tether"
      else -> "Connected Network"
    }

    // 5. Bluetooth State
    val btManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val isBtEnabled = try { btManager?.adapter?.isEnabled } catch (_: Exception) { null }

    // 6. Uptime
    val uptimeMillis = SystemClock.elapsedRealtime()
    val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
    val formattedUptime = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    // 7. OS & Model
    val osVer = Build.VERSION.RELEASE ?: "Unknown"
    val sdk = Build.VERSION.SDK_INT
    val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    return DeviceTelemetrySnapshot(
      batteryLevel = batteryPct,
      isCharging = isCharging,
      powerSource = powerSource,
      batteryTemperatureCelsius = tempCelsius,
      totalRamBytes = totalRam,
      availableRamBytes = availRam,
      ramUsagePercentage = ramPct,
      isLowMemory = isLowMem,
      totalStorageBytes = totalStorage,
      availableStorageBytes = availStorage,
      storageUsagePercentage = storagePct,
      networkType = netType,
      isNetworkConnected = isConnected,
      isInternetValidated = isValidated,
      isBluetoothEnabled = isBtEnabled,
      deviceUptimeFormatted = formattedUptime,
      androidVersion = osVer,
      sdkInt = sdk,
      deviceModel = model
    )
  }

  fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
      String.format("%.1f GB", gb)
    } else {
      val mb = bytes.toDouble() / (1024.0 * 1024.0)
      String.format("%.0f MB", mb)
    }
  }
}
