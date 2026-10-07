package com.ferrisfeed.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FerrisApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // TODO 28b: schedule the weekly curriculum sync + the 6h widget
        // refresh. KEEP (not REPLACE) so a reinstall never double-enqueues.
        com.ferrisfeed.data.CurriculumSyncWorker.schedule(this)
        com.ferrisfeed.feed.WidgetRefreshWorker.schedule(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val streakChannel = NotificationChannel(
            STREAK_CHANNEL_ID,
            "Streak reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily 9pm nudge with an unsolved trap question."
        }
        val syncChannel = NotificationChannel(
            SYNC_CHANNEL_ID,
            "Content sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Weekly curriculum pack downloads."
        }
        manager.createNotificationChannels(listOf(streakChannel, syncChannel))
    }

    companion object {
        const val STREAK_CHANNEL_ID = "streak_reminders"
        const val SYNC_CHANNEL_ID = "content_sync"
    }
}
