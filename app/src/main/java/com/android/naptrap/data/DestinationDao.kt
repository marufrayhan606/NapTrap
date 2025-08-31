package com.android.naptrap.data

import androidx.room.*

@Dao
interface DestinationDao {
    @Query("SELECT * FROM destinations")
    suspend fun getAllDestinations(): List<Destination>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(destination: Destination)

    @Delete
    suspend fun deleteDestination(destination: Destination)
}
