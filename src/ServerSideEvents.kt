package info.benjaminhill

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.seconds

@ExperimentalTime
val launchTime = TimeSource.Monotonic.markNow()

/** Maybe to differentiate launches? */
private val rndStr = (('a'..'z') + ('A'..'Z') + ('0'..'9')).let { charPool ->
    (1..10)
        .map { _ -> Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

@ExperimentalTime
private val flowOfEvents = flow {
    while (true) {
        delay(1.seconds)
        emit(
            SseEvent(
                data = "demo data ${System.currentTimeMillis()}",
                event = "update",
                id = "id_${rndStr}_${launchTime.elapsedNow().inMicroseconds.toBigDecimal().toPlainString()}"
            )
        )
    }
}
    .distinctUntilChanged()
    .conflate()


@ExperimentalTime
internal suspend fun PipelineContext<Unit, ApplicationCall>.doSseGet() {
    println("doSseGet")
    call.response.cacheControl(CacheControl.NoCache(null))
    call.response.header("X-Accel-Buffering", "no")

    // serializing them in a way that is compatible with the Server-Sent Events specification.
    // https://www.html5rocks.com/en/tutorials/eventsource/basics/
    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
        flowOfEvents
            .collect { event ->
                println("Collected event: $event")
                if (event.id != null) {
                    write("id: ${event.id}\n")
                }
                if (event.event != null) {
                    write("event: ${event.event}\n")
                }
                for (dataLine in event.data.lines()) {
                    write("data: $dataLine\n")
                }
                write("\n")
                flush()
            }
    }
}

/**
 * The data class representing a SSE Event that will be sent to the client.
 */
private data class SseEvent(
    val data: String,
    val event: String? = null,
    val id: String? = null
)
