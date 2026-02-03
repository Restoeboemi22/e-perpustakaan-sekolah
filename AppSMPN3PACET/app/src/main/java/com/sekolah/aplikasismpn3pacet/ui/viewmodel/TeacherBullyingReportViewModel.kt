package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.sekolah.aplikasismpn3pacet.data.entity.BullyingReportWithReporter

sealed class TeacherBullyingReportUiState {
    object Idle : TeacherBullyingReportUiState()
    object Loading : TeacherBullyingReportUiState()
    data class Success(val reports: List<BullyingReportWithReporter>) : TeacherBullyingReportUiState()
    data class Error(val message: String) : TeacherBullyingReportUiState()
}

class TeacherBullyingReportViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherBullyingReportUiState>(TeacherBullyingReportUiState.Idle)
    val uiState: StateFlow<TeacherBullyingReportUiState> = _uiState.asStateFlow()

    init {
        loadAllReports()
    }

    fun loadAllReports() {
        viewModelScope.launch {
            _uiState.value = TeacherBullyingReportUiState.Loading
            try {
                // In real app: Filter by teacher's assigned class
                // For now, since "Wali Kelas" usually oversees one class (e.g., VII-A),
                // we should ideally filter reports related to students in that class.
                // However, current implementation fetches ALL reports.
                // The issue user reported: "Only Miko's report is visible".
                // This means other reports were NOT in the local Android database.
                // We just fixed this by seeding the other 2 reports in LoginViewModel.
                
                repository.getAllBullyingReports().collect { reports ->
                    _uiState.value = TeacherBullyingReportUiState.Success(reports)
                }
            } catch (e: Exception) {
                _uiState.value = TeacherBullyingReportUiState.Error(e.message ?: "Gagal memuat laporan.")
            }
        }
    }

    fun updateReportStatus(report: com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport, newStatus: com.sekolah.aplikasismpn3pacet.data.ReportStatus) {
        viewModelScope.launch {
            try {
                repository.updateBullyingReportStatus(report, newStatus)
                // loadAllReports() is not needed because Flow will automatically emit new data
            } catch (e: Exception) {
                _uiState.value = TeacherBullyingReportUiState.Error(e.message ?: "Gagal memperbarui status laporan.")
            }
        }
    }
}
