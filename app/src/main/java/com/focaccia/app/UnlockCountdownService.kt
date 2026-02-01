package com.focaccia.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UnlockCountdownService : Service() {

    companion object {
        private const val CHANNEL_ID = "unlock_timer"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_RELOCK = "com.focaccia.app.ACTION_RELOCK"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RELOCK) {
            BlockedAppsRepository.setBlockingDisabledUntil(this, 0L)
            sendBroadcast(Intent(ACTION_RELOCK).setPackage(packageName))
            stopSelf()
            return START_NOT_STICKY
        }

        val until = BlockedAppsRepository.getBlockingDisabledUntil(this)
        val remaining = until - System.currentTimeMillis()
        if (remaining <= 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(remaining))

        scope.launch {
            while (true) {
                val left = until - System.currentTimeMillis()
                if (left <= 0) break
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(left))
                delay(1000)
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Unlock Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows countdown while apps are unblocked"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timeText = "%d:%02d".format(minutes, seconds)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val relockIntent = Intent(this, UnlockCountdownService::class.java).apply {
            action = ACTION_RELOCK
        }
        val relockPendingIntent = PendingIntent.getService(
            this, 1, relockIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Apps unblocked")
            .setContentText("$timeText remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_lock_lock, "Reblock", relockPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
