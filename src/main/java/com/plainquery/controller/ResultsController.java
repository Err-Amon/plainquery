package com.plainquery.controller;

import com.plainquery.model.QueryResult;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ResultsController {

    private static final Logger LOG = Logger.getLogger(ResultsController.class.getName());

    @FXML private TableView<ObservableList<String>> resultsTable;
    @FXML private TextField                         filterField;
    @FXML private Label                             rowCountLabel;
    @FXML private Button                            exportButton;

    private QueryResult currentResult;

    @FXML
    public void initialize() {
        exportButton.setDisable(true);

        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter(newVal);
        });
    }

    public void displayResult(QueryResult result) {
        if (result == null) {
            clearDisplay();
            return;
        }

        this.currentResult = result;
        filterField.clear();
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();

        List<String> columnNames = result.getColumnNames();
        for (int i = 0; i < columnNames.size(); i++) {
            final int colIndex = i;
            TableColumn<ObservableList<String>, String> col =
                new TableColumn<>(columnNames.get(i));
            col.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (colIndex < row.size()) {
                    return new javafx.beans.property.SimpleStringProperty(row.get(colIndex));
                }
                return new javafx.beans.property.SimpleStringProperty("");
            });
            col.setPrefWidth(120);
            resultsTable.getColumns().add(col);
        }

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        for (List<Object> row : result.getRows()) {
            ObservableList<String> stringRow = FXCollections.observableArrayList();
            for (Object cell : row) {
                stringRow.add(cell != null ? cell.toString() : "");
            }
            data.add(stringRow);
        }

        resultsTable.setItems(data);
        rowCountLabel.setText(result.getRowCount() + " rows");
        exportButton.setDisable(false);
    }

    private void applyFilter(String filterText) {
        if (currentResult == null) return;

        ObservableList<ObservableList<String>> allData = FXCollections.observableArrayList();
        for (List<Object> row : currentResult.getRows()) {
            ObservableList<String> stringRow = FXCollections.observableArrayList();
            for (Object cell : row) {
                stringRow.add(cell != null ? cell.toString() : "");
            }
            allData.add(stringRow);
        }

        if (filterText == null || filterText.isBlank()) {
            resultsTable.setItems(allData);
            rowCountLabel.setText(currentResult.getRowCount() + " rows");
            return;
        }

        String lower = filterText.toLowerCase();
        FilteredList<ObservableList<String>> filtered = new FilteredList<>(allData, row ->
            row.stream().anyMatch(cell -> cell.toLowerCase().contains(lower))
        );

        resultsTable.setItems(filtered);
        rowCountLabel.setText(filtered.size() + " rows (filtered)");
    }

    @FXML
    private void onExport() {
        if (currentResult == null || currentResult.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("query_results.csv");

        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());
        if (file == null) return;

        try {
            exportToCsv(file, currentResult);
            rowCountLabel.setText("Exported to: " + file.getName());
        } catch (IOException e) {
            LOG.warning("Export failed: " + e.getMessage());
            rowCountLabel.setText("Export failed: " + e.getMessage());
        }
    }

    private void exportToCsv(File file, QueryResult result) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            List<String> cols = result.getColumnNames();
            writer.write(String.join(",", escapeCsvRow(cols)));
            writer.newLine();

            for (List<Object> row : result.getRows()) {
                List<String> cells = new ArrayList<>();
                for (Object cell : row) {
                    cells.add(cell != null ? cell.toString() : "");
                }
                writer.write(String.join(",", escapeCsvRow(cells)));
                writer.newLine();
            }
        }
    }

    private List<String> escapeCsvRow(List<String> values) {
        List<String> escaped = new ArrayList<>(values.size());
        for (String value : values) {
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                escaped.add("\"" + value.replace("\"", "\"\"") + "\"");
            } else {
                escaped.add(value);
            }
        }
        return escaped;
    }

    public void clearDisplay() {
        currentResult = null;
        resultsTable.getColumns().clear();
        resultsTable.getItems().clear();
        rowCountLabel.setText("");
        exportButton.setDisable(true);
        filterField.clear();
    }
}
