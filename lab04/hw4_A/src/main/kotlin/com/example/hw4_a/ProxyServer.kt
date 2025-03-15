package com.example.hw4_a

import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.*
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

@RestController
@RequestMapping("/")
class ProxyController {

    private val restClient = RestClient.builder().build()

    @GetMapping("/")
    fun proxyGet(@RequestParam url: String): ResponseEntity<String> {
        try {
            val response = restClient.get().uri(url).retrieve().toEntity<String>()
            logInfo("GET", url, response.statusCode.value())
            return ResponseEntity(response.body, response.statusCode)
        } catch (e: HttpServerErrorException) {
            logInfo("GET", url, e.statusCode.value(), e.message)
            return ResponseEntity(e.responseBodyAsString, e.statusCode)
        } catch (e: HttpClientErrorException) {
            logInfo("GET", url, e.statusCode.value(), e.message)
            return ResponseEntity(e.responseBodyAsString, e.statusCode)
        } catch (e: ResourceAccessException) {
            logInfo("GET", url, HttpStatus.SERVICE_UNAVAILABLE.value(), e.message)
            return ResponseEntity("Service Unavailable: Unable to reach the server", HttpStatus.SERVICE_UNAVAILABLE)
        } catch (e: Exception) {
            logInfo("GET", url, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.message)
            return ResponseEntity("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private val logger: Logger = Logger.getLogger("logger")
    private val logFile = File("logs/proxy.log")

    private fun logInfo(op: String, url: String, statusCode: Int, err: String? = null) {
        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }

        var msg = ""
        msg += if (err.isNullOrBlank() && statusCode == 200) {
            "$op for $url: $statusCode SUCCESSFUL\n"
        } else {
            "WITH ERROR ${err ?: ""}\n" // в err описана операция, при которой возникла ошибка
        }

        logger.log(Level.INFO, msg)
        logFile.appendText(msg)
    }
}