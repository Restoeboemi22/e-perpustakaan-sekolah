package com.sekolah.aplikasismpn3pacet.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sekolah.aplikasismpn3pacet.data.SubmissionStatus
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLogWithDetails
import com.sekolah.aplikasismpn3pacet.data.entity.User
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherLiteracyUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherLiteracyViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLiteracyScreen(
    teacherUser: User,
    viewModel: TeacherLiteracyViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingLogs by viewModel.logs.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Perlu Dinilai", "Riwayat")

    LaunchedEffect(teacherUser.id) {
        viewModel.loadPendingLogs(teacherUser.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penilaian Literasi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = { 
                        viewModel.loadPendingLogs(teacherUser.id)
                        android.widget.Toast.makeText(context, "Menyegarkan data...", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
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
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (uiState) {
                is TeacherLiteracyUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TeacherLiteracyUiState.Error -> {
                    val errorMsg = (uiState as TeacherLiteracyUiState.Error).message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = errorMsg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val items = if (selectedTab == 0) pendingLogs else historyLogs
                        
                        if (items.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (selectedTab == 0) "Tidak ada tugas yang perlu dinilai." else "Belum ada riwayat tugas.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(items) { item ->
                                LiteracyGradeCard(
                                    item = item,
                                    isGrading = selectedTab == 0,
                                    onGrade = { grade, feedback ->
                                        viewModel.gradeLog(item.log, grade, feedback)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiteracyGradeCard(
    item: LiteracyLogWithDetails,
    isGrading: Boolean,
    onGrade: (String, String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Student Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = item.student.user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(item.log.submissionDate ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!isGrading) {
                    StatusChip(status = item.log.status)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Book Details (Template Sama dengan Siswa)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    LabelValue(label = "Judul Buku", value = item.log.bookTitle)
                    Spacer(modifier = Modifier.height(8.dp))
                    LabelValue(label = "Penulis", value = item.log.author)
                }
                Column(modifier = Modifier.weight(0.6f)) {
                    LabelValue(label = "Durasi Baca", value = item.log.readingDuration)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!isGrading && item.log.grade != null) {
                        LabelValue(label = "Nilai", value = item.log.grade ?: "-")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary
            Text(
                text = "Ringkasan:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.log.summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            )

            // Grading Button or Feedback Display
            if (isGrading) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Beri Nilai")
                }
            } else if (!item.log.feedback.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Catatan Guru:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.log.feedback ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }

    if (showDialog) {
        GradingDialog(
            onDismiss = { showDialog = false },
            onSubmit = { grade, feedback ->
                onGrade(grade, feedback)
                showDialog = false
            }
        )
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusChip(status: SubmissionStatus) {
    val (bgColor, textColor) = when (status) {
        SubmissionStatus.PENDING -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        SubmissionStatus.GRADED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        SubmissionStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = when(status) {
                SubmissionStatus.PENDING -> "Menunggu"
                SubmissionStatus.GRADED -> "Dinilai"
                SubmissionStatus.REJECTED -> "Ditolak"
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GradingDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var grade by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    val grades = listOf("A", "B", "C", "D")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nilai Tugas Literasi") },
        text = {
            Column {
                Text("Pilih Nilai:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    grades.forEach { g ->
                        FilterChip(
                            selected = grade == g,
                            onClick = { grade = g },
                            label = { Text(g) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Catatan / Feedback") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(grade, feedback) },
                enabled = grade.isNotEmpty()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
