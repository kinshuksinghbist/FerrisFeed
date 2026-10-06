package com.ferrisfeed.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import com.ferrisfeed.data.FerrisDatabase
import com.ferrisfeed.data.ProgressStore
import com.ferrisfeed.data.ReelDao
import com.ferrisfeed.feed.DefaultFeedRepository
import com.ferrisfeed.feed.FeedRepository
import com.ferrisfeed.feed.ReelLocalDataSource
import com.ferrisfeed.feed.RoomReelDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-level bindings. Only references types that actually exist:
 * Room + DataStore live in :data, the feed repository + its Room bridge
 * live in :feature-feed. There is no ProgressDao (SRS columns live on the
 * reel rows and are updated through [ReelDao]) and no PathRepository (the
 * path screen renders [defaultPathNodes] until the engine is wired).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FerrisDatabase = FerrisDatabase.getInstance(context)

    @Provides
    fun provideReelDao(db: FerrisDatabase): ReelDao = db.reelDao()

    /**
     * Feed scroll position + saved/liked sets. A separate file from
     * ProgressStore's "ferris_progress": two DataStore instances must never
     * share one file.
     */
    @Provides
    @Singleton
    fun provideFeedDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("ferris_feed")
        }

    @Provides
    @Singleton
    fun provideProgressStore(
        @ApplicationContext context: Context
    ): ProgressStore = ProgressStore(context)

    @Provides
    @Singleton
    fun provideReelLocalDataSource(
        dao: ReelDao,
        dataStore: DataStore<Preferences>
    ): ReelLocalDataSource = RoomReelDataSource(dao, dataStore)

    @Provides
    @Singleton
    fun provideFeedRepository(
        local: ReelLocalDataSource
    ): FeedRepository = DefaultFeedRepository(local)

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
