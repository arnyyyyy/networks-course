package com.example.hw4_b

import org.springframework.http.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.toEntity
import java.io.File
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger

@Configuration
@ConfigurationProperties(prefix = "proxy")
class Config {
    lateinit var blacklist: List<String>
}

@RestController
@RequestMapping("/")
class ProxyController(private val config: Config) {

    private val restClient = RestClient.builder().build()
    private val cacheDir = File("proxyCache").apply { mkdirs() }

    @GetMapping("/")
    fun Get(@RequestParam url: String): ResponseEntity<String> {
        if (config.blacklist.contains(url)) {
            val log = "BLOCKED: Can not open $url: access denied"
            logInfo("GET", url, HttpStatus.FORBIDDEN.value(), log)
            return ResponseEntity(log, HttpStatus.FORBIDDEN)
        }

        val cacheFile = File(cacheDir, url.replace(Regex("[:/]"), "_"))
        val metaInfFile = File(cacheFile.absolutePath + ".meta")

        if (cacheFile.exists() && metaInfFile.exists()) {
            val metaInf = metaInfFile.readLines().map { line ->
                val (key, value) = line.split(":", limit = 2)
                key.trim() to value.trim()
            }.toMap()

            val ifModifiedSince = metaInf["Last-Modified"]
            val ifNoneMatch = metaInf["ETag"]

            val header = HttpHeaders()
            if (ifModifiedSince != null) {
                header.set("If-Modified-Since", ifModifiedSince)
            }
            if (ifNoneMatch != null) {
                header.set("If-None-Match", ifNoneMatch)
            }

            try {
                val response = restClient.get()
                    .uri(url)
                    .headers { it.addAll(header) }
                    .retrieve()
                    .toEntity<String>()

                return if (response.statusCode == HttpStatus.NOT_MODIFIED) {
                    val data = Files.readString(cacheFile.toPath())
                    logInfo("GET: CACHE HIT!", url, HttpStatus.OK.value(), "CACHE HIT:\n$data")
                    ResponseEntity(data, HttpStatus.OK)
                } else {
                    handleCacheSaving(url, response)
                    logInfo("GET", url, response.statusCode.value(), "SAVED TO CACHE:\n${response.body}")
                    ResponseEntity(response.body, response.statusCode)
                }
            } catch (e: HttpStatusCodeException) {
                return if (e.statusCode == HttpStatus.NOT_MODIFIED) {
                    val cachedBody = Files.readString(cacheFile.toPath())
                    logInfo("GET", url, HttpStatus.OK.value(), "304 CACHE:\n$cachedBody")
                    ResponseEntity(cachedBody, HttpStatus.OK)
                } else {
                    logInfo("GET", url, e.statusCode.value(), "ERROR: ${e.responseBodyAsString}")
                    ResponseEntity(e.responseBodyAsString, e.statusCode)
                }
            }
        }

        try {
            val response = restClient.get()
                .uri(url)
                .retrieve()
                .toEntity(String::class.java)

            handleCacheSaving(url, response)
            logInfo("GET", url, response.statusCode.value(), "SERVER:\n${response.body}")

            return ResponseEntity(response.body, response.statusCode)
        } catch (e: HttpStatusCodeException) {
            logInfo("GET", url, e.statusCode.value(), e.responseBodyAsString)
            return ResponseEntity(e.responseBodyAsString, e.statusCode)
        }
    }

    private fun handleCacheSaving(url: String, response: ResponseEntity<String>) {
        val cacheFile = File(cacheDir, url.replace(Regex("[:/]"), "_"))
        val metaInf = File(cacheFile.absolutePath + ".meta")

        try {
            cacheFile.writeText(response.body ?: "")
            metaInf.writeText(buildMetadata(response.headers))
            logInfo("SAVED TO CACHE", url, HttpStatus.OK.value())
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to save cache for $url: ${e.message}")
        }
    }

    private fun buildMetadata(headers: HttpHeaders): String {
        val lastModified = headers["Last-Modified"]?.firstOrNull() ?: ""
        val etag = headers["ETag"]?.firstOrNull() ?: ""
        return "Last-Modified: $lastModified\nETag: $etag"
    }


    private val logger: Logger = Logger.getLogger("ProxyLogger")
    private val logFile = File("logs/proxy.log")

    private fun logInfo(op: String, url: String, statusCode: Int, err: String? = null) {
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }

        var msg = ""
        msg += if (err.isNullOrBlank() || statusCode == HttpStatus.OK.value()) {
            "$op for $url: $statusCode SUCCESSFUL\n"
        } else {
            "WITH ERROR ${err ?: ""}\n" // в err описана операция, при которой возникла ошибка
        }

        logger.log(Level.INFO, msg)
        logFile.appendText(msg)
    }
}