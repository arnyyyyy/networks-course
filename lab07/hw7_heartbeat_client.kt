import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.measureNanoTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round


const val PORT = 8120
const val BUFFER_SIZE = 1024

fun buildPingMessage(clientId: String, sequenceNumber: Int): String {
    val timestampMillis = System.currentTimeMillis()
    val formattedTime = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Date(timestampMillis))
    return "$clientId $sequenceNumber $timestampMillis $formattedTime"
}



fun main() {
    val serverAddress = InetAddress.getByName("localhost")
    val serverPort = PORT
    val clientId = UUID.randomUUID().toString().take(8)
    val dateFormat = SimpleDateFormat("HH:mm:ss")

    var sequence = 0
    val rtts = mutableListOf<Double>()
    var lost = 0
    val totalPings = 10
    var lostPackets = 0

   
    println("[$clientId] Starting heartbeat")

    DatagramSocket().use { socket ->
        socket.soTimeout = 1000

        for (sequence in 1..10) {
            val timestamp = System.currentTimeMillis()
            val message = buildPingMessage(clientId, sequence)
            val sendData = message.toByteArray()
            val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, serverPort)

            val receiveBuffer = ByteArray(BUFFER_SIZE)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            print("[$clientId] Sending seq=$sequence\n")

            try {
                val rttNanos = measureNanoTime {
                    socket.send(sendPacket)
                    socket.receive(receivePacket)
                }

                val rttMs = rttNanos / 1_000_000.0
                rtts.add(rttMs)

                val reply = String(receivePacket.data, 0, receivePacket.length)
                println("[$clientId] Received seq=$sequence | RTT = ${"%.2f".format(rttMs)} ms | reply=\"$reply\"\n")
            } catch (e: SocketTimeoutException) {
                println("[$clientId] Timeout on seq=$sequence\n")
                lostPackets++
            }

            Thread.sleep(1000)
        }

        socket.close()
    }


    println("--- Heartbeat Statistics [$clientId] ---")
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
}
