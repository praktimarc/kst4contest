package kst4contest.view

/**
 * Build identity shown in window titles and in the about text.
 *
 * Reads the version the Gradle build stamped into the jar manifest and falls
 * back to a marker that is obviously not a release, so an unstamped build can
 * never be mistaken for one.
 */
object BuildInfo {

    const val UNKNOWN_VERSION: String = "dev-unstamped"

    val version: String
        get() = BuildInfo::class.java.`package`?.implementationVersion ?: UNKNOWN_VERSION

    fun windowTitle(base: String): String = "$base $version"
}
