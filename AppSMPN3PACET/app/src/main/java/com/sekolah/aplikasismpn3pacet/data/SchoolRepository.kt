package com.sekolah.aplikasismpn3pacet.data

import com.sekolah.aplikasismpn3pacet.data.entity.Attendance
import com.sekolah.aplikasismpn3pacet.data.entity.SchoolInformation
import com.sekolah.aplikasismpn3pacet.data.entity.Student
import com.sekolah.aplikasismpn3pacet.data.entity.User
import com.sekolah.aplikasismpn3pacet.data.entity.Notification
import com.sekolah.aplikasismpn3pacet.data.NotificationType
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLog
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyTask
import com.sekolah.aplikasismpn3pacet.data.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

import com.sekolah.aplikasismpn3pacet.data.entity.BullyingReportWithReporter
import com.sekolah.aplikasismpn3pacet.data.entity.SchoolSchedule
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SchoolRepository(private val schoolDao: SchoolDao) {
    private val firebaseDb = Firebase.database("https://eperpus-sekolah-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val firestoreDb = Firebase.firestore

    // Device Binding Check
    suspend fun checkDeviceBinding(username: String, deviceId: String): Pair<Boolean, String?> {
        return try {
            // 1. Check if device is bound to ANOTHER user
            val deviceQuery = firestoreDb.collection("students")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .await()
            
            for (d in deviceQuery.documents) {
                val dUsername = d.getString("username")
                if (dUsername != null && dUsername != username) {
                    val dName = d.getString("name") ?: dUsername
                    return Pair(false, "Perangkat ini terkunci untuk akun $dName. Tidak bisa login dengan akun lain.")
                }
            }

            // 2. Check if user is bound to ANOTHER device
            val userQuery = firestoreDb.collection("students")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (userQuery.isEmpty) {
                // If user not found in Firestore but found locally (from previous sync), allow?
                // Or maybe strictly require Firestore for binding.
                // Let's assume sync ensures they exist.
                return Pair(true, null) // Allow login if not found (maybe teacher or offline mode fallback)
            }

            val doc = userQuery.documents[0]
            val serverDeviceId = doc.getString("deviceId")

            if (serverDeviceId.isNullOrEmpty()) {
                // Bind device to this user
                firestoreDb.collection("students").document(doc.id)
                    .update("deviceId", deviceId)
                    .await()
                return Pair(true, null)
            } else {
                if (serverDeviceId == deviceId) {
                    return Pair(true, null)
                } else {
                    return Pair(false, "Akun ini terkunci di perangkat lain. Hubungi Admin untuk reset.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If offline/error, default to ALLOW but log warning?
            // Or BLOCK? Security vs Usability.
            // For now, allow with warning if it's network error, but we can't easily distinguish.
            // Safe default for school app: Allow if error (don't block legitimate users due to bad net),
            // BUT this defeats the purpose of locking.
            // Let's BLOCK if it's a logic check, but if exception...
            // User requested "Reset Device Binding", so locking is important.
            // I'll return FALSE if error.
            Pair(false, "Gagal memverifikasi perangkat: ${e.message}")
        }
    }

    // Firebase Sync Logic
    fun startStudentSync() {
        firestoreDb.collection("students")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                CoroutineScope(Dispatchers.IO).launch {
                    snapshots?.forEach { doc ->
                        try {
                            val name = doc.getString("name") ?: return@forEach
                            val username = doc.getString("username") ?: name
                            val password = doc.getString("password") ?: doc.getString("nisn") ?: "123456"
                            val nisn = doc.getString("nisn") ?: ""
                            val genderStr = doc.getString("gender") ?: "MALE"
                            
                            // Check if user exists
                            var user = schoolDao.getUserByUsername(username)
                            if (user == null) {
                                // Create new User
                                user = User(
                                    username = username,
                                    password = password,
                                    fullName = name,
                                    role = com.sekolah.aplikasismpn3pacet.data.UserRole.STUDENT,
                                    email = "${username.replace(" ", "").lowercase()}@student.smpn3pacet.sch.id",
                                    nisNip = nisn,
                                    phone = null,
                                    address = null,
                                    gender = if (genderStr.equals("FEMALE", ignoreCase = true)) com.sekolah.aplikasismpn3pacet.data.Gender.FEMALE else com.sekolah.aplikasismpn3pacet.data.Gender.MALE,
                                    birthDate = null,
                                    profilePicture = null
                                )
                                val userId = schoolDao.insertUser(user)
                                user = user.copy(id = userId)
                            } else {
                                // Update password if changed
                                if (user.password != password) {
                                    user = user.copy(password = password)
                                    schoolDao.insertUser(user)
                                }
                            }

                            // Check if student record exists
                            val existingStudent = schoolDao.getStudentByUserId(user.id)
                            if (existingStudent == null) {
                                val student = Student(
                                    userId = user.id,
                                    classId = null, // Use null to avoid FK constraint issues
                                    parentName = null,
                                    parentPhone = null,
                                    parentEmail = null,
                                    admissionYear = null
                                )
                                schoolDao.insertStudent(student)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    fun startScheduleSync() {
        firebaseDb.child("schedules").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    snapshot.children.forEach { daySnapshot ->
                        try {
                            // Key is already 1-based (1=Sunday, 2=Monday, ...) matching Calendar constants
                            val dayOfWeek = daySnapshot.key?.toIntOrNull() ?: return@forEach
                            
                            val dayName = daySnapshot.child("dayName").getValue(String::class.java) ?: ""
                            // Map Dashboard keys (entryTime/exitTime) to Android keys (startTime/endTime)
                            val startTime = daySnapshot.child("entryTime").getValue(String::class.java) ?: "00:00"
                            val endTime = daySnapshot.child("exitTime").getValue(String::class.java) ?: "00:00"
                            
                            // Map Dashboard isEnabled to Android isHoliday
                            val isEnabled = daySnapshot.child("isEnabled").getValue(Boolean::class.java) ?: false
                            val isHoliday = !isEnabled

                            // Sync with local DB
                            val existing = schoolDao.getSchoolScheduleByDay(dayOfWeek)
                            val scheduleToSave = SchoolSchedule(
                                id = existing?.id ?: 0,
                                dayOfWeek = dayOfWeek,
                                dayName = dayName,
                                startTime = startTime,
                                endTime = endTime,
                                isHoliday = isHoliday
                            )
                            schoolDao.insertSchoolSchedule(scheduleToSave)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun startAttendanceSync() {
        firebaseDb.child("attendance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    snapshot.children.forEach { logSnapshot ->
                        try {
                            val studentIdStr = logSnapshot.child("studentId").getValue(String::class.java)
                            val studentId = studentIdStr?.toLongOrNull() ?: return@forEach
                            
                            val dateMillis = logSnapshot.child("date").getValue(Long::class.java) ?: return@forEach
                            val statusStr = logSnapshot.child("status").getValue(String::class.java) ?: "PRESENT"
                            val checkInTime = logSnapshot.child("checkInTime").getValue(String::class.java)
                            val notes = logSnapshot.child("notes").getValue(String::class.java)
                            val proofDocument = logSnapshot.child("proofDocument").getValue(String::class.java)
                            
                            // Map Status
                            val status = try {
                                com.sekolah.aplikasismpn3pacet.data.AttendanceStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                com.sekolah.aplikasismpn3pacet.data.AttendanceStatus.PRESENT
                            }

                            // Use date to find existing record for that day
                            val calendar = java.util.Calendar.getInstance()
                            calendar.timeInMillis = dateMillis
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            calendar.set(java.util.Calendar.MINUTE, 0)
                            calendar.set(java.util.Calendar.SECOND, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            val startOfDay = calendar.timeInMillis
                            val endOfDay = startOfDay + 86400000

                            val existing = schoolDao.getAttendanceForDate(studentId, startOfDay, endOfDay)
                            
                            val log = if (existing != null) {
                                existing.copy(
                                    status = status,
                                    checkInTime = checkInTime,
                                    notes = notes,
                                    proofDocument = proofDocument
                                )
                            } else {
                                Attendance(
                                    studentId = studentId,
                                    date = java.util.Date(dateMillis),
                                    status = status,
                                    checkInTime = checkInTime,
                                    checkInMethod = com.sekolah.aplikasismpn3pacet.data.CheckInMethod.MANUAL,
                                    notes = notes,
                                    proofDocument = proofDocument,
                                    recordedBy = null
                                )
                            }
                            schoolDao.insertAttendance(log)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }



    fun syncScheduleToFirebase(schedule: SchoolSchedule) {
        val scheduleMap = mapOf(
            "dayName" to schedule.dayName,
            "startTime" to schedule.startTime,
            "endTime" to schedule.endTime,
            "isHoliday" to schedule.isHoliday
        )
        firebaseDb.child("schedules").child(schedule.dayOfWeek.toString()).setValue(scheduleMap)
    }

    fun syncAttendanceToFirebase(attendance: Attendance) {
        val logMap = mapOf(
            "studentId" to attendance.studentId.toString(),
            "date" to attendance.date.time,
            "status" to attendance.status.name,
            "checkInTime" to attendance.checkInTime,
            "checkInMethod" to (attendance.checkInMethod?.name ?: "MANUAL"),
            "notes" to attendance.notes,
            "proofDocument" to attendance.proofDocument
        )
        firebaseDb.child("attendance").push().setValue(logMap)
    }

    suspend fun insertUser(user: User) = schoolDao.insertUser(user)
    suspend fun getUserByUsername(username: String) = schoolDao.getUserByUsername(username)
    suspend fun getUserById(id: Long) = schoolDao.getUserById(id)
    suspend fun deleteUser(user: User) = schoolDao.deleteUser(user)
    suspend fun deleteNotificationsByUserId(userId: Long) = schoolDao.deleteNotificationsByUserId(userId)
    
    suspend fun insertStudent(student: Student) = schoolDao.insertStudent(student)
    suspend fun getStudentByUserId(userId: Long): Student? = schoolDao.getStudentByUserId(userId)
    suspend fun getStudentById(id: Long): Student? = schoolDao.getStudentById(id)
    suspend fun deleteStudent(student: Student) = schoolDao.deleteStudent(student)
    fun getAllStudents() = schoolDao.getAllStudents()
    
    suspend fun insertAttendance(attendance: Attendance) = schoolDao.insertAttendance(attendance)
    suspend fun deleteAttendancesByStudentId(studentId: Long) = schoolDao.deleteAttendancesByStudentId(studentId)
    fun getAttendanceHistory(studentId: Long): Flow<List<Attendance>> = schoolDao.getAttendanceHistory(studentId)
    suspend fun getAttendanceForDate(studentId: Long, start: Long, end: Long): Attendance? = schoolDao.getAttendanceForDate(studentId, start, end)

    suspend fun getTeacherByUserId(userId: Long) = schoolDao.getTeacherByUserId(userId)
    suspend fun insertTeacher(teacher: com.sekolah.aplikasismpn3pacet.data.entity.Teacher) = schoolDao.insertTeacher(teacher)
    suspend fun getClassByHomeroomTeacherId(teacherId: Long) = schoolDao.getClassByHomeroomTeacherId(teacherId)
    suspend fun insertClass(classEntity: com.sekolah.aplikasismpn3pacet.data.entity.ClassEntity) = schoolDao.insertClass(classEntity)
    fun getStudentsWithUserByClassId(classId: Long) = schoolDao.getStudentsWithUserByClassId(classId)
    fun getAttendanceForStudents(studentIds: List<Long>, start: Long, end: Long) = schoolDao.getAttendanceForStudents(studentIds, start, end)

    suspend fun insertSchoolInformation(info: SchoolInformation) = schoolDao.insertSchoolInformation(info)

    // Schedule
    suspend fun insertSchoolSchedule(schedule: SchoolSchedule) = schoolDao.insertSchoolSchedule(schedule)
    fun getSchoolSchedules() = schoolDao.getSchoolSchedules()
    suspend fun getSchoolScheduleByDay(day: Int) = schoolDao.getSchoolScheduleByDay(day)

    fun getSchoolInformation() = schoolDao.getSchoolInformation()

    // Virtual Pet
    suspend fun insertVirtualPet(pet: com.sekolah.aplikasismpn3pacet.data.entity.VirtualPet) = schoolDao.insertVirtualPet(pet)
    suspend fun updateVirtualPet(pet: com.sekolah.aplikasismpn3pacet.data.entity.VirtualPet) = schoolDao.updateVirtualPet(pet)
    fun getVirtualPetByStudentId(studentId: Long) = schoolDao.getVirtualPetByStudentId(studentId)

    // Pet Quests & Achievements
    suspend fun insertPetQuest(quest: com.sekolah.aplikasismpn3pacet.data.entity.PetQuest) = schoolDao.insertPetQuest(quest)
    suspend fun updatePetQuest(quest: com.sekolah.aplikasismpn3pacet.data.entity.PetQuest) = schoolDao.updatePetQuest(quest)
    fun getPetQuests(petId: Long) = schoolDao.getPetQuests(petId)
    suspend fun deletePetQuests(petId: Long) = schoolDao.deletePetQuests(petId)

    suspend fun insertPetAchievement(achievement: com.sekolah.aplikasismpn3pacet.data.entity.PetAchievement) = schoolDao.insertPetAchievement(achievement)
    suspend fun updatePetAchievement(achievement: com.sekolah.aplikasismpn3pacet.data.entity.PetAchievement) = schoolDao.updatePetAchievement(achievement)
    fun getPetAchievements(petId: Long) = schoolDao.getPetAchievements(petId)

    // Discipline
    fun getDisciplineRecordsByStudentId(studentId: Long) = schoolDao.getDisciplineRecordsByStudentId(studentId)
    suspend fun deleteDisciplineRecordsByStudentId(studentId: Long) = schoolDao.deleteDisciplineRecordsByStudentId(studentId)
    suspend fun deleteBullyingReportsByStudentId(studentId: Long) = schoolDao.deleteBullyingReportsByStudentId(studentId)
    fun getAllDisciplineRules() = schoolDao.getAllDisciplineRules()
    suspend fun getDisciplineRuleByName(name: String) = schoolDao.getDisciplineRuleByName(name)
    suspend fun insertDisciplineRule(rule: com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRule) = schoolDao.insertDisciplineRule(rule)
    suspend fun insertDisciplineRecord(record: com.sekolah.aplikasismpn3pacet.data.entity.DisciplineRecord) {
        schoolDao.insertDisciplineRecord(record)
        
        // Auto-generate notification
        val student = schoolDao.getStudentById(record.studentId)
        val rule = schoolDao.getDisciplineRuleById(record.ruleId)
        
        if (student != null && rule != null) {
            val notification = Notification(
                userId = student.userId,
                title = "Pelanggaran Disiplin Dicatat",
                message = "Anda tercatat melakukan pelanggaran: ${rule.ruleName}. Poin berkurang: ${rule.points}",
                type = NotificationType.WARNING,
                relatedFeature = "DISCIPLINE"
            )
            schoolDao.insertNotification(notification)
        }
    }

    // Bullying Report
    suspend fun insertBullyingReport(report: com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport) {
        schoolDao.insertBullyingReport(report)
        
        // Auto-generate confirmation notification
        if (report.reporterId != null) {
            val student = schoolDao.getStudentById(report.reporterId)
            if (student != null) {
                // Determine notification type based on status change
                if (report.status == com.sekolah.aplikasismpn3pacet.data.ReportStatus.PENDING) {
                     val notification = Notification(
                        userId = student.userId,
                        title = "Laporan Terkirim",
                        message = "Laporan Anda telah diterima. Kami akan segera menindaklanjutinya dengan menjaga kerahasiaan Anda.",
                        type = NotificationType.SUCCESS,
                        relatedFeature = "BULLYING_REPORT"
                    )
                    schoolDao.insertNotification(notification)
                }
            }
        }
    }

    fun syncBullyingReportToFirebase(report: com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport) {
        // Use push() to generate unique ID as multiple devices might have same local IDs
        // But we want to be able to update it if status changes? 
        // For now, let's assume one-way sync for submission or use a composite key if needed.
        // Simple push for submission:
        
        val reportMap = mapOf(
            "androidId" to report.id,
            "reporterId" to report.reporterId.toString(),
            "isAnonymous" to report.isAnonymous,
            "incidentDate" to report.incidentDate.time,
            "incidentLocation" to report.incidentLocation,
            "incidentType" to report.incidentType.name,
            "description" to report.description,
            "status" to report.status.name,
            "priority" to report.priority.name,
            "createdAt" to System.currentTimeMillis()
        )
        
        // Use a composite key to allow updates if we want to sync status back/forth later
        // or just push. Let's use push for now to avoid overwrites.
        firebaseDb.child("bullying_reports").push().setValue(reportMap)
    }


    suspend fun updateBullyingReportStatus(report: com.sekolah.aplikasismpn3pacet.data.entity.BullyingReport, newStatus: com.sekolah.aplikasismpn3pacet.data.ReportStatus) {
        val updatedReport = report.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis(),
            resolvedAt = if (newStatus == com.sekolah.aplikasismpn3pacet.data.ReportStatus.RESOLVED || newStatus == com.sekolah.aplikasismpn3pacet.data.ReportStatus.CLOSED) System.currentTimeMillis() else null
        )
        schoolDao.insertBullyingReport(updatedReport)
        
        // Notify student about status update
        if (report.reporterId != null) {
            val student = schoolDao.getStudentById(report.reporterId)
            if (student != null) {
                 val message = when (newStatus) {
                    com.sekolah.aplikasismpn3pacet.data.ReportStatus.INVESTIGATING -> "Laporan Anda sedang kami tinjau dan investigasi."
                    com.sekolah.aplikasismpn3pacet.data.ReportStatus.RESOLVED -> "Laporan Anda telah selesai ditindaklanjuti. Terima kasih atas keberanian Anda melapor."
                    com.sekolah.aplikasismpn3pacet.data.ReportStatus.CLOSED -> "Laporan Anda telah ditutup."
                    else -> "Status laporan Anda telah diperbarui."
                }
                
                val notification = Notification(
                    userId = student.userId,
                    title = "Update Laporan Bullying",
                    message = message,
                    type = NotificationType.INFO,
                    relatedFeature = "BULLYING_REPORT"
                )
                schoolDao.insertNotification(notification)
            }
        }
    }

    fun getBullyingReportsByReporterId(reporterId: Long) = schoolDao.getBullyingReportsByReporterId(reporterId)
    fun getAllBullyingReports(): Flow<List<BullyingReportWithReporter>> = schoolDao.getAllBullyingReports()
    suspend fun getPendingBullyingReportsSync() = schoolDao.getPendingBullyingReportsSync()

    // Notification
    suspend fun insertNotification(notification: com.sekolah.aplikasismpn3pacet.data.entity.Notification) = schoolDao.insertNotification(notification)
    fun getNotificationsForUser(userId: Long) = schoolDao.getNotificationsForUser(userId)
    fun getUnreadNotificationCount(userId: Long) = schoolDao.getUnreadNotificationCount(userId)
    suspend fun markNotificationAsRead(notificationId: Long) = schoolDao.markNotificationAsRead(notificationId)

    // Habits
    suspend fun insertHabitLog(log: com.sekolah.aplikasismpn3pacet.data.entity.HabitLog) = schoolDao.insertHabitLog(log)
    suspend fun updateHabitLog(log: com.sekolah.aplikasismpn3pacet.data.entity.HabitLog) = schoolDao.updateHabitLog(log)
    fun getHabitLogsByStudentAndDateRange(studentId: Long, startDate: Long, endDate: Long) = schoolDao.getHabitLogsByStudentAndDateRange(studentId, startDate, endDate)

    // Literacy
    suspend fun insertLiteracyLog(log: LiteracyLog) = schoolDao.insertLiteracyLog(log)
    suspend fun updateLiteracyLog(log: LiteracyLog) = schoolDao.updateLiteracyLog(log)
    suspend fun deleteLiteracyLog(log: LiteracyLog) = schoolDao.deleteLiteracyLog(log)
    suspend fun deleteLiteracyLogsByStudentId(studentId: Long) = schoolDao.deleteLiteracyLogsByStudentId(studentId)
    fun getLiteracyLogsByStudent(studentId: Long) = schoolDao.getLiteracyLogsByStudent(studentId)
    fun getLiteracyLogsByStudentsAndStatus(studentIds: List<Long>, status: SubmissionStatus) = schoolDao.getLiteracyLogsByStudentsAndStatus(studentIds, status)
    fun getLiteracyLogsByStudents(studentIds: List<Long>) = schoolDao.getLiteracyLogsByStudents(studentIds)
    suspend fun getPendingLiteracyLogsSync() = schoolDao.getPendingLiteracyLogsSync()

    // Literacy Task
    suspend fun insertLiteracyTask(task: LiteracyTask) = schoolDao.insertLiteracyTask(task)
    suspend fun deleteAllLiteracyTasks() = schoolDao.deleteAllLiteracyTasks()
    fun getActiveLiteracyTask() = schoolDao.getActiveLiteracyTask()

    // Firebase Listeners
    private var isListeningToLiteracy = false

    fun listenToLiteracyTasks() {
        if (isListeningToLiteracy) return
        isListeningToLiteracy = true
        
        firebaseDb.child("literacy_tasks").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val tasks = mutableListOf<LiteracyTask>()
                        snapshot.children.forEach { taskSnapshot ->
                            try {
                                val title = taskSnapshot.child("title").getValue(String::class.java) ?: ""
                                val description = taskSnapshot.child("description").getValue(String::class.java) ?: ""
                                
                                val pointsVal = taskSnapshot.child("points").value
                                val points = when(pointsVal) {
                                    is Long -> pointsVal.toInt()
                                    is String -> pointsVal.toIntOrNull() ?: 0
                                    is Int -> pointsVal
                                    else -> 0
                                }

                                val durationVal = taskSnapshot.child("durationMinutes").value
                                val durationMinutes = when(durationVal) {
                                    is Long -> durationVal.toInt()
                                    is String -> durationVal.toIntOrNull() ?: 45
                                    is Int -> durationVal
                                    else -> 45
                                }

                                val isActive = taskSnapshot.child("isActive").getValue(Boolean::class.java) ?: true
                                
                                val createdAtVal = taskSnapshot.child("createdAt").value
                                val createdAt = when(createdAtVal) {
                                    is Long -> createdAtVal
                                    is String -> {
                                        // Try parsing ISO date if needed, or just Long string
                                        createdAtVal.toLongOrNull() ?: System.currentTimeMillis()
                                    }
                                    else -> System.currentTimeMillis()
                                }

                                tasks.add(
                                    LiteracyTask(
                                        title = title,
                                        description = description,
                                        points = points,
                                        durationMinutes = durationMinutes,
                                        isActive = isActive,
                                        createdAt = createdAt
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Replace local cache
                        if (tasks.isNotEmpty()) {
                            schoolDao.deleteAllLiteracyTasks()
                            tasks.forEach { schoolDao.insertLiteracyTask(it) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private var isListeningToLiteracyLogs = false

    fun listenToLiteracyLogUpdates() {
        if (isListeningToLiteracyLogs) return
        isListeningToLiteracyLogs = true
        
        firebaseDb.child("literacy_logs").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        snapshot.children.forEach { logSnapshot ->
                            val androidId = logSnapshot.key?.toLongOrNull() ?: return@forEach
                            val statusStr = logSnapshot.child("status").getValue(String::class.java) ?: "PENDING"
                            val grade = logSnapshot.child("grade").getValue(String::class.java)
                            val feedback = logSnapshot.child("feedback").getValue(String::class.java)
                            val teacherIdStr = logSnapshot.child("teacherId").getValue(String::class.java)
                            
                            val studentIdRaw = logSnapshot.child("studentId").getValue(String::class.java)
                            val studentUsername = logSnapshot.child("studentUsername").getValue(String::class.java)
                            val studentUserIdRaw = logSnapshot.child("studentUserId").getValue(String::class.java)
                            val studentFullName = logSnapshot.child("studentFullName").getValue(String::class.java)
                            val studentNisNip = logSnapshot.child("studentNisNip").getValue(String::class.java)

                            var studentId = studentIdRaw?.toLongOrNull() ?: 0L
                            if (!studentUsername.isNullOrBlank()) {
                                val user = schoolDao.getUserByUsername(studentUsername)
                                val student = if (user != null) schoolDao.getStudentByUserId(user.id) else null
                                if (student != null) {
                                    studentId = student.id
                                }
                            } else if (!studentUserIdRaw.isNullOrBlank()) {
                                val userId = studentUserIdRaw.toLongOrNull()
                                if (userId != null) {
                                    val student = schoolDao.getStudentByUserId(userId)
                                    if (student != null) {
                                        studentId = student.id
                                    }
                                }
                            } else {
                                val allStudents = schoolDao.getAllStudents().firstOrNull() ?: emptyList()
                                if (!studentNisNip.isNullOrBlank()) {
                                    for (s in allStudents) {
                                        val u = schoolDao.getUserById(s.userId)
                                        if (u?.nisNip == studentNisNip) {
                                            studentId = s.id
                                            break
                                        }
                                    }
                                }
                                if (studentId == 0L && !studentFullName.isNullOrBlank()) {
                                    val targetName = studentFullName.trim().lowercase()
                                    for (s in allStudents) {
                                        val u = schoolDao.getUserById(s.userId)
                                        val name = u?.fullName?.trim()?.lowercase()
                                        if (name == targetName) {
                                            studentId = s.id
                                            break
                                        }
                                    }
                                }
                            }
                            val bookTitle = logSnapshot.child("bookTitle").getValue(String::class.java) ?: ""
                            val author = logSnapshot.child("author").getValue(String::class.java) ?: ""
                            val summary = logSnapshot.child("summary").getValue(String::class.java) ?: ""
                            val readingDuration = logSnapshot.child("readingDuration").getValue(String::class.java) ?: ""
                            val submissionTime = logSnapshot.child("submissionDate").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val createdAt = logSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val updatedAt = logSnapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            val status = try {
                                SubmissionStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                SubmissionStatus.PENDING
                            }

                            val log = LiteracyLog(
                                id = androidId,
                                studentId = studentId,
                                bookTitle = bookTitle,
                                author = author,
                                readingDuration = readingDuration,
                                summary = summary,
                                submissionDate = java.util.Date(submissionTime),
                                status = status,
                                grade = grade,
                                feedback = feedback,
                                teacherId = teacherIdStr?.toLongOrNull(),
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                            
                            // Upsert
                            schoolDao.insertLiteracyLog(log)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // One-time pull to ensure initial data is loaded even if listener hasn't captured updates yet
    fun refreshLiteracyLogsForStudents(targetStudentIds: List<Long>) {
        if (targetStudentIds.isEmpty()) return
        firebaseDb.child("literacy_logs").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        snapshot.children.forEach { logSnapshot ->
                            val androidId = logSnapshot.key?.toLongOrNull() ?: return@forEach
                            val statusStr = logSnapshot.child("status").getValue(String::class.java) ?: "PENDING"
                            val grade = logSnapshot.child("grade").getValue(String::class.java)
                            val feedback = logSnapshot.child("feedback").getValue(String::class.java)
                            val teacherIdStr = logSnapshot.child("teacherId").getValue(String::class.java)
                            
                            val studentIdRaw = logSnapshot.child("studentId").getValue(String::class.java)
                            val studentUsername = logSnapshot.child("studentUsername").getValue(String::class.java)
                            val studentUserIdRaw = logSnapshot.child("studentUserId").getValue(String::class.java)
                            val studentFullName = logSnapshot.child("studentFullName").getValue(String::class.java)
                            val studentNisNip = logSnapshot.child("studentNisNip").getValue(String::class.java)

                            var studentId = studentIdRaw?.toLongOrNull() ?: 0L
                            if (!studentUsername.isNullOrBlank()) {
                                val user = schoolDao.getUserByUsername(studentUsername)
                                val student = if (user != null) schoolDao.getStudentByUserId(user.id) else null
                                if (student != null) studentId = student.id
                            } else if (!studentUserIdRaw.isNullOrBlank()) {
                                val userId = studentUserIdRaw.toLongOrNull()
                                if (userId != null) {
                                    val student = schoolDao.getStudentByUserId(userId)
                                    if (student != null) studentId = student.id
                                }
                            } else {
                                val allStudents = schoolDao.getAllStudents().firstOrNull() ?: emptyList()
                                if (!studentNisNip.isNullOrBlank()) {
                                    for (s in allStudents) {
                                        val u = schoolDao.getUserById(s.userId)
                                        if (u?.nisNip == studentNisNip) { studentId = s.id; break }
                                    }
                                }
                                if (studentId == 0L && !studentFullName.isNullOrBlank()) {
                                    val targetName = studentFullName.trim().lowercase()
                                    for (s in allStudents) {
                                        val u = schoolDao.getUserById(s.userId)
                                        val name = u?.fullName?.trim()?.lowercase()
                                        if (name == targetName) { studentId = s.id; break }
                                    }
                                }
                            }
                            
                            if (studentId !in targetStudentIds) return@forEach

                            val bookTitle = logSnapshot.child("bookTitle").getValue(String::class.java) ?: ""
                            val author = logSnapshot.child("author").getValue(String::class.java) ?: ""
                            val summary = logSnapshot.child("summary").getValue(String::class.java) ?: ""
                            val readingDuration = logSnapshot.child("readingDuration").getValue(String::class.java) ?: ""
                            val submissionTime = logSnapshot.child("submissionDate").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val createdAt = logSnapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                            val updatedAt = logSnapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            val status = try { SubmissionStatus.valueOf(statusStr) } catch (_: Exception) { SubmissionStatus.PENDING }

                            val log = LiteracyLog(
                                id = androidId,
                                studentId = studentId,
                                bookTitle = bookTitle,
                                author = author,
                                readingDuration = readingDuration,
                                summary = summary,
                                submissionDate = java.util.Date(submissionTime),
                                status = status,
                                grade = grade,
                                feedback = feedback,
                                teacherId = teacherIdStr?.toLongOrNull(),
                                createdAt = createdAt,
                                updatedAt = updatedAt
                            )
                            schoolDao.insertLiteracyLog(log)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun syncLiteracyLogToFirebase(log: LiteracyLog) {
        val logMap = mapOf(
            "androidId" to log.id,
            "studentId" to log.studentId.toString(),
            "bookTitle" to log.bookTitle,
            "author" to log.author,
            "readingDuration" to log.readingDuration,
            "summary" to log.summary,
            "submissionDate" to log.submissionDate.time,
            "status" to log.status.name,
            "grade" to (log.grade ?: ""),
            "feedback" to (log.feedback ?: ""),
            "teacherId" to (log.teacherId?.toString() ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        firebaseDb.child("literacy_logs").child(log.id.toString()).setValue(logMap)
            .addOnFailureListener { e ->
                e.printStackTrace()
                // Log error locally or via Crashlytics if available
                println("Firebase Sync Failed: ${e.message}")
            }
    }

    fun syncLiteracyLogToFirebaseWithUser(log: LiteracyLog, studentUserId: Long, studentUsername: String) {
        val logMap = mapOf(
            "androidId" to log.id,
            "studentId" to log.studentId.toString(),
            "studentUserId" to studentUserId.toString(),
            "studentUsername" to studentUsername,
            "bookTitle" to log.bookTitle,
            "author" to log.author,
            "readingDuration" to log.readingDuration,
            "summary" to log.summary,
            "submissionDate" to log.submissionDate.time,
            "status" to log.status.name,
            "grade" to (log.grade ?: ""),
            "feedback" to (log.feedback ?: ""),
            "teacherId" to (log.teacherId?.toString() ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        firebaseDb.child("literacy_logs").child(log.id.toString()).setValue(logMap)
    }

    fun syncLiteracyLogToFirebaseWithIdentity(
        log: LiteracyLog,
        studentUserId: Long,
        studentUsername: String,
        studentFullName: String?,
        studentNisNip: String?
    ) {
        val logMap = mutableMapOf<String, Any?>(
            "androidId" to log.id,
            "studentId" to log.studentId.toString(),
            "studentUserId" to studentUserId.toString(),
            "studentUsername" to studentUsername,
            "bookTitle" to log.bookTitle,
            "author" to log.author,
            "readingDuration" to log.readingDuration,
            "summary" to log.summary,
            "submissionDate" to log.submissionDate.time,
            "status" to log.status.name,
            "grade" to (log.grade ?: ""),
            "feedback" to (log.feedback ?: ""),
            "teacherId" to (log.teacherId?.toString() ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        if (!studentFullName.isNullOrBlank()) logMap["studentFullName"] = studentFullName
        if (!studentNisNip.isNullOrBlank()) logMap["studentNisNip"] = studentNisNip
        firebaseDb.child("literacy_logs").child(log.id.toString()).setValue(logMap)
            .addOnFailureListener { e ->
                e.printStackTrace()
                println("Firebase Sync Failed (Identity): ${e.message}")
            }
    }

    fun syncLiteracyLogToFirebaseWithIdentityAsync(
        log: LiteracyLog,
        studentUserId: Long,
        studentUsername: String,
        studentFullName: String?,
        studentNisNip: String?
    ): com.google.android.gms.tasks.Task<Void> {
        val logMap = mutableMapOf<String, Any?>(
            "androidId" to log.id,
            "studentId" to log.studentId.toString(),
            "studentUserId" to studentUserId.toString(),
            "studentUsername" to studentUsername,
            "bookTitle" to log.bookTitle,
            "author" to log.author,
            "readingDuration" to log.readingDuration,
            "summary" to log.summary,
            "submissionDate" to log.submissionDate.time,
            "status" to log.status.name,
            "grade" to (log.grade ?: ""),
            "feedback" to (log.feedback ?: ""),
            "teacherId" to (log.teacherId?.toString() ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )
        if (!studentFullName.isNullOrBlank()) logMap["studentFullName"] = studentFullName
        if (!studentNisNip.isNullOrBlank()) logMap["studentNisNip"] = studentNisNip
        return firebaseDb.child("literacy_logs").child(log.id.toString()).setValue(logMap)
    }
}
