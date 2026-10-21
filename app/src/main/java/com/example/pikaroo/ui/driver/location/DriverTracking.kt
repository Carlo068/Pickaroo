package com.example.pikaroo.ui.driver.location

import com.example.pikaroo.common.preferences.StringStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrackingState(
    val isActive: Boolean = false,
    val orderId: String? = null,
    val lastFix: GeoFix? = null,
    val stopReason: StopReason? = null
)

/** Lo que el servicio de seguimiento le cuenta a la pantalla: si está compartiendo y la última posición. */
class DriverTracking {
    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    fun started(orderId: String) {
        _state.value = TrackingState(isActive = true, orderId = orderId)
    }

    fun fix(fix: GeoFix) {
        _state.update { it.copy(lastFix = fix) }
    }

    fun stopped(reason: StopReason) {
        _state.update { it.copy(isActive = false, stopReason = reason) }
    }
}

enum class LocationMode { GPS, SIMULATED }

/** El modo simulado solo existe para demos y pruebas; por defecto siempre se usa el GPS real. */
class DriverLocationPreferences(private val storage: StringStore) {
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<LocationMode> = _mode.asStateFlow()

    fun setMode(mode: LocationMode) {
        storage.put(KEY_MODE, mode.name)
        _mode.value = mode
    }

    private fun read(): LocationMode =
        LocationMode.entries.firstOrNull { it.name == storage.get(KEY_MODE) } ?: LocationMode.GPS

    private companion object {
        const val KEY_MODE = "driver_location_mode"
    }
}
