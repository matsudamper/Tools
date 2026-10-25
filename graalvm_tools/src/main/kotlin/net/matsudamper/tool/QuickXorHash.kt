package net.matsudamper.tool

import java.io.InputStream
import java.util.Base64

/**
 * SharePoint/OneDrive for Businessで使われるQuickXorHash
 * https://learn.microsoft.com/en-us/onedrive/developer/code-snippets/quickxorhash
 */
class QuickXorHash {
    private val data = LongArray((WIDTH_IN_BITS - 1) / 64 + 1)
    private var lengthSoFar: Long = 0
    private var shiftSoFar: Int = 0

    fun update(array: ByteArray, start: Int = 0, size: Int = array.size) {
        var vectorArrayIndex = shiftSoFar / 64
        var vectorOffset = shiftSoFar % 64
        val iterations = minOf(size, WIDTH_IN_BITS)

        for (i in 0 until iterations) {
            val isLastCell = vectorArrayIndex == data.size - 1
            val bitsInVectorCell = if (isLastCell) WIDTH_IN_BITS % 64 else 64

            if (vectorOffset <= bitsInVectorCell - 8) {
                var j = start + i
                while (j < start + size) {
                    data[vectorArrayIndex] = data[vectorArrayIndex] xor
                            ((array[j].toLong() and 0xFF) shl vectorOffset)
                    j += WIDTH_IN_BITS
                }
            } else {
                val index1 = vectorArrayIndex
                val index2 = if (isLastCell) 0 else vectorArrayIndex + 1
                val low = bitsInVectorCell - vectorOffset

                var xoredByte = 0L
                var j = start + i
                while (j < start + size) {
                    xoredByte = xoredByte xor (array[j].toLong() and 0xFF)
                    j += WIDTH_IN_BITS
                }
                data[index1] = data[index1] xor (xoredByte shl vectorOffset)
                data[index2] = data[index2] xor (xoredByte ushr low)
            }

            vectorOffset += SHIFT
            while (vectorOffset >= bitsInVectorCell) {
                vectorArrayIndex = if (isLastCell) 0 else vectorArrayIndex + 1
                vectorOffset -= bitsInVectorCell
            }
        }

        shiftSoFar = ((shiftSoFar + SHIFT.toLong() * (size % WIDTH_IN_BITS)) % WIDTH_IN_BITS).toInt()
        lengthSoFar += size
    }

    fun digest(): ByteArray {
        val result = ByteArray((WIDTH_IN_BITS - 1) / 8 + 1)
        for (i in data.indices) {
            for (b in 0 until 8) {
                val index = i * 8 + b
                if (index >= result.size) break
                result[index] = (data[i] ushr (b * 8)).toByte()
            }
        }
        for (b in 0 until 8) {
            val index = WIDTH_IN_BITS / 8 - 8 + b
            result[index] = (result[index].toInt() xor (lengthSoFar ushr (b * 8)).toInt()).toByte()
        }
        return result
    }

    fun digestBase64(): String = Base64.getEncoder().encodeToString(digest())

    companion object {
        private const val WIDTH_IN_BITS = 160
        private const val SHIFT = 11

        fun hashBase64(inputStream: InputStream): String {
            val hash = QuickXorHash()
            val buffer = ByteArray(1024 * 1024)
            inputStream.use { stream ->
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    hash.update(buffer, 0, read)
                }
            }
            return hash.digestBase64()
        }
    }
}
