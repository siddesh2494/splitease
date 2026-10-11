package com.splitease.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.splitease.app.data.repository.SplitRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SplitEaseApp : Application() {

    @Inject lateinit var repo: SplitRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        // Offline-first: materialize any recurring expenses that came due while the app was closed.
        appScope.launch { repo.materializeRecurring() }
    }

    companion object { const val CHANNEL_REMINDERS = "reminders" }
}
