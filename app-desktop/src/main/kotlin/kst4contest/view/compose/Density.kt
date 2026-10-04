package kst4contest.view.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The measurements of the settings windows, in one place so the tabs cannot drift
 * apart.
 *
 * Material 3 ships phone densities: a text field is 56dp tall, a button 40dp and
 * fully rounded, a checkbox reserves a 48dp touch target. The Station tab alone has
 * over thirty fields, so those defaults turn a settings window into a scrolling
 * exercise and make a desktop application look like a phone.
 *
 * Every height here is a **minimum**, never a fixed size. The configured base font
 * size scales all text through Typography.scaledTo, and a fixed height would clip a
 * larger font instead of growing with it.
 */
internal object Density {

    /** Text fields, pickers and anything else on the field column. */
    val FIELD_MIN_HEIGHT = 30.dp

    /** Inside a text field. The vertical value is what buys back the 56dp. */
    val FIELD_CONTENT_PADDING = PaddingValues(horizontal = 8.dp, vertical = 2.dp)

    val BUTTON_MIN_HEIGHT = 24.dp

    val BUTTON_CONTENT_PADDING = PaddingValues(horizontal = 8.dp, vertical = 1.dp)

    /** Square-ish, not a pill: the pill shape is the strongest mobile tell. */
    val BUTTON_SHAPE = RoundedCornerShape(4.dp)

    /** One row of a list or table. */
    val ROW_MIN_HEIGHT = 26.dp

    val TAB_MIN_HEIGHT = 30.dp

    /**
     * Left and right of a tab title. Material reserves 90dp minimum width per tab,
     * which eleven tabs cannot live with; this is what a tab costs beyond its text.
     */
    val TAB_HORIZONTAL_PADDING = 10.dp

    /** The box itself; the 48dp touch target around it is switched off separately. */
    val CHECK_SIZE = 18.dp

    /** The label column of the forms, wide enough for the longest label in use. */
    val LABEL_COLUMN_WIDTH = 260.dp

    /** Between a label and its field, and between two fields. */
    val FIELD_GAP = 3.dp

    /** Between buttons in a row. */
    val BUTTON_GAP = 6.dp

    /** Around a selected row, and the hairline between rows. */
    val SELECTION_BORDER = 1.5.dp

    /** Kept free at the right edge so the scrollbar never sits on a control. */
    val SCROLLBAR_GUTTER = 12.dp

    /** Grab area between two columns; the line drawn inside it is a hairline. */
    val COLUMN_HANDLE_WIDTH = 5.dp

    val HAIRLINE = 1.dp
}
