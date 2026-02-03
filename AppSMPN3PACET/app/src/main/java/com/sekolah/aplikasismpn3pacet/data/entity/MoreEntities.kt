package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sekolah.aplikasismpn3pacet.data.*
import java.util.Date

@Entity(
    tableName = "books",
    indices = [Index(value = ["isbn"], unique = true)]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isbn: String,
    val title: String,
    val author: String,
    val publisher: String?,
    @ColumnInfo(name = "publication_year") val publicationYear: Int?,
    val category: String?,
    @ColumnInfo(name = "cover_image") val coverImage: String?,
    val description: String?,
    @ColumnInfo(name = "total_copies") val totalCopies: Int,
    @ColumnInfo(name = "available_copies") val availableCopies: Int,
    val location: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "book_loans",
    foreignKeys = [
        ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["book_id"]),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"])
    ],
    indices = [Index("book_id"), Index("student_id")]
)
data class BookLoan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "loan_date") val loanDate: Date,
    @ColumnInfo(name = "due_date") val dueDate: Date,
    @ColumnInfo(name = "return_date") val returnDate: Date?,
    val status: BookLoanStatus,
    @ColumnInfo(name = "fine_amount") val fineAmount: Double = 0.0,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendances",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["recorded_by"])
    ],
    indices = [Index("student_id"), Index("recorded_by")]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_id") val studentId: Long,
    val date: Date,
    val status: AttendanceStatus,
    @ColumnInfo(name = "check_in_time") val checkInTime: String?, // TIME type in SQL, string or long here
    @ColumnInfo(name = "check_in_method") val checkInMethod: CheckInMethod?,
    val notes: String?,
    @ColumnInfo(name = "proof_document") val proofDocument: String?,
    @ColumnInfo(name = "recorded_by") val recordedBy: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "discipline_rules")
data class DisciplineRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "rule_name") val ruleName: String,
    val category: RuleCategory,
    val points: Int,
    val severity: RuleSeverity,
    val description: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "discipline_records",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"]),
        ForeignKey(entity = DisciplineRule::class, parentColumns = ["id"], childColumns = ["rule_id"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["recorded_by"])
    ],
    indices = [Index("student_id"), Index("rule_id"), Index("recorded_by")]
)
data class DisciplineRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    val date: Date,
    val points: Int,
    val description: String?,
    val evidence: String?, // URL or path
    @ColumnInfo(name = "recorded_by") val recordedBy: Long?,
    val status: RecordStatus,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "literacy_logs",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["teacher_id"])
    ],
    indices = [Index("student_id"), Index("teacher_id")]
)
data class LiteracyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "book_title") val bookTitle: String,
    val author: String,
    @ColumnInfo(name = "reading_duration") val readingDuration: String,
    val summary: String,
    @ColumnInfo(name = "submission_date") val submissionDate: Date,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val grade: String? = null, // e.g., "A", "85", "Baik"
    val feedback: String? = null,
    @ColumnInfo(name = "teacher_id") val teacherId: Long? = null, // Who graded it
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
