package kst4contest.view.compose.map

import androidx.compose.ui.graphics.ImageBitmap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * The decoded tiles the map draws from.
 *
 * A decoded 256x256 tile is about 262 KB, against roughly 20 KB for the PNG it came
 * from. The encoded cache below this one is capped at 2048 entries; this one was not
 * capped at all, so an evening of panning at zoom 12 to 15 retained hundreds of
 * megabytes of bitmaps that will never be looked at again.
 */
class TileBitmapCacheTest {

    private fun bitmap() = ImageBitmap(1, 1)

    @Test
    fun `a tile that was put in comes back out`() {
        val cache = TileBitmapCache(maxEntries = 4)
        val tile = bitmap()

        cache.put("6/1/1", tile)

        assertEquals(tile, cache["6/1/1"])
    }

    @Test
    fun `an unknown tile is absent rather than an error`() {
        assertNull(TileBitmapCache(maxEntries = 4)["6/1/1"])
    }

    @Test
    fun `the cache does not grow past its bound`() {
        val cache = TileBitmapCache(maxEntries = 3)

        repeat(10) { cache.put("6/$it/0", bitmap()) }

        assertEquals(3, cache.size)
    }

    /** The one dropped is the one that has gone longest without being drawn. */
    @Test
    fun `the least recently drawn tile is the one dropped`() {
        val cache = TileBitmapCache(maxEntries = 3)
        cache.put("a", bitmap())
        cache.put("b", bitmap())
        cache.put("c", bitmap())

        /* Drawing "a" again makes "b" the oldest. */
        cache["a"]
        cache.put("d", bitmap())

        assertNotNull(cache["a"])
        assertNull(cache["b"])
        assertNotNull(cache["c"])
        assertNotNull(cache["d"])
    }

    @Test
    fun `a tile that turned out to be unusable can be removed`() {
        val cache = TileBitmapCache(maxEntries = 3)
        cache.put("6/1/1", bitmap())

        cache.remove("6/1/1")

        assertNull(cache["6/1/1"])
        assertEquals(0, cache.size)
    }

    @Test
    fun `a bound of zero keeps nothing rather than everything`() {
        val cache = TileBitmapCache(maxEntries = 0)

        cache.put("6/1/1", bitmap())

        assertEquals(0, cache.size)
    }
}
