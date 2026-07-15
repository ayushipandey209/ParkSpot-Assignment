# Assignment: ParkSpot — Shared Parking Slot Reservation (Firestore)

Unlike a bug-fix task, this one is a **build from scratch**, based on a
feature brief — the way a real ticket/spec would come in. Deliberately
under-specified in places; that's normal for real work.

## The brief (as written by product)

> We run a shared parking area with a fixed number of numbered slots
> (e.g. A1–A20). Employees should be able to open the app, see which slots
> are free for a given time window, and reserve one. Reservations are for
> a specific start and end time (not just "today") — someone might book
> Slot A3 from 9am–11am, and someone else should still be able to book the
> same slot for 2pm–4pm the same day.
>
> Two people must never be able to double-book an overlapping time window
> on the same slot, even if they tap "reserve" at the exact same moment.
> Everyone's screen should update in real time as slots get booked or
> freed up (someone cancels).

## What's already there

- You'll create your own Firebase project for this assignment (free
  tier is enough).
- Build a simple email password auth and design your database schema

## Features to build

1. List of parking slots (A1–A20 is fine, hardcode if you want) showing
   real-time availability for a time window the user picks.
2. Reserve a slot for a chosen start/end time.
3. Cancel your own reservation.
4. View your own upcoming reservations.
5. **Hard requirement**: no two reservations on the same slot may have
   overlapping time windows, under any concurrency scenario — this is
   the part we care about most, not the UI polish.
6. Firestore Security Rules that:
   - Only let a user create/cancel their own reservations, not someone
     else's.

## What to submit

- Your code (repo git link).
- Your Firestore Security Rules file.
- A short written note (~half a page): how did you guarantee no
  double-booking? What was your first approach, and did you have to
  change it? (If you didn't have to change it, say why you were
  confident in it from the start.)
- **Your full AI chat/agent transcript**, unedited.
- A short note on anywhere the AI's suggested approach didn't hold up
  once you thought it through or tested it with two devices/tabs at once.
