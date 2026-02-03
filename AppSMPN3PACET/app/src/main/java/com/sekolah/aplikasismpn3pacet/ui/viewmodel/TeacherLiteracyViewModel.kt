package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLog
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLogWithDetails
import com.sekolah.aplikasismpn3pacet.data.entity.StudentWithUser
import com.sekolah.aplikasismpn3pacet.data.SubmissionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

import android.util.Log

class TeacherLiteracyViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherLiteracyUiState>(TeacherLiteracyUiState.Idle)
    val uiState: StateFlow<TeacherLiteracyUiState> = _uiState

    private val _logs = MutableStateFlow<List<LiteracyLogWithDetails>>(emptyList())
    val logs: StateFlow<List<LiteracyLogWithDetails>> = _logs

    private val _historyLogs = MutableStateFlow<List<LiteracyLogWithDetails>>(emptyList())
    val historyLogs: StateFlow<List<LiteracyLogWithDetails>> = _historyLogs

    private val _students = MutableStateFlow<List<StudentWithUser>>(emptyList())
    val students: StateFlow<List<StudentWithUser>> = _students

    private var currentTeacherId: Long? = null
    
    init {
        repository.listenToLiteracyLogUpdates()
    }

    fun loadPendingLogs(userId: Long) {
        viewModelScope.launch {
            _uiState.value = TeacherLiteracyUiState.Loading
            try {
                // Fetch teacher entity first to get teacherId
                val teacher = repository.getTeacherByUserId(userId)
                
                if (teacher != null) {
                    currentTeacherId = teacher.id
                    // Fetch teacher's class using Teacher ID
                    val teacherClass = repository.getClassByHomeroomTeacherId(teacher.id)
                    
                    if (teacherClass != null) {
                         // Get students in the class
                         repository.getStudentsWithUserByClassId(teacherClass.id).collect { studentsList ->
                            _students.value = studentsList
                            val studentIds = studentsList.map { it.student.id }
                             if (studentIds.isNotEmpty()) {
                                 // Force a one-time refresh from Firebase to populate local DB
                                 repository.refreshLiteracyLogsForStudents(studentIds)
                                 // Pending logs
                                 launch {
                                     repository.getLiteracyLogsByStudentsAndStatus(studentIds, SubmissionStatus.PENDING).collect { logsList ->
                                         _logs.value = logsList
                                     }
                                 }
                                 // History logs (All logs)
                                 launch {
                                     repository.getLiteracyLogsByStudents(studentIds).collect { allLogs ->
                                         _historyLogs.value = allLogs
                                         _uiState.value = TeacherLiteracyUiState.Success
                                     }
                                 }
                             } else {
                                 _logs.value = emptyList()
                                 _historyLogs.value = emptyList()
                                 _uiState.value = TeacherLiteracyUiState.Success
                             }
                         }
                    } else {
                         // Fallback: Teacher might not be a homeroom teacher or data missing
                         _uiState.value = TeacherLiteracyUiState.Error("Anda belum memiliki kelas perwalian.")
                    }
                } else {
                    _uiState.value = TeacherLiteracyUiState.Error("Data guru tidak ditemukan untuk akun ini.")
                }
            } catch (e: Exception) {
                _uiState.value = TeacherLiteracyUiState.Error(e.message ?: "Gagal memuat data")
            }
        }
    }

    fun gradeLog(log: LiteracyLog, grade: String, feedback: String) {
        viewModelScope.launch {
            try {
                val teacherId = currentTeacherId ?: return@launch
                val updatedLog = log.copy(
                    grade = grade,
                    feedback = feedback,
                    teacherId = teacherId,
                    status = SubmissionStatus.GRADED,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateLiteracyLog(updatedLog)
                
                // Simulate Sync to Dashboard
                syncToDashboard(updatedLog)
                
                // The flow will automatically update the list, removing the graded item
            } catch (e: Exception) {
                _uiState.value = TeacherLiteracyUiState.Error("Gagal menyimpan nilai: ${e.message}")
            }
        }
    }

    private fun syncToDashboard(log: LiteracyLog) {
        Log.d("DashboardSync", "Syncing Literacy Log to Dashboard: ID=${log.id}, Status=${log.status}, Grade=${log.grade}")
        repository.syncLiteracyLogToFirebase(log)
    }
}

sealed class TeacherLiteracyUiState {
    object Idle : TeacherLiteracyUiState()
    object Loading : TeacherLiteracyUiState()
    object Success : TeacherLiteracyUiState()
    data class Error(val message: String) : TeacherLiteracyUiState()
}
