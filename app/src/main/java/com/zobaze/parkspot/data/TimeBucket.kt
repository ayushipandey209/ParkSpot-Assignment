package com.zobaze.parkspot.data

/**
 * Time is discretized into fixed-width buckets. This is the heart of the
 * no-double-booking guarantee: a reservation "owns" one lock document per
 * bucket it covers, and a lock document can be created exactly once.
 *
 * Overlap between two reservations on the same slot is equivalent to them
 * sharing at least one bucket. We can therefore reduce the (hard) "does any
 * existing reservation overlap this time window?" query into a set of
 * (easy) "does this specific lock document already exist?" checks by ID —
 * which is the only kind of check a Firestore transaction can actually do.
 */
object TimeBucket {

    /** Bucket width. 30 minutes → reservations must align to :00 / :30. */
    const val BUCKET_MINUTES: Long = 30
    private const val BUCKET_MILLIS: Long = BUCKET_MINUTES * 60 * 1000

    /** Floor of an epoch-millis instant to its bucket index. */
    fun indexOf(epochMillis: Long): Long = Math.floorDiv(epochMillis, BUCKET_MILLIS)

    /** True if [epochMillis] sits exactly on a bucket boundary. */
    fun isAligned(epochMillis: Long): Boolean = epochMillis % BUCKET_MILLIS == 0L

    /**
     * The bucket indices covered by the half-open interval [startMillis, endMillis).
     * A booking of 09:00–11:00 covers 09:00, 09:30, 10:00, 10:30 (not 11:00).
     */
    fun indicesFor(startMillis: Long, endMillis: Long): List<Long> {
        require(endMillis > startMillis) { "end must be after start" }
        val first = indexOf(startMillis)
        val last = indexOf(endMillis - 1) // -1 makes the end exclusive
        return (first..last).toList()
    }

    /**
     * Deterministic lock-document ID for a (slot, bucket) pair.
     * Its existence == that slot is taken for that bucket.
     */
    fun lockId(slotId: String, bucketIndex: Long): String = "${slotId}__$bucketIndex"
}
