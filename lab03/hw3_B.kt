import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Invalid number of args");
        return
    }

    val port = args[0].toIntOrNull()
    if (port == null) {
        println("Invalid port number");
        return
    }

    runServer(port)
}

fun runServer(port: Int) {
    try {
        println("Trying to make connection");

        val serverSocket = ServerSocket(port)
        println("Server work is started");

        while (true) {
            val clientSocket = serverSocket.accept()
            Thread {
                handleRequest(clientSocket)
            }.start()
        }
    } catch (e: IOException) {
        println("Failed to start server: ${e.message}")
    }
}
fun handleRequest(clientSocket: Socket) {
    val remoteAddress = clientSocket.inetAddress.hostAddress
    val remotePort = clientSocket.port
    println("Connection from $remoteAddress:$remotePort")

    try {
        val request = readRequest(clientSocket)
        println("Request received:\n$request")

        val (method, path) = parseRequest(request[0])
        if (method != "GET") {
            sendResponse(clientSocket, 405, "Method Not Allowed")
            return
        }

        handleFileRequest(clientSocket, path)
    } catch (e: Exception) {
        e.printStackTrace()
        sendResponse(clientSocket, 500, "Internal Server Error")
    } finally {
        clientSocket.close()
        println("Connection from $remoteAddress:$remotePort closed")

    }
}

fun readRequest(clientSocket: Socket): List<String> {
    val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
    val request = mutableListOf<String>()
    while (true) {
        val line = reader.readLine()
        if (line.isEmpty()) {
            break;
        }
        request.add(line)
    }
    return request
}

fun parseRequest(request: String): Pair<String, String> {
    val parts = request.split(" ")
    if (parts.size < 2) {
        throw IllegalArgumentException("Incorrect request format")
    }
    return Pair(parts[0], parts[1])
}

fun handleFileRequest(clientSocket: Socket, path: String) {
    val filePath = Paths.get(path.substring(1))
    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
        val content = Files.readAllBytes(filePath)
        sendResponse(clientSocket, 200, "OK", content)
    } else {
        sendResponse(clientSocket, 404, "Not Found")
    }
}

fun sendResponse(clientSocket: Socket, statusCode: Int, statusMessage: String, content: ByteArray? = null) {
    val outputStream = clientSocket.getOutputStream()

    val headers = StringBuilder()
    headers.append("HTTP/1.1 $statusCode $statusMessage\r\n")
    headers.append("Content-Type: text/html; charset=UTF-8\r\n")
    headers.append("Connection: close\r\n")
    headers.append("Content-Length: ${content?.size ?: statusMessage.toByteArray().size}\r\n")
    headers.append("\r\n")

    outputStream.write(headers.toString().toByteArray()) 

    if (content != null) {
        outputStream.write(content)
    } else {
        outputStream.write(statusMessage.toByteArray())
    }

    outputStream.flush()
    outputStream.close() 
    
    clientSocket.close()

}
