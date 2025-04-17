import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.random.Random

const val PORT = 8120
const val PACKET_LOSS_PROBABILITY = 0.3f

fun maybeDropPacket(direction: String, action: () -> Unit) {
    if (Random.nextFloat() > PACKET_LOSS_PROBABILITY) {
        action()
    } 
}

fun calculateChecksum(data: ByteArray): Byte {
    var sum = 0
    for (byte in data) sum = (sum + byte) and 0xFF
    return sum.toByte()
}

fun main() {
    val serverSocket = DatagramSocket(PORT)
    val outputStream = FileOutputStream("received_file_SUM.txt")
    var expectedSeq = 0

    println("Server started. Waiting for packets...")

    while (true) {
        val buffer = ByteArray(1026)
        val packet = DatagramPacket(buffer, buffer.size)
        var receivedSuccessfully = false

        try {
            maybeDropPacket("RECEIVE:") {
                serverSocket.receive(packet)
                receivedSuccessfully = true
            }
        } catch (e: Exception) {
            println("Error during packet receive: ${e.message}")
            continue
        }

        if (!receivedSuccessfully) continue

        val data = packet.data

        val seqByte = data[0]
        val seq = seqByte.toInt() and 0xFF 
        if (seq == 255) { 
            println("\nTermination packet received. Closing connection.")
            break
        }


        val receivedChecksum = data[1]
        val payload = data.copyOfRange(2, packet.length)
        val calculatedChecksum = calculateChecksum(payload)

        println("Packet seq = $seq | Received checksum: ${receivedChecksum.toUByte()} | Calculated checksum: ${calculatedChecksum.toUByte()}")

        if (receivedChecksum != calculatedChecksum) {
            println("Checksum mismatch. Packet corrupted. Ignoring.\n")
            continue
        }

        val ackNumber: Int

        if (seq == expectedSeq) {
            outputStream.write(payload)
            ackNumber = seq
            expectedSeq = 1 - expectedSeq
        } else {
            ackNumber = 1 - expectedSeq
        }

        val ack = byteArrayOf(ackNumber.toByte())
        val ackPacket = DatagramPacket(ack, ack.size, packet.address, packet.port)

        maybeDropPacket("SEND:") {
            serverSocket.send(ackPacket)
        }
    }

    outputStream.close()
    serverSocket.close()
    println("File successfully received and server stopped.")
}
