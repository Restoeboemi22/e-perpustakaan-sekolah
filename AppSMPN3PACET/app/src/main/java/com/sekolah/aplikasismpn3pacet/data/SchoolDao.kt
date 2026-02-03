package com.sekolah.aplikasismpn3pacet.data

import androidx.room.*
import com.sekolah.aplikasismpn3pacet.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    // User
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Delete
    suspend fun deleteUser(user: User)

    // Student
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Transaction
    @Query("SELECT * FROM students WHERE user_id = :userId")
    suspend fun getStudentByUserId(userId: Long): Student?

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentById(id: Long): Student?

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<Student>>

    // Teacher
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    // Class
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: ClassEntity): Long

    @Query("SELECT * FROM classes")
    fun getAllClasses(): Flow<List<ClassEntity>>

    // General Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplineRule(rule: DisciplineRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplineRecord(record: DisciplineRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBullyingReport(report: BullyingReport): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    // Queries
    @Transaction
    @Query("SELECT * FROM discipline_records WHERE student_id = :studentId ORDER BY date DESC")
    fun getDisciplineRecordsByStudentId(studentId: Long): Flow<List<DisciplineRecordWithRule>>

    @Query("DELETE FROM discipline_records WHERE student_id = :studentId")
    suspend fun deleteDisciplineRecordsByStudentId(studentId: Long)

    @Query("SELECT * FROM discipline_rules")
    fun getAllDisciplineRules(): Flow<List<DisciplineRule>>

    @Query("SELECT * FROM discipline_rules WHERE id = :id")
    suspend fun getDisciplineRuleById(id: Long): DisciplineRule?

    @Query("SELECT * FROM discipline_rules WHERE rule_name = :ruleName")
    suspend fun getDisciplineRuleByName(ruleName: String): DisciplineRule?

    @Query("SELECT * FROM bullying_reports WHERE reporter_id = :reporterId ORDER BY created_at DESC")
    fun getBullyingReportsByReporterId(reporterId: Long): Flow<List<BullyingReport>>

    @Transaction
    @Query("SELECT * FROM bullying_reports ORDER BY created_at DESC")
    fun getAllBullyingReports(): Flow<List<BullyingReportWithReporter>>

    @Query("DELETE FROM bullying_reports WHERE reporter_id = :studentId OR victim_id = :studentId OR perpetrator_id = :studentId")
    suspend fun deleteBullyingReportsByStudentId(studentId: Long)

    @Query("SELECT * FROM bullying_reports WHERE status != 'RESOLVED' AND status != 'CLOSED'")
    suspend fun getPendingBullyingReportsSync(): List<BullyingReport>

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<Notification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    fun getUnreadNotificationCount(userId: Long): Flow<Int>

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    suspend fun deleteNotificationsByUserId(userId: Long)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: Long)

    // Attendance
    @Query("DELETE FROM attendances WHERE student_id = :studentId")
    suspend fun deleteAttendancesByStudentId(studentId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Query("SELECT * FROM attendances WHERE student_id = :studentId ORDER BY date DESC")
    fun getAttendanceHistory(studentId: Long): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE student_id = :studentId AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getAttendanceForDate(studentId: Long, startOfDay: Long, endOfDay: Long): Attendance?

    // Teacher & Class Queries
    @Query("SELECT * FROM teachers WHERE user_id = :userId")
    suspend fun getTeacherByUserId(userId: Long): Teacher?

    @Query("SELECT * FROM classes WHERE homeroom_teacher_id = :teacherId")
    suspend fun getClassByHomeroomTeacherId(teacherId: Long): ClassEntity?

    @Transaction
    @Query("SELECT * FROM students WHERE class_id = :classId ORDER BY (SELECT full_name FROM users WHERE users.id = students.user_id) ASC")
    fun getStudentsWithUserByClassId(classId: Long): Flow<List<StudentWithUser>>

    @Query("SELECT * FROM attendances WHERE student_id IN (:studentIds) AND date >= :startDate AND date <= :endDate")
    fun getAttendanceForStudents(studentIds: List<Long>, startDate: Long, endDate: Long): Flow<List<Attendance>>

    // Literacy Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiteracyLog(log: LiteracyLog): Long

    @Update
    suspend fun updateLiteracyLog(log: LiteracyLog)

    @Delete
    suspend fun deleteLiteracyLog(log: LiteracyLog)

    @Query("DELETE FROM literacy_logs WHERE student_id = :studentId")
    suspend fun deleteLiteracyLogsByStudentId(studentId: Long)

    @Query("SELECT * FROM literacy_logs WHERE student_id = :studentId ORDER BY submission_date DESC")
    fun getLiteracyLogsByStudent(studentId: Long): Flow<List<LiteracyLog>>

    @Transaction
    @Query("SELECT * FROM literacy_logs WHERE student_id IN (:studentIds) AND status = :status ORDER BY submission_date ASC")
    fun getLiteracyLogsByStudentsAndStatus(studentIds: List<Long>, status: SubmissionStatus): Flow<List<LiteracyLogWithDetails>>

    @Transaction
    @Query("SELECT * FROM literacy_logs WHERE student_id IN (:studentIds) ORDER BY submission_date DESC")
    fun getLiteracyLogsByStudents(studentIds: List<Long>): Flow<List<LiteracyLogWithDetails>>

    @Query("SELECT * FROM literacy_logs WHERE status = 'PENDING'")
    suspend fun getPendingLiteracyLogsSync(): List<LiteracyLog>

    // Literacy Tasks (Weekly Challenge)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiteracyTask(task: LiteracyTask)

    @Query("DELETE FROM literacy_tasks")
    suspend fun deleteAllLiteracyTasks()

    @Query("SELECT * FROM literacy_tasks WHERE createdAt = :createdAt LIMIT 1")
    suspend fun getLiteracyTaskByCreatedAt(createdAt: Long): LiteracyTask?

    @Query("SELECT * FROM literacy_tasks WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveLiteracyTask(): Flow<LiteracyTask?>

    @Query("SELECT * FROM school_information LIMIT 1")
    fun getSchoolInformation(): Flow<SchoolInformation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolInformation(info: SchoolInformation)

    // School Schedule
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolSchedule(schedule: SchoolSchedule)

    @Query("SELECT * FROM school_schedules ORDER BY dayOfWeek ASC")
    fun getSchoolSchedules(): Flow<List<SchoolSchedule>>

    @Query("SELECT * FROM school_schedules WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    suspend fun getSchoolScheduleByDay(dayOfWeek: Int): SchoolSchedule?

    // Virtual Pet
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVirtualPet(pet: VirtualPet): Long

    @Update
    suspend fun updateVirtualPet(pet: VirtualPet)

    @Query("SELECT * FROM virtual_pets WHERE student_id = :studentId LIMIT 1")
    fun getVirtualPetByStudentId(studentId: Long): Flow<VirtualPet?>

    // Pet Quests
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetQuest(quest: PetQuest): Long

    @Update
    suspend fun updatePetQuest(quest: PetQuest)

    @Query("SELECT * FROM pet_quests WHERE pet_id = :petId")
    fun getPetQuests(petId: Long): Flow<List<PetQuest>>

    @Query("DELETE FROM pet_quests WHERE pet_id = :petId")
    suspend fun deletePetQuests(petId: Long)

    // Pet Achievements
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetAchievement(achievement: PetAchievement): Long

    @Update
    suspend fun updatePetAchievement(achievement: PetAchievement)

    @Query("SELECT * FROM pet_achievements WHERE pet_id = :petId")
    fun getPetAchievements(petId: Long): Flow<List<PetAchievement>>

    // Habits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog): Long

    @Update
    suspend fun updateHabitLog(log: HabitLog)

    @Query("SELECT * FROM habit_logs WHERE student_id = :studentId AND date >= :startDate AND date <= :endDate")
    fun getHabitLogsByStudentAndDateRange(studentId: Long, startDate: Long, endDate: Long): Flow<List<HabitLog>>
}
