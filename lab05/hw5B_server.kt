import java.io.*
import java.net.ServerSocket
import java.net.Socket


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
    } catch (e: Exception) {
        println("Failed to start server: ${e.message}")
    }
}

fun handleRequest(clientSocket: Socket) {
    val remoteAddress = clientSocket.inetAddress.hostAddress
    val remotePort = clientSocket.port
    println("Connection from $remoteAddress:$remotePort")

    try {
        val input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val output = BufferedOutputStream(clientSocket.getOutputStream())
        val writer = PrintWriter(output, true)

        val command = input.readLine()
        println("Executing command: $command")

        val process = ProcessBuilder("bash", "-c", command).start()

        val stdout = BufferedReader(InputStreamReader(process.inputStream))
        val stderr = BufferedReader(InputStreamReader(process.errorStream))

        sendResponse(process, writer)
    } catch (e: Exception) {
        println("Error executing command: ${e.message}")
    } finally {
        clientSocket.close()
        println("Connection closed")
    }
}


fun sendResponse(process: Process, writer: PrintWriter) {
    val stdout = BufferedReader(InputStreamReader(process.inputStream))
    val stderr = BufferedReader(InputStreamReader(process.errorStream))

    stdout.forEachLine {
        writer.println(it)
        writer.flush()
    }

    stderr.forEachLine {
        writer.println(it)
        writer.flush()
    }

    process.waitFor()
    if (process.exitValue() != 0) {
        writer.println("Command completed with exit code: ${process.exitValue()}")
    }
}
