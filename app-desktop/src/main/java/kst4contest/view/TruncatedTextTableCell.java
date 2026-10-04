package kst4contest.view;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.function.Function;
import java.util.function.BiFunction;

/**
 * Displays text normally and exposes the full value only when it is clipped.
 * An optional functional explanation remains available and is combined with
 * the full value when both are needed.
 */
public class TruncatedTextTableCell<S> extends TableCell<S, String> {

    private final Function<String, String> formatter;
    private final BiFunction<S, String, String> functionalTooltipProvider;
    private final Tooltip tooltip = new Tooltip();
    private final Text textMeasurement = new Text();
    private String fullText = "";

    public TruncatedTextTableCell() {
        this(Function.identity(), null);
    }

    public TruncatedTextTableCell(Function<String, String> formatter) {
        this(formatter, null);
    }

    public TruncatedTextTableCell(
            Function<String, String> formatter,
            BiFunction<S, String, String> functionalTooltipProvider
    ) {
        this.formatter = formatter == null ? Function.identity() : formatter;
        this.functionalTooltipProvider = functionalTooltipProvider;
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(800);
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setShowDuration(Duration.seconds(30));
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            fullText = "";
            setText(null);
            setGraphic(null);
            setTooltip(null);
            return;
        }

        String formatted = formatter.apply(item);
        fullText = formatted == null ? "" : formatted;
        setText(fullText);
        setGraphic(null);
        updateTooltip();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateTooltip();
    }

    private void updateTooltip() {
        if (isEmpty()) {
            setTooltip(null);
            return;
        }

        String functionalText = resolveFunctionalTooltip();
        boolean clipped = isTextClipped();
        String tooltipText = TruncatedTextTooltipSupport.buildTooltipText(
                fullText,
                clipped,
                functionalText
        );
        if (tooltipText == null) {
            setTooltip(null);
            return;
        }

        tooltip.setText(tooltipText);
        setTooltip(tooltip);
    }

    private String resolveFunctionalTooltip() {
        if (functionalTooltipProvider == null || getTableRow() == null) {
            return null;
        }
        return functionalTooltipProvider.apply(getTableRow().getItem(), fullText);
    }

    private boolean isTextClipped() {
        if (fullText.isEmpty()) {
            return false;
        }

        textMeasurement.setText(fullText);
        textMeasurement.setFont(getFont());
        double requiredWidth = textMeasurement.getLayoutBounds().getWidth();
        double availableWidth = Math.max(0.0,
                getWidth() - snappedLeftInset() - snappedRightInset() - 2.0);
        return TruncatedTextTooltipSupport.isTextClipped(requiredWidth, availableWidth);
    }
}
