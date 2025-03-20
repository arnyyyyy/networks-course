package com.example.hw5a2

import java.io.*
import java.net.Socket
import java.util.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class MyEmailSender(private val hostName: String, private val port: Int) {
    private lateinit var socket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter

    fun connect() {
        socket = Socket(hostName, port)
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    }

    // тк gmail не работает без Tls
    fun upgradeToTls() {
        val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val sslSocket = sslSocketFactory.createSocket(
            socket,
            hostName,
            port,
            true
        ) as SSLSocket
        sslSocket.startHandshake()

        socket = sslSocket
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    }

    fun getResponse(): String {
        return reader.readLine()
    }

    fun close() {
        reader.close()
        writer.close()
        socket.close()
    }

    fun sendMessageAndCheckReply(message: String, expectedCode: Int = -1, noResponse: Boolean = false) {
        println("\nCommand: $message")
        writer.write(message)
        writer.flush()

        if (noResponse) return

        val response = reader.readLine()
        println("Response: $response")

        if (expectedCode != -1 && !response.startsWith(expectedCode.toString())) {
            throw Exception("Expected $expectedCode code but got: $response")
        }
    }

    companion object {
        fun encodeTo64(toEncode: String): String {
            return Base64.getEncoder().encodeToString(toEncode.toByteArray())
        }

        fun encodeFileTo64(filePath: String): String {
            val file = File(filePath)
            val bytes = file.readBytes()
            return Base64.getEncoder().encodeToString(bytes)
        }
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Error: please set recipient")
        return
    }

    val recipient = args[0]

    val properties = Properties()
    properties.load(ClassLoader.getSystemResourceAsStream("application.properties"))

    val smtpUsername = properties.getProperty("SmtpUser")
    val smtpPassword = properties.getProperty("SmtpPwd")


    val sender = MyEmailSender("smtp.gmail.com", 587)

    try {

        sender.connect()

        val response = sender.getResponse()
        println(response)
        if (!response.startsWith("220")) {
            throw Exception("Expected 220 code but got: $response")
        }

        sender.sendMessageAndCheckReply("HELO smtp.gmail.com\r\n", 250)

        sender.sendMessageAndCheckReply("STARTTLS\r\n", 220)
        sender.upgradeToTls()

        sender.sendMessageAndCheckReply("HELO smtp.gmail.com\r\n", 250)

        sender.sendMessageAndCheckReply("AUTH LOGIN\r\n", 334)
        sender.sendMessageAndCheckReply("${MyEmailSender.encodeTo64(smtpUsername)}\r\n", 334)
        sender.sendMessageAndCheckReply("${MyEmailSender.encodeTo64(smtpPassword)}\r\n", 235)

        sender.sendMessageAndCheckReply("MAIL FROM: <$smtpUsername>\r\n", 250)

        sender.sendMessageAndCheckReply("RCPT TO: <${recipient}>\r\n", 250)


        sender.sendMessageAndCheckReply("DATA\r\n", 354)

//         task 2
        val message = StringBuilder().apply {
            append("From: <$smtpUsername>\r\n")
            append("To: <${recipient}>\r\n")
            append("Subject: TEST TASK2\r\n")
            append("\r\n")
            append("HELLO!\r\n")
        }

        // task 3
//        val bound = "bound"
//        val message = StringBuilder().apply {
//            append("From: <$smtpUsername>\r\n")
//            append("To: <${recipient}>\r\n")
//            append("Subject: TEST TASK3\r\n")
//            append("MIME-Version: 1.0\r\n")
//            append("Content-Type: multipart/mixed; boundary=\"$bound\"\r\n")
//            append("\r\n")
//
//            append("--$bound\r\n")
//            append("Content-Type: text/plain; charset=utf-8\r\n")
//            append("\r\n")
//            append("Stephen Makcey Art\r\n")
//            append("\r\n")
//
//            append("--$bound\r\n")
//            append("Content-Type: image/png; name=\"IntroducingTheDark.jpg\"\r\n")
//            append("Content-Disposition: attachment; filename=\"IntroducingTheDark.jpg\"\r\n")
//            append("Content-Transfer-Encoding: base64\r\n")
//            append("\r\n")
//            append(MyEmailSender.encodeFileTo64("IntroducingTheDark.jpg"))
//            append("\r\n")
//
//            append("--$bound--\r\n")
//        }

        sender.sendMessageAndCheckReply(message.toString(), noResponse = true)

        sender.sendMessageAndCheckReply(".\r\n", 250)

        sender.sendMessageAndCheckReply("QUIT\r\n")
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        sender.close()
    }
}