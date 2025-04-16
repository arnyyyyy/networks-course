import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.random.Random
import kotlin.system.measureNanoTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


val wordPool = listOf(
    "hello", "world", "word", "client", "kotlin", "udp", "ping", "test", "echo", "abacaba",
)

const val PORT = 8120
const val BUFFER_SIZE = 1024

fun buildPingMessage(sequenceNumber: Int): String {
    val timestamp = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val formattedTime = timestamp.format(formatter) 
    val randomText = wordPool.random()
    return "Ping $sequenceNumber $formattedTime $randomText"
}

fun main() {
    val serverAddress = InetAddress.getByName("localhost")
    val serverPort = PORT

    DatagramSocket().use { socket ->
        socket.soTimeout = 1000

        for (sequenceNumber in 1..10) {
            val message = buildPingMessage(sequenceNumber)
            val sendData = message.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, serverPort)

            print("Client sent message: $message\n")

            try {
                val receiveBuffer = ByteArray(BUFFER_SIZE)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                val rttNanos = measureNanoTime {
                    socket.send(sendPacket)
                    socket.receive(receivePacket)
                }

                val response = String(receivePacket.data, 0, receivePacket.length)

                print("Received response: $response\n")
                println("RTT: ${"%.3f".format(rttNanos / 1_000_000_000.0)} seconds\n")
            } catch (e: SocketTimeoutException) {
                println("Request timed out\n")
            }

            Thread.sleep(1000)
        }
        socket.close()
    }
    println("Finished sending pings\n")
}