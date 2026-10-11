package com.splitease.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.splitease.app.MainActivity
import com.splitease.app.SplitEaseApp

/** Local notification for payment requests / bill reminders. Fired via [Notifier.show]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifier.show(
            context,
            intent.getStringExtra("title") ?: "SplitEase",
            intent.getStringExtra("text") ?: "You have a pending payment",
            intent.getIntExtra("id", 1),
        )
    }
}

object Notifier {
    fun show(context: Context, title: String, text: String, id: Int) {
        // Android 13+: POST_NOTIFICATIONS is a runtime permission.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, SplitEaseApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
