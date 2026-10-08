package kst4contest.view.compose.tabs

import kst4contest.view.compose.JavaFxStylesheet
import java.io.File

/** What writing the shipped stylesheet into the profile did. */
sealed interface StylesheetExportOutcome {

    /** Written; the operator can now edit it by hand. */
    data class Written(val path: String) : StylesheetExportOutcome

    /**
     * There was already a file, and it was left exactly as it was.
     *
     * The important case. That file is the operator's own work, and replacing it on a
     * misclick is the one thing this button could do that cannot be undone from inside the
     * application.
     */
    data class AlreadyThere(val path: String) : StylesheetExportOutcome

    /** Nothing was written, and this is why. */
    data class Failed(val reason: String) : StylesheetExportOutcome
}

/**
 * Writes the shipped stylesheet of one design to a file, unless something is already there.
 *
 * The file is the middle of the three palette layers — the hand-editing route the copy in the
 * application directory has been promising since it was first written and never read. Giving
 * the operator the shipped sheet as a starting point is what makes that route usable: the
 * roles live on three different selectors, which is not guessable.
 *
 * @param target where to write; its directory is created when missing
 * @param darkMode true for the evening design, false for the daylight one
 * @return what happened, never an exception
 */
fun exportShippedStylesheet(target: File, darkMode: Boolean): StylesheetExportOutcome {

    if (target.exists()) {
        return StylesheetExportOutcome.AlreadyThere(target.path)
    }

    val resource =
        if (darkMode) JavaFxStylesheet.EVENING_RESOURCE else JavaFxStylesheet.DAYLIGHT_RESOURCE

    return runCatching {
        target.parentFile?.mkdirs()

        val source = JavaFxStylesheet::class.java.getResource(resource)
            ?: return StylesheetExportOutcome.Failed("$resource is not on the classpath")

        target.writeText(source.readText())
        StylesheetExportOutcome.Written(target.path) as StylesheetExportOutcome
    }.getOrElse { failure ->
        StylesheetExportOutcome.Failed(
            failure.message ?: failure.javaClass.simpleName
        )
    }
}
