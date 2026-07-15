package com.zobaze.parkspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zobaze.parkspot.data.model.Reservation
import com.zobaze.parkspot.ui.ParkingViewModel
import com.zobaze.parkspot.ui.TimeGrid
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String,
    onSignOut: () -> Unit,
    vm: ParkingViewModel = viewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ParkSpot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Book a slot") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("My reservations") })
            }
            when (tab) {
                0 -> BookTab(vm)
                else -> MyReservationsTab(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookTab(vm: ParkingViewModel) {
    val window by vm.window.collectAsState()
    val taken by vm.takenSlots.collectAsState()
    val busy by vm.busySlot.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pick a time window", fontWeight = FontWeight.SemiBold)

        // Date chips: today + next 6 days
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..3).forEach { offset ->
                val d = LocalDate.now().plusDays(offset.toLong())
                FilterChip(
                    selected = window.date == d,
                    onClick = { vm.setDate(d) },
                    label = { Text(if (offset == 0) "Today" else TimeGrid.dateLabel(d)) },
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeDropdown(
                label = "Start",
                selectedIndex = window.startSlot,
                count = TimeGrid.SLOTS_PER_DAY,
                itemLabel = { TimeGrid.slotLabel(it) },
                onSelect = { vm.setStartSlot(it) },
                modifier = Modifier.weight(1f),
            )
            TimeDropdown(
                label = "Duration",
                selectedIndex = window.durationSlots - 1,
                count = 16, // up to 8 hours
                itemLabel = { "${(it + 1) * 30} min" },
                onSelect = { vm.setDuration(it + 1) },
                modifier = Modifier.weight(1f),
            )
        }

        if (!window.isValid) {
            Text(
                "That window runs past midnight. Shorten the duration or pick an earlier start.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(
                "Showing availability for ${TimeGrid.dateLabel(window.date)}, " +
                    "${TimeGrid.slotLabel(window.startSlot)}–${TimeGrid.slotLabel(window.endSlotExclusive)}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.allSlots, key = { it }) { slot ->
                val isTaken = slot in taken
                SlotCell(
                    slot = slot,
                    taken = isTaken,
                    busy = busy == slot,
                    enabled = window.isValid && !isTaken && busy == null,
                    onReserve = { vm.reserve(slot) },
                )
            }
        }
    }
}

@Composable
private fun SlotCell(
    slot: String,
    taken: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onReserve: () -> Unit,
) {
    val container = when {
        taken -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().height(72.dp).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(slot, fontWeight = FontWeight.Bold)
            when {
                busy -> CircularProgressIndicator(Modifier.height(16.dp))
                taken -> Text("Taken", style = MaterialTheme.typography.labelSmall)
                else -> OutlinedButton(
                    onClick = onReserve,
                    enabled = enabled,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) { Text("Reserve", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown(
    label: String,
    selectedIndex: Int,
    count: Int,
    itemLabel: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = itemLabel(selectedIndex.coerceIn(0, count - 1)),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0 until count).forEach { i ->
                DropdownMenuItem(
                    text = { Text(itemLabel(i)) },
                    onClick = { onSelect(i); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun MyReservationsTab(vm: ParkingViewModel) {
    val reservations by vm.myReservations.collectAsState()
    val now = System.currentTimeMillis()
    val upcoming = reservations.filter { it.endMillis >= now }

    if (upcoming.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No upcoming reservations yet.")
            Text("Book one from the other tab.", style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(upcoming, key = { it.id }) { r ->
            ReservationCard(r, onCancel = { vm.cancel(r) })
        }
    }
}

@Composable
private fun ReservationCard(r: Reservation, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Slot ${r.slotId}", fontWeight = FontWeight.Bold)
                Text(formatRange(r.startMillis, r.endMillis), style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

private fun formatRange(startMillis: Long, endMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(endMillis).atZone(zone)
    return "${start.format(dayFmt)} · ${start.format(timeFmt)}–${end.format(timeFmt)}"
}
