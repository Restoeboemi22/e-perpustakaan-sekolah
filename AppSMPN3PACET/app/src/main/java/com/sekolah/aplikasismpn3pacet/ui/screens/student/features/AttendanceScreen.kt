package com.sekolah.aplikasismpn3pacet.ui.screens.student.features

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.sekolah.aplikasismpn3pacet.data.AttendanceStatus
import com.sekolah.aplikasismpn3pacet.data.entity.Attendance
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.AttendanceUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Timer for live clock
    var currentTime by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            kotlinx.coroutines.delay(1000)
        }
    }

    val performCheckIn = { userId: Long ->
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.checkIn(userId, location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Gagal mendapatkan lokasi. Pastikan GPS aktif.", Toast.LENGTH_LONG).show()
                    viewModel.checkIn(userId) // Will likely trigger error again but shows intent
                }
            }.addOnFailureListener {
                 Toast.makeText(context, "Gagal mendapatkan lokasi: ${it.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Izin lokasi diperlukan.", Toast.LENGTH_SHORT).show()
        }
    }

    var pendingCheckInUserId by remember { mutableStateOf<Long?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            pendingCheckInUserId?.let { performCheckIn(it) }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak. Tidak dapat melakukan absensi.", Toast.LENGTH_LONG).show()
        }
    }

    val onCheckInClick = { userId: Long ->
         pendingCheckInUserId = userId
         if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
             performCheckIn(userId)
         } else {
             requestPermissionLauncher.launch(arrayOf(
                 Manifest.permission.ACCESS_FINE_LOCATION,
                 Manifest.permission.ACCESS_COARSE_LOCATION
             ))
         }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Absensi Siswa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is AttendanceUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AttendanceUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning, 
                            contentDescription = "Error", 
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onCheckInClick(0L) }) {
                            Text("Coba Lagi")
                        }
                    }
                }
                is AttendanceUiState.Success -> {
                    AttendanceContent(
                        state = state,
                        currentTime = currentTime,
                        onCheckIn = { onCheckInClick(state.student.userId) }
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceContent(
    state: AttendanceUiState.Success,
    currentTime: Date,
    onCheckIn: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateFormatter.format(currentTime),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = timeFormatter.format(currentTime),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = "Jadwal Masuk: ${state.todaySchedule}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), 
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (state.todayAttendance == null) {
                        if (state.isHoliday) {
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color.Gray,
                                    disabledContentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("HARI LIBUR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Tidak ada jadwal absensi hari ini.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = onCheckIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHECK IN SEKARANG", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Tekan tombol di atas untuk absensi masuk.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val isLate = state.todayAttendance.status == AttendanceStatus.LATE
                        val statusColor = if (isLate) Color(0xFFE65100) else Color(0xFF2E7D32) // Orange or Green
                        val statusText = if (isLate) "Terlambat" else "Hadir Tepat Waktu"
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null, 
                                    tint = statusColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Sudah Absensi",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "$statusText • ${state.todayAttendance.checkInTime}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Section
        item {
            val presentCount = state.history.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            val sickCount = state.history.count { it.status == AttendanceStatus.SICK }
            val permitCount = state.history.count { it.status == AttendanceStatus.PERMIT }
            val alphaCount = state.history.count { it.status == AttendanceStatus.ABSENT }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rekapitulasi Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryColumn("Hadir", presentCount.toString(), Color(0xFF4CAF50))
                        SummaryColumn("Sakit", sickCount.toString(), Color(0xFF2196F3))
                        SummaryColumn("Izin", permitCount.toString(), Color(0xFF9C27B0))
                        SummaryColumn("Alpa", alphaCount.toString(), Color(0xFFF44336))
                    }
                }
            }
        }

        // History Section
        item {
            Text(
                text = "Riwayat Absensi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat absensi", color = Color.Gray)
                }
            }
        } else {
            items(state.history) { attendance ->
                AttendanceItem(attendance)
            }
        }
    }
}

@Composable
fun AttendanceItem(attendance: Attendance) {
    val dateDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    
    val (color, icon, statusText) = when (attendance.status) {
        AttendanceStatus.PRESENT -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Hadir")
        AttendanceStatus.LATE -> Triple(Color(0xFFFF9800), Icons.Default.Warning, "Terlambat")
        AttendanceStatus.ABSENT -> Triple(Color(0xFFF44336), Icons.Default.Warning, "Alpa")
        AttendanceStatus.SICK -> Triple(Color(0xFF2196F3), Icons.Default.DateRange, "Sakit")
        AttendanceStatus.PERMIT -> Triple(Color(0xFF9C27B0), Icons.Default.DateRange, "Izin")
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = dateDateFormatter.format(attendance.date),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (attendance.checkInTime != null) {
                Text(
                    text = attendance.checkInTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SummaryColumn(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
