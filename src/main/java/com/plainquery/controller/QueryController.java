package com.plainquery.controller;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryResult;
import com.plainquery.service.ChartService;
import com.plainquery.service.QueryService;
import com.plainquery.service.SchemaService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class QueryController {

    private static final Logger LOG = Logger.getLogger(QueryController.class.getName());

    @FXML private TextField   naturalLanguageField;
    @FXML private Button      askButton;
    @FXML private TitledPane  sqlEditorPane;
    @FXML private TextArea    sqlEditorArea;
    @FXML private Button      runSqlButton;
    @FXML private StackPane   chartPane;
    @FXML private Label       statusLabel;

    private QueryService    queryService;
    private ChartService    chartService;
    private SchemaService   schemaService;
    private ResultsController resultsController;

    public void setDependencies(QueryService queryService,
                                ChartService chartService,
                                SchemaService schemaService,
                                ResultsController resultsController) {
        Objects.requireNonNull(queryService,      "QueryService must not be null");
        Objects.requireNonNull(chartService,      "ChartService must not be null");
        Objects.requireNonNull(schemaService,     "SchemaService must not be null");
        Objects.requireNonNull(resultsController, "ResultsController must not be null");
        this.queryService      = queryService;
        this.chartService      = chartService;
        this.schemaService     = schemaService;
        this.resultsController = resultsController;
    }

    @FXML
    public void initialize() {
        sqlEditorPane.setExpanded(false);

        naturalLanguageField.setOnAction(e -> onAsk());

        naturalLanguageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (schemaService == null) return;
            List<String> allColumns = schemaService.getAllColumnNames();
            if (newVal == null || newVal.isBlank()) return;
        });
    }

    public void loadSqlIntoEditor(String sql) {
        if (sql != null && !sql.isBlank()) {
            sqlEditorArea.setText(sql);
            sqlEditorPane.setExpanded(true);
        }
    }

    public void setNaturalLanguageText(String text) {
        if (text != null) {
            naturalLanguageField.setText(text);
        }
    }

    @FXML
    private void onAsk() {
        String question = naturalLanguageField.getText();
        if (question == null || question.isBlank()) {
            showStatus("Please enter a question.", true);
            return;
        }

        setControlsDisabled(true);
        showStatus("Translating and executing...", false);

        Task<QueryResult> task = new Task<>() {
            @Override
            protected QueryResult call() throws QueryException {
                return queryService.executeNaturalLanguage(question.trim());
            }
        };

        task.setOnSucceeded(e -> {
            QueryResult result = task.getValue();
            Platform.runLater(() -> {
                resultsController.displayResult(result);
                showStatus("Done. " + result.getRowCount() + " rows returned.", false);
                setControlsDisabled(false);
                updateChart(result);
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            Platform.runLater(() -> {
                showStatus("Error: " + msg, true);
                setControlsDisabled(false);
            });
            LOG.warning("Natural language query failed: " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onRunSql() {
        String sql = sqlEditorArea.getText();
        if (sql == null || sql.isBlank()) {
            showStatus("SQL editor is empty.", true);
            return;
        }

        setControlsDisabled(true);
        showStatus("Executing SQL...", false);

        Task<QueryResult> task = new Task<>() {
            @Override
            protected QueryResult call() throws QueryException {
                return queryService.executeSql(sql.trim());
            }
        };

        task.setOnSucceeded(e -> {
            QueryResult result = task.getValue();
            Platform.runLater(() -> {
                resultsController.displayResult(result);
                showStatus("Done. " + result.getRowCount() + " rows returned.", false);
                setControlsDisabled(false);
                updateChart(result);
            });
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            Platform.runLater(() -> {
                showStatus("Error: " + msg, true);
                setControlsDisabled(false);
            });
            LOG.warning("SQL execution failed: " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateChart(QueryResult result) {
        chartPane.getChildren().clear();
        Optional<Node> chart = chartService.buildChart(result);
        chart.ifPresent(node -> chartPane.getChildren().add(node));
    }

    private void setControlsDisabled(boolean disabled) {
        askButton.setDisable(disabled);
        runSqlButton.setDisable(disabled);
        naturalLanguageField.setDisable(disabled);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #e74c3c;"
            : "-fx-text-fill: #bdc3c7;");
    }
}
