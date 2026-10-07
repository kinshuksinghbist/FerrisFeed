package com.ferrisfeed.feed

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ferrisfeed.data.ProgressStore
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Widget refresh worker (TODO 28b).
 *
 * 6h tick: resolves the reel of the day (currently the [WidgetReelProvider.NoOp]
 * fallback; Room Daily-Mix-head binding lands with the FeedModule DI line)
 * plus the live streak from [ProgressStore], then pushes state to every
 * installed [ReelOfDayWidget] instance. Best-effort: any failure retries
 * with exponential backoff, never crashes the app.
 *
 * Plain [CoroutineWorker] (not a Hilt worker) so it survives the
 * HiltWorkerFactory delegating setup in [com.ferrisfeed.app.FerrisApp].
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val store = ProgressStore(applicationContext)
            val streak = runCatching { store.streakDays.first() }.getOrDefault(0)
            val reel = runCatching { WidgetReelProvider.NoOp.reelOfDay() }
                .getOrNull() ?: return Result.retry()
            val manager = GlanceAppWidgetManager(applicationContext)
            val ids = runCatching {
                manager.getGlanceIds(ReelOfDayWidget::class.java)
            }.getOrDefault(emptyList())
            ids.forEach { id ->
                runCatching { ReelOfDayWidget.refresh(applicationContext, id, reel, streak) }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "widget-refresh"
        const val MAX_ATTEMPTS = 3

        private val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
