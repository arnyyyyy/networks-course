import java.net.InetAddress
import java.net.ServerSocket
import java.net.DatagramSocket

object IPUtility {
    fun getAvailablePorts( rangeStart: Int, rangeEnd: Int, ip: InetAddress): List<Int> {
        val availablePorts = mutableListOf<Int>()

        for (port in rangeStart..rangeEnd) {
            if (isPortAvailable(ip, port)) {
                availablePorts.add(port)
            }
        }

        return availablePorts
    }

    private fun isPortAvailable(ip: InetAddress, port: Int): Boolean {
        var tcpSocket: ServerSocket? = null
        var udpSocket: DatagramSocket? = null
        return try {
            tcpSocket = ServerSocket(port, 0, ip)
            tcpSocket.reuseAddress = true
            udpSocket = DatagramSocket(port, ip)
            udpSocket.reuseAddress = true
            true
        } catch (_: Exception) {
            false
        } finally {
            try {
                tcpSocket?.close()
                udpSocket?.close()
            } catch (_: Exception) {
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        return
    }

    try {
        val ip = InetAddress.getByName(args[0])
        val rangeStart = args[1].toInt()
        val rangeEnd = args[2].toInt()

        val availablePorts = IPUtility.getAvailablePorts(rangeStart, rangeEnd, ip)

        println("Available ports on $ip from $rangeStart to $rangeEnd:")
        if (availablePorts.isEmpty()) {
            println("No available ports")
        } else {
            println(availablePorts.joinToString(", "))
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
