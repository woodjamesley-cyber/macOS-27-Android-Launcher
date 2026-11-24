package com.goldengate.desktop.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers every launchable Android application. Uses the standard launcher intent query, which
 * also satisfies Android 11+ package visibility rules without needing QUERY_ALL_PACKAGES.
 */
class AppsRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    suspend fun load(iconSizePx: Int = 132): List<InstalledApp> = withContext(Dispatchers.IO) {
        resolveLauncherActivities()
            .asSequence()
            .mapNotNull { info ->
                runCatching {
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    if (label.isEmpty()) return@runCatching null
                    val component = ComponentName(info.packageName, info.name).flattenToString()
                    InstalledApp(
                        label = label,
                        packageName = info.packageName,
                        component = component,
                        icon = loadIcon(info, iconSizePx)
                    )
                }.getOrNull()
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun resolveLauncherActivities(): List<ActivityInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.mapNotNull { it.activityInfo }
    }

    private fun loadIcon(info: ActivityInfo, size: Int): ImageBitmap? = runCatching {
        val drawable = info.loadIcon(pm) ?: return null
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()

    fun appLabel(component: String): String? = runCatching {
        val cn = ComponentName.unflattenFromString(component) ?: return null
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getActivityInfo(cn, PackageManager.ComponentInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getActivityInfo(cn, 0)
        }
        info.loadLabel(pm)?.toString()
    }.getOrNull()
}
