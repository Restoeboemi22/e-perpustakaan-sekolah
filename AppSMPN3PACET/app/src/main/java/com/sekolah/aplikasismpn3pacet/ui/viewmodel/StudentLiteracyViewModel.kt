package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLog
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyTask
import com.sekolah.aplikasismpn3pacet.data.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import android.util.Log

class StudentLiteracyViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LiteracyUiState>(LiteracyUiState.Idle)
    val uiState: StateFlow<LiteracyUiState> = _uiState

    private val _logs = MutableStateFlow<List<LiteracyLog>>(emptyList())
    val logs: StateFlow<List<LiteracyLog>> = _logs

    // Active Weekly Challenge
    val activeTask: Flow<LiteracyTask?> = repository.getActiveLiteracyTask()

    init {
        repository.listenToLiteracyTasks()
        repository.listenToLiteracyLogUpdates()
    }

    fun loadLogs(studentId: Long) {
        viewModelScope.launch {
            repository.getLiteracyLogsByStudent(studentId).collect {
                _logs.value = it
            }
        }
    }
    
    fun loadLogsByUserId(userId: Long) {
        viewModelScope.launch {
             val student = repository.getStudentByUserId(userId)
             if (student != null) {
                 loadLogs(student.id)
             }
        }
    }

    fun submitLog(studentId: Long, bookTitle: String, author: String, readingDuration: String, summary: String) {
        if (bookTitle.isBlank() || author.isBlank() || readingDuration.isBlank() || summary.isBlank()) {
            _uiState.value = LiteracyUiState.Error("Semua kolom harus diisi")
            return
        }

        viewModelScope.launch {
            _uiState.value = LiteracyUiState.Loading
            try {
                val log = LiteracyLog(
                    studentId = studentId,
                    bookTitle = bookTitle,
                    author = author,
                    readingDuration = readingDuration,
                    summary = summary,
                    submissionDate = Date(),
                    status = SubmissionStatus.PENDING
                )
                val newId = repository.insertLiteracyLog(log)
                val persistedLog = log.copy(id = newId)
                
                val student = repository.getStudentById(studentId)
                val user = if (student != null) repository.getUserById(student.userId) else null
                if (user != null) {
                    repository.syncLiteracyLogToFirebaseWithIdentityAsync(
                        persistedLog,
                        user.id,
                        user.username,
                        user.fullName,
                        user.nisNip
                    ).addOnSuccessListener {
                        _uiState.value = LiteracyUiState.Success("Laporan literasi berhasil dikirim")
                    }.addOnFailureListener { e ->
                        _uiState.value = LiteracyUiState.Error(e.message ?: "Gagal sinkron ke Firebase")
                    }
                } else {
                    repository.syncLiteracyLogToFirebase(persistedLog)
                    _uiState.value = LiteracyUiState.Success("Laporan literasi berhasil dikirim")
                }
                
                // Simulate Sync to Dashboard
                syncToDashboard(persistedLog)
                
            } catch (e: Exception) {
                _uiState.value = LiteracyUiState.Error(e.message ?: "Gagal mengirim laporan")
            }
        }
    }
    
    private fun syncToDashboard(log: LiteracyLog) {
        Log.d("DashboardSync", "Syncing New Literacy Log to Dashboard: Title=${log.bookTitle}, Author=${log.author}, Duration=${log.readingDuration}, Summary=${log.summary}, Status=${log.status}")
        // In a real app, this would be an API call to the web dashboard backend
    }
    
    fun submitLogByUserId(userId: Long, bookTitle: String, author: String, readingDuration: String, summary: String) {
         if (bookTitle.isBlank() || author.isBlank() || readingDuration.isBlank() || summary.isBlank()) {
             _uiState.value = LiteracyUiState.Error("Semua kolom harus diisi")
             return
         }
         
         viewModelScope.launch {
             _uiState.value = LiteracyUiState.Loading
             try {
                 val student = repository.getStudentByUserId(userId)
                 val user = repository.getUserById(userId)
                 if (student == null || user == null) {
                     _uiState.value = LiteracyUiState.Error("Data siswa tidak ditemukan")
                     return@launch
                 }
                 
                 val log = LiteracyLog(
                     studentId = student.id,
                     bookTitle = bookTitle,
                     author = author,
                     readingDuration = readingDuration,
                     summary = summary,
                     submissionDate = Date(),
                     status = SubmissionStatus.PENDING
                 )
                 val newId = repository.insertLiteracyLog(log)
                 val persistedLog = log.copy(id = newId)
                 
                repository.syncLiteracyLogToFirebaseWithIdentityAsync(
                    persistedLog,
                    userId,
                    user.username,
                    user.fullName,
                    user.nisNip
                ).addOnSuccessListener {
                    _uiState.value = LiteracyUiState.Success("Laporan literasi berhasil dikirim")
                }.addOnFailureListener { e ->
                    _uiState.value = LiteracyUiState.Error(e.message ?: "Gagal sinkron ke Firebase")
                }
             } catch (e: Exception) {
                 _uiState.value = LiteracyUiState.Error(e.message ?: "Gagal mengirim laporan")
             }
         }
    }
    
    fun resetState() {
        _uiState.value = LiteracyUiState.Idle
    }
}

sealed class LiteracyUiState {
    object Idle : LiteracyUiState()
    object Loading : LiteracyUiState()
    data class Success(val message: String) : LiteracyUiState()
    data class Error(val message: String) : LiteracyUiState()
}
