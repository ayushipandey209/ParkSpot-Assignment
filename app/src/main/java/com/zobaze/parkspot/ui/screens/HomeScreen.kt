package com.zobaze.parkspot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zobaze.parkspot.data.model.Reservation
import com.zobaze.parkspot.ui.ParkingViewModel
import com.zobaze.parkspot.ui.TimeGrid
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.tooling.preview.Preview
import com.zobaze.parkspot.ui.WindowSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String,
    onSignOut: () -> Unit,
    vm: ParkingViewModel = viewModel(),
) {
    val window by vm.window.collectAsState()
    val taken by vm.takenSlots.collectAsState()
    val busy by vm.busySlot.collectAsState()
    val reservations by vm.myReservations.collectAsState()
    val message by vm.message.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    HomeScreenContent(
        userEmail = userEmail,
        tab = tab,
        onTabSelected = { tab = it },
        window = window,
        takenSlots = taken,
        busySlot = busy,
        allSlots = vm.allSlots,
        myReservations = reservations,
        onSetDate = vm::setDate,
        onSetStartSlot = vm::setStartSlot,
        onSetDuration = vm::setDuration,
        onReserve = vm::reserve,
        onCancel = vm::cancel,
        onSignOut = onSignOut,
        snackbarHost = { SnackbarHost(snackbar) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    userEmail: String,
    tab: Int,
    onTabSelected: (Int) -> Unit,
    window: WindowSelection,
    takenSlots: Set<String>,
    busySlot: String?,
    allSlots: List<String>,
    myReservations: List<Reservation>,
    onSetDate: (LocalDate) -> Unit,
    onSetStartSlot: (Int) -> Unit,
    onSetDuration: (Int) -> Unit,
    onReserve: (String) -> Unit,
    onCancel: (Reservation) -> Unit,
    onSignOut: () -> Unit,
    snackbarHost: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ParkSpot", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout, 
                            contentDescription = "Sign out",
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF0F172A)
                )
            )
        },
        snackbarHost = snackbarHost,
        containerColor = Color(0xFFF8FAFC), // Cool slate 50 background
        modifier = modifier,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                contentColor = Color(0xFF2563EB), // Cool Blue indicator & active text color
                containerColor = Color.White
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Book a slot", fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Medium) },
                    selectedContentColor = Color(0xFF2563EB),
                    unselectedContentColor = Color(0xFF64748B)
                )
                Tab(
                    selected = tab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("My reservations", fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Medium) },
                    selectedContentColor = Color(0xFF2563EB),
                    unselectedContentColor = Color(0xFF64748B)
                )
            }
            when (tab) {
                0 -> BookTab(
                    window = window,
                    takenSlots = takenSlots,
                    busySlot = busySlot,
                    allSlots = allSlots,
                    onSetDate = onSetDate,
                    onSetStartSlot = onSetStartSlot,
                    onSetDuration = onSetDuration,
                    onReserve = onReserve
                )
                else -> MyReservationsTab(
                    reservations = myReservations,
                    onCancel = onCancel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookTab(
    window: WindowSelection,
    takenSlots: Set<String>,
    busySlot: String?,
    allSlots: List<String>,
    onSetDate: (LocalDate) -> Unit,
    onSetStartSlot: (Int) -> Unit,
    onSetDuration: (Int) -> Unit,
    onReserve: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Pick a time window",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            fontSize = 16.sp
        )

        // Scrollable date selection row (Today + next 6 days)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..6).forEach { offset ->
                val d = LocalDate.now().plusDays(offset.toLong())
                FilterChip(
                    selected = window.date == d,
                    onClick = { onSetDate(d) },
                    label = { Text(if (offset == 0) "Today" else TimeGrid.dateLabel(d), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF475569)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = window.date == d,
                        borderColor = Color(0xFFE2E8F0),
                        selectedBorderColor = Color(0xFF2563EB)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeDropdown(
                label = "Start",
                selectedIndex = window.startSlot,
                count = TimeGrid.SLOTS_PER_DAY,
                itemLabel = { TimeGrid.slotLabel(it) },
                onSelect = onSetStartSlot,
                modifier = Modifier.weight(1f),
            )
            TimeDropdown(
                label = "Duration",
                selectedIndex = window.durationSlots - 1,
                count = 16, // up to 8 hours
                itemLabel = { "${(it + 1) * 30} min" },
                onSelect = { onSetDuration(it + 1) },
                modifier = Modifier.weight(1f),
            )
        }

        if (!window.isValid) {
            Text(
                "That window runs past midnight. Shorten the duration or pick an earlier start.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text(
                "Showing availability for ${TimeGrid.dateLabel(window.date)}, " +
                    "${TimeGrid.slotLabel(window.startSlot)}–${TimeGrid.slotLabel(window.endSlotExclusive)}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(allSlots, key = { it }) { slot ->
                val isTaken = slot in takenSlots
                SlotCell(
                    slot = slot,
                    taken = isTaken,
                    busy = busySlot == slot,
                    enabled = window.isValid && !isTaken && busySlot == null,
                    onReserve = { onReserve(slot) },
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
    val containerColor = when {
        taken -> Color(0xFFFEF2F2) // Soft red background
        else -> Color(0xFFEFF6FF)  // Soft blue background
    }
    val borderColor = when {
        taken -> Color(0xFFFCA5A5) // Soft red border
        else -> Color(0xFFBFDBFE)  // Soft blue border
    }
    val textColor = when {
        taken -> Color(0xFF991B1B) // Dark red text
        else -> Color(0xFF1E40AF)  // Dark blue text
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(slot, fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            when {
                busy -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
                taken -> Text(
                    "Taken", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = textColor, 
                    fontWeight = FontWeight.Bold
                )
                else -> OutlinedButton(
                    onClick = onReserve,
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2563EB),
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) { 
                    Text("Reserve", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) 
                }
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
private fun MyReservationsTab(
    reservations: List<Reservation>,
    onCancel: (Reservation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val upcoming = reservations.filter { it.endMillis >= now }

    if (upcoming.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No upcoming reservations yet.", fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
            Text("Book one from the other tab.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
        }
        return
    }

    LazyColumn(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(upcoming, key = { it.id }) { r ->
            ReservationCard(r, onCancel = { onCancel(r) })
        }
    }
}

@Composable
private fun ReservationCard(r: Reservation, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Slot ${r.slotId}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatRange(r.startMillis, r.endMillis), 
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFDC2626) // Dark Red for cancellation
                ),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)), // Light red border
                shape = RoundedCornerShape(10.dp)
            ) { 
                Text("Cancel", fontWeight = FontWeight.Bold) 
            }
        }
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

private fun formatRange(startMillis: Long, endMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(startMillis).atZone(zone)
    val end = Instant.ofEpochMilli(endMillis).atZone(zone)
    val startStr = start.format(timeFmt).lowercase()
    val endStr = end.format(timeFmt).lowercase()
    return "${start.format(dayFmt)} · $startStr–$endStr"
}

@Preview(showBackground = true)
@Composable
private fun SlotCellFreePreview() {
    MaterialTheme {
        SlotCell(slot = "A3", taken = false, busy = false, enabled = true, onReserve = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SlotCellTakenPreview() {
    MaterialTheme {
        SlotCell(slot = "A5", taken = true, busy = false, enabled = false, onReserve = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ReservationCardPreview() {
    MaterialTheme {
        ReservationCard(
            r = Reservation(
                id = "mock_id",
                slotId = "A3",
                userId = "mock_user",
                startMillis = System.currentTimeMillis() + 3600000,
                endMillis = System.currentTimeMillis() + 7200000
            ),
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreenContent(
            userEmail = "employee@company.com",
            tab = 0,
            onTabSelected = {},
            window = WindowSelection(),
            takenSlots = setOf("A1", "A4", "A10"),
            busySlot = null,
            allSlots = (1..20).map { "A$it" },
            myReservations = listOf(
                Reservation(
                    id = "r1",
                    slotId = "A3",
                    startMillis = System.currentTimeMillis() + 3600000,
                    endMillis = System.currentTimeMillis() + 7200000
                )
            ),
            onSetDate = {},
            onSetStartSlot = {},
            onSetDuration = {},
            onReserve = {},
            onCancel = {},
            onSignOut = {},
            snackbarHost = {}
        )
    }
}
