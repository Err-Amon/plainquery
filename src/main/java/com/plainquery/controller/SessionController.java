package com.plainquery.controller;

import com.plainquery.model.QuerySession;
import com.plainquery.model.QueryHistoryEntry;
import com.plainquery.service.QuerySessionService;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
public class SessionController {
    
    private static final Logger LOG = Logger.getLogger(SessionController.class.getName());
    
    @FXML private ListView<QuerySession> sessionListView;
    @FXML private TextField sessionNameField;
    
    private QuerySessionService querySessionService;
    private Stage stage;
    private SessionChangeListener sessionChangeListener;
    
    
    @FXML
    public void initialize() {
        sessionListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                sessionNameField.setText(newValue.getName());
            }
        });
    }
    

    public void setQuerySessionService(QuerySessionService querySessionService) {
        this.querySessionService = querySessionService;
        refreshSessionList();
    }
    
 
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void setSessionChangeListener(SessionChangeListener listener) {
        this.sessionChangeListener = listener;
    }
    

    private void refreshSessionList() {
        if (querySessionService != null) {
            sessionListView.getItems().clear();
            sessionListView.getItems().addAll(querySessionService.loadAllSessions());
        }
    }
    

    @FXML
    private void onCreateSession() {
        if (querySessionService != null) {
            String defaultName = "Session " + (querySessionService.getSessionCount() + 1);
            QuerySession newSession = querySessionService.createSession(defaultName);
            refreshSessionList();
            sessionListView.getSelectionModel().select(newSession);
            LOG.log(Level.INFO, "New session created: " + newSession.getName());
        }
    }
    

    @FXML
    private void onRenameSession() {
        QuerySession selectedSession = sessionListView.getSelectionModel().getSelectedItem();
        if (selectedSession != null) {
            String newName = sessionNameField.getText().trim();
            if (!newName.isEmpty() && !newName.equals(selectedSession.getName())) {
                querySessionService.renameSession(selectedSession.getId(), newName);
                refreshSessionList();
                sessionListView.getSelectionModel().select(selectedSession);
                LOG.log(Level.INFO, "Session renamed: " + newName);
            }
        }
    }
    
    /**
     * Handles deleting the selected session
     */
    @FXML
    private void onDeleteSession() {
        QuerySession selectedSession = sessionListView.getSelectionModel().getSelectedItem();
        if (selectedSession != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Session");
            alert.setHeaderText("Delete Session: " + selectedSession.getName());
            alert.setContentText("Are you sure you want to delete this session and all its queries? This action cannot be undone.");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                querySessionService.deleteSession(selectedSession.getId());
                refreshSessionList();
                sessionNameField.clear();
                LOG.log(Level.INFO, "Session deleted: " + selectedSession.getName());
            }
        }
    }
    
    /**
     * Handles selecting the active session
     */
    @FXML
    private void onSelectSession() {
        QuerySession selectedSession = sessionListView.getSelectionModel().getSelectedItem();
        if (selectedSession != null) {
            querySessionService.setActiveSession(selectedSession.getId());
            if (sessionChangeListener != null) {
                sessionChangeListener.onSessionChanged(selectedSession);
            }
            LOG.log(Level.INFO, "Active session set to: " + selectedSession.getName());
        }
    }
    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
    public interface SessionChangeListener {
        void onSessionChanged(QuerySession newSession);
    }
}
