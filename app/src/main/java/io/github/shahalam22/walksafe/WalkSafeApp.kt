package io.github.shahalam22.walksafe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WalkSafeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            GUIDANCE_CHANNEL,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val GUIDANCE_CHANNEL = "guidance"
    }
}
