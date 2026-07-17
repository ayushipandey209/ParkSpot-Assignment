# ParkSpot — Shared Parking Slot Reservation

An Android app (Kotlin + Jetpack Compose + Firebase) for reserving numbered
parking slots (A1–A20) for a specific start/end time, with a hard guarantee
that no two reservations on the same slot ever overlap in time — even under
concurrent taps — and live updates across all devices.

## Features

- Email/password auth (Firebase Auth).
- Pick a date + time window; see real-time availability of A1–A20 for it.
- Reserve a free slot; cancel your own reservation.
- "My reservations" tab showing your upcoming bookings.
- Concurrency-safe: overlapping double-booking is impossible (see
  [`DESIGN_NOTE.md`](DESIGN_NOTE.md)).
- Firestore Security Rules enforcing per-user ownership and single-claim locks
  ([`firestore.rules`](firestore.rules)).

## Architecture at a glance

```
MainActivity ─ AuthViewModel ── AuthRepository ─── Firebase Auth
             └ HomeScreen ─ ParkingViewModel ─ ReservationRepository ─ Firestore
                                                     │
                              TimeBucket (30-min quantization → deterministic lock IDs)
```

Data model in Firestore:

- `reservations/{id}` — `{ slotId, userId, userEmail, startMillis, endMillis, lockIds[], createdAtMillis }`
- `slotLocks/{slotId__bucketIndex}` — `{ slotId, bucketIndex, userId, reservationId }`
  One doc per (slot, 30-min bucket); its **existence** means that bucket is taken.


## How to test the no-double-booking guarantee

The interesting test is concurrent contention on the *same* slot + window:

1. Run the app on **two emulators/devices** (or two accounts), sign in as
   different users.
2. On both, select the same date and the same window (e.g. Today 09:00, 2h).
3. Tap **Reserve** on the **same slot** (say A3) on both, as close to
   simultaneously as you can.
4. Exactly **one** succeeds; the other gets "A3 was just taken…". Both screens
   then show A3 as *Taken* in real time.
5. Cancel from the owner's "My reservations" tab → A3 goes back to free on both
   screens live.

Also try adjacent windows (09:00–11:00 vs 11:00–13:00 on the same slot) — both
must succeed, because they share no bucket.

### Unit tests

`app/src/test/java/.../TimeBucketTest.kt` pins the core property (windows
overlap iff they share a bucket, end-exclusive). Run with:
```bash
./gradlew test
```

## Files to look at first

- [`DESIGN_NOTE.md`](DESIGN_NOTE.md) — the no-double-booking approach and the
  first approach I had to abandon.
- `app/src/main/java/com/zobaze/parkspot/data/TimeBucket.kt` — the quantization.
- `app/src/main/java/com/zobaze/parkspot/data/ReservationRepository.kt` — the
  transaction.
- [`firestore.rules`](firestore.rules) — ownership + create-only locks.
# ParkSpot-Assignment
