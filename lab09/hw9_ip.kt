import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object NetUtility {

    fun getMyAddress(): Pair<InetAddress?, InetAddress?> {
        val networkInterface = getNetworkInterface() ?: return Pair(null, null)

        val interfaceAddresses = networkInterface.interfaceAddresses
        for (address in interfaceAddresses) {
            val inetAddr = address.address
            if (inetAddr is Inet4Address) {
                val maskLength = address.networkPrefixLength.toInt()
                val mask = prefixLengthToSubnetMask(maskLength)
                return Pair(inetAddr, mask)
            }
        }

        return Pair(null, null)
    }

    private fun getNetworkInterface(): NetworkInterface? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null

        for (networkInterface in interfaces.asSequence()) {
            if (!networkInterface.isUp || networkInterface.isLoopback || networkInterface.isVirtual) continue
            if (networkInterface.inetAddresses.toList().any { it is Inet4Address }) {
                return networkInterface
            }
        }

        return null
    }

    private fun prefixLengthToSubnetMask(prefixLength: Int): InetAddress {
        val mask = IntArray(4)
        for (i in 0 until 4) {
            val remainingBits = prefixLength - i * 8
            mask[i] = when {
                remainingBits >= 8 -> 255
                remainingBits <= 0 -> 0
                else -> (0xFF shl (8 - remainingBits)) and 0xFF
            }
        }
        return InetAddress.getByAddress(mask.map { it.toByte() }.toByteArray())
    }
}

fun main() {
    val (ip, mask) = NetUtility.getMyAddress()
    println("IP Address: ${ip?.hostAddress}")
    println("Subnet Mask: ${mask?.hostAddress}")
}

