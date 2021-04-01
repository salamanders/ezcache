package info.benjaminhill

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.date.*
import kotlin.time.ExperimentalTime

// Can't use CIO for ServerSideEvents: https://youtrack.jetbrains.com/issue/KTOR-605
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
fun Application.module(testing: Boolean = false) {

    routing {

        get("/ts") { doTsGet() }
        put("/cache") { doCachePut() }
        post("/cache") { doCachePut() }
        get("/cache") { doCacheGet() }
        get("/sse") { doSseGet() }

        static("/") { resources(resourcePackage = "static") }
        defaultResource(resource = "index.html", resourcePackage = "static")
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            println("Caught an exception: $cause")
            call.respondText(
                text = cause.message ?: "unknown issue",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(ConditionalHeaders)

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(CachingHeaders) {
        options { outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.CSS -> CachingOptions(
                    CacheControl.MaxAge(maxAgeSeconds = 24 * 60 * 60),
                    expires = null as? GMTDate?
                )
                else -> null
            }
        }
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(PartialContent) {
        // Maximum number of ranges that will be accepted from a HTTP request.
        // If the HTTP request specifies more ranges, they will all be merged into a single range.
        maxRangeCount = 10
    }

    install(ContentNegotiation) {
        gson {
        }
    }


}

