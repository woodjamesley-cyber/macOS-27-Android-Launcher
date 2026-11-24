package com.goldengate.desktop.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return if (index == 0) "${bytes} ${units[index]}" else String.format(Locale.US, "%.1f %s", value, units[index])
}

fun formatDate(millis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}.getOrDefault("—")

fun fileKindLabel(name: String, mime: String?, isDirectory: Boolean): String {
    if (isDirectory) return "Folder"
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        mime?.startsWith("image/") == true -> "Image"
        mime?.startsWith("video/") == true -> "Movie"
        mime?.startsWith("audio/") == true -> "Audio"
        mime == "application/pdf" -> "PDF"
        extension in setOf("txt", "md", "rtf") -> "Text"
        extension in setOf("kt", "java", "js", "ts", "py", "json", "xml", "html", "css") -> "Code"
        extension in setOf("zip", "tar", "gz", "7z") -> "Archive"
        else -> mime?.substringBefore('/')?.replaceFirstChar { it.uppercase() } ?: "File"
    }
}
