package com.pharmacy.iposca;

import com.pharmacy.iposca.api.InventoryRestAPI;
import com.pharmacy.iposca.api.SupplierRestAPI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import spark.Spark;

/**
 * JavaFX Application Launcher for IPOS-CA Pharmacy Management System
 * Starts BOTH REST APIs in background and loads JavaFX UI
 */
public class Launcher extends Application {

    public static final int WIDTH = 900;
    public static final int HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // ✅ Start IPOS-CA REST API (Port 4567) - Pharmacy System
        new Thread(() -> {
            InventoryRestAPI.start(4567);
        }).start();

        // ✅ Start IPOS-SA REST API (Port 4568) - Supplier System
        new Thread(() -> {
            SupplierRestAPI.start(4568);
        }).start();

        // Wait for APIs to initialize
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Load JavaFX UI
        Parent root = FXMLLoader.load(getClass().getResource("/com/pharmacy/iposca/LoginView.fxml"));
        primaryStage.setTitle("IPOS-CA - Pharmacy Management System");
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));

        // Safety: Shutdown Spark when the window closes
        primaryStage.setOnCloseRequest(t -> {
            Spark.stop();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}