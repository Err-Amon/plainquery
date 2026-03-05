package com.plainquery.controller;

import com.plainquery.exception.QueryException;
import com.plainquery.model.ColumnDefinition;
import com.plainquery.model.QueryResult;
import com.plainquery.model.TableSchema;
import com.plainquery.service.SchemaService;
import com.plainquery.service.SqliteExecutor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class ProfileController {

    private static final Logger LOG = Logger.getLogger(ProfileController.class.getName());

    @FXML private ComboBox<String>           tableSelector;
    @FXML private TableView<ColumnStatRow>   profileTable;
    @FXML private TableColumn<ColumnStatRow, String> colNameCol;
    @FXML private TableColumn<ColumnStatRow, String> colTypeCol;
    @FXML private TableColumn<ColumnStatRow, String> colMinCol;
    @FXML private TableColumn<ColumnStatRow, String> colMaxCol;
    @FXML private TableColumn<ColumnStatRow, String> colDistinctCol;
    @FXML private TableColumn<ColumnStatRow, String> colNullCol;
    @FXML private Label                      profileStatusLabel;

    private SchemaService  schemaService;
    private SqliteExecutor executor;
    private Connection     connection;

    public void setDependencies(SchemaService schemaService,
                                SqliteExecutor executor,
                                Connection connection) {
        Objects.requireNonNull(schemaService, "SchemaService must not be null");
        Objects.requireNonNull(executor,      "SqliteExecutor must not be null");
        Objects.requireNonNull(connection,    "Connection must not be null");
        this.schemaService = schemaService;
        this.executor      = executor;
        this.connection    = connection;
    }

    @FXML
    public void initialize() {
        colNameCol.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        colTypeCol.setCellValueFactory(new PropertyValueFactory<>("columnType"));
        colMinCol.setCellValueFactory(new PropertyValueFactory<>("min"));
        colMaxCol.setCellValueFactory(new PropertyValueFactory<>("max"));
        colDistinctCol.setCellValueFactory(new PropertyValueFactory<>("distinct"));
        colNullCol.setCellValueFactory(new PropertyValueFactory<>("nullCount"));

        tableSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadProfile(newVal);
        });
    }

    @FXML
    private void onTableSelected() {
        String selectedTable = tableSelector.getValue();
        if (selectedTable != null) {
            loadProfile(selectedTable);
        }
    }

    public void refreshTableList() {
        if (schemaService == null) return;
        List<String> tableNames = new ArrayList<>();
        for (TableSchema s : schemaService.getAll()) {
            tableNames.add(s.getTableName());
        }
        tableSelector.setItems(FXCollections.observableArrayList(tableNames));
        if (!tableNames.isEmpty()) {
            tableSelector.setValue(tableNames.get(0));
        }
    }

    private void loadProfile(String tableName) {
        if (executor == null || connection == null) return;

        TableSchema schema = schemaService.getByName(tableName).orElse(null);
        if (schema == null) return;

        Task<List<ColumnStatRow>> task = new Task<>() {
            @Override
            protected List<ColumnStatRow> call() throws Exception {
                List<ColumnStatRow> rows = new ArrayList<>();
                for (ColumnDefinition col : schema.getColumns()) {
                    ColumnStatRow row = computeStats(tableName, col);
                    rows.add(row);
                }
                return rows;
            }
        };

        task.setOnSucceeded(e -> {
            ObservableList<ColumnStatRow> data =
                FXCollections.observableArrayList(task.getValue());
            profileTable.setItems(data);
            profileStatusLabel.setText("");
        });

        task.setOnFailed(e -> {
            String msg = task.getException() != null
                ? task.getException().getMessage() : "Unknown error";
            Platform.runLater(() -> profileStatusLabel.setText("Profile error: " + msg));
            LOG.warning("Profile load failed: " + msg);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private ColumnStatRow computeStats(String tableName, ColumnDefinition col)
            throws QueryException {

        String quotedTable  = "\"" + tableName + "\"";
        String quotedColumn = "\"" + col.getName().replace("\"", "\"\"") + "\"";

        String statSql = "SELECT "
            + "MIN(" + quotedColumn + ") AS min_val, "
            + "MAX(" + quotedColumn + ") AS max_val, "
            + "COUNT(DISTINCT " + quotedColumn + ") AS distinct_count, "
            + "SUM(CASE WHEN " + quotedColumn + " IS NULL THEN 1 ELSE 0 END) AS null_count "
            + "FROM " + quotedTable;

        QueryResult result = executor.execute(statSql, connection, 1);

        String min      = "";
        String max      = "";
        String distinct = "";
        String nulls    = "";

        if (!result.isEmpty()) {
            List<Object> row = result.getRows().get(0);
            min      = row.get(0) != null ? row.get(0).toString() : "NULL";
            max      = row.get(1) != null ? row.get(1).toString() : "NULL";
            distinct = row.get(2) != null ? row.get(2).toString() : "0";
            nulls    = row.get(3) != null ? row.get(3).toString() : "0";
        }

        return new ColumnStatRow(
            col.getName(),
            col.getColumnType().name(),
            min, max, distinct, nulls);
    }

    public static final class ColumnStatRow {
        private final String columnName;
        private final String columnType;
        private final String min;
        private final String max;
        private final String distinct;
        private final String nullCount;

        public ColumnStatRow(String columnName, String columnType,
                             String min, String max,
                             String distinct, String nullCount) {
            this.columnName = columnName;
            this.columnType = columnType;
            this.min        = min;
            this.max        = max;
            this.distinct   = distinct;
            this.nullCount  = nullCount;
        }

        public String getColumnName() { return columnName; }
        public String getColumnType() { return columnType; }
        public String getMin()        { return min; }
        public String getMax()        { return max; }
        public String getDistinct()   { return distinct; }
        public String getNullCount()  { return nullCount; }
    }
}
