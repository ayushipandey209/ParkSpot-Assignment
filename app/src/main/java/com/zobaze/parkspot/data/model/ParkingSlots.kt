package com.zobaze.parkspot.data.model

/** Fixed set of numbered slots. Hardcoded per the brief (A1–A20).
 * For a production app, you'd fetch slots from Firestore so admins can add/remove them without a new app releasee */

object ParkingSlots {
    val ALL: List<String> = (1..20).map { "A$it" }
}
