import java.io.FileOutputStream
import java.net.*
import kotlin.random.Random

const val PORT = 8120
const val PACKET_LOSS_PROBABILITY = 0.3f

fun maybeDropPacket(direction: String, action: () -> Unit) {
    if (Random.nextFloat() > PACKET_LOSS_PROBABILITY) {
        action()
    } else {
        println("$direction Packet lost during simulated network drop.")
    }
}

fun main() {
    val serverSocket = DatagramSocket(PORT)
    val outputStream = FileOutputStream("received_file.txt")
    var expectedSeq = 0

    println("Server started. Waiting for packets...")

    while (true) {
        val buffer = ByteArray(1025)
        val packet = DatagramPacket(buffer, buffer.size)
        var receivedSuccessfully = false
    
        try {
            maybeDropPacket("RECIEVE:") {
                serverSocket.receive(packet)
                receivedSuccessfully = true
            }
        } catch (e: Exception) {
            println("Error during packet receive: ${e.message}")
            continue
        }
    
        if (!receivedSuccessfully) continue 
    
        val data = packet.data
        val seq = data[0].toInt()
    
        if (seq == -1) {
            println("\nTermination packet received. Closing connection.")
            break
        }
    
        val ackNumber = if (seq == expectedSeq) {
            outputStream.write(data, 1, packet.length - 1)
            println("Received packet with seq = $seq. Sending ACK\n")
            seq.also { expectedSeq = 1 - expectedSeq }
        } else {
            println("Duplicate or unexpected packet. Resending last ACK ${1 - expectedSeq}.")
            1 - expectedSeq
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
