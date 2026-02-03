package com.sekolah.aplikasismpn3pacet.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sekolah.aplikasismpn3pacet.data.*
import java.util.Date

@Entity(
    tableName = "virtual_pets",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["student_id"], unique = true)]
)
data class VirtualPet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "pet_name") val petName: String,
    @ColumnInfo(name = "pet_type") val petType: String,
    val level: Int = 1,
    @ColumnInfo(name = "experience_points") val experiencePoints: Int = 0,
    val health: Int = 100,
    val happiness: Int = 100,
    val hunger: Int = 0,
    val intelligence: Int = 50,
    val energy: Int = 100,
    val social: Int = 100,
    val coins: Int = 0,
    @ColumnInfo(name = "last_quest_reset") val lastQuestReset: Long? = null,
    @ColumnInfo(name = "last_fed") val lastFed: Long?,
    @ColumnInfo(name = "last_played") val lastPlayed: Long?,
    val status: PetStatus,
    val accessories: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "school_information")
data class SchoolInformation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "school_name") val schoolName: String,
    val npsn: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radius: Double? = null, // In meters
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pet_activities",
    foreignKeys = [
        ForeignKey(entity = VirtualPet::class, parentColumns = ["id"], childColumns = ["pet_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("pet_id")]
)
data class PetActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    @ColumnInfo(name = "activity_type") val activityType: ActivityType,
    @ColumnInfo(name = "activity_description") val activityDescription: String?,
    @ColumnInfo(name = "xp_earned") val xpEarned: Int,
    @ColumnInfo(name = "health_change") val healthChange: Int,
    @ColumnInfo(name = "happiness_change") val happinessChange: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bullying_reports",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["reporter_id"]),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["victim_id"]),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["perpetrator_id"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["assigned_to"])
    ],
    indices = [Index("reporter_id"), Index("victim_id"), Index("perpetrator_id"), Index("assigned_to")]
)
data class BullyingReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "reporter_id") val reporterId: Long?,
    @ColumnInfo(name = "is_anonymous") val isAnonymous: Boolean,
    @ColumnInfo(name = "victim_id") val victimId: Long?,
    @ColumnInfo(name = "perpetrator_id") val perpetratorId: Long?,
    @ColumnInfo(name = "incident_date") val incidentDate: Date,
    @ColumnInfo(name = "incident_location") val incidentLocation: String?,
    @ColumnInfo(name = "incident_type") val incidentType: IncidentType,
    val description: String?,
    val evidence: String?, // JSON
    val status: ReportStatus,
    val priority: ReportPriority,
    @ColumnInfo(name = "assigned_to") val assignedTo: Long?,
    @ColumnInfo(name = "resolution_notes") val resolutionNotes: String?,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pet_quests",
    foreignKeys = [
        ForeignKey(entity = VirtualPet::class, parentColumns = ["id"], childColumns = ["pet_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("pet_id")]
)
data class PetQuest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val reward: Int,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pet_achievements",
    foreignKeys = [
        ForeignKey(entity = VirtualPet::class, parentColumns = ["id"], childColumns = ["pet_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("pet_id")]
)
data class PetAchievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pet_id") val petId: Long,
    val title: String,
    val description: String,
    val icon: String,
    val unlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["student_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("student_id"), Index(value = ["student_id", "date", "habit_id"], unique = true)]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "habit_id") val habitId: Int, // 1-7
    val date: Long, // Start of day timestamp
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
