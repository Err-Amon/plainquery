package com.plainquery.controller;

import com.plainquery.config.AppConfig;
import com.plainquery.exception.CsvLoadException;
import com.plainquery.exception.InsufficientMemoryException;
import com.plainquery.model.CsvLoadResult;
import com.plainquery.service.CsvLoaderService;
import com.plainquery.service.SchemaService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class MainController {

    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    @FXML private VBox               dropZone;
    @FXML private ListView<String>   fileListView;
    @FXML private Label              statusBar;

    private CsvLoaderService  csvLoaderService;
    private SchemaService     schemaService;
    private Connection        connection;
    private AppConfig         config;
    private QueryController   queryController;
    private ProfileController profileController;
    private HistoryController historyController;

    private final ObservableList<String> loadedFiles = FXCollections.observableArrayList();

    public void setDependencies(CsvLoaderService csvLoaderService,
                                SchemaService schemaService,
                                Connection connection,
                                AppConfig config,
                                QueryController queryController,
                                ProfileController profileController,
                                HistoryController historyController) {
        Objects.requireNonNull(csvLoaderService,  "CsvLoaderService must not be null");
        Objects.requireNonNull(schemaService,     "SchemaService must not be null");
        Objects.requireNonNull(connection,        "Connection must not be null");
        Objects.requireNonNull(config,            "AppConfig must not be null");
        Objects.requireNonNull(queryController,   "QueryController must not be null");
        Objects.requireNonNull(profileController, "ProfileController must not be null");
        Objects.requireNonNull(historyController, "HistoryController must not be null");
        this.csvLoaderService  = csvLoaderService;
        this.schemaService     = schemaService;
        this.connection        = connection;
        this.config            = config;
        this.queryController   = queryController;
        this.profileController = profileController;
        this.historyController = historyController;
    }

    @FXML
    public void initialize() {
        fileListView.setItems(loadedFiles);

        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone
                    && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> csvFiles = new ArrayList<>();
                for (File f : db.getFiles()) {
                    if (f.getName().toLowerCase().endsWith(".csv")) {
                        csvFiles.add(f);
                    }
                }
                if (!csvFiles.isEmpty()) {
                    loadFiles(csvFiles);
                    success = true;
                } else {
                    setStatus("Only CSV files are supported.", true);
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @FXML
    private void onOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        List<File> files = fileChooser.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            loadFiles(files);
        }
    }

    private void loadFiles(List<File> files) {
        for (File file : files) {
            loadSingleFile(file);
        }
    }

    private void loadSingleFile(File file) {
        setStatus("Loading: " + file.getName() + "...", false);

        Task<CsvLoadResult> task = new Task<>() {
            @Override
            protected CsvLoadResult call()
                    throws CsvLoadException, InsufficientMemoryException {
                return csvLoaderService.load(file, connection);
            }
        };

        task.setOnSucceeded(e -> {
            CsvLoadResult result = task.getValue();
            Platform.runLater(() -> {
                schemaService.register(result.getSchema());
                if (!loadedFiles.contains(file.getName())) {
                    loadedFiles.add(file.getName());
                }
                profileController.refreshTableList();
                historyController.loadRecent();
                setStatus("Loaded: " + result.getTableName()
                    + " (" + result.getRowsInserted() + " rows)", false);
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            Platform.runLater(() ->
                setStatus("Failed to load " + file.getName() + ": " + msg, true));
            LOG.warning("CSV load failed for " + file.getName() + ": " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onClearAll() {
        schemaService.clear();
        loadedFiles.clear();
        profileController.refreshTableList();
        setStatus("All data cleared.", false);
    }

    @FXML
    private void onOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/SettingsDialog.fxml"));
            Parent root = loader.load();

            SettingsController settingsController = loader.getController();
            settingsController.setConfig(config);

            Stage stage = new Stage();
            stage.setTitle("Settings");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(dropZone.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            LOG.warning("Could not open settings dialog: " + e.getMessage());
            setStatus("Could not open settings.", true);
        }
    }

    @FXML
    private void onExit() {
        Stage stage = (Stage) dropZone.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAbout() {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle("About PlainQuery");
        a.setHeaderText("PlainQuery");
        a.setContentText("PlainQuery — local-first CSV playground\nVersion 1.0.0");
        a.initOwner(dropZone.getScene().getWindow());
        a.showAndWait();
    }

    @FXML
    private void onRemoveFile() {
        String sel = fileListView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            loadedFiles.remove(sel);
            setStatus("Removed: " + sel, false);
        }
    }

    @FXML
    private void onDragOver(DragEvent event) {
        if (dropZone != null && dropZone.getOnDragOver() != null) {
            dropZone.getOnDragOver().handle(event);
        }
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        if (dropZone != null && dropZone.getOnDragDropped() != null) {
            dropZone.getOnDragDropped().handle(event);
        }
    }

    private void setStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusBar.setText(message);
            statusBar.setStyle(isError
                ? "-fx-text-fill: #e74c3c;"
                : "-fx-text-fill: #bdc3c7;");
        });
    }
}
