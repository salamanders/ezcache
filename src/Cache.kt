package info.benjaminhill

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import java.net.FileNameMap
import java.net.URLConnection
import java.util.concurrent.TimeUnit


internal suspend fun PipelineContext<Unit, ApplicationCall>.doCacheGet() {
    println("doCacheGet")
    val fileName = call.request.queryParameters["fileName"]
    require(fileName != null) { "Missing required parameter 'fileName'" }
    val cacheFile = cache.getIfPresent(fileName)
    require(cacheFile != null) { "Unable to locate cached item '$fileName'" }

    val mimeType = fileNameMap.getContentTypeFor(fileName).split("/")
    call.respondBytes(contentType = ContentType(mimeType[0], mimeType[1])) {
        cacheFile.content
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.doCachePut() {
    println("doCachePut")
    try {
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
                    content = it.readNBytes(10_000)
                    check(content.size < 10_000) { "Too large a file, should be under 10kb" }
                }
            }
            part.dispose()
        }
        val cachedFile = CachedFile(content = content)
        cache.put(fileName, cachedFile)

        call.respond(
            mapOf(
                "action" to "put",
                "size" to cachedFile.size,
                "ts" to cachedFile.ts
            )
        )
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.doTsGet() {
    println("doTsGet")
    val fileNames = call.request.queryParameters.getAll("fileName") ?: listOf()
    require(fileNames.isNotEmpty()) { "Missing at least one 'fileName'" }
    call.respond(fileNames.map {
        it to (cache.getIfPresent(it)?.ts ?: -1)
    }.toMap())
}

private class CachedFile(
    val ts: Long = System.currentTimeMillis(),
    val content: ByteArray,
) {
    val size: Int by lazy {
        content.size
    }
}

private var cache: Cache<String, CachedFile> = CacheBuilder.newBuilder()
    .maximumSize(1_000)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build()

private var fileNameMap: FileNameMap = URLConnection.getFileNameMap()
