import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

const val PORT = 8120
const val BUFFER_SIZE = 1024
const val PACKET_LOSS_PROBABILITY = 0.2

fun main() {
    val socket = DatagramSocket(PORT)
    val buffer = ByteArray(BUFFER_SIZE)

    println("UDP-server is listening on the port: $PORT\n")

    while (true) {
        val receivePacket = DatagramPacket(buffer, buffer.size)
        socket.receive(receivePacket)

        handleClientPacket(socket, receivePacket)
    }
}

fun handleClientPacket(socket: DatagramSocket, packet: DatagramPacket) {
    val receivedMsg = String(packet.data, 0, packet.length)
    val clientAddr = packet.address
    val clientPort = packet.port

    print("Got message: \"$receivedMsg\" from $clientAddr:$clientPort\n")

    if (shouldSimulatePacketLoss()) {
        print("Lost packet simulation: server does not respond to $receivedMsg\n")
        return
    }

    val responseMsg = receivedMsg.uppercase()
    sendResponse(socket, responseMsg, clientAddr, clientPort)
}

fun shouldSimulatePacketLoss(): Boolean {
    return Random.nextDouble() < PACKET_LOSS_PROBABILITY
}

fun sendResponse(socket: DatagramSocket, message: String, address: InetAddress, port: Int) {
    val responseData = message.toByteArray()
    val responsePacket = DatagramPacket(responseData, responseData.size, address, port)
    socket.send(responsePacket)

    print("Uppercase response: $message sent to $address:$port\n")
    println("Sent response to $address:$port\n")
}