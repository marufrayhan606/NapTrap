package com.android.naptrap.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationRepository @Inject constructor(
    private val dao: DestinationDao
) {
    suspend fun getAllDestinations() = dao.getAllDestinations()
    suspend fun insertDestination(destination: Destination) = dao.insertDestination(destination)
    suspend fun deleteDestination(destination: Destination) = dao.deleteDestination(destination)
    suspend fun updateTracking(id: Int, isTracked: Boolean) = dao.updateTracking(id, isTracked)
}
