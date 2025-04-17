import kotlin.experimental.inv
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class BitChecksumTest {
    private fun toBytes(vararg values: Int): ByteArray {
        return values.flatMap { value ->
            listOf((value shr 8).toByte(), (value and 0xFF).toByte())
        }.toByteArray()
    }

    @Test
    fun testValidChecksum() {
        val data = toBytes(735, 812, 52, 2570)
        val checksum = BitChecksum.calculateChecksum(data)
        assertTrue(BitChecksum.isValidChecksum(data, checksum), "Checksum should be valid")
    }

    @Test
    fun testCorruptedData() {
        val data = toBytes(735, 812, 52, 2570)
        val checksum = BitChecksum.calculateChecksum(data)

        val corruptedMiddle = data.copyOf()
        corruptedMiddle[2] = corruptedMiddle[2].inc()
        assertFalse(BitChecksum.isValidChecksum(corruptedMiddle, checksum), "Checksum should fail on corrupted middle byte")

        val corruptedFirst = data.copyOf()
        corruptedFirst[0] = corruptedFirst[0].inv()
        assertFalse(BitChecksum.isValidChecksum(corruptedFirst, checksum), "Checksum should fail on corrupted first byte")

        val corruptedLast = data.copyOf()
        corruptedLast[corruptedLast.size - 1] = (corruptedLast.last().toInt() xor 0x01).toByte()
        assertFalse(BitChecksum.isValidChecksum(corruptedLast, checksum), "Checksum should fail on corrupted last byte")
    }

    @Test
    fun testOddLengthData() {
        val data = toBytes(812, 52) + byteArrayOf(0x1F)
        val checksum = BitChecksum.calculateChecksum(data)
        assertTrue(BitChecksum.isValidChecksum(data, checksum), "Checksum should be valid with odd-length data")
    }

    @Test
    fun testChecksumIsConsistent() {
        val data = toBytes(2570, 52, 812, 735)
        val checksum1 = BitChecksum.calculateChecksum(data)
        val checksum2 = BitChecksum.calculateChecksum(data)
        assertEquals(checksum1, checksum2, "Checksum should be consistent")
    }
}
