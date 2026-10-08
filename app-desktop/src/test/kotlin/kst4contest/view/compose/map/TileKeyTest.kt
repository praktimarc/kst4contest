package kst4contest.view.compose.map

import kst4contest.view.map.MapViewport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Which tiles a view needs.
 *
 * The world wraps east to west; the tile index does not. A view straddling the
 * antimeridian asks for indices outside the valid range, and asking the tile service
 * for one of those is a 404 — a hole in the map rather than a wrapped tile.
 */
class TileKeyTest {

    @Test
    fun `a tile index past the eastern edge comes back round to the west`() {
        assertEquals(0, wrapTileX(64, 63))
        assertEquals(1, wrapTileX(65, 63))
        assertEquals(63, wrapTileX(-1, 63))
        assertEquals(62, wrapTileX(-2, 63))
        assertEquals(0, wrapTileX(-64, 63))
    }

    @Test
    fun `a tile index inside the range is left alone`() {
        assertEquals(0, wrapTileX(0, 63))
        assertEquals(33, wrapTileX(33, 63))
        assertEquals(63, wrapTileX(63, 63))
    }

    @Test
    fun `every tile a view asks for exists at that zoom`() {
        for (zoom in 3..10) {
            val lastIndex = (1 shl zoom) - 1
            /* Hard against the antimeridian, where the wrap actually happens. */
            val viewport = MapViewport(179.5, 0.0, zoom.toDouble(), 1200.0, 800.0)

            val keys = visibleTileKeys(viewport)

            assertTrue(keys.isNotEmpty()) { "no tiles requested at zoom $zoom" }
            assertTrue(keys.all { it.x in 0..lastIndex && it.y in 0..lastIndex }) {
                "zoom $zoom asked for " + keys.filter { it.x !in 0..lastIndex || it.y !in 0..lastIndex }
            }
            assertTrue(keys.all { it.zoom == zoom })
        }
    }

    @Test
    fun `a view beyond the poles asks for no tiles above or below the world`() {
        val viewport = MapViewport(10.0, 84.0, 3.0, 1200.0, 800.0)

        val keys = visibleTileKeys(viewport)
        val lastIndex = (1 shl 3) - 1

        assertTrue(keys.all { it.y in 0..lastIndex }) { "asked for a row that does not exist: $keys" }
    }

    @Test
    fun `the cache key is the path the tile service uses`() {
        assertEquals("6/33/21", TileKey(6, 33, 21).cacheKey)
    }
}
