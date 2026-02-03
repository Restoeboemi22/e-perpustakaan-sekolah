package com.sekolah.aplikasismpn3pacet.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PRIMARY_USER_ID = "primary_user_id"
        private const val KEY_PRIMARY_USER_NAME = "primary_user_name"
    }

    fun saveUserSession(userId: Long) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserSession(): Long? {
        val userId = sharedPreferences.getLong(KEY_USER_ID, -1L)
        return if (userId != -1L) userId else null
    }

    fun clearUserSession() {
        sharedPreferences.edit().remove(KEY_USER_ID).apply()
    }

    // Device Locking Mechanism
    fun savePrimaryUser(userId: Long, username: String) {
        // Only save if not already set (Double check, though ViewModel handles logic)
        if (getPrimaryUserId() == null) {
            sharedPreferences.edit()
                .putLong(KEY_PRIMARY_USER_ID, userId)
                .putString(KEY_PRIMARY_USER_NAME, username)
                .apply()
        }
    }

    fun getPrimaryUserId(): Long? {
        val userId = sharedPreferences.getLong(KEY_PRIMARY_USER_ID, -1L)
        return if (userId != -1L) userId else null
    }

    fun getPrimaryUserName(): String? {
        return sharedPreferences.getString(KEY_PRIMARY_USER_NAME, null)
    }

    fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}
