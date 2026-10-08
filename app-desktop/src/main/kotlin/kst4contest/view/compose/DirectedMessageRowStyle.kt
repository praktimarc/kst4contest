package kst4contest.view.compose

import androidx.compose.ui.graphics.Color
import kst4contest.view.PrivateMessageRowStyleResolver

/**
 * The row colouring of the directed-message table.
 *
 * A new private message is highlighted and the highlight fades over five minutes, so a call
 * that arrived while the operator was looking at the station list is still findable — being
 * noticed is what that pane exists for. Own messages are blue at any age: they are context,
 * not news.
 *
 * The steps come from [PrivateMessageRowStyleResolver], which the JavaFX row factory uses, so
 * the two cannot drift apart. The colours are the stylesheets' `.messageHighlight*-column`,
 * identical in both designs.
 */
object DirectedMessageRowStyle {

    /** `.messageHighlightOwn-column`. */
    val OWN = Color(0xFF00FFFF)

    /** `.messageHighlight30-column`: just arrived. */
    val FRESH = Color(0xFF33CC33)

    /**
     * The fading green of `.messageHighlight30` through `.messageHighlight300`, in the order
     * the resolver's age limits name them.
     */
    private val FADE = listOf(
        FRESH,
        Color(0xFF40BF40),
        Color(0xFF4DB34D),
        Color(0xFF59A659),
        Color(0xFF669966),
        Color(0xFF738C73),
    )

    /** The resolver's style class names, in the same order as [FADE]. */
    private val FADE_CLASSES = listOf(
        "messageHighlight30-column",
        "messageHighlight60-column",
        "messageHighlight90-column",
        "messageHighlight120-column",
        "messageHighlight180-column",
        "messageHighlight300-column",
    )

    /**
     * How a row is coloured, or null once the message is no longer news.
     *
     * The style class is asked of the resolver rather than recomputed here, so a change to
     * the fade steps reaches both views at once.
     */
    fun accentFor(ownMessage: Boolean, ageSeconds: Long): CellAccent? {
        val styleClass = PrivateMessageRowStyleResolver.resolveStyleClass(ownMessage, ageSeconds)
            ?: return null

        if (styleClass == PrivateMessageRowStyleResolver.OWN_STYLE_CLASS) {
            return CellAccent(background = OWN, foreground = Color.Black)
        }

        val index = FADE_CLASSES.indexOf(styleClass)
        if (index < 0) return null

        return CellAccent(background = FADE[index], foreground = Color.White)
    }
}
