package com.goldengate.desktop.data

import android.content.Context
import android.provider.CalendarContract
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Immutable
data class CalEvent(
    val title: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
    val calendarName: String?
)

object CalendarStore {

    /** Never throws: without permission you simply get an empty list. */
    suspend fun todayEvents(context: Context): List<CalEvent> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return events(context, start, end)
    }

    suspend fun events(context: Context, startMillis: Long, endMillis: Long): List<CalEvent> =
        withContext(Dispatchers.IO) {
            runCatching {
                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.CALENDAR_DISPLAY_NAME
                )
                val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                    .appendPath(startMillis.toString())
                    .appendPath(endMillis.toString())
                    .build()
                val list = mutableListOf<CalEvent>()
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        list += CalEvent(
                            title = cursor.getString(0) ?: "Untitled event",
                            start = cursor.getLong(1),
                            end = cursor.getLong(2),
                            allDay = cursor.getInt(3) == 1,
                            location = cursor.getString(4),
                            calendarName = cursor.getString(5)
                        )
                    }
                }
                list
            }.getOrElse { emptyList() }
        }

    fun upcoming(events: List<CalEvent>): CalEvent? {
        val now = Instant.now().toEpochMilli()
        return events.filter { it.end >= now }.minByOrNull { it.start }
    }
}
