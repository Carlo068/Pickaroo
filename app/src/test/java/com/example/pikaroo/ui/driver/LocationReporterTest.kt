package com.example.pikaroo.ui.driver

import com.example.pikaroo.common.mock.MockErrors
import com.example.pikaroo.ui.auth.data.SessionExpiredException
import com.example.pikaroo.ui.driver.data.DriverRepository
import com.example.pikaroo.ui.driver.location.GeoFix
import com.example.pikaroo.ui.driver.location.LocationReporter
import com.example.pikaroo.ui.driver.location.LocationSource
import com.example.pikaroo.ui.driver.location.StopReason
import com.example.pikaroo.ui.driver.model.AvailableOrderDto
import com.example.pikaroo.ui.orders.model.OrderDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private class RecordingRepository : DriverRepository {
    data class Report(val orderId: String, val lat: Double, val lng: Double)

    val reports = mutableListOf<Report>()
    val outcomes = ArrayDeque<Exception?>()

    override suspend fun reportLocation(orderId: String, lat: Double, lng: Double) {
        outcomes.removeFirstOrNull()?.let { throw it }
        reports += Report(orderId, lat, lng)
    }

    override suspend fun available(): List<AvailableOrderDto> = TODO()
    override suspend fun current(): OrderDto? = TODO()
    override suspend fun accept(orderId: String): OrderDto = TODO()
    override suspend fun pickup(orderId: String): OrderDto = TODO()
    override suspend fun deliver(orderId: String): OrderDto = TODO()
}

/** Emite una posición cada [gapMillis]; con [endWith] termina lanzando ese error. */
private class ScriptedSource(
    private val count: Int,
    private val gapMillis: Long = 0,
    private val endWith: Exception? = null
) : LocationSource {
    override fun fixes(): Flow<GeoFix> = flow {
        repeat(count) { i ->
            emit(GeoFix(19.0 + i, -99.0 - i, atMillis = i.toLong()))
            delay(gapMillis)
        }
        endWith?.let { throw it }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocationReporterTest {

    private val repository = RecordingRepository()

    private fun reporter(
        source: LocationSource,
        minInterval: Long = 3_000,
        clock: () -> Long,
        onFix: (GeoFix) -> Unit = {}
    ) = LocationReporter(repository, source, minInterval, clock, onFix)

    @Test
    fun `every position is sent when they are further apart than the minimum interval`() = runTest {
        val reporter = reporter(ScriptedSource(count = 3, gapMillis = 5_000), clock = { currentTime })

        val reason = reporter.run("order_1")

        assertEquals(StopReason.SOURCE_STOPPED, reason)
        assertEquals(
            listOf(
                RecordingRepository.Report("order_1", 19.0, -99.0),
                RecordingRepository.Report("order_1", 20.0, -100.0),
                RecordingRepository.Report("order_1", 21.0, -101.0)
            ),
            repository.reports
        )
    }

    @Test
    fun `positions that arrive too soon are not sent`() = runTest {
        val reporter = reporter(ScriptedSource(count = 7, gapMillis = 1_000), minInterval = 3_000, clock = { currentTime })

        reporter.run("order_1")

        assertEquals(listOf(19.0, 22.0, 25.0), repository.reports.map { it.lat })
    }

    @Test
    fun `every position is shown to the driver even when it is not sent`() = runTest {
        val seen = mutableListOf<Double>()
        val reporter = reporter(ScriptedSource(count = 4, gapMillis = 1_000), clock = { currentTime }, onFix = { seen += it.lat })

        reporter.run("order_1")

        assertEquals(listOf(19.0, 20.0, 21.0, 22.0), seen)
        assertEquals(2, repository.reports.size)
    }

    @Test
    fun `a failed send is retried with the very next position`() = runTest {
        repository.outcomes += IOException("offline")
        val reporter = reporter(ScriptedSource(count = 3, gapMillis = 1_000), clock = { currentTime })

        reporter.run("order_1")

        assertEquals(listOf(20.0), repository.reports.map { it.lat })
    }

    @Test
    fun `a server error is also treated as temporary`() = runTest {
        repository.outcomes += MockErrors.http(500, "SERVER_ERROR", "boom")
        val reporter = reporter(ScriptedSource(count = 2, gapMillis = 1_000), clock = { currentTime })

        val reason = reporter.run("order_1")

        assertEquals(StopReason.SOURCE_STOPPED, reason)
        assertEquals(1, repository.reports.size)
    }

    @Test
    fun `reporting stops when the server says the delivery is over`() = runTest {
        for (failure in listOf(MockErrors.conflict("INVALID_STATUS"), MockErrors.forbidden("NOT_YOUR_DELIVERY"), MockErrors.notFound("ORDER_NOT_FOUND"))) {
            repository.reports.clear()
            repository.outcomes += failure
            val reporter = reporter(ScriptedSource(count = 5, gapMillis = 5_000), clock = { currentTime })

            assertEquals(StopReason.DELIVERY_ENDED, reporter.run("order_1"))
            assertTrue(repository.reports.isEmpty())
        }
    }

    @Test
    fun `reporting stops when the session can no longer be renewed`() = runTest {
        repository.outcomes += SessionExpiredException()
        val reporter = reporter(ScriptedSource(count = 5, gapMillis = 5_000), clock = { currentTime })

        assertEquals(StopReason.SESSION_EXPIRED, reporter.run("order_1"))
    }

    @Test
    fun `a location source without permission is reported as such`() = runTest {
        val reporter = reporter(ScriptedSource(count = 0, endWith = SecurityException("no permission")), clock = { currentTime })

        assertEquals(StopReason.NO_PERMISSION, reporter.run("order_1"))
    }

    @Test
    fun `any other source failure stops reporting without crashing`() = runTest {
        val reporter = reporter(ScriptedSource(count = 1, endWith = IllegalStateException("gps off")), clock = { currentTime })

        assertEquals(StopReason.SOURCE_STOPPED, reporter.run("order_1"))
        assertEquals(1, repository.reports.size)
    }

    @Test
    fun `cancelling stops the reporting`() = runTest {
        val reporter = reporter(ScriptedSource(count = 100, gapMillis = 5_000), clock = { currentTime })

        val job = launch { reporter.run("order_1") }
        advanceTimeBy(12_000)
        runCurrent()
        job.cancel()
        val sentWhileRunning = repository.reports.size
        advanceTimeBy(100_000)
        runCurrent()

        assertEquals(3, sentWhileRunning)
        assertEquals(sentWhileRunning, repository.reports.size)
    }
}
