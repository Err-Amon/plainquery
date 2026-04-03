package com.plainquery.controller;

import com.plainquery.exception.QueryException;
import com.plainquery.model.QueryResult;
import com.plainquery.service.ChartService;
import com.plainquery.service.QueryService;
import com.plainquery.service.SchemaService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class QueryController {

    private static final Logger LOG = Logger.getLogger(QueryController.class.getName());

    @FXML private javafx.scene.control.TextArea questionField;
    @FXML private Button      askButton;
    @FXML private TitledPane  sqlPane;
    @FXML private TextArea    sqlEditor;
    @FXML private Button      runSqlButton;
    @FXML private StackPane   chartContainer;
    @FXML private javafx.scene.control.ProgressIndicator progressIndicator;
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
        sqlPane.setExpanded(false);
    }

    public void loadSqlIntoEditor(String sql) {
        if (sql != null && !sql.isBlank()) {
            sqlEditor.setText(sql);
            sqlPane.setExpanded(true);
        }
    }

    @FXML
    private void onFormatSql() {
        String sql = sqlEditor.getText();
        if (sql != null && !sql.isBlank()) {
            String formattedSql = com.plainquery.util.SqlSyntaxHighlighter.formatSql(sql);
            sqlEditor.setText(formattedSql);
        }
    }

    public void setQuestionText(String text) {
        if (text != null) {
            questionField.setText(text);
        }
    }

    @FXML
    private void onAsk() {
        String question = questionField.getText();
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
            resultsController.displayResult(result);
            showStatus("Done. " + result.getRowCount() + " rows returned.", false);
            setControlsDisabled(false);
            updateChart(result);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            Platform.runLater(() -> {
                showStatus("Error: " + msg, true);
                setControlsDisabled(false);
                showErrorDialog("Query Failed", "An error occurred while processing your query:", msg);
            });
            LOG.log(java.util.logging.Level.WARNING, "Natural language query failed: " + msg, ex);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onRunSql() {
        String sql = sqlEditor.getText();
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
            resultsController.displayResult(result);
            showStatus("Done. " + result.getRowCount() + " rows returned.", false);
            setControlsDisabled(false);
            updateChart(result);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            Platform.runLater(() -> {
                showStatus("Error: " + msg, true);
                setControlsDisabled(false);
                showErrorDialog("SQL Execution Failed", "An error occurred while executing the SQL:", msg);
            });
            LOG.log(java.util.logging.Level.WARNING, "SQL execution failed: " + msg, ex);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateChart(QueryResult result) {
        chartContainer.getChildren().clear();
        Optional<Node> chart = chartService.buildChart(result);
        if (chart.isPresent()) {
            chartContainer.getChildren().add(chart.get());
        } else {
            javafx.scene.control.Label noChartLabel = new javafx.scene.control.Label(
                "No suitable chart type for this data\nTry queries with time series, categorical, or numeric data");
            noChartLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px; -fx-text-alignment: center;");
            noChartLabel.setWrapText(true);
            chartContainer.getChildren().add(noChartLabel);
        }
    }

    private void setControlsDisabled(boolean disabled) {
        askButton.setDisable(disabled);
        runSqlButton.setDisable(disabled);
        questionField.setDisable(disabled);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError
            ? "-fx-text-fill: #e74c3c;"
            : "-fx-text-fill: #bdc3c7;");
    }

    private void showErrorDialog(String title, String header, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.initOwner(questionField.getScene().getWindow());
        alert.showAndWait();
    }

    @FXML
    private void onClearQuestion() {
        questionField.clear();
    }

    @FXML
    private void onCopySql() {
        String sql = sqlEditor.getText();
        if (sql == null || sql.isBlank()) return;
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(sql);
        clipboard.setContent(content);
        showStatus("SQL copied to clipboard.", false);
    }
}
