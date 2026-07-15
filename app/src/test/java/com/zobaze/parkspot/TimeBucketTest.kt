package com.zobaze.parkspot

import com.zobaze.parkspot.data.TimeBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the correctness core: the claim that two reservations
 * overlap in time IFF they share at least one 30-minute bucket. This is the
 * property the whole no-double-booking design rests on, so it's worth pinning
 * down independently of Firebase.
 */
class TimeBucketTest {

    private val MIN = 60_000L // one minute in millis
    private fun at(hour: Int, minute: Int = 0) = (hour * 60 + minute) * MIN

    private fun sharesBucket(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean {
        val a = TimeBucket.indicesFor(aStart, aEnd).toSet()
        val b = TimeBucket.indicesFor(bStart, bEnd).toSet()
        return a.intersect(b).isNotEmpty()
    }

    @Test fun endIsExclusive_09to11_coversFourBuckets() {
        val indices = TimeBucket.indicesFor(at(9), at(11))
        assertEquals(4, indices.size) // 09:00, 09:30, 10:00, 10:30 — NOT 11:00
    }

    @Test fun adjacentWindows_doNotShareABucket() {
        // 09:00–11:00 and 11:00–13:00 touch but must NOT be treated as overlapping.
        assertFalse(sharesBucket(at(9), at(11), at(11), at(13)))
    }

    @Test fun overlappingWindows_shareABucket() {
        // 09:00–11:00 and 10:00–12:00 overlap on 10:00 & 10:30.
        assertTrue(sharesBucket(at(9), at(11), at(10), at(12)))
    }

    @Test fun containedWindow_shareABucket() {
        // 09:30–10:00 sits entirely inside 09:00–11:00.
        assertTrue(sharesBucket(at(9), at(11), at(9, 30), at(10)))
    }

    @Test fun lockIdIsDeterministic() {
        val idx = TimeBucket.indexOf(at(9))
        assertEquals(TimeBucket.lockId("A3", idx), TimeBucket.lockId("A3", idx))
        assertFalse(TimeBucket.lockId("A3", idx) == TimeBucket.lockId("A4", idx))
    }
}
