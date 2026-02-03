package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.IncidentType
import com.sekolah.aplikasismpn3pacet.data.ReportPriority
import com.sekolah.aplikasismpn3pacet.data.ReportStatus
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport
import com.sekolah.aplikasismpn3pacet.data.entity.Notification
import com.sekolah.aplikasismpn3pacet.data.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class BullyingReportUiState {
    object Idle : BullyingReportUiState()
    object Loading : BullyingReportUiState()
    data class Success(val reports: List<BullyingReport>) : BullyingReportUiState()
    data class Error(val message: String) : BullyingReportUiState()
    object SubmitSuccess : BullyingReportUiState()
}

class BullyingReportViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<BullyingReportUiState>(BullyingReportUiState.Idle)
    val uiState: StateFlow<BullyingReportUiState> = _uiState.asStateFlow()

    fun loadReports(userId: Long) {
        viewModelScope.launch {
            _uiState.value = BullyingReportUiState.Loading
            try {
                val student = repository.getStudentByUserId(userId)
                if (student == null) {
                    _uiState.value = BullyingReportUiState.Error("Data siswa tidak ditemukan.")
                    return@launch
                }

                repository.getBullyingReportsByReporterId(student.id).collect { reports ->
                    _uiState.value = BullyingReportUiState.Success(reports)
                }
            } catch (e: Exception) {
                _uiState.value = BullyingReportUiState.Error(e.message ?: "Gagal memuat riwayat laporan.")
            }
        }
    }

    fun submitReport(
        userId: Long,
        description: String,
        incidentType: IncidentType,
        isAnonymous: Boolean,
        incidentDate: Date = Date()
    ) {
        viewModelScope.launch {
            _uiState.value = BullyingReportUiState.Loading
            try {
                val student = repository.getStudentByUserId(userId)
                val reporterId = if (student != null) student.id else null

                // If strictly anonymous, we might want to hide reporterId, 
                // but for now let's keep it linked but flagged as anonymous 
                // so the school can still trace if needed for safety, 
                // but display as "Anonymous" in UI.
                
                if (reporterId == null && !isAnonymous) {
                     _uiState.value = BullyingReportUiState.Error("Data pelapor tidak valid.")
                    return@launch
                }

                val report = BullyingReport(
                    reporterId = reporterId,
                    isAnonymous = isAnonymous,
                    victimId = null, // Optional for now
                    perpetratorId = null, // Optional for now
                    incidentDate = incidentDate,
                    incidentLocation = "Lingkungan Sekolah", // Default or add input
                    incidentType = incidentType,
                    description = description,
                    evidence = null,
                    status = ReportStatus.PENDING,
                    priority = ReportPriority.MEDIUM, // Default
                    assignedTo = null,
                    resolutionNotes = null,
                    resolvedAt = null
                )

                repository.insertBullyingReport(report)
                
                // Simulate Sync
                syncReportToDashboard(report)
                
                _uiState.value = BullyingReportUiState.SubmitSuccess
            } catch (e: Exception) {
                _uiState.value = BullyingReportUiState.Error(e.message ?: "Gagal mengirim laporan.")
            }
        }
    }
    
    private fun syncReportToDashboard(report: BullyingReport) {
        // Sync to Firebase
        try {
            repository.syncBullyingReportToFirebase(report)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun resetState() {
        _uiState.value = BullyingReportUiState.Idle
    }
}
