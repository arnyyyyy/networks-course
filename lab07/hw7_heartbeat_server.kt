import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

const val PORT = 8120
const val BUFFER_SIZE = 1024
const val PACKET_LOSS_PROBABILITY = 0.2
const val CLIENT_TIMEOUT = 3000L

data class ClientStatus(var lastSeen: Long, var lastSeq: Int)

fun main() {
    val socket = DatagramSocket(PORT)
    val buffer = ByteArray(BUFFER_SIZE)
    val clients = ConcurrentHashMap<String, ClientStatus>()

    println("UDP server is listening on port $PORT")

    Thread {
        while (true) {
            val now = System.currentTimeMillis()
            clients.entries.removeIf { (id, status) ->
                if (now - status.lastSeen > CLIENT_TIMEOUT) {
                    println("[$id] Inactive for > ${CLIENT_TIMEOUT}ms — disconnected")
                    true
                } else false
            }
            Thread.sleep(1000)
        }
    }.start()

    while (true) {
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)
        handlePacket(socket, packet, clients)
    }
}

fun handlePacket(
    socket: DatagramSocket,
    packet: DatagramPacket,
    clients: ConcurrentHashMap<String, ClientStatus>
) {
    val msg = String(packet.data, 0, packet.length)
    val parts = msg.split(" ")

    if (parts.size < 4) {
        println("Invalid packet received: \"$msg\"")
        return
    }
    
    val clientId = parts[0]
    val seq = parts[1].toIntOrNull() ?: return
    val timestamp = parts[2].toLongOrNull() ?: return
    val humanTime = parts.drop(3).joinToString(" ") 
    val now = System.currentTimeMillis()
    val delay = now - timestamp
    
    clients[clientId] = ClientStatus(now, seq)
    println("[$clientId] seq=$seq | delay=${delay}ms | sent at $humanTime")

    if (shouldSimulatePacketLoss()) {
        println("[$clientId] Simulated packet loss at seq=$seq")
        return
    }

    sendResponse(socket, msg, packet)
}


fun shouldSimulatePacketLoss(): Boolean {
    return Random.nextDouble() < PACKET_LOSS_PROBABILITY
}

fun sendResponse(socket: DatagramSocket, message: String, packet: DatagramPacket,) {
    val responseData = message.toByteArray()
    val responsePacket = DatagramPacket(responseData, responseData.size, packet.address, packet.port)
    socket.send(responsePacket)

    print("Uppercase response: $message sent to ${packet.address}:${packet.port}\n")
    println("Sent response to ${packet.address}:${packet.port}\n")
}