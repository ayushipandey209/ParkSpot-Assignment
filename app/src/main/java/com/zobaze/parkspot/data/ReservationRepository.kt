package com.zobaze.parkspot.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.zobaze.parkspot.data.model.ParkingSlots
import com.zobaze.parkspot.data.model.Reservation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Thrown when the requested slot/time window is already (partly) taken. */
class ReservationConflictException(val slotId: String) :
    Exception("Slot $slotId is already booked for part of that time window")

class ReservationRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
) {
    private val reservations = db.collection("reservations")
    private val locks = db.collection("slotLocks")

    /**
     * Atomically reserve [slotId] for [startMillis, endMillis).
     *
     * Correctness argument:
     *  - The window is reduced to a set of bucket lock documents with
     *    deterministic IDs.
     *  - Inside a single Firestore transaction we READ every lock doc first
     *    (transactions require all reads before writes), abort if any exists,
     *    then WRITE the reservation + all lock docs.
     *  - Firestore transactions are serializable: if two clients race, the
     *    one that commits second is re-run against the now-existing lock and
     *    aborts. So no overlapping double-booking can commit, even at the
     *    exact same millisecond.
     *  - The security rules make slotLock docs create-only (no updates), so
     *    a lock can never be silently overwritten even by a hand-rolled client.
     */
    suspend fun reserve(
        user: FirebaseUser,
        slotId: String,
        startMillis: Long,
        endMillis: Long,
    ): Result<Unit> = runCatching {
        require(slotId in ParkingSlots.ALL) { "Unknown slot $slotId" }
        require(endMillis > startMillis) { "End time must be after start time" }
        require(TimeBucket.isAligned(startMillis) && TimeBucket.isAligned(endMillis)) {
            "Times must align to ${TimeBucket.BUCKET_MINUTES}-minute boundaries"
        }

        val indices = TimeBucket.indicesFor(startMillis, endMillis)
        val lockIds = indices.map { TimeBucket.lockId(slotId, it) }
        val lockRefs = lockIds.map { locks.document(it) }
        val reservationRef = reservations.document()

        db.runTransaction { txn ->
            // ---- READ phase (all reads before any write) ----
            val snapshots = lockRefs.map { txn.get(it) }
            if (snapshots.any { it.exists() }) {
                throw ReservationConflictException(slotId)
            }

            // ---- WRITE phase ----
            val reservation = Reservation(
                slotId = slotId,
                userId = user.uid,
                userEmail = user.email ?: "",
                startMillis = startMillis,
                endMillis = endMillis,
                lockIds = lockIds,
                createdAtMillis = System.currentTimeMillis(),
            )
            txn.set(reservationRef, reservation) // @DocumentId is ignored on write

            lockRefs.forEachIndexed { i, ref ->
                txn.set(
                    ref,
                    mapOf(
                        "slotId" to slotId,
                        "bucketIndex" to indices[i],
                        "userId" to user.uid,
                        "reservationId" to reservationRef.id,
                    ),
                )
            }
            null
        }.await()
        Unit
    }

    /**
     * Cancel [reservation]. Deletes the reservation doc and every lock it
     * owns in one atomic batch, freeing the slot. Ownership is enforced by
     * the security rules (a user can only delete docs whose userId is theirs).
     */
    suspend fun cancel(reservation: Reservation): Result<Unit> = runCatching {
        db.runBatch { batch ->
            batch.delete(reservations.document(reservation.id))
            reservation.lockIds.forEach { batch.delete(locks.document(it)) }
        }.await()
        Unit
    }

    /**
     * Real-time set of slot IDs that are TAKEN for the window
     * [startMillis, endMillis). Everything in ParkingSlots.ALL not in this
     * set is free. Backed by a Firestore snapshot listener so every device
     * updates live as bookings come and go.
     */
    fun observeTakenSlots(startMillis: Long, endMillis: Long): Flow<Set<String>> = callbackFlow {
        val qStart = TimeBucket.indexOf(startMillis)
        val qEnd = TimeBucket.indexOf(endMillis - 1)
        val registration = locks
            .whereGreaterThanOrEqualTo("bucketIndex", qStart)
            .whereLessThanOrEqualTo("bucketIndex", qEnd)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val taken = snap?.documents
                    ?.mapNotNull { it.getString("slotId") }
                    ?.toSet()
                    ?: emptySet()
                trySend(taken)
            }
        awaitClose { registration.remove() }
    }

    /** Real-time list of the signed-in user's reservations, soonest first. */
    fun observeMyReservations(userId: String): Flow<List<Reservation>> = callbackFlow {
        val registration = reservations
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err); return@addSnapshotListener
                }
                val list = snap?.toObjects(Reservation::class.java)
                    ?.sortedBy { it.startMillis }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
}
