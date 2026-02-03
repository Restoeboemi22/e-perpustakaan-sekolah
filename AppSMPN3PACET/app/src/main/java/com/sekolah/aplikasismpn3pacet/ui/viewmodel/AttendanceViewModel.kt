package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.AttendanceStatus
import com.sekolah.aplikasismpn3pacet.data.CheckInMethod
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.Attendance
import com.sekolah.aplikasismpn3pacet.data.entity.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

sealed class AttendanceUiState {
    object Loading : AttendanceUiState()
    data class Success(
        val student: Student,
        val history: List<Attendance>,
        val todayAttendance: Attendance?,
        val todaySchedule: String,
        val isHoliday: Boolean
    ) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
}

class AttendanceViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var currentStudentId: Long? = null

    fun loadData(userId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AttendanceUiState.Loading
                val student = repository.getStudentByUserId(userId)
                
                if (student == null) {
                    _uiState.value = AttendanceUiState.Error("Data siswa tidak ditemukan")
                    return@launch
                }

                currentStudentId = student.id

                // Load history
                repository.getAttendanceHistory(student.id).collect { history ->
                    // Check today's attendance
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)
                    
                    val tomorrow = Calendar.getInstance()
                    tomorrow.time = today.time
                    tomorrow.add(Calendar.DAY_OF_YEAR, 1)

                    val todayAttendance = repository.getAttendanceForDate(
                        student.id, 
                        today.timeInMillis, 
                        tomorrow.timeInMillis
                    )

                    // Determine schedule based on day of week
                    val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                    val dbSchedule = repository.getSchoolScheduleByDay(dayOfWeek)
                    
                    val schedule = if (dbSchedule != null && !dbSchedule.isHoliday) {
                        "${dbSchedule.startTime} - ${dbSchedule.endTime}"
                    } else {
                        "Libur"
                    }

                    _uiState.value = AttendanceUiState.Success(
                        student = student,
                        history = history,
                        todayAttendance = todayAttendance,
                        todaySchedule = schedule,
                        isHoliday = schedule == "Libur"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AttendanceUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun checkIn(userId: Long, lat: Double? = null, long: Double? = null) {
        viewModelScope.launch {
            val studentId = currentStudentId ?: return@launch
            
            try {
                // 0. Location Validation
                val schoolInfo = repository.getSchoolInformation().firstOrNull()
                if (schoolInfo?.latitude != null && schoolInfo.longitude != null && schoolInfo.radius != null) {
                    if (lat == null || long == null) {
                         _uiState.value = AttendanceUiState.Error("Lokasi tidak ditemukan. Harap aktifkan GPS.")
                         return@launch
                    }
                    
                    val distance = calculateDistance(lat, long, schoolInfo.latitude, schoolInfo.longitude)
                    if (distance > schoolInfo.radius) {
                         _uiState.value = AttendanceUiState.Error("Anda berada di luar jangkauan sekolah (${distance.toInt()}m). Maksimal ${schoolInfo.radius.toInt()}m.")
                         return@launch
                    }
                }

                val now = Date()
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val currentTimeMinutes = hour * 60 + minute
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // 1. Holiday & Weekend Validation
                val dbSchedule = repository.getSchoolScheduleByDay(dayOfWeek)
                
                if (dbSchedule == null || dbSchedule.isHoliday) {
                     _uiState.value = AttendanceUiState.Error("Hari ini libur. Tidak dapat melakukan absensi.")
                     return@launch
                }

                // 2. Time Window Validation
                // Allowed Check-in: 06:00 - End of School Day
                val checkInStart = 6 * 60 // 06:00 AM allow check-in start
                
                // Parse End Time from DB
                val endTimeParts = dbSchedule.endTime.split("[:.]".toRegex())
                val endHour = endTimeParts.getOrNull(0)?.toIntOrNull() ?: 13
                val endMinute = endTimeParts.getOrNull(1)?.toIntOrNull() ?: 0
                val endTime = endHour * 60 + endMinute

                if (currentTimeMinutes < checkInStart) {
                     _uiState.value = AttendanceUiState.Error("Belum waktunya absensi. Absensi dibuka pukul 06:00.")
                     return@launch
                }
                
                if (currentTimeMinutes > endTime) {
                     _uiState.value = AttendanceUiState.Error("Waktu absensi telah berakhir.")
                     return@launch
                }

                // 3. Status Determination
                // Parse Start Time from DB for Late Threshold
                val startTimeParts = dbSchedule.startTime.split("[:.]".toRegex())
                val startHour = startTimeParts.getOrNull(0)?.toIntOrNull() ?: 7
                val startMinute = startTimeParts.getOrNull(1)?.toIntOrNull() ?: 0
                val lateThreshold = startHour * 60 + startMinute
                
                val isLate = currentTimeMinutes > lateThreshold
                
                val status = if (isLate) {
                    AttendanceStatus.LATE
                } else {
                    AttendanceStatus.PRESENT
                }

                val attendance = Attendance(
                    studentId = studentId,
                    date = now,
                    status = status,
                    checkInTime = String.format("%02d:%02d", hour, calendar.get(Calendar.MINUTE)),
                    checkInMethod = CheckInMethod.MANUAL,
                    notes = "Check-in via Aplikasi",
                    proofDocument = null,
                    recordedBy = null // Self check-in
                )

                repository.insertAttendance(attendance)
                
                // Real-time Sync to Dashboard via Firebase
                repository.syncAttendanceToFirebase(attendance)
                
                // Flow will automatically update UI
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun syncAttendanceToDashboard(attendance: Attendance) {
        // In a real app, this would make an API call to the Next.js backend
        // Example: POST http://10.0.2.2:3000/api/attendance/sync
        /*
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.instance // Assuming Retrofit setup
                val log = AttendanceLogDto(
                    studentId = attendance.studentId,
                    date = attendance.date.time,
                    status = attendance.status.name,
                    checkInTime = attendance.checkInTime
                )
                apiService.syncAttendance(log)
            } catch (e: Exception) {
                // Queue for later sync
            }
        }
        */
        println("Syncing attendance to dashboard: $attendance")
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}
