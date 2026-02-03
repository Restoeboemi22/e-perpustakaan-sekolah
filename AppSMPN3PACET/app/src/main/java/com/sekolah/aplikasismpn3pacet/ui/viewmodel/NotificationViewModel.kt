package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.Notification
import com.sekolah.aplikasismpn3pacet.data.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NotificationUiState {
    object Loading : NotificationUiState()
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int
    ) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

class NotificationViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            try {
                repository.getNotificationsForUser(userId).collect { notifications ->
                    // Also collect unread count
                    repository.getUnreadNotificationCount(userId).collect { count ->
                        _uiState.value = NotificationUiState.Success(notifications, count)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NotificationUiState.Error(e.message ?: "Gagal memuat notifikasi")
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.markNotificationAsRead(notificationId)
            } catch (e: Exception) {
                // Handle error silently or show snackbar
            }
        }
    }
    
    // For testing/demo purposes
    fun createTestNotification(userId: Long) {
        viewModelScope.launch {
            val notification = Notification(
                userId = userId,
                title = "Pengingat Jadwal",
                message = "Jangan lupa besok ada ulangan Matematika bab Aljabar.",
                type = NotificationType.INFO,
                relatedFeature = "SCHEDULE"
            )
            repository.insertNotification(notification)
        }
    }
}
