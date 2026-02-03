package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeacherNotificationUiState {
    object Idle : TeacherNotificationUiState()
    object Loading : TeacherNotificationUiState()
    data class Success(val notifications: List<Notification>) : TeacherNotificationUiState()
    data class Error(val message: String) : TeacherNotificationUiState()
}

class TeacherNotificationViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherNotificationUiState>(TeacherNotificationUiState.Idle)
    val uiState: StateFlow<TeacherNotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            _uiState.value = TeacherNotificationUiState.Loading
            try {
                repository.getNotificationsForUser(userId).collect { notifications ->
                    _uiState.value = TeacherNotificationUiState.Success(notifications)
                }
            } catch (e: Exception) {
                _uiState.value = TeacherNotificationUiState.Error(e.message ?: "Gagal memuat notifikasi.")
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }
}
