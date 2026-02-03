package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.AttendanceStatus
import com.sekolah.aplikasismpn3pacet.data.CheckInMethod
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.Attendance
import com.sekolah.aplikasismpn3pacet.data.entity.ClassEntity
import com.sekolah.aplikasismpn3pacet.data.entity.StudentWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class StudentAttendanceSummary(
    val student: StudentWithUser,
    val present: Int,
    val sick: Int,
    val permission: Int,
    val alpha: Int
)

data class StudentDailyAttendance(
    val student: StudentWithUser,
    val status: AttendanceStatus?,
    val time: String?,
    val note: String?
)

sealed class TeacherAttendanceUiState {
    object Loading : TeacherAttendanceUiState()
    data class Success(
        val classEntity: ClassEntity,
        val students: List<StudentAttendanceSummary>,
        val dailyAttendance: List<StudentDailyAttendance>,
        val selectedMonth: Int,
        val selectedYear: Int,
        val selectedDate: Long
    ) : TeacherAttendanceUiState()
    data class Error(val message: String) : TeacherAttendanceUiState()
}

class TeacherAttendanceViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherAttendanceUiState>(TeacherAttendanceUiState.Loading)
    val uiState: StateFlow<TeacherAttendanceUiState> = _uiState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    private var currentUserId: Long? = null

    fun loadData(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                _uiState.value = TeacherAttendanceUiState.Loading
                val teacher = repository.getTeacherByUserId(userId)
                if (teacher == null) {
                    _uiState.value = TeacherAttendanceUiState.Error("Data guru tidak ditemukan")
                    return@launch
                }

                val classEntity = repository.getClassByHomeroomTeacherId(teacher.id)
                if (classEntity == null) {
                    _uiState.value = TeacherAttendanceUiState.Error("Anda belum memiliki kelas ampuan")
                    return@launch
                }

                combine(
                    repository.getStudentsWithUserByClassId(classEntity.id),
                    _selectedMonth,
                    _selectedYear,
                    _selectedDate
                ) { students, month, year, date ->
                    Quadruple(students, month, year, date)
                }.collect { (students, month, year, date) ->
                    // Calculate start and end of month
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startMonth = calendar.timeInMillis
                    
                    calendar.add(Calendar.MONTH, 1)
                    val endMonth = calendar.timeInMillis - 1

                    // Calculate start and end of day
                    calendar.timeInMillis = date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startDay = calendar.timeInMillis
                    val endDay = startDay + 86400000 - 1

                    val studentIds = students.map { it.student.id }
                    
                    if (studentIds.isEmpty()) {
                         _uiState.value = TeacherAttendanceUiState.Success(
                            classEntity = classEntity,
                            students = emptyList(),
                            dailyAttendance = emptyList(),
                            selectedMonth = month,
                            selectedYear = year,
                            selectedDate = date
                        )
                    } else {
                        try {
                             // Fetch Monthly Data
                             val monthAttendances = repository.getAttendanceForStudents(studentIds, startMonth, endMonth).first()
                             
                             val summaries = students.map { studentWithUser ->
                                val studentAttendances = monthAttendances.filter { it.studentId == studentWithUser.student.id }
                                StudentAttendanceSummary(
                                    student = studentWithUser,
                                    present = studentAttendances.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE },
                                    sick = studentAttendances.count { it.status == AttendanceStatus.SICK },
                                    permission = studentAttendances.count { it.status == AttendanceStatus.PERMIT },
                                    alpha = studentAttendances.count { it.status == AttendanceStatus.ABSENT }
                                )
                            }

                            // Fetch Daily Data
                            val dailyAttendances = repository.getAttendanceForStudents(studentIds, startDay, endDay).first()
                            val dailyList = students.map { studentWithUser ->
                                val att = dailyAttendances.find { it.studentId == studentWithUser.student.id }
                                StudentDailyAttendance(
                                    student = studentWithUser,
                                    status = att?.status,
                                    time = att?.checkInTime,
                                    note = att?.notes
                                )
                            }
                            
                            _uiState.value = TeacherAttendanceUiState.Success(
                                classEntity = classEntity,
                                students = summaries,
                                dailyAttendance = dailyList,
                                selectedMonth = month,
                                selectedYear = year,
                                selectedDate = date
                            )
                        } catch (e: Exception) {
                             val summaries = students.map { studentWithUser ->
                                StudentAttendanceSummary(studentWithUser, 0, 0, 0, 0)
                             }
                             val dailyList = students.map { s -> StudentDailyAttendance(s, null, null, null) }

                             _uiState.value = TeacherAttendanceUiState.Success(
                                classEntity = classEntity,
                                students = summaries,
                                dailyAttendance = dailyList,
                                selectedMonth = month,
                                selectedYear = year,
                                selectedDate = date
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = TeacherAttendanceUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun setMonth(month: Int) {
        _selectedMonth.value = month
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
    }

    fun setDate(date: Long) {
        _selectedDate.value = date
    }

    fun updateAttendance(studentId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            try {
                val date = _selectedDate.value
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDay = calendar.timeInMillis
                val endDay = startDay + 86400000

                val existing = repository.getAttendanceForDate(studentId, startDay, endDay)

                val attendance = if (existing != null) {
                    existing.copy(
                        status = status,
                        checkInMethod = CheckInMethod.MANUAL,
                        recordedBy = currentUserId,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    Attendance(
                        studentId = studentId,
                        date = Date(date),
                        status = status,
                        checkInTime = null,
                        checkInMethod = CheckInMethod.MANUAL,
                        notes = "Input Manual Guru",
                        proofDocument = null,
                        recordedBy = currentUserId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                repository.insertAttendance(attendance)
            } catch (e: Exception) {
                // Keep silent or show error? For now silent as flow will update UI
            }
        }
    }

    data class Quadruple<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)
}
