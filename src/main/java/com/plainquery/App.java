package com.plainquery;

import com.plainquery.config.AppConfig;
import com.plainquery.controller.HistoryController;
import com.plainquery.controller.MainController;
import com.plainquery.controller.ProfileController;
import com.plainquery.controller.QueryController;
import com.plainquery.controller.ResultsController;
import com.plainquery.controller.SettingsController;
import com.plainquery.db.DataSourceFactory;
import com.plainquery.db.HistoryDataSourceFactory;
import com.plainquery.service.ChartService;
import com.plainquery.service.ChartServiceImpl;
import com.plainquery.service.CsvLoaderService;
import com.plainquery.service.CsvLoaderServiceImpl;
import com.plainquery.service.HistoryService;
import com.plainquery.service.HistoryServiceImpl;
import com.plainquery.service.QueryService;
import com.plainquery.service.QueryServiceImpl;
import com.plainquery.service.SchemaService;
import com.plainquery.service.SchemaServiceImpl;
import com.plainquery.service.SqliteExecutor;
import com.plainquery.service.SqliteExecutorImpl;
import com.plainquery.service.ai.AiConnector;
import com.plainquery.service.ai.AiConnectorFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private DataSourceFactory       dataSourceFactory;
    private HistoryDataSourceFactory historyDataSourceFactory;

    static {
        try (InputStream is = App.class.getResourceAsStream("/logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (IOException e) {
            System.err.println("Could not load logging configuration: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppConfig config = new AppConfig();

        dataSourceFactory        = new DataSourceFactory(config);
        historyDataSourceFactory = new HistoryDataSourceFactory(config);

        Connection dataConnection;
        Connection historyConnection;
        try {
            dataConnection    = dataSourceFactory.getConnection();
            historyConnection = historyDataSourceFactory.getConnection();
        } catch (SQLException e) {
            LOG.severe("Failed to open database connections: " + e.getMessage());
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }

        SchemaService    schemaService    = new SchemaServiceImpl();
        SqliteExecutor   sqliteExecutor   = new SqliteExecutorImpl();
        CsvLoaderService csvLoaderService = new CsvLoaderServiceImpl();
        HistoryService   historyService   = new HistoryServiceImpl(historyConnection);
        ChartService     chartService     = new ChartServiceImpl();

        AiConnector aiConnector;
        try {
            aiConnector = AiConnectorFactory.create(config);
        } catch (IllegalStateException e) {
            LOG.warning("AI connector not configured: " + e.getMessage());
            aiConnector = (question, schemas) -> {
                throw new com.plainquery.exception.AiConnectorException(
                    "No API key configured. Please open Settings to add your API key.");
            };
        }

        QueryService queryService = new QueryServiceImpl(
            aiConnector, sqliteExecutor, schemaService,
            historyService, config, dataConnection);

        FXMLLoader loader = new FXMLLoader(getResource("/fxml/MainWindow.fxml"));

        // Pre-create controllers and wire dependencies with correct signatures
        ResultsController resultsController = new ResultsController();
        QueryController queryController = new QueryController();
        HistoryController historyController = new HistoryController();
        ProfileController profileController = new ProfileController();
        MainController mainController = new MainController();

        // wire controller dependencies
        queryController.setDependencies(queryService, chartService, schemaService, resultsController);
        historyController.setDependencies(historyService, (String nl) -> queryController.setQuestionText(nl));
        profileController.setDependencies(schemaService, sqliteExecutor, dataConnection);
        mainController.setDependencies(csvLoaderService, schemaService, dataConnection, config,
                                       queryController, profileController, historyController);

        loader.setControllerFactory(controllerClass -> {
            if (controllerClass == MainController.class) return mainController;
            if (controllerClass == QueryController.class) return queryController;
            if (controllerClass == ResultsController.class) return resultsController;
            if (controllerClass == HistoryController.class) return historyController;
            if (controllerClass == ProfileController.class) return profileController;
            try {
                return controllerClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    "Could not instantiate controller: " + controllerClass.getName(), e);
            }
        });

        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        URL cssUrl = getClass().getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        applyAppIcon(primaryStage);

        primaryStage.setTitle("PlainQuery");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        LOG.fine("PlainQuery started successfully");
    }

    @Override
    public void stop() {
        LOG.fine("PlainQuery shutting down");
        if (dataSourceFactory != null) {
            dataSourceFactory.close();
        }
        if (historyDataSourceFactory != null) {
            historyDataSourceFactory.close();
        }
    }

    private void applyAppIcon(Stage stage) {
        try (InputStream is = getClass().getResourceAsStream("/icons/app-icon-256.png")) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (IOException e) {
            LOG.fine("App icon not found, continuing without icon");
        }
    }

    private URL getResource(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            throw new RuntimeException("Required resource not found on classpath: " + path);
        }
        return url;
    }
}
