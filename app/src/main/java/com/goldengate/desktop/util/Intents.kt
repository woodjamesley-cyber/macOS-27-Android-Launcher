package com.goldengate.desktop.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/** Intent helpers that degrade gracefully instead of crashing on devices without a target app. */
object Intents {

    fun launchComponent(context: Context, component: String): Boolean = runCatching {
        val cn = ComponentName.unflattenFromString(component) ?: return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(cn)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        true
    }.getOrElse { false }

    fun launchIntent(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse { e ->
        if (e is ActivityNotFoundException) toast(context, "No app can open that")
        false
    }

    fun openUrl(context: Context, url: String): Boolean = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrElse {
        toast(context, "No browser installed")
        false
    }

    fun openBrowser(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_BROWSER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (runCatching { context.startActivity(home) }.isSuccess) true
        else openUrl(context, "https://duckduckgo.com/")
    }

    fun openGallery(context: Context): Boolean {
        val gallery = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_APP_GALLERY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (runCatching { context.startActivity(gallery) }.isSuccess) {
            true
        } else {
            val pick = Intent(Intent.ACTION_VIEW)
                .setType("image/*")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(pick) }.isSuccess) true
            else {
                toast(context, "No gallery app installed")
                false
            }
        }
    }

    fun openFile(context: Context, uri: Uri, mime: String?): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mime ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrElse {
        toast(context, "No app can open this file")
        false
    }

    fun openSystemSettings(context: Context, action: String, data: Uri? = null): Boolean = runCatching {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (data != null) this.data = data
        }
        context.startActivity(intent)
        true
    }.getOrElse {
        openSystemSettings(context, Settings.ACTION_SETTINGS)
    }

    fun openHomeSettings(context: Context) {
        val roleSettings = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(roleSettings) }.isFailure) {
            openSystemSettings(context, Settings.ACTION_SETTINGS)
        }
    }

    fun openAppDetails(context: Context, packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        openSystemSettings(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    }

    fun openPanel(context: Context, action: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openSystemSettings(context, action)
        } else {
            openSystemSettings(context, Settings.ACTION_SETTINGS)
        }
    }

    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launchIntent(context, Intent.createChooser(send, "Share"))
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun dial(context: Context, number: String) {
        openUrl(context, "tel:$number")
    }

    fun mail(context: Context, address: String) {
        openUrl(context, "mailto:$address")
    }
}
