package com.plainquery.controller;

import com.plainquery.config.AppConfig;
import com.plainquery.config.AppConfig.DbMode;
import com.plainquery.config.AiProvider;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Toggle;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.logging.Logger;

public final class SettingsController {

    private static final Logger LOG = Logger.getLogger(SettingsController.class.getName());

    @FXML private ComboBox<AiProvider> providerComboBox;
    @FXML private PasswordField        apiKeyField;
    @FXML private RadioButton          memoryModeRadio;
    @FXML private RadioButton          fileModeRadio;
    @FXML private ToggleGroup          dbModeGroup;
    @FXML private Spinner<Integer>     rowLimitSpinner;
    @FXML private Label                statusLabel;

    private AppConfig config;

    public void setConfig(AppConfig config) {
        Objects.requireNonNull(config, "AppConfig must not be null");
        this.config = config;
        populateFields();
    }

    @FXML
    public void initialize() {
        providerComboBox.setItems(
            FXCollections.observableArrayList(AiProvider.values()));
        rowLimitSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 100_000, 10_000, 1000));
        rowLimitSpinner.setEditable(true);
    }

    private void populateFields() {
        providerComboBox.setValue(config.getAiProvider());
        apiKeyField.setText(config.getApiKey());
        rowLimitSpinner.getValueFactory().setValue(config.getPreviewRowLimit());

        if (config.getDbMode() == DbMode.MEMORY) {
            memoryModeRadio.setSelected(true);
        } else {
            fileModeRadio.setSelected(true);
        }
    }

    @FXML
    private void onSave() {
        if (config == null) {
            showStatus("Configuration not initialized.", true);
            return;
        }

        AiProvider selectedProvider = providerComboBox.getValue();
        if (selectedProvider == null) {
            showStatus("Please select an AI provider.", true);
            return;
        }

        String apiKey = apiKeyField.getText();
        if (apiKey == null || apiKey.isBlank()) {
            showStatus("API key must not be empty.", true);
            return;
        }

        Integer rowLimit = rowLimitSpinner.getValue();
        if (rowLimit == null || rowLimit < 100) {
            showStatus("Row limit must be at least 100.", true);
            return;
        }

        config.setAiProvider(selectedProvider);
        config.setApiKey(apiKey.trim());
        config.setPreviewRowLimit(rowLimit);

        Toggle selected = dbModeGroup.getSelectedToggle();
        if (selected == memoryModeRadio) {
            config.setDbMode(DbMode.MEMORY);
        } else {
            config.setDbMode(DbMode.FILE);
        }

        config.flush();
        LOG.fine("Settings saved");
        showStatus("Settings saved.", false);
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) providerComboBox.getScene().getWindow();
        stage.close();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }
}
