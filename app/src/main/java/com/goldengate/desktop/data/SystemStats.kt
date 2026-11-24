package com.goldengate.desktop.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.runtime.Immutable

@Immutable
data class SystemSnapshot(
    val memUsedMb: Long,
    val memAvailableMb: Long,
    val memTotalMb: Long,
    val storageUsedGb: Float,
    val storageTotalGb: Float,
    val uptimeLabel: String,
    val deviceModel: String,
    val androidVersion: String,
    val cores: Int,
    val refreshRate: Float,
    val screenDpWidth: Int,
    val screenDpHeight: Int,
    val density: Float,
    val isTablet: Boolean,
    val lowRam: Boolean
)

fun systemSnapshot(context: Context): SystemSnapshot {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memory = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memory)
    val totalMb = memory.totalMem / (1024 * 1024)
    val availMb = memory.availMem / (1024 * 1024)

    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = stat.blockCountLong * stat.blockSizeLong
    val availableBytes = stat.availableBlocksLong * stat.blockSizeLong

    val uptimeMinutes = SystemClock.elapsedRealtime() / 60_000
    val hours = uptimeMinutes / 60
    val minutes = uptimeMinutes % 60
    val metrics = context.resources.displayMetrics

    return SystemSnapshot(
        memUsedMb = (totalMb - availMb).coerceAtLeast(0),
        memAvailableMb = availMb,
        memTotalMb = totalMb,
        storageUsedGb = (totalBytes - availableBytes) / (1024f * 1024f * 1024f),
        storageTotalGb = totalBytes / (1024f * 1024f * 1024f),
        uptimeLabel = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
        deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        cores = Runtime.getRuntime().availableProcessors(),
        refreshRate = runCatching {
            (context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager)
                .getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f
        }.getOrDefault(60f),
        screenDpWidth = (metrics.widthPixels / metrics.density).toInt(),
        screenDpHeight = (metrics.heightPixels / metrics.density).toInt(),
        density = metrics.density,
        isTablet = metrics.widthPixels / metrics.density >= 600f,
        lowRam = activityManager.isLowRamDevice
    )
}
