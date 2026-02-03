package com.sekolah.aplikasismpn3pacet.ui.screens.student.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.SevenHabitsViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SevenHabitsScreen(
    viewModel: SevenHabitsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Helper for months
    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val years = (2024..2040).toList()
    var isYearDropdownExpanded by remember { mutableStateOf(false) }
    var isMonthDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("7 KAIH") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Checklist Mingguan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Month and Week Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Dropdown
                ExposedDropdownMenuBox(
                    expanded = isYearDropdownExpanded,
                    onExpandedChange = { isYearDropdownExpanded = !isYearDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isYearDropdownExpanded) },
                        modifier = Modifier.menuAnchor(),
                        label = { Text("Tahun") }
                    )
                    ExposedDropdownMenu(
                        expanded = isYearDropdownExpanded,
                        onDismissRequest = { isYearDropdownExpanded = false }
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    viewModel.updateYear(year)
                                    isYearDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = isMonthDropdownExpanded,
                    onExpandedChange = { isMonthDropdownExpanded = !isMonthDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = months.getOrElse(uiState.selectedMonth) { "" },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMonthDropdownExpanded) },
                        modifier = Modifier.menuAnchor(),
                        label = { Text("Bulan") }
                    )
                    ExposedDropdownMenu(
                        expanded = isMonthDropdownExpanded,
                        onDismissRequest = { isMonthDropdownExpanded = false }
                    ) {
                        months.forEachIndexed { index, month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    viewModel.updateMonth(index)
                                    isMonthDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Week Selection
                // Simple Row of buttons or a dropdown. Buttons might be easier for 1-4.
                // Let's use a ScrollableRow or just 4 small buttons if space permits.
                // Or a Dropdown for "Minggu Ke-X"
            }
            
            // Week Selection Buttons
            Text("Pilih Minggu:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..4) {
                    FilterChip(
                        selected = uiState.selectedWeek == i,
                        onClick = { viewModel.updateWeek(i) },
                        label = { Text("Minggu $i") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1976D2)) // Blue header
                    .border(1.dp, Color.Black)
            ) {
                // No
                TableCell(text = "No", weight = 0.1f, color = Color.White, isHeader = true)
                // Kebiasaan
                TableCell(text = "Kebiasaan", weight = 0.4f, color = Color.White, isHeader = true)
                // Days
                val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                days.forEach { day ->
                    TableCell(text = day, weight = 0.12f, color = Color.White, isHeader = true)
                }
            }

            // Table Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Black)
            ) {
                items(viewModel.habits) { habit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray) // Row border
                    ) {
                        // No
                        TableCell(text = "${habit.id}", weight = 0.1f, alignment = Alignment.Center)
                        
                        // Kebiasaan
                        Box(
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(4.dp)
                                .heightIn(min = 60.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = habit.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Checkboxes for Days (0=Mon, 6=Sun)
                        for (i in 0..6) {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = uiState.weekStart
                            calendar.add(Calendar.DAY_OF_YEAR, i)
                            val date = calendar.timeInMillis
                            
                            val isChecked = uiState.habitLogs.any { 
                                it.habitId == habit.id && isSameDay(it.date, date) && it.isCompleted 
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.12f)
                                    .heightIn(min = 60.dp)
                                    .border(0.5.dp, Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleHabit(habit.id, i, checked)
                                    }
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Color.Black, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    color: Color = Color.Black,
    isHeader: Boolean = false,
    alignment: Alignment = Alignment.Center
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .heightIn(min = if (isHeader) 40.dp else 60.dp)
            .padding(4.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            style = if (isHeader) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}