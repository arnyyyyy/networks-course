import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.math.round
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
    val totalPings = 10

    DatagramSocket().use { socket ->
        socket.soTimeout = 1000

        val rtts = mutableListOf<Double>()
        var lostPackets = 0

        println("PING ${serverAddress.hostAddress}:$serverPort with $totalPings UDP requests\n")

        for (sequenceNumber in 1..totalPings) {
            val message = buildPingMessage(sequenceNumber)
            val sendData = message.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, serverPort)

            println("Client sent message: $message")

            try {
                val receiveBuffer = ByteArray(BUFFER_SIZE)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                val rttNanos = measureNanoTime {
                    socket.send(sendPacket)
                    socket.receive(receivePacket)
                }

                val rttMillis = rttNanos / 1_000_000.0
                rtts.add(rttMillis)

                val response = String(receivePacket.data, 0, receivePacket.length)
                println("Received response: $response")
                println("RTT: ${"%.3f".format(rttMillis / 1000)} seconds")

                val min = rtts.minOrNull()!!
                val max = rtts.maxOrNull()!!
                val avg = rtts.average()

                println("RTT stats: min: ${"%.2f".format(min)} ms, max: ${"%.2f".format(max)} ms, avg: ${"%.2f".format(avg)} ms\n")
            } catch (e: SocketTimeoutException) {
                println("Request timed out\n")
                lostPackets++
            }

            Thread.sleep(1000)
        }

        println("Finished sending pings\n")

        println("--- Ping Statistics ---")
        val received = rtts.size
        val loss = (lostPackets.toDouble() / totalPings) * 100

        if (rtts.isNotEmpty()) {
            val min = rtts.minOrNull()!!
            val max = rtts.maxOrNull()!!
            val avg = rtts.average()

            println("Packets: sent = $totalPings, received = $received, lost = $lostPackets (${round(loss)}%)")
            println("RTT: min = ${"%.2f".format(min)} ms, max = ${"%.2f".format(max)} ms, avg = ${"%.2f".format(avg)} ms")
        } else {
            println("All packets lost. No RTT statistics available.")
        }
        socket.close()
    }
}
