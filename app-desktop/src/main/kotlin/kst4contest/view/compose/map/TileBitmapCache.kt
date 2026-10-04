package kst4contest.view.compose.map

import androidx.compose.ui.graphics.ImageBitmap

/**
 * The decoded tiles the map draws from, bounded.
 *
 * A decoded 256x256 tile costs about 262 KB against roughly 20 KB for the PNG it was
 * decoded from, so this cache is the expensive one and the one that has to have a limit.
 * It had none: an evening of panning at zoom 12 to 15 retained hundreds of megabytes of
 * bitmaps of squares nobody would look at again.
 *
 * Least-recently-drawn goes first, because the tiles worth keeping are the ones around
 * where the operator is looking, and reading a tile to draw it is exactly the signal for
 * that.
 *
 * Written to from a tile-fetch thread and read from the composition thread, so every
 * access is synchronised. A LinkedHashMap in access order is the eviction policy; it is
 * not thread-safe on its own, and access order means a plain read mutates it.
 */
internal class TileBitmapCache(private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    private val entries = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>): Boolean =
            size > maxEntries
    }

    val size: Int get() = synchronized(entries) { entries.size }

    operator fun get(key: String): ImageBitmap? = synchronized(entries) { entries[key] }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(entries) {
            if (maxEntries <= 0) {
                return
            }
            entries[key] = bitmap
        }
    }

    fun remove(key: String) {
        synchronized(entries) { entries.remove(key) }
    }

    fun contains(key: String): Boolean = synchronized(entries) { entries.containsKey(key) }

    private companion object {
        /**
         * Enough for several screens' worth at any zoom, so panning back and forth over
         * the same area never refetches, and about 130 MB of bitmaps at worst.
         */
        const val DEFAULT_MAX_ENTRIES = 512
    }
}
