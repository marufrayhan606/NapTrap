package com.android.naptrap.di

import android.content.Context
import androidx.room.Room
import com.android.naptrap.data.AppDatabase
import com.android.naptrap.data.DestinationDao
import com.android.naptrap.data.DestinationRepository
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
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "naptrap_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDestinationDao(db: AppDatabase): DestinationDao = db.destinationDao()

    @Provides
    @Singleton
    fun provideDestinationRepository(dao: DestinationDao): DestinationRepository = DestinationRepository(dao)
}
