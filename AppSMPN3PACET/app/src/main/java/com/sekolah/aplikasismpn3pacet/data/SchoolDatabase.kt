package com.sekolah.aplikasismpn3pacet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sekolah.aplikasismpn3pacet.data.entity.*

@Database(
    entities = [
        User::class,
        Student::class,
        Teacher::class,
        ClassEntity::class,
        Book::class,
        BookLoan::class,
        Attendance::class,
        DisciplineRule::class,
        DisciplineRecord::class,
        VirtualPet::class,
        PetActivity::class,
        PetQuest::class,
        PetAchievement::class,
        BullyingReport::class,
        Notification::class,
        SchoolInformation::class,
        HabitLog::class,
        LiteracyLog::class,
        LiteracyTask::class,
        SchoolSchedule::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SchoolDatabase : RoomDatabase() {

    abstract fun schoolDao(): SchoolDao

    companion object {
        @Volatile
        private var INSTANCE: SchoolDatabase? = null

        fun getDatabase(context: Context): SchoolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SchoolDatabase::class.java,
                    "school_database"
                )
                .fallbackToDestructiveMigration() // For development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
