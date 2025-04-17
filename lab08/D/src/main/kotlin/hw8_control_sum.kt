// у меня не получилось запустить тесты без использования gradle, поэтому
// для запуска нужно открыть эту папку D как отдельный проект и там уже запустить тесты
// в IDE либо с помощью ./gradlew test

object BitChecksum {
    private const val K = 16

    fun getRawSum(data: ByteArray): Int {
        val allBits = data.joinToString(separator = "") {
            it.toInt().and(0xFF).toString(2).padStart(8, '0')
        }

        var sum = 0
        for (i in allBits.indices step K) {
            val blockBits = allBits.substring(i, (i + K).coerceAtMost(allBits.length))
            val blockValue = Integer.parseInt(blockBits, 2)
            sum += blockValue
        }
        return sum % (1 shl K)
    }

    fun calculateChecksum(data: ByteArray): Int {
        val rawSum = getRawSum(data)
        val allOnes = (1 shl K) - 1
        return rawSum xor allOnes
    }

    fun isValidChecksum(data: ByteArray, checksum: Int): Boolean {
        val rawSum = getRawSum(data)
        val result = rawSum xor checksum
        return result == (1 shl K) - 1
    }
}
