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
import java.util.logging.Level;
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
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("JSON Files", "*.json"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fileChooser.setInitialFileName("query_results.csv");

        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());
        if (file == null) return;

        try {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".csv")) {
                exportToCsv(file, currentResult);
            } else if (fileName.endsWith(".json")) {
                exportToJson(file, currentResult);
            } else if (fileName.endsWith(".xlsx")) {
                exportToExcel(file, currentResult);
            } else {
                // Default to CSV if no extension recognized
                exportToCsv(file, currentResult);
            }
            rowCountLabel.setText("Exported to: " + file.getName());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Export failed: " + e.getMessage(), e);
            rowCountLabel.setText("Export failed: " + e.getMessage());
            showErrorDialog("Export Failed", "An error occurred while exporting results:", e.getMessage());
        }
    }

    private void showErrorDialog(String title, String header, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.initOwner(resultsTable.getScene().getWindow());
        alert.showAndWait();
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

    private void exportToJson(File file, QueryResult result) throws IOException {
        List<String> columnNames = result.getColumnNames();
        List<List<Object>> rows = result.getRows();
        
        List<java.util.Map<String, Object>> jsonData = new ArrayList<>();
        for (List<Object> row : rows) {
            java.util.Map<String, Object> jsonRow = new java.util.HashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                Object value = row.get(i);
                jsonRow.put(columnNames.get(i), value);
            }
            jsonData.add(jsonRow);
        }
        
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writeValue(file, jsonData);
    }

    private void exportToExcel(File file, QueryResult result) throws IOException {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Results");
            
            // Create header row
            org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
            for (int i = 0; i < result.getColumnNames().size(); i++) {
                org.apache.poi.xssf.usermodel.XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(result.getColumnNames().get(i));
                
                // Style header
                org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.xssf.usermodel.XSSFFont font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            for (int i = 0; i < result.getRows().size(); i++) {
                org.apache.poi.xssf.usermodel.XSSFRow dataRow = sheet.createRow(i + 1);
                List<Object> rowData = result.getRows().get(i);
                
                for (int j = 0; j < rowData.size(); j++) {
                    org.apache.poi.xssf.usermodel.XSSFCell cell = dataRow.createCell(j);
                    Object value = rowData.get(j);
                    
                    if (value instanceof Number) {
                        if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                        } else if (value instanceof Long) {
                            cell.setCellValue((Long) value);
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                        } else if (value instanceof Float) {
                            cell.setCellValue((Float) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < result.getColumnNames().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
                workbook.write(outputStream);
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
