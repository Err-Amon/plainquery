package com.plainquery.controller;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryHistoryEntry;
import com.plainquery.service.HistoryService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    @FXML private ListView<QueryHistoryEntry> historyListView;
    @FXML private TextField                   searchField;
    @FXML private Label                       historyStatusLabel;
    @FXML private Button                      reloadButton;
    @FXML private Button                      loadSqlButton;
    @FXML private Button                      starButton;
    @FXML private Button                      deleteButton;

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
        historyListView.setCellFactory(lv -> new HistoryCell());

        historyListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                QueryHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
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
            historyListView.setItems(items);
            historyStatusLabel.setText(items.size() + " entries");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> historyStatusLabel.setText("Error: " + msg));
            LOG.warning("History load failed: " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onSearch() {
        String term = searchField.getText();
        if (term == null || term.isBlank()) {
            loadRecent();
        } else {
            search(term);
        }
    }

    @FXML
    private void onShowRecent() {
        loadRecent();
    }

    @FXML
    private void onReloadSelected() {
        QueryHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected != null && onSelectQuery != null) {
            onSelectQuery.accept(selected.getNaturalLanguage());
        }
    }

    @FXML
    private void onDelete() {
        QueryHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected == null || historyService == null) return;

        try {
            historyService.deleteById(selected.getId());
            loadRecent();
        } catch (QueryException e) {
            historyStatusLabel.setText("Error: " + e.getMessage());
            LOG.warning("Delete history entry failed: " + e.getMessage());
        }
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
            historyListView.setItems(items);
            historyStatusLabel.setText(items.size() + " results");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> historyStatusLabel.setText("Search error: " + msg));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onToggleStar() {
        QueryHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected == null || historyService == null) return;

        try {
            historyService.toggleStar(selected.getId());
            loadRecent();
        } catch (QueryException e) {
            historyStatusLabel.setText("Error: " + e.getMessage());
            LOG.warning("Toggle star failed: " + e.getMessage());
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
            historyListView.setItems(items);
            historyStatusLabel.setText(items.size() + " starred");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> historyStatusLabel.setText("Error: " + msg));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onShowAll() {
        if (historyService == null) return;

        Task<List<QueryHistoryEntry>> task = new Task<>() {
            @Override
            protected List<QueryHistoryEntry> call() throws QueryException {
                return historyService.getAll();
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<QueryHistoryEntry> items =
                FXCollections.observableArrayList(task.getValue());
            historyListView.setItems(items);
            historyStatusLabel.setText(items.size() + " entries");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> historyStatusLabel.setText("Error: " + msg));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onDeleteAll() {
        if (historyService == null) return;

        // Confirm deletion
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete All History");
        alert.setHeaderText("Are you sure you want to delete all query history?");
        alert.setContentText("This action cannot be undone.");
        
        alert.initOwner(historyListView.getScene().getWindow());
        
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    historyService.deleteAll();
                    loadRecent();
                } catch (QueryException e) {
                    historyStatusLabel.setText("Error: " + e.getMessage());
                    LOG.warning("Delete all history failed: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onLoadSql() {
        QueryHistoryEntry selected = historyListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // We need to pass the SQL to QueryController
            // Let's add a callback for this
            if (onLoadSqlCallback != null) {
                onLoadSqlCallback.accept(selected.getGeneratedSql());
            }
        }
    }

    // Callback to load SQL into editor
    private Consumer<String> onLoadSqlCallback;
    
    public void setOnLoadSqlCallback(Consumer<String> callback) {
        this.onLoadSqlCallback = callback;
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
