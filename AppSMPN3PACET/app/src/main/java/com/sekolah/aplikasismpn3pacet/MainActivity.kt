package com.sekolah.aplikasismpn3pacet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sekolah.aplikasismpn3pacet.data.UserRole
import com.sekolah.aplikasismpn3pacet.ui.screens.LoginScreen
import com.sekolah.aplikasismpn3pacet.ui.screens.student.StudentDashboardScreen
import com.sekolah.aplikasismpn3pacet.ui.screens.student.features.AttendanceScreen
import com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherDashboardScreen
import com.sekolah.aplikasismpn3pacet.ui.theme.AplikasiSMPN3PacetTheme
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.AttendanceViewModel
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginViewModel
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.SchoolViewModelFactory
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.StudentLiteracyViewModel
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherLiteracyViewModel
import com.sekolah.aplikasismpn3pacet.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions granted/rejected
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as SchoolApplication
        val repository = app.repository
        val userPreferences = app.userPreferences

        checkAndRequestPermissions()
        scheduleNotificationWorker()
        
        setContent {
            AplikasiSMPN3PacetTheme {
                val navController = rememberNavController()
                
                // Menggunakan ViewModel yang sama untuk berbagi state login antar layar
                val loginViewModel: LoginViewModel = viewModel(
                    factory = SchoolViewModelFactory(repository, userPreferences)
                )
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                viewModel = loginViewModel,
                                onLoginSuccess = {
                                    val state = loginViewModel.loginState.value
                                    if (state is com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success) {
                                        val route = if (state.user.role == UserRole.TEACHER) "teacher_dashboard" else "student_dashboard"
                                        navController.navigate(route) {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable("student_dashboard") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user
                            
                            if (user != null) {
                                StudentDashboardScreen(
                                    user = user,
                                    onNavigate = { route -> navController.navigate(route) },
                                    onLogout = {
                                        loginViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("student_dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                // Fallback jika state hilang
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    navController.navigate("login")
                                }
                            }
                        }

                        composable("attendance") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val attendanceViewModel: AttendanceViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                // Load data only once
                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    attendanceViewModel.loadData(user.id)
                                }

                                AttendanceScreen(
                                    viewModel = attendanceViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        
                        composable("teacher_dashboard") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user
                            
                            if (user != null && user.role == UserRole.TEACHER) {
                                TeacherDashboardScreen(
                                    user = user,
                                    onNavigate = { route -> navController.navigate(route) },
                                    onLogout = {
                                        loginViewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo("teacher_dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    navController.navigate("login")
                                }
                            }
                        }

                        // Teacher Features
                        composable("teacher_student_list") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherStudentListViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    viewModel.loadStudents(user.id)
                                }
                                com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherStudentListScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("teacher_attendance") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherAttendanceViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    viewModel.loadData(user.id)
                                }
                                com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherAttendanceScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("teacher_discipline") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherDisciplineViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherDisciplineScreen(
                                    viewModel = viewModel,
                                    userId = user.id,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("teacher_literacy") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: TeacherLiteracyViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherLiteracyScreen(
                                    teacherUser = user,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("teacher_bullying_reports") {
                            val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherBullyingReportViewModel = viewModel(
                                factory = SchoolViewModelFactory(repository)
                            )
                            com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherBullyingReportsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("teacher_notifications") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user
                            
                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.TeacherNotificationViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    viewModel.loadNotifications(user.id)
                                }
                                com.sekolah.aplikasismpn3pacet.ui.screens.teacher.TeacherNotificationScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        
                        // Student Features
                        composable("library") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user
                            
                            if (user != null) {
                                val studentViewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.StudentLiteracyViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.LibraryScreen(
                                    user = user,
                                    onBack = { navController.popBackStack() },
                                    viewModel = studentViewModel
                                )
                            } else {
                                // Fallback jika user null (jarang terjadi di flow ini)
                                navController.popBackStack()
                            }
                        }
                        composable("discipline") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.DisciplineViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                
                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.DisciplineScreen(
                                    viewModel = viewModel,
                                    userId = user.id,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("virtual_pet") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.VirtualPetViewModel = viewModel(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return com.sekolah.aplikasismpn3pacet.ui.viewmodel.VirtualPetViewModel(repository) as T
                                        }
                                    }
                                )
                                
                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    val student = repository.getStudentByUserId(user.id)
                                    if (student != null) {
                                        viewModel.loadPet(student.id)
                                    }
                                }

                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.VirtualPetScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("seven_habits") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.SevenHabitsViewModel = viewModel(
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return com.sekolah.aplikasismpn3pacet.ui.viewmodel.SevenHabitsViewModel(repository) as T
                                        }
                                    }
                                )

                                androidx.compose.runtime.LaunchedEffect(user.id) {
                                    val student = repository.getStudentByUserId(user.id)
                                    if (student != null) {
                                        viewModel.loadHabits(student.id)
                                    }
                                }

                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.SevenHabitsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("report_bullying") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.BullyingReportViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.BullyingReportScreen(
                                    viewModel = viewModel,
                                    userId = user.id,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("notifications") {
                            val loginState = loginViewModel.loginState.collectAsState().value
                            val user = (loginState as? com.sekolah.aplikasismpn3pacet.ui.viewmodel.LoginState.Success)?.user

                            if (user != null) {
                                val viewModel: com.sekolah.aplikasismpn3pacet.ui.viewmodel.NotificationViewModel = viewModel(
                                    factory = SchoolViewModelFactory(repository)
                                )
                                com.sekolah.aplikasismpn3pacet.ui.screens.student.features.NotificationScreen(
                                    viewModel = viewModel,
                                    userId = user.id,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Placeholders for other features
                        composable("schedule") { Text("Jadwal Pelajaran (Coming Soon)") }
                        composable("assignments") { Text("Tugas (Coming Soon)") }
                        composable("grades") { Text("Nilai (Coming Soon)") }
                        composable("profile") { Text("Profil Siswa (Coming Soon)") }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Check Location Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun scheduleNotificationWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TeacherTaskNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
