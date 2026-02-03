package com.sekolah.aplikasismpn3pacet.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekolah.aplikasismpn3pacet.data.ReportStatus
import com.sekolah.aplikasismpn3pacet.data.entity.BullyingReportWithReporter
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherBullyingReportUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherBullyingReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBullyingReportsScreen(
    viewModel: TeacherBullyingReportViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Masuk") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is TeacherBullyingReportUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TeacherBullyingReportUiState.Error -> {
                    Text(
                        text = (uiState as TeacherBullyingReportUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is TeacherBullyingReportUiState.Success -> {
                    val reports = (uiState as TeacherBullyingReportUiState.Success).reports
                    if (reports.isEmpty()) {
                        Text(
                            text = "Belum ada laporan masuk.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(reports) { item ->
                                TeacherReportItem(
                                    item = item,
                                    onUpdateStatus = { report, status ->
                                        viewModel.updateReportStatus(report, status)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun TeacherReportItem(
    item: BullyingReportWithReporter,
    onUpdateStatus: (com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport, ReportStatus) -> Unit
) {
    val report = item.report
    val reporterName = item.reporter?.user?.fullName ?: "Siswa Tidak Dikenal"
    val className = item.reporter?.classEntity?.className ?: ""
    val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
    
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
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pelapor: ",
                    style = MaterialTheme.typography.labelMedium
                )
                
                if (report.isAnonymous) {
                    Text(
                        text = "(ANONIM) ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                val displayText = if (className.isNotEmpty()) {
                    "$reporterName ($className)"
                } else {
                    reporterName
                }
                
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (report.status) {
                    ReportStatus.PENDING -> {
                        OutlinedButton(
                            onClick = { onUpdateStatus(report, ReportStatus.CLOSED) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Tolak")
                        }
                        Button(
                            onClick = { onUpdateStatus(report, ReportStatus.INVESTIGATING) }
                        ) {
                            Text("Proses")
                        }
                    }
                    ReportStatus.INVESTIGATING -> {
                         OutlinedButton(
                            onClick = { onUpdateStatus(report, ReportStatus.CLOSED) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Tutup")
                        }
                        Button(
                            onClick = { onUpdateStatus(report, ReportStatus.RESOLVED) }
                        ) {
                            Text("Selesai")
                        }
                    }
                    ReportStatus.RESOLVED -> {
                         Text(
                            text = "Laporan Selesai",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                     ReportStatus.CLOSED -> {
                         Text(
                            text = "Laporan Ditutup",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: ReportStatus) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.error to "Menunggu"
        ReportStatus.INVESTIGATING -> MaterialTheme.colorScheme.tertiary to "Ditinjau"
        ReportStatus.RESOLVED -> MaterialTheme.colorScheme.primary to "Selesai"
        ReportStatus.CLOSED -> MaterialTheme.colorScheme.secondary to "Ditutup"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
