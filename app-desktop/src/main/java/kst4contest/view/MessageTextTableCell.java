package kst4contest.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays a single-line message text in a TableView.
 *
 * <p>The cell provides two functions that JavaFX's default text cell cannot
 * combine:
 * <ul>
 *     <li>a tooltip containing the complete text, but only while the visible
 *     cell is too narrow for that text;</li>
 *     <li>clickable HTTP, HTTPS and www links inside otherwise normal text.</li>
 * </ul>
 *
 * <p>The row type is generic because the same implementation is used by the
 * ChatMessage tables and the DX-cluster table.</p>
 *
 * @param <S> row type of the surrounding TableView
 */
public final class MessageTextTableCell<S> extends TableCell<S, String> {

    private static final Pattern WEB_LINK_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\"']+"
    );

    private static final String TRAILING_LINK_PUNCTUATION = ".,;:!?)]}";

    private final Consumer<String> linkOpener;
    private final Predicate<String> highlightedTextPredicate;
    private final String normalStyleClass;
    private final String highlightedStyleClass;

    private final HBox contentBox = new HBox(0);
    private final Rectangle contentClip = new Rectangle();
    private final Tooltip fullTextTooltip = new Tooltip();

    private String displayedText = "";
    private boolean fullTextTooltipInstalled = false;

    /**
     * Creates a normal message cell without additional text highlighting.
     *
     * @param linkOpener callback which opens a validated HTTP or HTTPS URL
     */
    public MessageTextTableCell(Consumer<String> linkOpener) {
        this(linkOpener, text -> false, null, null);
    }

    /**
     * Creates a message cell with optional CSS highlighting.
     *
     * <p>KST4Contest uses this variant for public messages which contain the
     * operator's own callsign. Only the supplied CSS classes are added or
     * removed. The standard {@code table-cell} class and all other JavaFX
     * state remain untouched.</p>
     *
     * @param linkOpener              callback which opens a validated URL
     * @param highlightedTextPredicate identifies highlighted messages
     * @param normalStyleClass        CSS class for normal messages, may be null
     * @param highlightedStyleClass   CSS class for highlighted messages, may be null
     */
    public MessageTextTableCell(
            Consumer<String> linkOpener,
            Predicate<String> highlightedTextPredicate,
            String normalStyleClass,
            String highlightedStyleClass
    ) {
        this.linkOpener = Objects.requireNonNull(linkOpener, "linkOpener");
        this.highlightedTextPredicate = highlightedTextPredicate == null
                ? text -> false
                : highlightedTextPredicate;
        this.normalStyleClass = normalStyleClass;
        this.highlightedStyleClass = highlightedStyleClass;

        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setFillHeight(false);
        contentBox.setClip(contentClip);

        fullTextTooltip.setWrapText(true);
        fullTextTooltip.setMaxWidth(800);
        fullTextTooltip.setShowDelay(Duration.millis(250));
        fullTextTooltip.setShowDuration(Duration.seconds(30));

        setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        removeOptionalStyleClasses();
        contentBox.getChildren().clear();

        if (empty || item == null || item.isEmpty()) {
            displayedText = "";
            fullTextTooltip.setText("");
            setText(null);
            setGraphic(null);
            setClippedTooltipActive(false);
            return;
        }

        displayedText = item;
        fullTextTooltip.setText(item);
        applyOptionalStyleClass(item);
        buildContent(item);

        setText(null);
        setGraphic(contentBox);
        updateClipAndTooltip();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        updateClipAndTooltip();
    }

    /**
     * Splits the displayed text into ordinary labels and clickable links.
     */
    private void buildContent(String text) {
        Matcher matcher = WEB_LINK_PATTERN.matcher(text);
        int nextPlainTextStart = 0;

        while (matcher.find()) {
            String rawMatch = matcher.group();
            String linkText = removeTrailingPunctuation(rawMatch);

            if (linkText.isEmpty()) {
                continue;
            }

            appendPlainText(text.substring(nextPlainTextStart, matcher.start()));
            appendLink(linkText);

            /*
             * Punctuation removed from the URL remains ordinary message text.
             * Starting here ensures it is included by the next substring.
             */
            nextPlainTextStart = matcher.start() + linkText.length();
        }

        appendPlainText(text.substring(nextPlainTextStart));
    }

    private void appendPlainText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        Label textFragment = new Label(text);
        textFragment.setPadding(Insets.EMPTY);
        textFragment.setMinWidth(Region.USE_PREF_SIZE);
        textFragment.setMaxWidth(Region.USE_PREF_SIZE);
        textFragment.setMouseTransparent(true);
        textFragment.textFillProperty().bind(textFillProperty());
        textFragment.getStyleClass().add("message-text-fragment");

        contentBox.getChildren().add(textFragment);
    }

    private void appendLink(String linkText) {
        Hyperlink hyperlink = new Hyperlink(linkText);
        hyperlink.setPadding(Insets.EMPTY);
        hyperlink.setMinWidth(Region.USE_PREF_SIZE);
        hyperlink.setMaxWidth(Region.USE_PREF_SIZE);
        hyperlink.setFocusTraversable(false);
        hyperlink.getStyleClass().add("message-table-link");
        hyperlink.setOnAction(event -> {
            openLink(linkText);
            event.consume();
        });

        contentBox.getChildren().add(hyperlink);
    }

    /**
     * Opens only HTTP and HTTPS targets. A visible www address receives an
     * HTTPS scheme before it is passed to the application.
     */
    private void openLink(String linkText) {
        try {
            String normalizedLink = linkText.toLowerCase(Locale.ROOT).startsWith("www.")
                    ? "https://" + linkText
                    : linkText;

            URI uri = URI.create(normalizedLink);
            String scheme = uri.getScheme();

            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))) {
                return;
            }

            linkOpener.accept(uri.toASCIIString());
        } catch (RuntimeException exception) {
            System.out.println(
                    "[MessageTextTableCell] Cannot open malformed link: "
                            + linkText
                            + " / "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Removes punctuation which commonly follows a link in normal prose.
     */
    private String removeTrailingPunctuation(String rawLink) {
        String result = rawLink;

        while (!result.isEmpty()
                && TRAILING_LINK_PUNCTUATION.indexOf(
                result.charAt(result.length() - 1)
        ) >= 0) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * Clips the one-line content and installs the full-text tooltip only if
     * the rendered nodes are wider than the usable cell area.
     */
    private void updateClipAndTooltip() {
        if (displayedText.isEmpty() || getGraphic() == null) {
            setClippedTooltipActive(false);
            return;
        }

        double availableWidth = Math.max(
                0,
                getWidth() - snappedLeftInset() - snappedRightInset()
        );
        double availableHeight = Math.max(
                0,
                getHeight() - snappedTopInset() - snappedBottomInset()
        );

        contentClip.setWidth(availableWidth);
        contentClip.setHeight(availableHeight);

        boolean textIsClipped = contentBox.prefWidth(-1) > availableWidth + 1;
        setClippedTooltipActive(textIsClipped);
    }

    /**
     * Installs the tooltip on the actual graphic node below the mouse pointer.
     *
     * <p>Installing it on the TableCell itself is not reliable when the cell
     * displays an HBox containing labels and hyperlinks. The graphic node is
     * the effective mouse target. Installation is tracked explicitly so
     * repeated layout passes neither add duplicate handlers nor leave a
     * tooltip attached to a reused empty cell.</p>
     */
    private void setClippedTooltipActive(boolean active) {
        /*
         * The tooltip is handled exclusively by the graphic node. Keeping a
         * second tooltip on the TableCell would allow two competing tooltip
         * targets for the same visible content.
         */
        setTooltip(null);

        if (active == fullTextTooltipInstalled) {
            return;
        }

        if (active) {
            Tooltip.install(contentBox, fullTextTooltip);
        } else {
            fullTextTooltip.hide();
            Tooltip.uninstall(contentBox, fullTextTooltip);
        }

        fullTextTooltipInstalled = active;
    }

    private void applyOptionalStyleClass(String item) {
        boolean highlighted;

        try {
            highlighted = highlightedTextPredicate.test(item);
        } catch (RuntimeException exception) {
            highlighted = false;
        }

        String styleClass = highlighted
                ? highlightedStyleClass
                : normalStyleClass;

        if (styleClass != null
                && !styleClass.isBlank()
                && !getStyleClass().contains(styleClass)) {
            getStyleClass().add(styleClass);
        }
    }

    private void removeOptionalStyleClasses() {
        if (normalStyleClass != null) {
            getStyleClass().remove(normalStyleClass);
        }
        if (highlightedStyleClass != null) {
            getStyleClass().remove(highlightedStyleClass);
        }
    }
}