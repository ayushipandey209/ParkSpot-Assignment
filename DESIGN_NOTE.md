# Design note: how ParkSpot guarantees no double-booking

**The requirement.** No two reservations on the same slot may have overlapping
time windows — under *any* concurrency, including two people tapping "Reserve"
on the same slot and window at the same millisecond.

## First approach (and why I dropped it)

My instinct was the obvious one: when a user reserves slot A3 for 9–11, run a
Firestore **transaction** that (1) queries the `reservations` collection for
existing bookings on A3, (2) checks whether any overlaps [9:00, 11:00), and
(3) writes the new reservation if none do.

That doesn't work, for a concrete reason: **Firestore transactions cannot run
queries.** Inside a transaction you may only `get()` documents *by ID*
(`transaction.get(ref)`), not `where(...)` queries. So "read all overlapping
reservations, then write atomically" is not an operation Firestore offers. The
same limitation blocks the security-rules angle — rules can't query the
collection to check for overlap either. Any check I did *outside* the
transaction would race: two clients both read "no overlap", both write, both
succeed → double-booking.

## Final approach: quantize time into claimable buckets

I reduced the hard question ("does any existing reservation overlap this
window?") to a set of easy questions ("does *this specific* document exist?"),
because existence-by-ID is exactly what transactions *can* check.

- Time is discretized into fixed **30-minute buckets**. Two windows on a slot
  overlap **iff** they share at least one bucket (proven by `TimeBucketTest`).
- Each `(slot, bucket)` pair maps to a **lock document** with a deterministic
  ID, e.g. `A3__876540`. Its existence means "A3 is taken for that bucket".
- Reserving 9–11 on A3 means claiming the buckets 09:00, 09:30, 10:00, 10:30
  (end is exclusive). In **one transaction** I `get()` all four lock docs; if
  any exists, I abort with a conflict; otherwise I write the reservation doc
  plus all four lock docs. Firestore transactions are **serializable**, so if
  two clients race, the second to commit is re-run against the now-existing
  lock and aborts. No overlapping pair can ever both commit.

There's a **second, independent** line of defence in the security rules:
`slotLocks` are **create-only** (no `update` allowed). Firestore only fires the
`create` rule when a document doesn't already exist, so a bucket can be claimed
at most once even by a hand-rolled client that skips the transaction — the
racing writer hits `update`, which is denied.

## Why I'm confident

Correctness rests on two mechanisms that are both *by-ID* operations Firestore
actually supports — a serializable transaction and a create-only rule — rather
than on a query Firestore can't do atomically. Cancellation is the mirror
image: deleting a reservation deletes its lock docs in one batch, freeing the
buckets, and every screen updates live via snapshot listeners.

## Tradeoff I accepted

Time is quantized to 30 minutes, so bookings align to `:00`/`:30`. Arbitrary-
second windows would need a finer bucket size (more lock docs per booking) or
an interval-overlap structure, which Firestore isn't built for. For a parking
board, 30-minute granularity is the right call; the bucket width is a single
constant (`TimeBucket.BUCKET_MINUTES`) if it ever needs to change.
