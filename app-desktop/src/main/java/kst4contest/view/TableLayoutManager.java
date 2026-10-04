package kst4contest.view;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import kst4contest.model.ChatPreferences;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Applies persisted leaf-column widths and performs one content-based initial
 * sizing pass when no width has been stored yet.
 */
public final class TableLayoutManager {

    private static final double CELL_HORIZONTAL_PADDING = 16.0;
    private static final double DEFAULT_MINIMUM_WIDTH = 24.0;

    private TableLayoutManager() {
    }

    public static ColumnSpec column(String id, TableColumn<?, String> column) {
        return new ColumnSpec(id, column);
    }

    public static <S> void install(
            TableView<S> table,
            String tableId,
            ChatPreferences preferences,
            LayoutAutosave autosave,
            ColumnSpec... columnSpecs
    ) {
        Objects.requireNonNull(table, "table");
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(preferences, "preferences");
        Objects.requireNonNull(autosave, "autosave");

        Map<TableColumn<?, String>, ColumnState> states = new IdentityHashMap<>();
        for (ColumnSpec spec : columnSpecs) {
            if (spec == null || !spec.column.getColumns().isEmpty()) {
                continue;
            }

            if (spec.column.prefWidthProperty().isBound()) {
                spec.column.prefWidthProperty().unbind();
            }
            spec.column.setId(tableId + "." + spec.id);

            OptionalDouble storedWidth = preferences.getTableColumnWidth(tableId, spec.id);
            ColumnState state = new ColumnState(spec, storedWidth.isEmpty());
            states.put(spec.column, state);

            if (storedWidth.isPresent()) {
                setWidth(state, storedWidth.getAsDouble());
                state.initialized = true;
            }

            spec.column.widthProperty().addListener((observable, oldWidth, newWidth) -> {
                if (state.adjusting || !state.initialized || newWidth == null) {
                    return;
                }
                storeWidth(preferences, autosave, tableId, state.spec.id, newWidth.doubleValue());
            });
        }

        installEarlyManualResizeDetection(table, tableId, preferences, autosave, states);
        scheduleInitialSizingWhenUsable(table, tableId, preferences, autosave, states);
    }

    private static <S> void scheduleInitialSizingWhenUsable(
            TableView<S> table,
            String tableId,
            ChatPreferences preferences,
            LayoutAutosave autosave,
            Map<TableColumn<?, String>, ColumnState> states
    ) {
        if (states.values().stream().noneMatch(state -> !state.initialized)) {
            return;
        }

        if (table.getItems() != null && !table.getItems().isEmpty()) {
            Platform.runLater(() -> sizePendingColumns(table, tableId, preferences, autosave, states));
            return;
        }

        @SuppressWarnings("unchecked")
        final ListChangeListener<S>[] holder = new ListChangeListener[1];
        holder[0] = change -> {
            if (table.getItems() == null || table.getItems().isEmpty()) {
                return;
            }
            table.getItems().removeListener(holder[0]);
            Platform.runLater(() -> sizePendingColumns(table, tableId, preferences, autosave, states));
        };
        table.getItems().addListener(holder[0]);
    }

    private static <S> void sizePendingColumns(
            TableView<S> table,
            String tableId,
            ChatPreferences preferences,
            LayoutAutosave autosave,
            Map<TableColumn<?, String>, ColumnState> states
    ) {
        for (ColumnState state : states.values()) {
            if (state.initialized) {
                continue;
            }

            double width = state.spec.flexible
                    ? flexibleInitialWidth(table, state.spec)
                    : contentInitialWidth(table, state.spec);
            setWidth(state, width);
            state.initialized = true;
            storeWidth(preferences, autosave, tableId, state.spec.id, width);
        }
    }

    private static <S> double contentInitialWidth(TableView<S> table, ColumnSpec spec) {
        Text measurement = new Text();
        measurement.setFont(Font.getDefault());
        double requiredWidth = measure(spec.column.getText(), measurement);
        for (int rowIndex = 0; rowIndex < table.getItems().size(); rowIndex++) {
            ObservableValue<?> value = spec.column.getCellObservableValue(rowIndex);
            if (value == null || value.getValue() == null) {
                continue;
            }
            requiredWidth = Math.max(
                    requiredWidth,
                    measure(String.valueOf(value.getValue()), measurement)
            );
        }
        return calculateInitialContentWidth(
                requiredWidth,
                spec.minimumWidth,
                spec.maximumInitialWidth
        );
    }

    private static <S> double flexibleInitialWidth(TableView<S> table, ColumnSpec spec) {
        double tableShare = table.getWidth() > 1.0 ? table.getWidth() * 0.42 : spec.flexibleFallbackWidth;
        Text measurement = new Text();
        measurement.setFont(Font.getDefault());
        double headerWidth = measure(spec.column.getText(), measurement) + CELL_HORIZONTAL_PADDING;
        return clamp(Math.max(headerWidth, tableShare), spec.minimumWidth, spec.maximumInitialWidth);
    }

    private static <S> void installEarlyManualResizeDetection(
            TableView<S> table,
            String tableId,
            ChatPreferences preferences,
            LayoutAutosave autosave,
            Map<TableColumn<?, String>, ColumnState> states
    ) {
        Map<TableColumn<?, String>, Double> widthsAtHeaderPress = new IdentityHashMap<>();
        table.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!isColumnHeaderEvent(event)) {
                return;
            }
            widthsAtHeaderPress.clear();
            states.forEach((column, state) -> widthsAtHeaderPress.put(column, column.getWidth()));
        });
        table.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (widthsAtHeaderPress.isEmpty()) {
                return;
            }
            states.forEach((column, state) -> {
                Double oldWidth = widthsAtHeaderPress.get(column);
                if (oldWidth == null || Math.abs(oldWidth - column.getWidth()) <= 0.5) {
                    return;
                }
                state.initialized = true;
                storeWidth(preferences, autosave, tableId, state.spec.id, column.getWidth());
            });
            widthsAtHeaderPress.clear();
        });
    }

    private static boolean isColumnHeaderEvent(MouseEvent event) {
        Object target = event.getTarget();
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null && node.getParent() != null) {
            if (node.getStyleClass().contains("column-header")
                    || node.getStyleClass().contains("nested-column-header")) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private static void setWidth(ColumnState state, double width) {
        state.adjusting = true;
        try {
            state.spec.column.setPrefWidth(width);
        } finally {
            state.adjusting = false;
        }
    }

    private static void storeWidth(
            ChatPreferences preferences,
            LayoutAutosave autosave,
            String tableId,
            String columnId,
            double width
    ) {
        preferences.setTableColumnWidth(tableId, columnId, width);
        autosave.requestSave();
    }

    private static double measure(String value, Text measurement) {
        measurement.setText(value == null ? "" : value);
        return measurement.getLayoutBounds().getWidth();
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    /**
     * Calculates a compact content width. Package-private for focused sizing tests.
     */
    @SuppressWarnings("PMD.CommentDefaultAccessModifier")
    static double calculateInitialContentWidth(
            final double measuredWidth,
            final double minimum,
            final double maximum
    ) {
        return clamp(measuredWidth + CELL_HORIZONTAL_PADDING, minimum, maximum);
    }

    public static final class ColumnSpec {
        private final String id;
        private final TableColumn<?, String> column;
        private double minimumWidth = DEFAULT_MINIMUM_WIDTH;
        private double maximumInitialWidth = Double.MAX_VALUE;
        private double flexibleFallbackWidth = 320.0;
        private boolean flexible;

        private ColumnSpec(String id, TableColumn<?, String> column) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Column id must not be blank");
            }
            this.id = id;
            this.column = Objects.requireNonNull(column, "column");
        }

        public ColumnSpec maximumInitialWidth(double maximumInitialWidth) {
            this.maximumInitialWidth = maximumInitialWidth;
            return this;
        }

        public ColumnSpec flexible(double fallbackWidth) {
            flexible = true;
            flexibleFallbackWidth = fallbackWidth;
            return this;
        }
    }

    private static final class ColumnState {
        private final ColumnSpec spec;
        private boolean adjusting;
        private boolean initialized;

        private ColumnState(ColumnSpec spec, boolean awaitingInitialSizing) {
            this.spec = spec;
            initialized = !awaitingInitialSizing;
        }
    }
}
