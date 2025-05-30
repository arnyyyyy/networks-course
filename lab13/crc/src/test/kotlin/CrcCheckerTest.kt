import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.experimental.xor

class CrcCheckerTest {

    private val crcChecker = CrcChecker()
    private fun makeError(packet: ByteArray, byteIndex: Int, bitIndex: Int): ByteArray {
        val corrupted = packet.copyOf()
        corrupted[byteIndex] = corrupted[byteIndex].xor((1 shl bitIndex).toByte())
        return corrupted
    }

    @Test
    fun testCrcCalculationAndVerification() {
        val data = "abacaba".toByteArray()
        val encoded = crcChecker.encodePacket(data)
        assertTrue(crcChecker.verifyPacket(encoded))
    }

    @Test
    fun testErrorDetection() {
        val data = "amogus".toByteArray()
        val encoded = crcChecker.encodePacket(data)
        val corrupted = makeError(encoded, byteIndex = 0, bitIndex = 3)
        assertFalse(crcChecker.verifyPacket(corrupted))
    }

    @Test
    fun testSplitPackets() {
        val text = "qwert ooi".toByteArray()
        val packets = crcChecker.splitIntoPackets(text)
        assertEquals(2, packets.size)
        assertEquals("qwert", packets[0].decodeToString())
        assertEquals(" ooi", packets[1].decodeToString())
    }

    @Test
    fun testSplitPackets1() {
        val text = "q   o".toByteArray()
        val packets = crcChecker.splitIntoPackets(text)
        assertEquals(1, packets.size)
        assertEquals("q   o", packets[0].decodeToString())
    }

    @Test
    fun testSplitPackets3() {
        val crcChecker = CrcChecker(packetSize = 8)
        val text = "qwertyui ooiuytrew".toByteArray()
        val packets = crcChecker.splitIntoPackets(text)
        assertEquals(3, packets.size)
        assertEquals("qwertyui", packets[0].decodeToString())
        assertEquals(" ooiuytr", packets[1].decodeToString())
        assertEquals("ew", packets[2].decodeToString())
    }

    @Test
    fun testMultipleErrors() {
        val crcChecker = CrcChecker(packetSize = 5)
        val data = "hello".toByteArray()
        val encoded = crcChecker.encodePacket(data)

        var corrupted = encoded.copyOf()
        corrupted = makeError(corrupted, 1, 1)
        corrupted = makeError(corrupted, 3, 5)
        corrupted = makeError(corrupted, corrupted.size - 2, 2)

        assertFalse(crcChecker.verifyPacket(corrupted))
    }

    @Test
    fun testEmpty() {
        val crcChecker = CrcChecker(packetSize = 5)
        val empty = ByteArray(0)
        val packets = crcChecker.splitIntoPackets(empty)
        assertTrue(packets.isEmpty())
    }

    @Test
    fun testBig() {
        val crcChecker = CrcChecker(packetSize = 10)
        val largeData = ByteArray(100) { it.toByte() }
        val packets = crcChecker.splitIntoPackets(largeData)
        assertEquals(10, packets.size)
        packets.forEach { packet ->
            val encoded = crcChecker.encodePacket(packet)
            assertTrue(crcChecker.verifyPacket(encoded))
        }
    }
}
