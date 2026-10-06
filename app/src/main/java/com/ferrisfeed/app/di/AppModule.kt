package com.ferrisfeed.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.work.WorkManager
import com.ferrisfeed.data.local.FerrisDatabase
import com.ferrisfeed.data.local.ReelDao
import com.ferrisfeed.data.local.ProgressDao
import com.ferrisfeed.data.progress.ProgressStore
import com.ferrisfeed.data.progress.dataStore
import com.ferrisfeed.data.repo.FeedRepository
import com.ferrisfeed.data.repo.FeedRepositoryImpl
import com.ferrisfeed.data.repo.PathRepository
import com.ferrisfeed.data.repo.PathRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): FerrisDatabase {
        return Room.databaseBuilder(
            context,
            FerrisDatabase::class.java,
            "ferrisfeed.db"
        )
            .createFromAsset("curriculum.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideReelDao(db: FerrisDatabase): ReelDao = db.reelDao()

    @Provides
    fun provideProgressDao(db: FerrisDatabase): ProgressDao = db.progressDao()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideProgressStore(
        dataStore: DataStore<Preferences>
    ): ProgressStore = ProgressStore(dataStore)

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideFeedRepository(
        reelDao: ReelDao,
        progressDao: ProgressDao,
        progressStore: ProgressStore
    ): FeedRepository = FeedRepositoryImpl(reelDao, progressDao, progressStore)

    @Provides
    @Singleton
    fun providePathRepository(
        reelDao: ReelDao,
        progressDao: ProgressDao,
        progressStore: ProgressStore
    ): PathRepository = PathRepositoryImpl(reelDao, progressDao, progressStore)
}

private val Context.dataStore: DataStore<Preferences>
    get() = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { preferencesDataStoreFile("ferris_progress") }
    )
