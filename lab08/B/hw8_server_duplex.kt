import java.io.*
import java.net.*
import kotlin.random.Random

const val PORT = 8120
const val PACKET_LOSS_PROBABILITY = 0.3f
const val BUFFER_SIZE = 1024
const val TIMEOUT_MS = 1000

fun maybeDropPacket(direction: String, action: () -> Unit) {
    if (Random.nextFloat() > PACKET_LOSS_PROBABILITY) {
        action()
    } else {
        // println("$direction Packet lost during simulated network drop.")
    }
}

fun main() {
    val serverSocket = DatagramSocket(PORT)
    val outputStream = FileOutputStream("received_from_client_DUPLEX.txt")
    var expectedSeq = 0
    var clientAddress: InetAddress? = null
    var clientPort: Int = -1

    println("Server started. Waiting for file from client...")

    while (true) {
        val buffer = ByteArray(1025)
        val packet = DatagramPacket(buffer, buffer.size)
        var receivedSuccessfully = false

        try {
            maybeDropPacket("RECEIVE:") {
                serverSocket.receive(packet)
                receivedSuccessfully = true
            }
        } catch (e: Exception) {
            println("Error during receive: ${e.message}")
            continue
        }

        if (!receivedSuccessfully) continue

        val data = packet.data
        val seq = data[0].toInt()

        clientAddress = packet.address
        clientPort = packet.port

        if (seq == -1) {
            println("End of file from client.")
            break
        }

        val ackNum = if (seq == expectedSeq) {
            outputStream.write(data, 1, packet.length - 1)
            // println("Received packet $seq. Sending ACK.")
            seq.also { expectedSeq = 1 - expectedSeq }
        } else {
            // println("Duplicate packet. Resending last ACK ${1 - expectedSeq}.")
            1 - expectedSeq
        }

        val ack = byteArrayOf(ackNum.toByte())
        val ackPacket = DatagramPacket(ack, ack.size, clientAddress, clientPort)
        maybeDropPacket("SEND:") {
            serverSocket.send(ackPacket)
        }
    }

    outputStream.close()
    println("File received from client")


    val fileToSend = File("file_to_send_to_client.txt")
    if (!fileToSend.exists()) {
        println("Error: file to send back not found.")
        serverSocket.close()
        return
    }

    val fileBytes = fileToSend.readBytes()
    var seqNum = 0
    var offset = 0
    serverSocket.soTimeout = TIMEOUT_MS

    println("Sending file back to client (${fileBytes.size} bytes)...")

    while (offset < fileBytes.size) {
        val chunk = fileBytes.copyOfRange(offset, minOf(offset + BUFFER_SIZE, fileBytes.size))
        val packetData = byteArrayOf(seqNum.toByte()) + chunk
        val packetToSend = DatagramPacket(packetData, packetData.size, clientAddress, clientPort)

        while (true) {
            maybeDropPacket("SEND") {
                serverSocket.send(packetToSend)
                println("Sent packet $seqNum (offset: $offset)")
            }

            val ackBuf = ByteArray(1)
            val ackPacket = DatagramPacket(ackBuf, ackBuf.size)

            try {
                serverSocket.receive(ackPacket)
                if (ackBuf[0].toInt() == seqNum) {
                    println("ACK $seqNum received")
                    seqNum = 1 - seqNum
                    offset += chunk.size
                    break
                }
            } catch (e: SocketTimeoutException) {
                // println("Timeout waiting for ACK. Resending...")
            }
        }
    }

    val finishPacket = DatagramPacket(byteArrayOf(-1), 1, clientAddress, clientPort)
    maybeDropPacket("SEND:") {
        serverSocket.send(finishPacket)
    }

    println("File sent to client \nServer shutting down.")
    serverSocket.close()
}
