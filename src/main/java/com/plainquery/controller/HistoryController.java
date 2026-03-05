package com.plainquery.controller;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryHistoryEntry;
import com.plainquery.service.HistoryService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class HistoryController {

    private static final Logger LOG = Logger.getLogger(HistoryController.class.getName());

    private static final DateTimeFormatter DISPLAY_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int DEFAULT_RECENT_LIMIT = 100;

    @FXML private ListView<QueryHistoryEntry> historyList;
    @FXML private TextField                   searchField;
    @FXML private Label                       statusLabel;

    private HistoryService historyService;
    private Consumer<String> onSelectQuery;

    public void setDependencies(HistoryService historyService,
                                Consumer<String> onSelectQuery) {
        Objects.requireNonNull(historyService, "HistoryService must not be null");
        Objects.requireNonNull(onSelectQuery,  "onSelectQuery callback must not be null");
        this.historyService = historyService;
        this.onSelectQuery  = onSelectQuery;
    }

    @FXML
    public void initialize() {
        historyList.setCellFactory(lv -> new HistoryCell());

        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                QueryHistoryEntry selected = historyList.getSelectionModel().getSelectedItem();
                if (selected != null && onSelectQuery != null) {
                    onSelectQuery.accept(selected.getNaturalLanguage());
                }
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                loadRecent();
            } else {
                search(newVal);
            }
        });
    }

    public void loadRecent() {
        if (historyService == null) return;

        Task<List<QueryHistoryEntry>> task = new Task<>() {
            @Override
            protected List<QueryHistoryEntry> call() throws QueryException {
                return historyService.getRecent(DEFAULT_RECENT_LIMIT);
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<QueryHistoryEntry> items =
                FXCollections.observableArrayList(task.getValue());
            historyList.setItems(items);
            statusLabel.setText(items.size() + " entries");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> statusLabel.setText("Error: " + msg));
            LOG.warning("History load failed: " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void search(String term) {
        if (historyService == null) return;

        Task<List<QueryHistoryEntry>> task = new Task<>() {
            @Override
            protected List<QueryHistoryEntry> call() throws QueryException {
                return historyService.search(term);
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<QueryHistoryEntry> items =
                FXCollections.observableArrayList(task.getValue());
            historyList.setItems(items);
            statusLabel.setText(items.size() + " results");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> statusLabel.setText("Search error: " + msg));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onToggleStar() {
        QueryHistoryEntry selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null || historyService == null) return;

        try {
            historyService.toggleStar(selected.getId());
            loadRecent();
        } catch (QueryException e) {
            statusLabel.setText("Error: " + e.getMessage());
            LOG.warning("Toggle star failed: " + e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        QueryHistoryEntry selected = historyList.getSelectionModel().getSelectedItem();
        if (selected == null || historyService == null) return;

        try {
            historyService.deleteById(selected.getId());
            loadRecent();
        } catch (QueryException e) {
            statusLabel.setText("Error: " + e.getMessage());
            LOG.warning("Delete history entry failed: " + e.getMessage());
        }
    }

    @FXML
    private void onShowStarred() {
        if (historyService == null) return;

        Task<List<QueryHistoryEntry>> task = new Task<>() {
            @Override
            protected List<QueryHistoryEntry> call() throws QueryException {
                return historyService.getStarred();
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<QueryHistoryEntry> items =
                FXCollections.observableArrayList(task.getValue());
            historyList.setItems(items);
            statusLabel.setText(items.size() + " starred");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> statusLabel.setText("Error: " + msg));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private final class HistoryCell extends ListCell<QueryHistoryEntry> {
        @Override
        protected void updateItem(QueryHistoryEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setText(null);
                setStyle("");
            } else {
                String star = entry.isStarred() ? "* " : "  ";
                String time = entry.getExecutedAt().format(DISPLAY_FORMAT);
                setText(star + "[" + time + "] " + entry.getNaturalLanguage());
                setStyle(entry.isStarred() ? "-fx-font-weight: bold;" : "");
            }
        }
    }
}
