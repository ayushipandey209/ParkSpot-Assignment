package com.zobaze.parkspot.ui

import com.zobaze.parkspot.data.TimeBucket
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Maps the UI's discrete pickers (date + half-hour slot index) to epoch millis
 * that align exactly to TimeBucket boundaries. Slot index runs 0..47 across a
 * day (0 = 00:00, 18 = 09:00, 47 = 23:30).
 */
object TimeGrid {
    const val SLOTS_PER_DAY = (24 * 60 / TimeBucket.BUCKET_MINUTES).toInt() // 48

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")

    fun slotLabel(slot: Int): String {
        val minutes = slot * TimeBucket.BUCKET_MINUTES
        val totalHours = minutes / 60
        val m = minutes % 60
        val amPm = if (totalHours >= 12) "pm" else "am"
        val h = when {
            totalHours == 0L -> 12
            totalHours > 12 -> totalHours - 12
            else -> totalHours
        }
        return "%d:%02d %s".format(h, m, amPm)
    }

    fun dateLabel(date: LocalDate): String = date.format(dateFmt)

    /** Epoch millis for the start of [slot] on [date] in the device's zone. */
    fun millisAt(date: LocalDate, slot: Int): Long =
        date.atStartOfDay(zone).plusMinutes(slot * TimeBucket.BUCKET_MINUTES).toInstant().toEpochMilli()

    /** The half-hour slot index at-or-after "now" today (for a sensible default). */
    fun nextSlotFromNow(): Int {
        val now = java.time.LocalTime.now()
        val idx = ((now.hour * 60 + now.minute) / TimeBucket.BUCKET_MINUTES).toInt() + 1
        return idx.coerceIn(0, SLOTS_PER_DAY - 1)
    }
}
