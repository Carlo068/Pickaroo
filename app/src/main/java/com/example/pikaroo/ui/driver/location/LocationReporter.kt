package com.example.pikaroo.ui.driver.location

import android.os.SystemClock
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.driver.data.DriverRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

enum class StopReason {
    /** La entrega terminó o ya no es del repartidor: el servidor rechazó la posición (403, 404 o 409). */
    DELIVERY_ENDED,
    SESSION_EXPIRED,
    NO_PERMISSION,
    /** La fuente de ubicación terminó o falló. */
    SOURCE_STOPPED
}

private class StopSignal(val reason: StopReason) : RuntimeException()

/** Envía al servidor las posiciones de [source], sin pasar de una por [minIntervalMillis]. */
class LocationReporter(
    private val repository: DriverRepository,
    private val source: LocationSource,
    private val minIntervalMillis: Long = 3_000,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val onFix: (GeoFix) -> Unit = {}
) {

    /** Reporta hasta que algo lo detiene y devuelve por qué; se cancela con la corrutina. */
    suspend fun run(orderId: String): StopReason = try {
        var lastSentAt: Long? = null
        source.fixes().collect { fix ->
            onFix(fix)
            val now = clock()
            if (lastSentAt != null && now - lastSentAt!! < minIntervalMillis) return@collect
            if (send(orderId, fix)) lastSentAt = now
        }
        StopReason.SOURCE_STOPPED
    } catch (e: StopSignal) {
        e.reason
    } catch (e: CancellationException) {
        throw e
    } catch (_: SecurityException) {
        StopReason.NO_PERMISSION
    } catch (_: Exception) {
        StopReason.SOURCE_STOPPED
    }

    /** True si el servidor la aceptó; un fallo pasajero se reintenta con la siguiente posición. */
    private suspend fun send(orderId: String, fix: GeoFix): Boolean = try {
        repository.reportLocation(orderId, fix.lat, fix.lng)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: SessionExpiredException) {
        throw StopSignal(StopReason.SESSION_EXPIRED)
    } catch (e: HttpException) {
        if (e.code() in ENDED_CODES) throw StopSignal(StopReason.DELIVERY_ENDED)
        false
    } catch (_: Exception) {
        false
    }

    private companion object {
        val ENDED_CODES = setOf(403, 404, 409)
    }
}
