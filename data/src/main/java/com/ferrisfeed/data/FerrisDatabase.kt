package com.ferrisfeed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Offline-first database. Populated on first launch from the validated
 * curriculum JSON bundled in APK assets (see [CurriculumSeeder]) — there is
 * deliberately NO `createFromAsset("...db")` call: a prepackaged SQLite file
 * must carry Room's exact schema identity hash, and a missing/stale one
 * crashes on EVERY launch instead of degrading. Seeding from JSON lets Room
 * own its schema. A release pipeline may reintroduce a prebaked DB later;
 * until then this is the single supported path.
 *
 * - `reels` holds 400+ curated reels seeded at first launch.
 * - `reel_fts` is the FTS4 index, rebuilt automatically by Room triggers.
 * - Weekly [CurriculumSyncWorker] REPLACE-upserts new packs; user SRS columns
 *   for existing rows are preserved by merging (see worker), never overwritten.
 */
@Database(
    entities = [ReelEntity::class, ReelFts::class],
    version = 1,
    exportSchema = true,
)
abstract class FerrisDatabase : RoomDatabase() {
    abstract fun reelDao(): ReelDao

    companion object {
        const val DB_NAME = "ferris.db"

        @Volatile
        private var instance: FerrisDatabase? = null

        /** App-lifetime scope for background DB work (seeding). */
        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): FerrisDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): FerrisDatabase {
            return Room.databaseBuilder(context, FerrisDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // First launch ever: fill Room from bundled JSON off
                        // the opening thread. Feed observes the table and
                        // waits for rows (see FeedViewModel init).
                        ioScope.launch {
                            runCatching {
                                val database = getInstance(context)
                                CurriculumSeeder(context, database.reelDao())
                                    .seedIfEmpty()
                            }
                        }
                    }
                })
                .build()
        }

        /** Test / preview helper: in-memory DB with no asset dependency. */
        fun inMemory(context: Context): FerrisDatabase {
            return Room.inMemoryDatabaseBuilder(context, FerrisDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
