package com.sekolah.aplikasismpn3pacet.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekolah.aplikasismpn3pacet.data.AttendanceStatus
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.StudentAttendanceSummary
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherAttendanceUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherAttendanceViewModel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceScreen(
    viewModel: TeacherAttendanceViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val months = DateFormatSymbols().months
    
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf<Long?>(null) }
    var selectedStudentName by remember { mutableStateOf("") }

    if (showStatusDialog && selectedStudentId != null) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Kehadiran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Siswa: $selectedStudentName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    Button(
                        onClick = { 
                            viewModel.updateAttendance(selectedStudentId!!, AttendanceStatus.PRESENT)
                            showStatusDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) { Text("Hadir (H)") }
                    
                    Button(
                        onClick = { 
                            viewModel.updateAttendance(selectedStudentId!!, AttendanceStatus.SICK)
                            showStatusDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) { Text("Sakit (S)") }

                    Button(
                        onClick = { 
                            viewModel.updateAttendance(selectedStudentId!!, AttendanceStatus.PERMIT)
                            showStatusDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) { Text("Izin (I)") }

                    Button(
                        onClick = { 
                            viewModel.updateAttendance(selectedStudentId!!, AttendanceStatus.ABSENT)
                            showStatusDialog = false 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) { Text("Alpa (A)") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rekapitulasi Kehadiran") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is TeacherAttendanceUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TeacherAttendanceUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is TeacherAttendanceUiState.Success -> {
                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Monitoring Harian") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Rekap Bulanan") }
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Kelas: ${state.classEntity.className}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (selectedTab == 0) {
                            // Daily View
                            val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                            val currentDate = Date(state.selectedDate)

                            // Date Navigation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = {
                                    viewModel.setDate(state.selectedDate - 86400000) // Previous Day
                                }) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day")
                                }

                                Text(
                                    text = dateFormat.format(currentDate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = {
                                    viewModel.setDate(state.selectedDate + 86400000) // Next Day
                                }) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day")
                                }
                            }

                            // Daily List
                            if (state.dailyAttendance.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada data siswa", color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(state.dailyAttendance) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    selectedStudentId = item.student.student.id
                                                    selectedStudentName = item.student.user.fullName
                                                    showStatusDialog = true
                                                },
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.student.user.fullName, fontWeight = FontWeight.Bold)
                                                    Text(item.student.user.nisNip, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                    if (!item.note.isNullOrEmpty()) {
                                                        Text("Note: ${item.note}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    val statusColor = when (item.status) {
                                                        AttendanceStatus.PRESENT -> Color(0xFF4CAF50) // Green
                                                        AttendanceStatus.LATE -> Color(0xFFFFC107) // Amber
                                                        AttendanceStatus.SICK -> Color(0xFF2196F3) // Blue
                                                        AttendanceStatus.PERMIT -> Color(0xFF9C27B0) // Purple
                                                        AttendanceStatus.ABSENT -> Color(0xFFF44336) // Red
                                                        null -> Color.Gray
                                                    }
                                                    
                                                    val statusText = when (item.status) {
                                                        AttendanceStatus.PRESENT -> "HADIR"
                                                        AttendanceStatus.LATE -> "TERLAMBAT"
                                                        AttendanceStatus.SICK -> "SAKIT"
                                                        AttendanceStatus.PERMIT -> "IZIN"
                                                        AttendanceStatus.ABSENT -> "ALPHA"
                                                        null -> "BELUM ABSEN"
                                                    }

                                                    Surface(
                                                        color = statusColor.copy(alpha = 0.1f),
                                                        shape = MaterialTheme.shapes.small
                                                    ) {
                                                        Text(
                                                            text = statusText,
                                                            color = statusColor,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    
                                                    if (item.time != null) {
                                                        Text(item.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } else {
                            // Monthly View (Existing Logic)
                            // Filters
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Month Selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = months[state.selectedMonth],
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Bulan") },
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                                modifier = Modifier.fillMaxWidth().clickable { showMonthDropdown = true }
                                            )
                                            // Overlay transparent button to catch click
                                            Box(modifier = Modifier.matchParentSize().clickable { showMonthDropdown = true })
                                            
                                            DropdownMenu(
                                                expanded = showMonthDropdown,
                                                onDismissRequest = { showMonthDropdown = false }
                                            ) {
                                                months.forEachIndexed { index, month ->
                                                    if (month.isNotEmpty()) {
                                                        DropdownMenuItem(
                                                            text = { Text(month) },
                                                            onClick = {
                                                                viewModel.setMonth(index)
                                                                showMonthDropdown = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Year Selector
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedTextField(
                                                value = state.selectedYear.toString(),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Tahun") },
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                                modifier = Modifier.fillMaxWidth().clickable { showYearDropdown = true }
                                            )
                                            Box(modifier = Modifier.matchParentSize().clickable { showYearDropdown = true })

                                            DropdownMenu(
                                                expanded = showYearDropdown,
                                                onDismissRequest = { showYearDropdown = false }
                                            ) {
                                                val currentYear = 2024 // Base year
                                                for (i in 0..5) {
                                                    val year = currentYear + i
                                                    DropdownMenuItem(
                                                        text = { Text(year.toString()) },
                                                        onClick = {
                                                            viewModel.setYear(year)
                                                            showYearDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(8.dp)
                            ) {
                                Text("Nama", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("H", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                                Text("S", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                                Text("I", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                                Text("A", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
                            }

                            // List
                            if (state.students.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada siswa di kelas ini", color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(state.students) { summary ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(width = 0.5.dp, color = Color.LightGray)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2f)) {
                                                Text(summary.student.user.fullName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                Text(summary.student.user.nisNip, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Text(summary.present.toString(), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 14.sp)
                                            Text(summary.sick.toString(), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 14.sp)
                                            Text(summary.permission.toString(), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 14.sp)
                                            Text(summary.alpha.toString(), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
