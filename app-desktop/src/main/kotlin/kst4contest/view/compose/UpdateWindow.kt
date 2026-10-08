package kst4contest.view.compose

import kst4contest.view.i18n.LocalStrings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.util.function.Consumer

/**
 * The update window.
 *
 * Shown at startup and only when there is an update; [UpdateWindowState.updateAvailable]
 * decides that, and it is tested. Non-blocking like the settings window: the operator
 * reads it when they get round to it.
 */
object UpdateWindow {

    private val host = ComposeWindowHost("update-window")

    @JvmStatic
    val isOpen: Boolean
        get() = host.isOpen

    @JvmStatic
    fun applyDarkMode(darkMode: Boolean) = host.applyDarkMode(darkMode)

    @JvmStatic
    fun close() = host.close()

    /**
     * @param openReleasePage opens the given address in the system browser; passed in
     *        because reaching the browser is the host application's business. A
     *        Consumer rather than a Kotlin function type: this is called from Java, and
     *        a Kotlin lambda parameter would force the caller to return Unit by hand.
     */
    @JvmStatic
    fun show(
        state: UpdateWindowState,
        darkMode: Boolean,
        baseFontSizeSp: Float,
        /**
         * The palette of the active profile, handed to the theme. Null draws the shipped
         * palette.
         */
        paletteStore: PaletteStore? = null,
        /**
         * The interface language of the active profile, handed to the window. Null draws the
         * base language.
         */
        languageStore: kst4contest.view.i18n.LanguageStore? = null,
        widthDp: Float,
        heightDp: Float,
        openReleasePage: Consumer<String>,
        onResized: (Float, Float) -> Unit,
    ) {
        host.show(
            title = { kst4contest.view.i18n.CurrentStrings.get().updateTitle },
            darkMode = darkMode,
            baseFontSizeSp = baseFontSizeSp,
            paletteStore = paletteStore,
            languageStore = languageStore,
            widthDp = widthDp,
            heightDp = heightDp,
            onResized = onResized,
            alwaysOnTop = true,
        ) { _ ->
            UpdateContent(state, openReleasePage::accept)
        }
    }
}

@Composable
private fun UpdateContent(state: UpdateWindowState, openReleasePage: (String) -> Unit) {
    val strings = LocalStrings.current
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Density.FIELD_GAP),
        ) {
            Text(strings.updateAvailable, style = MaterialTheme.typography.titleMedium)

            Form.section(strings.updateVersions) {
                Form.labelledRow(strings.updateInstalledVersion) { Text(state.installedVersion) }
                Form.labelledRow(strings.updateLatestVersion) { Text(state.latestVersion) }
            }

            Form.section(strings.updateMainChanges) { Text(state.majorChanges) }

            Form.section(strings.updateAdditionalInformation) { Text(state.adminMessage) }

            Form.section(strings.updateDownload) {
                Text(
                    strings.updateOpenReleasePage,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openReleasePage(state.releasePageUrl) },
                )
            }

            /*
             * The JavaFX window drew these as a TreeView with a hidden root. The depth
             * is fixed at two — a heading and its lines — so two expandable sections do
             * the same job without a tree component.
             */
            UpdateBranch(strings.updateChangeLog, state.changeLog)
            UpdateBranch(strings.updateKnownBugs, state.knownBugs)
        }
    }
}

/** One branch of the former tree: a heading, and sections that fold open. */
@Composable
private fun UpdateBranch(title: String, sections: List<UpdateSection>) {
    val strings = LocalStrings.current
    if (sections.isEmpty()) {
        return
    }

    Form.section(title) {
        sections.forEach { section ->
            var expanded by remember(section.title) { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(if (expanded) "▾" else "▸")
                    Text(section.title, fontWeight = FontWeight.Bold)
                }

                if (expanded) {
                    section.entries.forEach { entry ->
                        Text(
                            entry,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 20.dp, bottom = 1.dp),
                        )
                    }
                }

                HorizontalDivider(thickness = Density.HAIRLINE)
            }
        }
    }
}
