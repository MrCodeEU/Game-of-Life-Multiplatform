import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class GameTimer(private val interval: Long, private val onTick: () -> Unit) {
    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(interval)
                onTick()
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}