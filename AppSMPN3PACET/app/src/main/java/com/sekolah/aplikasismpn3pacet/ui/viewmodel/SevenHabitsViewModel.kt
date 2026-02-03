package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.HabitLog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class HabitData(
    val id: Int,
    val title: String,
    val description: String
)

data class SevenHabitsUiState(
    val habitLogs: List<HabitLog> = emptyList(),
    val weekStart: Long = 0L,
    val weekEnd: Long = 0L,
    val isLoading: Boolean = false,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-11
    val selectedWeek: Int = 1, // 1-4
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

class SevenHabitsViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SevenHabitsUiState(isLoading = true))
    val uiState: StateFlow<SevenHabitsUiState> = _uiState.asStateFlow()

    private var currentStudentId: Long? = null

    val habits = listOf(
        HabitData(1, "Bangun Pagi", "Bangun sebelum pukul 05.00 WIB"),
        HabitData(2, "Beribadah", "Melaksanakan ibadah sesuai agama dan kepercayaan"),
        HabitData(3, "Berolahraga", "Melakukan aktivitas fisik minimal 30 menit"),
        HabitData(4, "Makan Sehat dan Bergizi", "Mengonsumsi makanan bergizi seimbang (4 sehat 5 sempurna)"),
        HabitData(5, "Gemar Belajar", "Membaca buku, mengerjakan tugas, dan belajar mandiri"),
        HabitData(6, "Bermasyarakat", "Bersosialisasi, membantu orang lain, dan aktif di lingkungan"),
        HabitData(7, "Tidur Lebih Awal", "Tidur sebelum pukul 21.00 WIB")
    )

    fun loadHabits(studentId: Long) {
        currentStudentId = studentId
        updateDateRange()
    }

    fun updateMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
        updateDateRange()
    }

    fun updateYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        updateDateRange()
    }

    fun updateWeek(week: Int) {
        _uiState.update { it.copy(selectedWeek = week) }
        updateDateRange()
    }

    private fun updateDateRange() {
        val studentId = currentStudentId ?: return
        val state = _uiState.value
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, state.selectedYear)
        calendar.set(Calendar.MONTH, state.selectedMonth)
        
        // Logic for Week 1 - Week 4
        // We assume Week 1 starts on the 1st of the month, or the first Monday?
        // Let's stick to simple "Week X of the month" concept where we just grab 7 day chunks
        // But the table is Mon-Sun.
        // Better approach: Find the first Monday of the month? Or just align with Calendar weeks.
        
        // Let's use Week of Month logic aligned to Monday
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.WEEK_OF_MONTH, state.selectedWeek)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        // Reset time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Special case: if selecting Week 1 moves us to previous month (e.g. if 1st is Thursday),
        // we might want to ensure we stay in or near the month.
        // But Calendar logic usually handles this "Week of Month" correctly.
        // However, if Week 1 starts in prev month, it might be confusing.
        // Let's just trust Calendar for now as it's consistent.
        
        // Force set month again if it drifted? 
        // Actually, if we set WEEK_OF_MONTH, it might drift.
        // Let's try a simpler approach for "School Weeks":
        // Week 1: 1st - 7th
        // Week 2: 8th - 14th...
        // BUT the UI has Mon-Sun columns.
        // So we MUST return a Mon-Sun range.
        
        // Hybrid:
        // Set to 1st of month.
        // Find the Monday of that week. That is Week 1?
        // Or is Week 1 the first FULL week?
        // Let's assume standard Calendar behavior:
        // Calendar.WEEK_OF_MONTH 1 is the first partial week.
        
        val startOfWeek = calendar.timeInMillis
        
        // End of week (Sunday)
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = startOfWeek
        endCal.add(Calendar.DAY_OF_YEAR, 6)
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        val endOfWeek = endCal.timeInMillis

        viewModelScope.launch {
            repository.getHabitLogsByStudentAndDateRange(studentId, startOfWeek, endOfWeek).collect { logs ->
                _uiState.update { 
                    it.copy(
                        habitLogs = logs,
                        weekStart = startOfWeek,
                        weekEnd = endOfWeek,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleHabit(habitId: Int, dayOffset: Int, isChecked: Boolean) {
        val studentId = currentStudentId ?: return
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = _uiState.value.weekStart
        
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        val targetDate = calendar.timeInMillis

        viewModelScope.launch {
            val log = HabitLog(
                studentId = studentId,
                habitId = habitId,
                date = targetDate,
                isCompleted = isChecked
            )
            repository.insertHabitLog(log)
        }
    }
}