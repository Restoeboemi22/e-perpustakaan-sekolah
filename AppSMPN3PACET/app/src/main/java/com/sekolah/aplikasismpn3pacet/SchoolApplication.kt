package com.sekolah.aplikasismpn3pacet

import android.app.Application
import com.sekolah.aplikasismpn3pacet.data.SchoolDatabase
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.UserPreferences

class SchoolApplication : Application() {
    val database by lazy { SchoolDatabase.getDatabase(this) }
    val repository by lazy { SchoolRepository(database.schoolDao()) }
    val userPreferences by lazy { UserPreferences(this) }
}
