package net.matsudamper.tool

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuickXorHashTest {
    private fun data(size: Int) = ByteArray(size) { ((it * 31 + 7) % 256).toByte() }

    @Test
    fun hash() {
        // 期待値はrcloneのquickxorhash実装で算出
        mapOf(
            0 to "AAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            1 to "BwAAAAAAAAAAAAAAAQAAAAAAAAA=",
            160 to "GD+KGCbnJ+lXCrS07BVjSpWmgvs=",
            161 to "fz+KGCbnJ+lXCrS07RVjSpWmgvs=",
            100000 to "GD+KGCbnJ+lXCrS07JNiSpWmgvs=",
        ).forEach { (size, expected) ->
            assertEquals(expected, QuickXorHash.hashBase64(data(size).inputStream()), "size=$size")
        }
    }

    @Test
    fun hashSplitUpdate() {
        val bytes = data(100000)
        val hash = QuickXorHash()
        var offset = 0
        for (chunk in listOf(1, 7, 159, 160, 161, 1000)) {
            hash.update(bytes, offset, chunk)
            offset += chunk
        }
        hash.update(bytes, offset, bytes.size - offset)
        assertEquals("GD+KGCbnJ+lXCrS07JNiSpWmgvs=", hash.digestBase64())
    }
}
