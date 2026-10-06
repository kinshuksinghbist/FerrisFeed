package com.ferrisfeed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Offline-first database. Ships pre-populated from the APK asset so the app
 * works fully in airplane mode on first launch (see docs/offline.md).
 *
 * - `reels` holds 400+ curated reels baked at release time.
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
        const val ASSET_PATH = "databases/ferris.db"
        const val DB_NAME = "ferris.db"

        @Volatile
        private var instance: FerrisDatabase? = null

        fun getInstance(context: Context): FerrisDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): FerrisDatabase {
            return Room.databaseBuilder(context, FerrisDatabase::class.java, DB_NAME)
                // Prepack: 400+ reels in APK assets, verified by release checklist.
                .createFromAsset(ASSET_PATH)
                .fallbackToDestructiveMigrationOnDowngrade()
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
