package com.example.pikaroo.common.mock

import android.content.Context
import com.example.pikaroo.common.preferences.PreferencesStringStore
import kotlinx.coroutines.delay
import retrofit2.HttpException
import kotlin.random.Random

/**
 * Stands in for PickarooAPI. Every request goes through [call], which adds network-like latency,
 * can fail on demand, and gives the block access to the [MockStore].
 */
class MockPickarooApi(
    private val store: MockStore,
    private val latencyMillis: () -> Long = { Random.nextLong(MIN_LATENCY_MS, MAX_LATENCY_MS + 1) }
) {

    private val injectedFailures = ArrayDeque<() -> HttpException>()

    suspend fun <T> call(block: MockStore.() -> T): T {
        delay(latencyMillis())
        val failure = synchronized(injectedFailures) { injectedFailures.removeFirstOrNull() }
        if (failure != null) throw failure()
        return store.block()
    }

    fun failNext(times: Int = 1, error: () -> HttpException) {
        synchronized(injectedFailures) { repeat(times) { injectedFailures.addLast(error) } }
    }

    fun reset() {
        synchronized(injectedFailures) { injectedFailures.clear() }
        store.reset()
    }

    companion object {
        const val MIN_LATENCY_MS = 300L
        const val MAX_LATENCY_MS = 600L

        fun create(context: Context, latencyMillis: () -> Long = { Random.nextLong(MIN_LATENCY_MS, MAX_LATENCY_MS + 1) }) =
            MockPickarooApi(MockStore(AssetSeedSource(context), PreferencesStringStore(context)), latencyMillis)
    }
}
