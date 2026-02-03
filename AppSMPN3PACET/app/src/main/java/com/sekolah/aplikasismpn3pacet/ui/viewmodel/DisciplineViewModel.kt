package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.RuleCategory
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecordWithRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DisciplineUiState {
    object Loading : DisciplineUiState()
    data class Success(
        val records: List<DisciplineRecordWithRule>,
        val violationPoints: Int,
        val achievementPoints: Int
    ) : DisciplineUiState()
    data class Error(val message: String) : DisciplineUiState()
}

class DisciplineViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<DisciplineUiState>(DisciplineUiState.Loading)
    val uiState: StateFlow<DisciplineUiState> = _uiState.asStateFlow()

    fun loadDisciplineData(userId: Long) {
        viewModelScope.launch {
            _uiState.value = DisciplineUiState.Loading
            try {
                // 1. Get Student by User ID
                val student = repository.getStudentByUserId(userId)
                if (student == null) {
                    _uiState.value = DisciplineUiState.Error("Data siswa tidak ditemukan.")
                    return@launch
                }

                // 2. Observe Discipline Records
                repository.getDisciplineRecordsByStudentId(student.id).collect { records ->
                    
                    val violationPoints = records
                        .filter { it.rule.category == RuleCategory.VIOLATION }
                        .sumOf { it.record.points }
                        
                    val achievementPoints = records
                        .filter { it.rule.category == RuleCategory.ACHIEVEMENT }
                        .sumOf { it.record.points }

                    // Simulate Sync to Dashboard
                    syncDisciplineToDashboard(records, student.id)

                    _uiState.value = DisciplineUiState.Success(records, violationPoints, achievementPoints)
                }
            } catch (e: Exception) {
                _uiState.value = DisciplineUiState.Error(e.message ?: "Terjadi kesalahan saat memuat data kedisiplinan")
            }
        }
    }

    private fun syncDisciplineToDashboard(records: List<DisciplineRecordWithRule>, studentId: Long) {
        // In a real app, this would make an API call to the Next.js backend
        // Example: POST http://10.0.2.2:3000/api/discipline/sync
        println("Syncing ${records.size} discipline records for student $studentId to Dashboard...")
        records.forEach { item ->
            println("Record: ${item.rule.ruleName} - ${item.record.points} pts")
        }
    }
}
