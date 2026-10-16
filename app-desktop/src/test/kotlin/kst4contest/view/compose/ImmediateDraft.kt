package kst4contest.view.compose

/**
 * A draft that applies every change the moment it is staged.
 *
 * For the tests that pin a tab state's own rules -- validation, normalisation, which effect
 * an edit triggers -- without the draft in between. The draft itself is pinned by
 * SettingsDraftTest, and that every drafted setting really waits for Apply by
 * DraftedSettingsTableTest. A failed write or effect throws here, so a test cannot pass
 * over one unnoticed.
 */
internal class ImmediateDraft : SettingsDraft() {

    override fun <T> stage(
        key: String,
        value: T,
        live: () -> T,
        write: (T) -> Unit,
        effectKey: String?,
        effect: (() -> Unit)?,
    ) {
        super.stage(key, value, live, write, effectKey, effect)
        val outcome = apply()
        check(outcome.isClean) { "apply failed: ${outcome.failures}" }
    }
}
