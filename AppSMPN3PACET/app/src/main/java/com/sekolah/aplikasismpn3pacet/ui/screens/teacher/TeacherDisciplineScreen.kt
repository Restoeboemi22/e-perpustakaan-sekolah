package com.sekolah.aplikasismpn3pacet.ui.screens.teacher

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecordWithRule
import com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRule
import com.sekolah.aplikasismpn3pacet.data.entity.StudentWithUser
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherDisciplineUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherDisciplineViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDisciplineScreen(
    viewModel: TeacherDisciplineViewModel,
    userId: Long,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val submissionStatus by viewModel.submissionStatus.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.loadData(userId)
    }

    LaunchedEffect(submissionStatus) {
        submissionStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSubmissionStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Catatan Siswa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is TeacherDisciplineUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is TeacherDisciplineUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is TeacherDisciplineUiState.Success -> {
                    StudentHistoryContent(
                        students = state.students,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHistoryContent(
    students: List<StudentWithUser>,
    viewModel: TeacherDisciplineViewModel
) {
    var selectedStudent by remember { mutableStateOf<StudentWithUser?>(null) }
    var showStudentDropdown by remember { mutableStateOf(false) }
    val history by viewModel.studentHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Student Dropdown for History
        ExposedDropdownMenuBox(
            expanded = showStudentDropdown,
            onExpandedChange = { showStudentDropdown = !showStudentDropdown }
        ) {
            OutlinedTextField(
                value = selectedStudent?.user?.fullName ?: "Pilih Siswa untuk Lihat Riwayat",
                onValueChange = {},
                readOnly = true,
                label = { Text("Nama Siswa") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStudentDropdown) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showStudentDropdown,
                onDismissRequest = { showStudentDropdown = false }
            ) {
                students.forEach { student ->
                    DropdownMenuItem(
                        text = { Text(student.user.fullName) },
                        onClick = {
                            selectedStudent = student
                            showStudentDropdown = false
                            viewModel.loadStudentHistory(student.student.id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedStudent != null) {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada data pelanggaran.", color = MaterialTheme.colorScheme.secondary)
                }
            } else {
                LazyColumn {
                    items(history) { record ->
                        DisciplineRecordItem(record)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Silakan pilih siswa terlebih dahulu",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun DisciplineRecordItem(item: DisciplineRecordWithRule) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.rule.ruleName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.record.points} Poin",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(item.record.date),
                style = MaterialTheme.typography.bodySmall
            )
            if (!item.record.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${item.record.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
