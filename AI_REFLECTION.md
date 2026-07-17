# AI reflection — ParkSpot

*Written in the first person, for the "where did the AI's approach not hold up"
part of the submission. It's grounded in what actually happened while building
this. The one section I've left for me to fill in is the two-device test result,
because that only exists once I run it — see the last section.*

## How I used AI

I used an AI agent to scaffold the project and draft the implementation, then
reviewed and corrected it. I treated its output as a strong first draft, not as
finished code — the interesting parts of this assignment were the ones where the
first draft was wrong.

## Where the AI's first approach didn't hold up

**1. "Query for overlaps inside a Firestore transaction" — the big one.**
The first design that came up for preventing double-booking was the intuitive
one: open a transaction, query the `reservations` collection for existing
bookings on that slot, check whether any overlap the requested window, and write
if none do. It reads like correct pseudocode. It isn't implementable, because
**Firestore transactions can't run queries** — inside a transaction you can only
`get()` documents *by ID*. The same limitation kills the "enforce it in security
rules" idea, since rules can't query the collection either. Any overlap check
done *outside* the transaction races: two clients both read "free", both write,
both succeed → double-booking. This is the assumption I had to catch and throw
out.

The fix was to stop asking "does anything overlap?" (a query) and instead make
the check "does *this specific* lock document exist?" (a get-by-ID, which
transactions *do* support). I quantized time into 30-minute buckets, mapped each
`(slot, bucket)` to a deterministic lock-document ID, and reserve inside one
transaction that reads all the lock docs by ID and aborts if any exists. I added
a second, independent guard in the rules: `slotLocks` are create-only, so a
bucket can't be claimed twice even by a client that bypasses the app. Full
reasoning is in `DESIGN_NOTE.md`.

**2. Small but real: code that looked right but wouldn't compile.**
A few things in the AI's draft needed catching on review:
- `Modifier.padding()` with no arguments — there's no zero-arg overload, so it
  wouldn't compile. Replaced with `fillMaxSize()`.
- A missing `import ...fillMaxWidth` in the auth screen — the symbol was used
  four times but never imported. I caught it by cross-checking every modifier
  used against the import list rather than trusting it looked complete.
- `java.time` (LocalDate/LocalDateTime) isn't available below API 26 without
  core-library desugaring, so I bumped `minSdk` from 24 to 26 rather than pull
  in desugaring for one date picker.

None of these are dramatic, but they're the reason I don't take AI-generated
Android code at face value — it produces plausible imports and API calls that
are subtly off, and only compiling/reading it line by line surfaces them.

**3. A judgment call the AI wouldn't make for me: dependency versions.**
The newest Firebase BoM was 34.15.0, but I pinned 33.7.0 — a version I know
plays well with the Kotlin/AGP combo here — rather than take the bleeding edge
on an assignment. That's the kind of tradeoff (latest vs. known-good) the AI
will happily default either way on; it's on me to decide.

## What I verified

- The core overlap property (two windows conflict **iff** they share a bucket,
  with the end time exclusive) is pinned by unit tests in `TimeBucketTest.kt` —
  adjacent windows (9–11 vs 11–13) don't conflict, overlapping and contained
  ones do.
- Field names written by the repository match exactly what the security rules
  check (`userId`, `slotId`, `startMillis`, `endMillis`, `bucketIndex`,
  `reservationId`).

## The test only I can run — two-device concurrency

> **TODO before submitting — run this and write what I actually saw.**
> Sign in as two users on two emulators/tabs, pick the same slot + same window,
> and tap Reserve at the same time. Expected: exactly one succeeds, the other
> gets the "just taken" message, and both screens flip to *Taken* live. I'll
> record the real outcome here (including anything that *didn't* behave as
> expected) once I've run it — that observation, not the theory above, is the
> real proof.
> i have shared the working video drive link 