import java.io.File
import java.io.FileOutputStream
import java.net.*
import kotlin.random.Random
import kotlin.system.exitProcess

const val PORT = 8120
const val BUFFER_SIZE = 1024
const val PACKET_LOSS_PROBABILITY = 0.3f
const val TIMEOUT_MS = 3000

fun maybeDropPacket(direction: String, action: () -> Unit) {
    if (Random.nextFloat() > PACKET_LOSS_PROBABILITY) {
        action()
    } else {
        // println("$direction Packet lost during simulated network drop.")
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: provide path to the file to send.")
        exitProcess(1)
    }

    val file = File(args[0])
    if (!file.exists() || !file.isFile) {
        println("Error: '${args[0]}' is not a valid file.")
        exitProcess(1)
    }

    val serverAddress = InetAddress.getByName("localhost")
    val socket = DatagramSocket().apply { soTimeout = TIMEOUT_MS }

    val data = file.readBytes()
    var seqNum = 0
    var offset = 0

    println("Sending file '${file.name}' (${data.size} bytes)...")

    while (offset < data.size) {
        val chunk = data.copyOfRange(offset, minOf(offset + BUFFER_SIZE, data.size))
        val packetData = byteArrayOf(seqNum.toByte()) + chunk
        val packet = DatagramPacket(packetData, packetData.size, serverAddress, PORT)

        while (true) {
            maybeDropPacket("SEND") {
                socket.send(packet)
                println("Sent packet $seqNum (offset: $offset)")
            }

            val ackBuf = ByteArray(1)
            val ackPacket = DatagramPacket(ackBuf, ackBuf.size)

            try {
                socket.receive(ackPacket)
                if (ackBuf[0].toInt() == seqNum) {
                    // println("ACK $seqNum received.")
                    seqNum = 1 - seqNum
                    offset += chunk.size
                    break
                }
            } catch (e: SocketTimeoutException) {
                // println("Timeout waiting for ACK. Resending...")
            }
        }
    }

    val finishPacket = DatagramPacket(byteArrayOf(-1), 1, serverAddress, PORT)
    maybeDropPacket("SEND") { socket.send(finishPacket) }
    println("Upload to server complete \nNow receiving file back...")

    val outputStream = FileOutputStream("file_recieved_from_server.txt")
    var expectedSeq = 0

    while (true) {
        val buffer = ByteArray(1025)
        val packet = DatagramPacket(buffer, buffer.size)
        var received = false

        try {
            maybeDropPacket("RECEIVE:") {
                socket.receive(packet)
                received = true
            }
        } catch (e: Exception) {
            println("Receive error: ${e.message}")
            continue
        }

        if (!received) continue

        val data = packet.data
        val seq = data[0].toInt()

        if (seq == -1) {
            println("File from server received completely")
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
        val ackPacket = DatagramPacket(ack, ack.size, serverAddress, PORT)
        maybeDropPacket("SEND:") { socket.send(ackPacket) }
    }

    outputStream.close()
    socket.close()
    println("Client finished.")
}
