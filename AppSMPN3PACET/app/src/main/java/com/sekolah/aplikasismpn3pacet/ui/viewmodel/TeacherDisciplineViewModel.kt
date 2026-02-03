package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.ClassEntity
import com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecord
import com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRule
import com.sekolah.aplikasismpn3pacet.data.entity.StudentWithUser
import com.sekolah.aplikasismpn3pacet.data.RecordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date

sealed class TeacherDisciplineUiState {
    object Loading : TeacherDisciplineUiState()
    data class Success(
        val classEntity: ClassEntity,
        val students: List<StudentWithUser>,
        val rules: List<DisciplineRule>
    ) : TeacherDisciplineUiState()
    data class Error(val message: String) : TeacherDisciplineUiState()
}

class TeacherDisciplineViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherDisciplineUiState>(TeacherDisciplineUiState.Loading)
    val uiState: StateFlow<TeacherDisciplineUiState> = _uiState.asStateFlow()

    private val _submissionStatus = MutableStateFlow<String?>(null)
    val submissionStatus: StateFlow<String?> = _submissionStatus.asStateFlow()

    private val _studentHistory = MutableStateFlow<List<com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecordWithRule>>(emptyList())
    val studentHistory: StateFlow<List<com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecordWithRule>> = _studentHistory.asStateFlow()

    fun loadData(userId: Long) {
        viewModelScope.launch {
            _uiState.value = TeacherDisciplineUiState.Loading
            try {
                val teacher = repository.getTeacherByUserId(userId)
                if (teacher == null) {
                    _uiState.value = TeacherDisciplineUiState.Error("Data guru tidak ditemukan")
                    return@launch
                }
                val classEntity = repository.getClassByHomeroomTeacherId(teacher.id)
                if (classEntity == null) {
                    _uiState.value = TeacherDisciplineUiState.Error("Anda belum memiliki kelas ampuan")
                    return@launch
                }

                // Combine flows: Students and Rules
                combine(
                    repository.getStudentsWithUserByClassId(classEntity.id),
                    repository.getAllDisciplineRules()
                ) { students, rules ->
                    TeacherDisciplineUiState.Success(
                        classEntity = classEntity,
                        students = students,
                        rules = rules
                    )
                }.collect { state ->
                    _uiState.value = state
                }

            } catch (e: Exception) {
                _uiState.value = TeacherDisciplineUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun loadStudentHistory(studentId: Long) {
        viewModelScope.launch {
            repository.getDisciplineRecordsByStudentId(studentId).collect { records ->
                _studentHistory.value = records
            }
        }
    }

    fun submitViolation(studentId: Long, ruleId: Long, points: Int, description: String?, recordedByUserId: Long) {
        viewModelScope.launch {
            try {
                val record = DisciplineRecord(
                    studentId = studentId,
                    ruleId = ruleId,
                    date = Date(),
                    points = points,
                    description = description,
                    evidence = null,
                    status = RecordStatus.APPROVED, // Teacher directly approves
                    recordedBy = recordedByUserId
                )
                repository.insertDisciplineRecord(record)
                _submissionStatus.value = "Pelanggaran berhasil dicatat"
            } catch (e: Exception) {
                _submissionStatus.value = "Gagal mencatat: ${e.message}"
            }
        }
    }

    fun clearSubmissionStatus() {
        _submissionStatus.value = null
    }
}
