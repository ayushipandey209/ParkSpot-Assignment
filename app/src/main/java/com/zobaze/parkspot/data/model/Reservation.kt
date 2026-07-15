package com.zobaze.parkspot.data.model

import com.google.firebase.firestore.DocumentId

/**
 * A single booking. Stored in the "reservations" collection.
 *
 * [lockIds] records every slotLock document this reservation owns, so that
 * cancellation can delete exactly those locks (freeing the slot) without
 * having to recompute the buckets.
 *
 * All defaults are present because Firestore deserializes via a no-arg
 * constructor + field setters.
 */
data class Reservation(
    @DocumentId val id: String = "",
    val slotId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val lockIds: List<String> = emptyList(),
    val createdAtMillis: Long = 0L,
)
