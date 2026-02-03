package com.sekolah.aplikasismpn3pacet.ui.screens.student.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekolah.aplikasismpn3pacet.data.IncidentType
import com.sekolah.aplikasismpn3pacet.data.ReportStatus
import com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.BullyingReportUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.BullyingReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BullyingReportScreen(
    viewModel: BullyingReportViewModel,
    userId: Long,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Buat Laporan, 1: Riwayat
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadReports(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lapor Bullying") },
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Buat Laporan") },
                    icon = { Icon(Icons.Default.Add, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Riwayat") },
                    icon = { Icon(Icons.Default.History, null) }
                )
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    0 -> CreateReportContent(
                        onSubmit = { desc, type, anon ->
                            viewModel.submitReport(userId, desc, type, anon)
                        },
                        uiState = uiState,
                        onReset = { viewModel.resetState() }
                    )
                    1 -> ReportHistoryContent(uiState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportContent(
    onSubmit: (String, IncidentType, Boolean) -> Unit,
    uiState: BullyingReportUiState,
    onReset: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(IncidentType.VERBAL) }
    var isAnonymous by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is BullyingReportUiState.SubmitSuccess) {
            showSuccessDialog = true
            description = ""
            selectedType = IncidentType.VERBAL
            isAnonymous = false
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false 
                onReset()
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSuccessDialog = false
                    onReset()
                }) { Text("OK") }
            },
            title = { Text("Laporan Terkirim") },
            text = { Text("Terima kasih atas laporan Anda. Sekolah akan segera menindaklanjuti laporan ini dengan menjaga kerahasiaan Anda.") }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Formulir Pelaporan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Laporkan tindakan bullying yang Anda alami atau saksikan. Kami menjamin kerahasiaan identitas Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Incident Type
        Text("Jenis Kejadian", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IncidentType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Deskripsi Kejadian") },
            placeholder = { Text("Ceritakan kronologi kejadian secara detail (siapa, kapan, dimana)...") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Anonymous
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { isAnonymous = !isAnonymous }
        ) {
            Checkbox(
                checked = isAnonymous,
                onCheckedChange = { isAnonymous = it }
            )
            Text("Laporkan secara anonim (Nama saya disembunyikan)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSubmit(description, selectedType, isAnonymous) },
            modifier = Modifier.fillMaxWidth(),
            enabled = description.isNotBlank() && uiState !is BullyingReportUiState.Loading
        ) {
            if (uiState is BullyingReportUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Kirim Laporan")
            }
        }

        if (uiState is BullyingReportUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun ReportHistoryContent(uiState: BullyingReportUiState) {
    when (uiState) {
        is BullyingReportUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is BullyingReportUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is BullyingReportUiState.Success, is BullyingReportUiState.SubmitSuccess -> {
            // Retrieve list from Success state. 
            // Note: SubmitSuccess doesn't carry list, so we might need to handle reloading or persist list in VM.
            // For simplicity, we assume VM updates state to Success after reload, or we only show list if Success.
            
            val reports = (uiState as? BullyingReportUiState.Success)?.reports ?: emptyList()

            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat laporan.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(reports) { report ->
                        ReportItem(report)
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun ReportItem(report: BullyingReport) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateFormatter.format(report.incidentDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(report.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Jenis: ${report.incidentType.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = report.description ?: "Tidak ada deskripsi",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            
            if (report.isAnonymous) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dikirim sebagai Anonim",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: ReportStatus) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> Color(0xFFFFA000) to "Menunggu"
        ReportStatus.INVESTIGATING -> Color(0xFF2196F3) to "Diproses"
        ReportStatus.RESOLVED -> Color(0xFF4CAF50) to "Selesai"
        ReportStatus.CLOSED -> Color.Gray to "Ditutup"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
