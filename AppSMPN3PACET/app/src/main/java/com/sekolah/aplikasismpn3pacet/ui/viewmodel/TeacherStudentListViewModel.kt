package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.ClassEntity
import com.sekolah.aplikasismpn3pacet.data.entity.StudentWithUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeacherStudentListUiState {
    object Loading : TeacherStudentListUiState()
    data class Success(
        val classEntity: ClassEntity,
        val students: List<StudentWithUser>
    ) : TeacherStudentListUiState()
    data class Error(val message: String) : TeacherStudentListUiState()
}

class TeacherStudentListViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherStudentListUiState>(TeacherStudentListUiState.Loading)
    val uiState: StateFlow<TeacherStudentListUiState> = _uiState.asStateFlow()

    fun loadStudents(userId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = TeacherStudentListUiState.Loading
                val teacher = repository.getTeacherByUserId(userId)
                if (teacher == null) {
                    _uiState.value = TeacherStudentListUiState.Error("Data guru tidak ditemukan")
                    return@launch
                }

                val classEntity = repository.getClassByHomeroomTeacherId(teacher.id)
                if (classEntity == null) {
                    _uiState.value = TeacherStudentListUiState.Error("Anda belum memiliki kelas ampuan")
                    return@launch
                }

                repository.getStudentsWithUserByClassId(classEntity.id).collect { students ->
                    _uiState.value = TeacherStudentListUiState.Success(
                        classEntity = classEntity,
                        students = students
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TeacherStudentListUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
