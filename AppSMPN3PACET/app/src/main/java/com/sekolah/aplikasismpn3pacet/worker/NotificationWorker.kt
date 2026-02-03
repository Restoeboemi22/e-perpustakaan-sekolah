package com.sekolah.aplikasismpn3pacet.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sekolah.aplikasismpn3pacet.MainActivity
import com.sekolah.aplikasismpn3pacet.R
import com.sekolah.aplikasismpn3pacet.SchoolApplication

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val application = applicationContext as SchoolApplication
        val repository = application.repository

        // Fetch pending items
        val pendingBullyingReports = repository.getPendingBullyingReportsSync()
        val pendingLiteracyLogs = repository.getPendingLiteracyLogsSync()

        val bullyingCount = pendingBullyingReports.size
        val literacyCount = pendingLiteracyLogs.size

        if (bullyingCount > 0 || literacyCount > 0) {
            sendNotification(bullyingCount, literacyCount)
        }

        return Result.success()
    }

    private fun sendNotification(bullyingCount: Int, literacyCount: Int) {
        val channelId = "teacher_tasks_channel"
        val notificationId = 1001

        // Create Channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tugas Guru"
            val descriptionText = "Notifikasi tugas tertunda untuk guru"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open App
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build Notification Content
        val title = "Tindakan Diperlukan"
        val messageBuilder = StringBuilder()
        
        if (bullyingCount > 0) {
            messageBuilder.append("$bullyingCount Laporan Bullying belum ditangani. ")
        }
        if (literacyCount > 0) {
            messageBuilder.append("$literacyCount Tugas Literasi belum dinilai.")
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Ensure this resource exists
            .setContentTitle(title)
            .setContentText(messageBuilder.toString())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Show Notification
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
        }
    }
}
