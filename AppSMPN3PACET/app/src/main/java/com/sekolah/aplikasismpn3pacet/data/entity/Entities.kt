package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sekolah.aplikasismpn3pacet.data.*
import java.util.Date

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true), Index(value = ["email"], unique = true), Index(value = ["nis_nip"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String, // Hashed
    val email: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val role: UserRole,
    @ColumnInfo(name = "nis_nip") val nisNip: String,
    val phone: String?,
    val address: String?,
    val gender: Gender?,
    @ColumnInfo(name = "birth_date") val birthDate: Date?,
    @ColumnInfo(name = "profile_picture") val profilePicture: String?,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long?, // Null means system-wide notification
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    @ColumnInfo(name = "is_read") val isRead: Boolean = false,
    @ColumnInfo(name = "related_feature") val relatedFeature: String? = null, // e.g., "DISCIPLINE", "HOMEWORK"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(entity = Teacher::class, parentColumns = ["id"], childColumns = ["homeroom_teacher_id"])
    ],
    indices = [Index(value = ["class_name"], unique = true)]
)
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "class_name") val className: String,
    @ColumnInfo(name = "grade_level") val gradeLevel: Int,
    @ColumnInfo(name = "homeroom_teacher_id") val homeroomTeacherId: Long?,
    @ColumnInfo(name = "academic_year") val academicYear: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ClassEntity::class, parentColumns = ["id"], childColumns = ["class_id"])
    ],
    indices = [Index("user_id"), Index("class_id")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "class_id") val classId: Long?,
    @ColumnInfo(name = "parent_name") val parentName: String?,
    @ColumnInfo(name = "parent_phone") val parentPhone: String?,
    @ColumnInfo(name = "parent_email") val parentEmail: String?,
    @ColumnInfo(name = "admission_year") val admissionYear: Int?,
    @ColumnInfo(name = "discipline_points") val disciplinePoints: Int = 100, // Default usually starts at 100 or 0 depending on system, schema implies total points.
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "teachers",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["user_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("user_id")]
)
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val subject: String?,
    @ColumnInfo(name = "is_homeroom_teacher") val isHomeroomTeacher: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
