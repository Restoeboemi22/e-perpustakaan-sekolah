package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.User
import com.sekolah.aplikasismpn3pacet.data.entity.Student
import com.sekolah.aplikasismpn3pacet.data.UserRole
import com.sekolah.aplikasismpn3pacet.data.RuleCategory
import com.sekolah.aplikasismpn3pacet.data.RuleSeverity
import com.sekolah.aplikasismpn3pacet.data.RecordStatus
import com.sekolah.aplikasismpn3pacet.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import com.sekolah.aplikasismpn3pacet.data.UserPreferences

class LoginViewModel(
    private val repository: SchoolRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    init {
        checkAndSeedData()
        checkSession()
        repository.startStudentSync()
        repository.startScheduleSync()
        repository.startAttendanceSync()
        repository.listenToLiteracyTasks()
        repository.listenToLiteracyLogUpdates()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val savedUserId = userPreferences.getUserSession()
            if (savedUserId != null) {
                _loginState.value = LoginState.Loading
                val user = repository.getUserById(savedUserId)
                if (user != null && user.isActive) {
                    _loginState.value = LoginState.Success(user)
                } else {
                    userPreferences.clearUserSession()
                    _loginState.value = LoginState.Idle
                }
            }
        }
    }

    private fun checkAndSeedData() {
        viewModelScope.launch {
            // Seed Admin (Legacy/Backend purpose)
            val admin = repository.getUserByUsername("admin")
            if (admin == null) {
                seedAdmin()
            }
            
            // Seed Teacher (For Mobile App Testing)
            val teacher = repository.getUserByUsername("Budi Santoso, S.Pd")
            if (teacher == null) {
                seedTeacherInternal()
            }

            // Seed Student (For Mobile App Testing)
            // val student = repository.getUserByUsername("siswa")
            // if (student == null) {
            //    seedStudentInternal()
            // }
            
            // Ensure Teacher and Class Data hierarchy exists for Budi Santoso (For "Data Siswa" feature)
            ensureTeacherAndClassData()

            // Seed Dashboard Students (Matches Web Dashboard)
            seedDashboardStudents()
            
            // Seed Dashboard Attendance (Matches Web Dashboard - 24 Alpha in Jan 2026)
            seedDashboardAttendance()

            // Seed Discipline Data
            seedDisciplineData()

            // Seed Bullying Reports (Ensure all 3 demo reports exist)
            seedBullyingReports()

            // Seed Literacy Data (Matches Web Dashboard)
            seedLiteracyLogs()
            
            // Seed Active Literacy Task (Weekly Challenge)
            // Removed hardcoded seed to allow Firebase sync
            // seedLiteracyTask()

            // Seed School Information (Location & Radius)
            seedSchoolInformation()

            // Seed School Schedules (Default values)
            seedSchoolSchedules()
            fixLegacySiswaClassAssignment()
        }
    }

    private suspend fun seedSchoolSchedules() {
        val existingSchedule = repository.getSchoolScheduleByDay(java.util.Calendar.MONDAY)
        if (existingSchedule == null) {
            val schedules = listOf(
                SchoolSchedule(dayOfWeek = java.util.Calendar.MONDAY, dayName = "Senin", startTime = "06:30", endTime = "13:00"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.TUESDAY, dayName = "Selasa", startTime = "06:30", endTime = "13:00"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.WEDNESDAY, dayName = "Rabu", startTime = "06:30", endTime = "13:00"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.THURSDAY, dayName = "Kamis", startTime = "06:30", endTime = "13:00"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.FRIDAY, dayName = "Jumat", startTime = "06:30", endTime = "10:40"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.SATURDAY, dayName = "Sabtu", startTime = "06:30", endTime = "11:20"),
                SchoolSchedule(dayOfWeek = java.util.Calendar.SUNDAY, dayName = "Minggu", startTime = "00:00", endTime = "00:00", isHoliday = true)
            )
            schedules.forEach { repository.insertSchoolSchedule(it) }
        }
    }

    private suspend fun fixLegacySiswaClassAssignment() {
        val teacherUser = repository.getUserByUsername("Budi Santoso, S.Pd") ?: return
        val teacherEntity = repository.getTeacherByUserId(teacherUser.id) ?: return
        val classEntity = repository.getClassByHomeroomTeacherId(teacherEntity.id) ?: return
        val siswaUser = repository.getUserByUsername("siswa") ?: return
        val siswaStudent = repository.getStudentByUserId(siswaUser.id) ?: return
        if (siswaStudent.classId != classEntity.id) {
            repository.insertStudent(siswaStudent.copy(classId = classEntity.id))
        }
    }

    private suspend fun seedSchoolInformation() {
        val existingInfo = repository.getSchoolInformation().firstOrNull()
        if (existingInfo == null) {
            val info = SchoolInformation(
                schoolName = "SMPN 3 Pacet",
                npsn = "20502727",
                address = "Jl. Pacet-Mojosari Km. 3",
                phone = "(0321) 123456",
                email = "info@smpn3pacet.sch.id",
                website = "https://smpn3pacet.sch.id",
                latitude = -7.6698, // Example coordinate
                longitude = 112.5398, // Example coordinate
                radius = 100.0 // 100 meters
            )
            repository.insertSchoolInformation(info)
        }
    }



    private suspend fun seedLiteracyLogs() {
        // Teacher for grading (Budi Santoso)
        val teacherUser = repository.getUserByUsername("Budi Santoso, S.Pd")
        val teacherId = teacherUser?.id // Can be null if not seeded yet, but it should be

        // Date Setup: 25 Jan 2026
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2026, java.util.Calendar.JANUARY, 25)
        val dateJan25 = calendar.time

        // 1. Seed for Miko (User Logged In)
        val mikoUser = repository.getUserByUsername("miko")
        if (mikoUser != null) {
            val student = repository.getStudentByUserId(mikoUser.id)
            if (student != null) {
                val existingLogs = repository.getLiteracyLogsByStudent(student.id).firstOrNull() ?: emptyList()
                
                // Cleanup unwanted logs or outdated entries
                existingLogs.forEach { log ->
                    // Remove "Bumi Manusia" (Extra data)
                    if (log.bookTitle == "Bumi Manusia") {
                        repository.deleteLiteracyLog(log)
                    } 
                    // Remove existing "Laskar Pelangi" to ensure we have the correct date/state
                    else if (log.bookTitle == "Laskar Pelangi") {
                        repository.deleteLiteracyLog(log)
                    }
                }

                // Insert the Correct Laskar Pelangi Log (Matches Dashboard)
                repository.insertLiteracyLog(
                    LiteracyLog(
                        studentId = student.id,
                        bookTitle = "Laskar Pelangi",
                        author = "Andrea Hirata",
                        readingDuration = "> 1 Jam",
                        summary = "Novel ini bercerita tentang kehidupan 10 anak di Belitung yang berjuang untuk sekolah di tengah keterbatasan.",
                        submissionDate = dateJan25,
                        status = com.sekolah.aplikasismpn3pacet.data.SubmissionStatus.GRADED,
                        grade = "92",
                        feedback = "Ringkasan yang sangat inspiratif, Miko! Teruslah membaca buku-buku bermutu.",
                        teacherId = teacherId
                    )
                )
            }
        }

        // 2. Cleanup for Siti Hidayat (Remove all logs to match Dashboard view)
        val sitiUser = repository.getUserByUsername("siti.hidayat")
        if (sitiUser != null) {
            val student = repository.getStudentByUserId(sitiUser.id)
            if (student != null) {
                val existingLogs = repository.getLiteracyLogsByStudent(student.id).firstOrNull() ?: emptyList()
                existingLogs.forEach { log ->
                    repository.deleteLiteracyLog(log)
                }
            }
        }
    }

    private suspend fun seedDashboardAttendance() {
        val teacherUser = repository.getUserByUsername("Budi Santoso, S.Pd") ?: return
        val teacherEntity = repository.getTeacherByUserId(teacherUser.id) ?: return
        val classEntity = repository.getClassByHomeroomTeacherId(teacherEntity.id) ?: return
        
        // Get students from flow (take first emission)
        val students = repository.getStudentsWithUserByClassId(classEntity.id).firstOrNull() ?: return
        
        val calendar = java.util.Calendar.getInstance()
        
        students.forEach { studentWithUser ->
             val studentId = studentWithUser.student.id
             
             // Generate 24 days of Alpha for January 2026 (Jan 2 - Jan 25)
             for (day in 2..25) {
                 calendar.set(2026, java.util.Calendar.JANUARY, day, 7, 0, 0)
                 calendar.set(java.util.Calendar.MILLISECOND, 0)
                 val date = calendar.time
                 
                 // Define start/end of day for query
                 val startCal = calendar.clone() as java.util.Calendar
                 startCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                 startCal.set(java.util.Calendar.MINUTE, 0)
                 startCal.set(java.util.Calendar.SECOND, 0)
                 val startOfDay = startCal.timeInMillis
                 
                 val endCal = calendar.clone() as java.util.Calendar
                 endCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                 endCal.set(java.util.Calendar.MINUTE, 59)
                 endCal.set(java.util.Calendar.SECOND, 59)
                 val endOfDay = endCal.timeInMillis
                 
                 val existing = repository.getAttendanceForDate(studentId, startOfDay, endOfDay)
                 if (existing == null) {
                     val attendance = Attendance(
                         studentId = studentId,
                         date = date,
                         status = com.sekolah.aplikasismpn3pacet.data.AttendanceStatus.ABSENT,
                         checkInTime = null,
                         checkInMethod = com.sekolah.aplikasismpn3pacet.data.CheckInMethod.MANUAL,
                         notes = "Alpha (Dashboard Sync)",
                         proofDocument = null,
                         recordedBy = teacherUser.id
                     )
                     repository.insertAttendance(attendance)
                 }
             }
        }
    }

    private suspend fun seedDashboardStudents() {
        val teacherUser = repository.getUserByUsername("Budi Santoso, S.Pd") ?: return
        val teacherEntity = repository.getTeacherByUserId(teacherUser.id) ?: return
        val classEntity = repository.getClassByHomeroomTeacherId(teacherEntity.id) ?: return

        // Students Order MUST match Dashboard Mock Data ID to ensure consistency
        // Only "Miko" remains in the dashboard
        val students = listOf(
            Triple("Miko", "12345678", com.sekolah.aplikasismpn3pacet.data.Gender.MALE),
            Triple("Wahyu", "00112233", com.sekolah.aplikasismpn3pacet.data.Gender.MALE)
        )
        
        // 1. Cleanup: Remove ALL students that are NOT in the new list (Strict Sync)
        // MODIFIED: Disabled aggressive cleanup to allow other users to login
        /* 
        val allStudents = repository.getAllStudents().firstOrNull() ?: emptyList()
        val newStudentNames = students.map { it.first }
        
        allStudents.forEach { student ->
             val user = repository.getUserById(student.userId)
             if (user != null) {
                 if (user.fullName !in newStudentNames) {
                     // Cleanup dependent data FIRST to avoid Foreign Key Constraint Violation
                     repository.deleteAttendancesByStudentId(student.id)
                     repository.deleteDisciplineRecordsByStudentId(student.id)
                     repository.deleteLiteracyLogsByStudentId(student.id)
                     repository.deleteBullyingReportsByStudentId(student.id)
                     // Pet Quests and Pet (Cascade works for Quests if Pet is deleted, but Pet references Student)
                     // VirtualPet has ForeignKey to Student with CASCADE? Let's assume no or manual needed.
                     // Actually VirtualPet entity has onDelete = ForeignKey.CASCADE for student_id
                     
                     // Delete student record
                     repository.deleteStudent(student)
                     
                     // Delete user account
                     repository.deleteNotificationsByUserId(user.id)
                     repository.deleteUser(user)
                 }
             } else {
                 // Orphan student record - Cleanup dependencies first
                 repository.deleteAttendancesByStudentId(student.id)
                 repository.deleteDisciplineRecordsByStudentId(student.id)
                 repository.deleteLiteracyLogsByStudentId(student.id)
                 repository.deleteBullyingReportsByStudentId(student.id)
                 repository.deleteStudent(student)
             }
        }
        */

        // 2. Insert/Update: Ensure new list exists
        students.forEach { (name, nisn, gender) ->
            // Use NISN as username for consistency, or name.lowercase().replace(" ", ".")
            // Dashboard screenshot shows email like siti.hidayat@siswa..., so let's use that format for username/email
            val username = name.lowercase().replace(" ", ".")
            val email = if (username == "miko") "miko@school.com" else "$username@siswa.spentgapa.sch.id"
            
            var user = repository.getUserByUsername(username)
            if (user == null) {
                val newUser = User(
                    username = username,
                    password = nisn, // Default password is NISN
                    email = email,
                    fullName = name,
                    role = UserRole.STUDENT,
                    nisNip = nisn,
                    phone = "08123456789", // Dummy
                    address = "Alamat Siswa",
                    gender = gender,
                    birthDate = null,
                    profilePicture = null
                )
                val userId = repository.insertUser(newUser)
                user = newUser.copy(id = userId)
            }

            var student = repository.getStudentByUserId(user.id)
            if (student == null) {
                val newStudent = Student(
                    userId = user.id,
                    classId = classEntity.id,
                    parentName = "Orang Tua $name",
                    parentPhone = "08123456789",
                    parentEmail = null,
                    admissionYear = 2024
                )
                repository.insertStudent(newStudent)
            } else if (student.classId != classEntity.id) {
                // Ensure student is in the correct class
                repository.insertStudent(student.copy(classId = classEntity.id))
            }
        }
    }

    private suspend fun ensureTeacherAndClassData() {
        // 1. Get Teacher User
        val teacherUser = repository.getUserByUsername("Budi Santoso, S.Pd") ?: return

        // 2. Ensure Teacher Entity exists
        var teacherEntity = repository.getTeacherByUserId(teacherUser.id)
        if (teacherEntity == null) {
            val newTeacher = Teacher(
                userId = teacherUser.id,
                subject = "Wali Kelas"
            )
            val teacherId = repository.insertTeacher(newTeacher)
            // Re-fetch to get the object with ID, or construct it
            teacherEntity = newTeacher.copy(id = teacherId)
        }

        // 3. Ensure Class "VII-A" exists and is assigned to this teacher
        var classEntity = repository.getClassByHomeroomTeacherId(teacherEntity.id)
        if (classEntity == null) {
             // Create new class for this teacher
             val newClass = ClassEntity(
                 className = "VII-A",
                 gradeLevel = 7,
                 homeroomTeacherId = teacherEntity.id,
                 academicYear = "2024/2025"
             )
             val classId = repository.insertClass(newClass)
             classEntity = newClass.copy(id = classId)
        }
        
        // Note: Individual student assignment is now handled by seedDashboardStudents()
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val user = repository.getUserByUsername(username)
            if (user != null && user.password == password) { // In real app, hash check
                if (user.role == UserRole.STUDENT || user.role == UserRole.TEACHER) {
                    
                    // Server-Side Device Binding Check (Student Only)
                    if (user.role == UserRole.STUDENT) {
                        try {
                            val deviceId = userPreferences.getDeviceId()
                            val (isAllowed, errorMessage) = repository.checkDeviceBinding(user.username, deviceId)
                            
                            if (!isAllowed) {
                                _loginState.value = LoginState.Error(errorMessage ?: "Login ditolak oleh server.")
                                return@launch
                            }
                        } catch (e: Exception) {
                            // If check fails (e.g. offline), what to do?
                            // For now, let's log and maybe allow if it's just network error?
                            // But repository returns Pair(false, msg) on error, so it's handled above.
                            // If userPreferences fails (unlikely), handle here.
                            e.printStackTrace()
                        }
                    }

                    userPreferences.saveUserSession(user.id)
                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error("Aplikasi ini khusus Siswa & Guru. Admin silakan akses Web Dashboard.")
                }
            } else {
                _loginState.value = LoginState.Error("Username atau password salah")
            }
        }
    }

    fun logout() {
        userPreferences.clearUserSession()
        _loginState.value = LoginState.Idle
    }

    // Temporary helper to seed admin
    suspend fun seedAdmin() {
        val admin = User(
            username = "admin",
            password = "admin123", // Plain text for demo
            email = "admin@school.com",
            fullName = "Administrator",
            role = UserRole.ADMIN,
            nisNip = "ADMIN001",
            phone = "08123456789",
            address = "School Office",
            gender = null,
            birthDate = null,
            profilePicture = null
        )
        repository.insertUser(admin)
    }

    // Public wrapper for UI
    fun seedTeacher() {
        viewModelScope.launch {
            seedTeacherInternal()
        }
    }

    // Temporary helper to seed teacher
    private suspend fun seedTeacherInternal() {
        val teacher = User(
            username = "Budi Santoso, S.Pd",
            password = "1234567890123456", // NUPTK as Password
            email = "guru@school.com",
            fullName = "Budi Santoso, S.Pd",
            role = UserRole.TEACHER,
            nisNip = "1234567890123456", // NUPTK
            phone = "081234567890",
            address = "Jl. Guru No. 1",
            gender = com.sekolah.aplikasismpn3pacet.data.Gender.MALE,
            birthDate = null,
            profilePicture = null
        )
        repository.insertUser(teacher)
    }

    // Public wrapper for UI
    fun seedStudent() {
        viewModelScope.launch {
            seedStudentInternal()
            seedMiko()
        }
    }

    // Temporary helper to seed student
    private suspend fun seedStudentInternal() {
        val student = User(
            username = "siswa",
            password = "siswa123",
            email = "siswa@school.com",
            fullName = "Budi Santoso",
            role = UserRole.STUDENT,
            nisNip = "2024001",
            phone = "08987654321",
            address = "Jl. Pendidikan No. 1",
            gender = com.sekolah.aplikasismpn3pacet.data.Gender.MALE,
            birthDate = null,
            profilePicture = null
        )
        val userId = repository.insertUser(student)
        
        // Seed Student Record linked to User
        val studentRecord = Student(
            userId = userId,
            classId = null, // Can be updated later
            parentName = "Budi Parent",
            parentPhone = "081234567890",
            parentEmail = "parent@school.com",
            admissionYear = 2024
        )
        repository.insertStudent(studentRecord)
    }

    // Seed Miko (Specific User Request)
    suspend fun seedMiko() {
        val miko = User(
            username = "miko",
            password = "12345678", // Sesuai NISN di dashboard
            email = "miko@school.com",
            fullName = "Miko",
            role = UserRole.STUDENT,
            nisNip = "12345678",
            phone = "08123456789",
            address = "Jl. Siswa No. 1",
            gender = com.sekolah.aplikasismpn3pacet.data.Gender.MALE,
            birthDate = null,
            profilePicture = null
        )
        val userId = repository.insertUser(miko)
        
        val studentRecord = Student(
            userId = userId,
            classId = null,
            parentName = "Orang Tua Miko",
            parentPhone = "081234567891",
            parentEmail = "parent.miko@school.com",
            admissionYear = 2024
        )
        repository.insertStudent(studentRecord)
    }


    private suspend fun seedBullyingReports() {
        val existingReports = repository.getAllBullyingReports().firstOrNull() ?: emptyList()

        // 1. Miko's Report (Anonim)
        val mikoReportDesc = "Melihat kakak kelas mengejek siswa lain di kantin saat istirahat."
        if (existingReports.none { it.report.description == mikoReportDesc }) {
            // Try to find Miko to link reporterId
            val mikoUser = repository.getUserByUsername("miko")
            var reporterId: Long? = null
            if (mikoUser != null) {
                val mikoStudent = repository.getStudentByUserId(mikoUser.id)
                if (mikoStudent != null) {
                    reporterId = mikoStudent.id
                }
            }

            // If Miko exists or not, we seed.
            repository.insertBullyingReport(
                BullyingReport(
                    reporterId = reporterId,
                    isAnonymous = true,
                    victimId = null,
                    perpetratorId = null,
                    incidentDate = java.util.Date(System.currentTimeMillis() - 86400000 * 2), // 2 days ago
                    incidentLocation = "Kantin",
                    incidentType = com.sekolah.aplikasismpn3pacet.data.IncidentType.VERBAL,
                    description = mikoReportDesc,
                    evidence = null,
                    status = com.sekolah.aplikasismpn3pacet.data.ReportStatus.INVESTIGATING,
                    priority = com.sekolah.aplikasismpn3pacet.data.ReportPriority.MEDIUM,
                    assignedTo = null,
                    resolutionNotes = null,
                    resolvedAt = null
                )
            )
        }

        // 2. Budi's Report (Verbal)
        val budiDesc = "Doni mengejek Ani dengan sebutan nama hewan di depan teman-teman lain saat jam istirahat. (Pelapor: Budi Santoso)"
        if (existingReports.none { it.report.description == budiDesc }) {
            repository.insertBullyingReport(
                BullyingReport(
                    reporterId = null, // External reporter (Budi)
                    isAnonymous = false,
                    victimId = null, // External victim (Ani)
                    perpetratorId = null, // External perpetrator (Doni)
                    incidentDate = java.util.Date(System.currentTimeMillis() - 172800000),
                    incidentLocation = "Kantin Sekolah",
                    incidentType = com.sekolah.aplikasismpn3pacet.data.IncidentType.VERBAL,
                    description = budiDesc,
                    evidence = null,
                    status = com.sekolah.aplikasismpn3pacet.data.ReportStatus.PENDING,
                    priority = com.sekolah.aplikasismpn3pacet.data.ReportPriority.MEDIUM,
                    assignedTo = null,
                    resolutionNotes = null,
                    resolvedAt = null
                )
            )
        }

        // 3. Cyber Report (Anonim)
        val cyberDesc = "Ada yang mengirim stiker tidak pantas dengan wajah Citra di grup kelas."
        if (existingReports.none { it.report.description == cyberDesc }) {
            repository.insertBullyingReport(
                BullyingReport(
                    reporterId = null, // External
                    isAnonymous = true,
                    victimId = null, // External (Citra)
                    perpetratorId = null, // External (Eko)
                    incidentDate = java.util.Date(System.currentTimeMillis() - 86400000),
                    incidentLocation = "Grup WhatsApp Kelas",
                    incidentType = com.sekolah.aplikasismpn3pacet.data.IncidentType.CYBER,
                    description = cyberDesc,
                    evidence = null,
                    status = com.sekolah.aplikasismpn3pacet.data.ReportStatus.INVESTIGATING,
                    priority = com.sekolah.aplikasismpn3pacet.data.ReportPriority.HIGH,
                    assignedTo = null,
                    resolutionNotes = null,
                    resolvedAt = null
                )
            )
        }
    }

    private suspend fun seedDisciplineData() {

        // 1. Always Seed/Update Rules
        val rules = listOf(
            DisciplineRule(ruleName = "Terlambat Sekolah", category = RuleCategory.VIOLATION_LATE, points = 5, severity = RuleSeverity.LOW, description = "Datang setelah bel masuk berbunyi"),
            DisciplineRule(ruleName = "Tidak Membawa Topi saat Upacara", category = RuleCategory.VIOLATION_ATTRIBUTE, points = 3, severity = RuleSeverity.LOW, description = "Kelengkapan seragam upacara"),
            DisciplineRule(ruleName = "Seragam Tidak Rapi", category = RuleCategory.VIOLATION_ATTRIBUTE, points = 2, severity = RuleSeverity.LOW, description = "Baju tidak dimasukkan"),
            DisciplineRule(ruleName = "Merokok di Lingkungan Sekolah", category = RuleCategory.VIOLATION_BEHAVIOR, points = 50, severity = RuleSeverity.HIGH, description = "Pelanggaran berat"),
            DisciplineRule(ruleName = "Bolos Pelajaran", category = RuleCategory.VIOLATION_BEHAVIOR, points = 10, severity = RuleSeverity.MEDIUM, description = "Meninggalkan kelas tanpa izin"),
            DisciplineRule(ruleName = "Memenangkan Lomba Tingkat Kabupaten", category = RuleCategory.ACHIEVEMENT, points = 25, severity = RuleSeverity.LOW, description = "Prestasi akademik/non-akademik"),
            DisciplineRule(ruleName = "Menjadi Petugas Upacara", category = RuleCategory.ACHIEVEMENT, points = 10, severity = RuleSeverity.LOW, description = "Partisipasi kegiatan sekolah")
        )
        
        rules.forEach { newRule ->
            val existing = repository.getDisciplineRuleByName(newRule.ruleName)
            if (existing == null) {
                repository.insertDisciplineRule(newRule)
            }
        }

        // 2. Seed Records and Reports for 'siswa' and 'miko'
        val siswaUser = repository.getUserByUsername("siswa")
        val mikoUser = repository.getUserByUsername("miko")
        
        seedUserData(siswaUser, isMiko = false)
        seedUserData(mikoUser, isMiko = true)
    }
    
    private suspend fun seedUserData(user: User?, isMiko: Boolean) {
        if (user != null) {
            val student = repository.getStudentByUserId(user.id)
            if (student != null) {
                // Seed Discipline Records
                val records = repository.getDisciplineRecordsByStudentId(student.id).firstOrNull() ?: emptyList()
                val allRules = repository.getAllDisciplineRules().firstOrNull() ?: emptyList()
                
                if (records.isEmpty()) {
                    if (isMiko) {
                         val achievementRule = allRules.find { it.ruleName == "Memenangkan Lomba Tingkat Kabupaten" }

                         if (achievementRule != null) {
                              repository.insertDisciplineRecord(
                                 DisciplineRecord(
                                     studentId = student.id,
                                     ruleId = achievementRule.id,
                                     date = java.util.Date(),
                                     points = achievementRule.points,
                                     description = "Juara 1 Lomba Coding",
                                     evidence = null,
                                     recordedBy = null,
                                     status = RecordStatus.APPROVED
                                 )
                             )
                         }
                         
                         // Seed Bullying Reports handled in seedBullyingReports()

                    } else {
                        // Siswa regular seeding (existing code logic simplified)
                        val lateRule = allRules.find { it.ruleName == "Terlambat Sekolah" }
                        val achievementRule = allRules.find { it.ruleName == "Menjadi Petugas Upacara" }
                        
                        if (lateRule != null) {
                            repository.insertDisciplineRecord(
                                DisciplineRecord(
                                    studentId = student.id,
                                    ruleId = lateRule.id,
                                    date = java.util.Date(),
                                    points = lateRule.points,
                                    description = "Terlambat 15 menit karena ban bocor",
                                    evidence = null,
                                    recordedBy = null,
                                    status = RecordStatus.APPROVED
                                )
                            )
                        }
                        if (achievementRule != null) {
                            repository.insertDisciplineRecord(
                                DisciplineRecord(
                                    studentId = student.id,
                                    ruleId = achievementRule.id,
                                    date = java.util.Date(System.currentTimeMillis() - 86400000),
                                    points = achievementRule.points,
                                    description = "Penggerek Bendera",
                                    evidence = null,
                                    recordedBy = null,
                                    status = RecordStatus.APPROVED
                                )
                            )
                        }
                    }
                }
                
                // Demo for Attribute Violation (Ensure this is added even if other records exist)
                if (isMiko) {
                     val attributeRule = repository.getDisciplineRuleByName("Seragam Tidak Rapi")
                     // Check if already has this specific violation
                     val hasAttributeViolation = records.any { it.rule.ruleName == "Seragam Tidak Rapi" }
                     
                     if (attributeRule != null && !hasAttributeViolation) {
                          repository.insertDisciplineRecord(
                             DisciplineRecord(
                                 studentId = student.id,
                                 ruleId = attributeRule.id,
                                 date = java.util.Date(),
                                 points = attributeRule.points,
                                 description = "Baju dikeluarkan saat jam pelajaran",
                                 evidence = null,
                                 recordedBy = null,
                                 status = RecordStatus.APPROVED
                             )
                         )
                     }
                } else {
                    // Force seed for Budi (Siswa) as well to ensure he has all categories
                    val attributeRule = repository.getDisciplineRuleByName("Seragam Tidak Rapi")
                    val lateRule = repository.getDisciplineRuleByName("Terlambat Sekolah")
                    val behaviorRule = repository.getDisciplineRuleByName("Bolos Pelajaran")
                    
                    val hasAttributeViolation = records.any { it.rule.ruleName == "Seragam Tidak Rapi" }
                    val hasLateViolation = records.any { it.rule.ruleName == "Terlambat Sekolah" }
                    val hasBehaviorViolation = records.any { it.rule.ruleName == "Bolos Pelajaran" }

                    if (attributeRule != null && !hasAttributeViolation) {
                        repository.insertDisciplineRecord(
                            DisciplineRecord(
                                studentId = student.id,
                                ruleId = attributeRule.id,
                                date = java.util.Date(),
                                points = attributeRule.points,
                                description = "Baju dikeluarkan saat jam pelajaran",
                                evidence = null,
                                recordedBy = null,
                                status = RecordStatus.APPROVED
                            )
                        )
                    }
                    
                    if (lateRule != null && !hasLateViolation) {
                        repository.insertDisciplineRecord(
                            DisciplineRecord(
                                studentId = student.id,
                                ruleId = lateRule.id,
                                date = java.util.Date(),
                                points = lateRule.points,
                                description = "Terlambat 15 menit karena ban bocor",
                                evidence = null,
                                recordedBy = null,
                                status = RecordStatus.APPROVED
                            )
                        )
                    }
                    
                    if (behaviorRule != null && !hasBehaviorViolation) {
                        repository.insertDisciplineRecord(
                            DisciplineRecord(
                                studentId = student.id,
                                ruleId = behaviorRule.id,
                                date = java.util.Date(),
                                points = behaviorRule.points,
                                description = "Meninggalkan kelas tanpa izin",
                                evidence = null,
                                recordedBy = null,
                                status = RecordStatus.APPROVED
                            )
                        )
                    }
                }
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class SchoolViewModelFactory(
    private val repository: SchoolRepository,
    private val userPreferences: UserPreferences? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            if (userPreferences == null) {
                throw IllegalArgumentException("UserPreferences must be provided for LoginViewModel")
            }
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository, userPreferences) as T
        }
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherAttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherAttendanceViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherStudentListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherStudentListViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherDisciplineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherDisciplineViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(DisciplineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DisciplineViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(BullyingReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BullyingReportViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(StudentLiteracyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentLiteracyViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherLiteracyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherLiteracyViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherBullyingReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherBullyingReportViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TeacherNotificationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TeacherNotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
