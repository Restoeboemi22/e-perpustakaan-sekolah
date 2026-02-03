package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.SchoolSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeacherScheduleViewModel(
    private val repository: SchoolRepository
) : ViewModel() {

    private val _schedules = MutableStateFlow<List<SchoolSchedule>>(emptyList())
    val schedules: StateFlow<List<SchoolSchedule>> = _schedules.asStateFlow()

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            repository.getSchoolSchedules().collect {
                _schedules.value = it
            }
        }
    }

    fun updateSchedule(schedule: SchoolSchedule) {
        viewModelScope.launch {
            repository.insertSchoolSchedule(schedule)
            repository.syncScheduleToFirebase(schedule)
        }
    }
}

class TeacherScheduleViewModelFactory(private val repository: SchoolRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
