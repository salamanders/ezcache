package info.benjaminhill

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.date.*
import java.net.FileNameMap
import java.net.URLConnection
import java.util.concurrent.TimeUnit


fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module(testing: Boolean = false) {

    routing {

        get("/ts") {
            val fileNames = call.request.queryParameters.getAll("fileName")!!
            require(fileNames.isNotEmpty()) { "Missing at least one 'fileName'"}
            call.respond(fileNames.map {
                it to cache.getIfPresent(it)?.ts
            }.toMap())
        }

        put("/cache") {
            // val postParameters: Parameters = call.receiveParameters()
            val multipart = call.receiveMultipart()
            lateinit var fileName: String
            lateinit var content: ByteArray

            // Processes each part of the multipart input content of the user
            multipart.forEachPart { part ->
                if (part is PartData.FormItem) {
                    when (part.name) {
                        "fileName" -> {
                            fileName = part.value
                        }
                        else -> error("Unknown form part:'${part.name}'")
                    }
                } else if (part is PartData.FileItem) {
                    //val ext = File(part.originalFileName!!).extension
                    part.streamProvider().use {
                        content = it.readAllBytes()
                    }
                }
                part.dispose()
            }
            cache.put(fileName, CachedFile(content = content))
            call.respond(HttpStatusCode.OK) { "Put ${content.size}" }
        }

        get("/cache") {
            val params = call.receiveParameters()
            val fileName = params["fileName"]
            require(fileName !=null) { "Missing required parameter 'fileName'" }
            val cacheFile = cache.getIfPresent(fileName)
            require(cacheFile != null) {  "Unable to locate cached item '$fileName'" }

            val mimeType = fileNameMap.getContentTypeFor(fileName).split("/")
            call.respondBytes(contentType = ContentType(mimeType[0], mimeType[1])) {
                cacheFile.content
            }
        }

        static("/") {
            resources(resourcePackage = "static")
        }
        defaultResource(resource = "index.html", resourcePackage = "static")
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            println("Caught an exception: $cause")
            call.respondText(
                text = cause.message ?: "unknown issue",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.InternalServerError)
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

class CachedFile(
    val ts: Long = System.currentTimeMillis(),
    val content: ByteArray,
)

var cache: Cache<String, CachedFile> = CacheBuilder.newBuilder()
    .maximumSize(1_000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build()

var fileNameMap: FileNameMap = URLConnection.getFileNameMap()
