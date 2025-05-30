import kotlin.experimental.xor

class CrcChecker(private val packetSize: Int = 5) {
    private val obrPol = 1010101

    fun splitIntoPackets(data: ByteArray): List<ByteArray> =
        data.asList().chunked(packetSize).map { it.toByteArray() }

    private fun crc(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor obrPol else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    fun encodePacket(packet: ByteArray): ByteArray {
        val crc = crc(packet)
        return packet + byteArrayOf((crc shr 8).toByte(), (crc and 0xFF).toByte())
    }

    fun verifyPacket(packetWithCrc: ByteArray): Boolean {
        if (packetWithCrc.size < 3) return false
        val data = packetWithCrc.copyOfRange(0, packetWithCrc.size - 2)
        val crcReceived = ((packetWithCrc[packetWithCrc.size - 2].toInt() and 0xFF) shl 8) or
                (packetWithCrc.last().toInt() and 0xFF)
        return crcReceived == crc(data)
    }
}

fun main() {
    val crcChecker = CrcChecker()
    val input = "abcdefghijklmnopqrstuvwxyz123457"
    val packets = crcChecker.splitIntoPackets(input.encodeToByteArray())
    val encodedPackets = packets.map { crcChecker.encodePacket(it) }.toMutableList()

    val errorPacketIndex = 1
    val corruptedPacket = encodedPackets[errorPacketIndex].copyOf()
    corruptedPacket[0] = corruptedPacket[0].xor((1 shl 3).toByte())
    encodedPackets[errorPacketIndex] = corruptedPacket


    encodedPackets.forEachIndexed { i, packet ->
        val data = packets[i].decodeToString()
        val isValid = crcChecker.verifyPacket(packet)

        println("Packet number ${i + 1}:")
        println("Data: \"$data\"")
        println(if (isValid) "OK!" else "ERROR AAAA!")
        println()
    }
}
