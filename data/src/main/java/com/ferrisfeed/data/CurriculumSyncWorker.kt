package com.ferrisfeed.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Weekly background sync for new curriculum packs + offline asset warming.
 *
 * - Downloads `packs/manifest.json` from the CDN, then any pack newer than
 *   [ProgressStore.getLastSync].
 * - Inserts new reels with INSERT IGNORE semantics: existing rows (which
 *   carry the user's SRS columns) are never overwritten. Pure content fixes
 *   ship with the next full DB asset instead.
 * - Warms the HTTP disk cache for images referenced by new reels so they
 *   render in airplane mode (see docs/offline.md). Coil prefetch is best
 *   effort; failures never fail the worker.
 *
 * Schedule: 7 days, requires unmetered network + battery not low +
 * storage not low. Manual "Check for new reels" in Settings calls
 * [syncNow] via a one-shot path in the same worker family.
 */
class CurriculumSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            val db = FerrisDatabase.getInstance(applicationContext)
            val store = ProgressStore(applicationContext)
            val lastSync = store.getLastSync()

            val manifest = fetchManifest()
            val freshPacks = manifest.packs.filter { it.updatedAtMillis > lastSync }

            var inserted = 0
            for (pack in freshPacks) {
                val reels = fetchPack(pack.url).reels.map { it.toEntity() }
                // Preserve SRS: only insert rows whose id is not already present.
                val newOnes = reels.filter { db.reelDao().getById(it.id) == null }
                if (newOnes.isNotEmpty()) {
                    db.reelDao().upsertAll(newOnes)
                    inserted += newOnes.size
                }
                warmImageCache(newOnes.mapNotNull { it.imageAsset })
            }

            store.setLastSync(System.currentTimeMillis())
            // Retry nothing: empty sync is a success; server errors retry.
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private fun fetchManifest(): PackManifest {
        val url = MANIFEST_URL
        val text = URL(url).readText()
        return json.decodeFromString(PackManifest.serializer(), text)
    }

    private fun fetchPack(url: String): PackPayload {
        val text = URL(url).readText()
        return json.decodeFromString(PackPayload.serializer(), text)
    }

    /** Best-effort disk-cache warm: HEAD each asset so the HTTP cache holds it. */
    private fun warmImageCache(assetPaths: List<String>) {
        for (path in assetPaths) {
            runCatching {
                val conn = URL("$ASSET_BASE/$path").openConnection()
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                conn.getInputStream().use { it.readBytes() }
            }
        }
    }

    companion object {
        const val WORK_NAME = "curriculum-weekly-sync"
        const val MAX_ATTEMPTS = 3
        // Pack CDN. Overridden per flavor via BuildConfig in real builds.
        const val MANIFEST_URL = "https://cdn.ferrisfeed.app/packs/manifest.json"
        const val ASSET_BASE = "https://cdn.ferrisfeed.app/assets"

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CurriculumSyncWorker>(7, TimeUnit.DAYS)
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

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

@Serializable
data class PackManifest(
    val packs: List<PackRef> = emptyList(),
)

@Serializable
data class PackRef(
    val id: String,
    val url: String,
    val updatedAtMillis: Long,
    val minDbVersion: Int = 1,
)

@Serializable
data class PackPayload(
    val reels: List<PackReel> = emptyList(),
)

@Serializable
data class PackReel(
    val id: String,
    val track: String,
    val level: Int,
    val topic: String,
    val topic_label: String = "",
    val hook: String,
    val bodyMd: String,
    val code: String? = null,
    val language: String = "rust",
    val takeaway: String,
    val trap: String = "",
    val quizJson: String? = null,
    val tagsCsv: String = "",
    val readTimeSec: Int = 60,
    val orderIndex: Int = 0,
    val imageAsset: String? = null,
) {
    fun toEntity(): ReelEntity = ReelEntity(
        id = id,
        track = track,
        level = level,
        // TODO 21: prefer the human label; the pack's topic key is the
        // fallback (same rule the asset seeder applies).
        topic = topic_label.trim().ifEmpty { topic },
        hook = hook,
        bodyMd = bodyMd,
        code = code,
        language = language,
        takeaway = takeaway,
        trap = trap,
        quizJson = quizJson,
        tagsCsv = tagsCsv,
        readTimeSec = readTimeSec,
        orderIndex = orderIndex,
        imageAsset = imageAsset,
    )
}
