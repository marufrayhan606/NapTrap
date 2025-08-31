package com.android.naptrap.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Destination::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
}
