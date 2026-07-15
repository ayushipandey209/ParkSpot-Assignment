package com.zobaze.parkspot.data.model

/** Fixed set of numbered slots. Hardcoded per the brief (A1–A20). */
object ParkingSlots {
    val ALL: List<String> = (1..20).map { "A$it" }
}
