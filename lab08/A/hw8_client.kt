import java.io.File
import java.net.*
import kotlin.random.Random
import kotlin.system.exitProcess

const val PORT = 8120
const val BUFFER_SIZE = 1024
const val PACKET_LOSS_PROBABILITY = 0.3f
const val TIMEOUT_MS = 1000

fun maybeDropPacket(direction: String, action: () -> Unit) {
    if (Random.nextFloat() > PACKET_LOSS_PROBABILITY) {
        action()
    } else {
        println("$direction Packet lost during simulated network drop.")
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
    val clientSocket = DatagramSocket().apply { soTimeout = TIMEOUT_MS }

    val data = file.readBytes()
    var seqNum = 0
    var offset = 0

    println("Sending file '${file.name}' (${data.size} bytes)...")

    while (offset < data.size) {
        val chunk = data.copyOfRange(offset, minOf(offset + BUFFER_SIZE, data.size))
        val packetData = byteArrayOf(seqNum.toByte()) + chunk
        val packet = DatagramPacket(packetData, packetData.size, serverAddress, PORT)

        while (true) {
            maybeDropPacket("SEND:") {
                clientSocket.send(packet)
                println("Sent packet with seq = $seqNum (offset: $offset)")
            }

            try {
                val ackBuf = ByteArray(1)
                val ackPacket = DatagramPacket(ackBuf, ackBuf.size)

                clientSocket.receive(ackPacket)

                if (ackBuf[0].toInt() == seqNum) {
                    println("ACK $seqNum received.")
                    seqNum = 1 - seqNum
                    offset += chunk.size
                    val progress = (offset * 100) / data.size
                    println("Progress: $progress%\n")
                    break
                } else {
                    println("Incorrect ACK received: ${ackBuf[0]}")
                }

            } catch (e: SocketTimeoutException) {
                println("Timeout waiting for ACK. Resending packet...")
            }
        }
    }

    val finishPacket = DatagramPacket(byteArrayOf(-1), 1, serverAddress, PORT)
    maybeDropPacket("SEND:") { clientSocket.send(finishPacket) }

    println("File transmission completed.\nClient stopped.")
    clientSocket.close()
}
