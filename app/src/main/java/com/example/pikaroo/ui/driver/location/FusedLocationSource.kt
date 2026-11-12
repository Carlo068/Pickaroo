package com.example.pikaroo.ui.driver.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** GPS real del teléfono con Google Fused Location (mejor precisión y batería que el LocationManager). */
class FusedLocationSource(
    context: Context,
    private val intervalMillis: Long = 3_000,
    private val now: () -> Long = System::currentTimeMillis
) : LocationSource {

    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    override fun fixes(): Flow<GeoFix> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(appContext)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_M)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(GeoFix(it.latitude, it.longitude, it.time)) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
            // Una posición reciente evita esperar al primer ciclo del GPS.
            client.lastLocation.addOnSuccessListener { last ->
                if (last != null && now() - last.time < MAX_LAST_FIX_AGE_MS) {
                    trySend(GeoFix(last.latitude, last.longitude, last.time))
                }
            }
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val MIN_UPDATE_INTERVAL_MS = 1_000L
        const val MIN_UPDATE_DISTANCE_M = 5f
        const val MAX_LAST_FIX_AGE_MS = 2 * 60_000L
    }
}
