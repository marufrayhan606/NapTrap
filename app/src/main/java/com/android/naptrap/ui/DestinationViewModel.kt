package com.android.naptrap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.naptrap.data.Destination
import com.android.naptrap.data.DestinationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DestinationViewModel @Inject constructor(
    private val repository: DestinationRepository
) : ViewModel() {
    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations

    fun loadDestinations() {
        viewModelScope.launch {
            _destinations.value = repository.getAllDestinations()
        }
    }

    fun addDestination(destination: Destination) {
        viewModelScope.launch {
            repository.insertDestination(destination)
            loadDestinations()
        }
    }

    fun removeDestination(destination: Destination) {
        viewModelScope.launch {
            repository.deleteDestination(destination)
            loadDestinations()
        }
    }

    fun updateTracking(id: Int, isTracked: Boolean) {
        viewModelScope.launch {
            repository.updateTracking(id, isTracked)
            loadDestinations()
        }
    }
}
