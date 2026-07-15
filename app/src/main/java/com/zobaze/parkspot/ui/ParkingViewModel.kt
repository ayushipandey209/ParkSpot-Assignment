package com.zobaze.parkspot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zobaze.parkspot.data.AuthRepository
import com.zobaze.parkspot.data.ReservationConflictException
import com.zobaze.parkspot.data.ReservationRepository
import com.zobaze.parkspot.data.model.ParkingSlots
import com.zobaze.parkspot.data.model.Reservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The time window currently being viewed/booked. */
data class WindowSelection(
    val date: LocalDate = LocalDate.now(),
    val startSlot: Int = TimeGrid.nextSlotFromNow(),
    val durationSlots: Int = 4, // 4 × 30min = 2 hours
) {
    val endSlotExclusive: Int get() = startSlot + durationSlots
    /** Booking spills past midnight — disallow to keep windows within one day. */
    val isValid: Boolean get() = endSlotExclusive <= TimeGrid.SLOTS_PER_DAY

    fun startMillis(): Long = TimeGrid.millisAt(date, startSlot)
    fun endMillis(): Long = TimeGrid.millisAt(date, startSlot) +
        durationSlots * com.zobaze.parkspot.data.TimeBucket.BUCKET_MINUTES * 60_000
}

class ParkingViewModel(
    private val reservations: ReservationRepository = ReservationRepository(),
    private val auth: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _window = MutableStateFlow(WindowSelection())
    val window: StateFlow<WindowSelection> = _window.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busySlot = MutableStateFlow<String?>(null)
    val busySlot: StateFlow<String?> = _busySlot.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val takenSlots: StateFlow<Set<String>> = _window
        .flatMapLatest { w ->
            if (w.isValid) reservations.observeTakenSlots(w.startMillis(), w.endMillis())
            else kotlinx.coroutines.flow.flowOf(emptySet())
        }
        .catch { _message.value = "Couldn't load availability: ${it.message}"; emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val myReservations: StateFlow<List<Reservation>> =
        (auth.currentUser?.uid?.let { reservations.observeMyReservations(it) }
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSlots: List<String> = ParkingSlots.ALL

    fun setDate(date: LocalDate) { _window.value = _window.value.copy(date = date) }
    fun setStartSlot(slot: Int) { _window.value = _window.value.copy(startSlot = slot) }
    fun setDuration(slots: Int) { _window.value = _window.value.copy(durationSlots = slots) }

    fun reserve(slotId: String) {
        val user = auth.currentUser ?: run { _message.value = "Please sign in again."; return }
        val w = _window.value
        if (!w.isValid) { _message.value = "That window runs past midnight — pick a shorter one."; return }
        if (_busySlot.value != null) return
        _busySlot.value = slotId
        viewModelScope.launch {
            val result = reservations.reserve(user, slotId, w.startMillis(), w.endMillis())
            _message.value = result.fold(
                onSuccess = { "Reserved $slotId ${TimeGrid.slotLabel(w.startSlot)}–${TimeGrid.slotLabel(w.endSlotExclusive)}" },
                onFailure = {
                    when (it) {
                        is ReservationConflictException ->
                            "$slotId was just taken for part of that window. Pick another slot or time."
                        else -> "Couldn't reserve: ${it.message}"
                    }
                },
            )
            _busySlot.value = null
        }
    }

    fun cancel(reservation: Reservation) {
        viewModelScope.launch {
            val result = reservations.cancel(reservation)
            _message.value = result.fold(
                onSuccess = { "Cancelled ${reservation.slotId}" },
                onFailure = { "Couldn't cancel: ${it.message}" },
            )
        }
    }

    fun consumeMessage() { _message.value = null }
}
