package com.pharmacy.iposca;

import com.pharmacy.iposca.api.InventoryRestAPI;
import com.pharmacy.iposca.api.SupplierRestAPI;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import spark.Spark;


import java.io.IOException;

/**
 * JavaFX Application Launcher for IPOS-CA Pharmacy Management System
 * Starts REST APIs in background and loads JavaFX UI
 */
public class Launcher extends Application {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Load .env file and export as system properties
        Dotenv dotenv = Dotenv.configure()
                .filename("IPOS.env")
                .directory(".")
                .systemProperties()
                .ignoreIfMissing()
                .load();
        System.out.println("IPOS_SA_API_KEY loaded = [" +
                System.getProperty("IPOS_SA_API_KEY", "NOT SET") + "]");

        // Start APIs before loading UI
        Thread apiThread = new Thread(() -> {
            InventoryRestAPI.start(4567);
            SupplierRestAPI.start(4568);
        });
        apiThread.setDaemon(true);
        apiThread.start();

        // Show loading screen while APIs initialize
        Stage loadingStage = new Stage();
        javafx.scene.control.Label loadingLabel = new javafx.scene.control.Label("Starting IPOS-CA...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-padding: 20px;");
        loadingStage.setScene(new Scene(new javafx.scene.layout.VBox(loadingLabel), 300, 100));
        loadingStage.show();

        // Waiting for APIs then loading the main UI
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait for APIs
                Platform.runLater(() -> {
                    loadingStage.close();

                    Parent root = null;
                    try {
                        root = FXMLLoader.load(getClass().getResource("/com/pharmacy/iposca/LoginView.fxml"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    primaryStage.setTitle("IPOS-CA - Pharmacy Management System");
                    primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));

                    primaryStage.setOnCloseRequest(t -> {
                        Platform.exit();
                        System.exit(0);
                    });

                    primaryStage.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}