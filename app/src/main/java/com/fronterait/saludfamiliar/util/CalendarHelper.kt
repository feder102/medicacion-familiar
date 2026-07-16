package com.fronterait.saludfamiliar.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import java.util.TimeZone

object CalendarHelper {
    fun getPrimaryCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val primaryIndex = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                var fallbackId: Long? = null
                do {
                    val id = cursor.getLong(idIndex)
                    if (fallbackId == null) fallbackId = id
                    val isPrimary = cursor.getInt(primaryIndex) == 1
                    if (isPrimary) {
                        return id
                    }
                } while (cursor.moveToNext())
                return fallbackId
            }
        }
        return null
    }

    fun addEvent(
        context: Context,
        calendarId: Long,
        title: String,
        description: String,
        startMillis: Long
    ): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, startMillis + 15 * 60 * 1000) // 15 mins
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        
        return try {
            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()
            
            // Add a reminder
            if (eventId != null) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, 0)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
            eventId
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteEvent(context: Context, eventId: Long) {
        try {
            val uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId.toString())
            context.contentResolver.delete(uri, null, null)
        } catch (e: SecurityException) {
            // Permission not granted or event not found
        } catch (e: Exception) {
        }
    }
}
