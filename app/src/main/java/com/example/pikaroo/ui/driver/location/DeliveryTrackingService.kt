package com.example.pikaroo.ui.driver.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.pikaroo.MainActivity
import com.example.pikaroo.PikarooApp
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que comparte la posición del repartidor mientras lleva un pedido, aunque
 * la app esté en segundo plano o la pantalla apagada. Se detiene solo cuando la entrega termina.
 */
class DeliveryTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null
    private var runningOrder: String? = null
    private var runningMode: LocationMode? = null

    private val app get() = application as PikarooApp

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val orderId = intent?.getStringExtra(EXTRA_ORDER_ID)
        if (orderId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()

        val mode = app.container.driverLocationPreferences.mode.value
        if (running?.isActive != true || runningOrder != orderId || runningMode != mode) {
            running?.cancel()
            runningOrder = orderId
            runningMode = mode
            running = scope.launch { track(orderId) }
        }
        return START_NOT_STICKY
    }

    private suspend fun track(orderId: String) {
        val tracking = app.container.driverTracking
        tracking.started(orderId)
        val reason = try {
            val source = app.locationSourceFactory.create()
            LocationReporter(app.container.driverRepository, source, onFix = tracking::fix).run(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: SessionExpiredException) {
            StopReason.SESSION_EXPIRED
        } catch (_: Exception) {
            StopReason.SOURCE_STOPPED
        }
        tracking.stopped(reason)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        if (app.container.driverTracking.state.value.isActive) {
            app.container.driverTracking.stopped(StopReason.SOURCE_STOPPED)
        }
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Entrega en curso", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Entrega en curso")
            .setContentText("Compartiendo tu ubicación con el cliente")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val EXTRA_ORDER_ID = "order_id"
        private const val CHANNEL_ID = "delivery_tracking"
        private const val NOTIFICATION_ID = 4101

        /** Llamar con la app visible: Android no deja arrancar servicios en primer plano desde segundo plano. */
        fun start(context: Context, orderId: String) {
            val intent = Intent(context, DeliveryTrackingService::class.java).putExtra(EXTRA_ORDER_ID, orderId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DeliveryTrackingService::class.java))
        }
    }
}
