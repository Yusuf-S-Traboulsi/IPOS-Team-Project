package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.Launcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Main Dashboard UI Controller
 * Handles navigation between different modules in the IPOS-CA system
 */
public class MainDashboard {

    @FXML
    private StackPane contentArea;

    /**
     * Load Inventory Management screen
     */
    @FXML
    private void showInventory() {
        loadScreen("/com/pharmacy/iposca/InventoryView.fxml");
    }

    /**
     * Load POS/Sales screen
     */
    @FXML
    private void showPOS() {
        loadScreen("/com/pharmacy/iposca/POSView.fxml");
    }

    /**
     * Load Customer Management screen
     */
    @FXML
    private void showCustomers() {
        loadScreen("/com/pharmacy/iposca/CustomerView.fxml");
    }

    /**
     * Load Admin/User Management screen
     */
    @FXML
    private void showAdmin() {
        loadScreen("/com/pharmacy/iposca/AdminView.fxml");
    }

    /**
     * Load Reports screen
     */
    @FXML
    private void showReports() {
        loadScreen("/com/pharmacy/iposca/ReportingView.fxml");
    }

    /**
     * Load Supplier Ordering screen (IPOS-SA)
     */
    @FXML
    private void showSuppliers() {
        loadScreen("/com/pharmacy/iposca/SupplierView.fxml");
    }

    /**
     * Load Discount Settings screen
     */
    @FXML
    private void showDiscountSettings() {loadScreen("com/pharmacy/iposca/discount_settings.fxml"); }

    /**
     * Load PU online sale communication screen
     */
    @FXML
    private void showOnlinePortal() {loadScreen("com/pharmacy/iposca/PUView.fxml");}
    /**
     * Load Merchant/Template Settings screen
     */
    @FXML
    private void showTemplates() {
        try {
            MerchantSettingsView templatesView = new MerchantSettingsView();
            contentArea.getChildren().setAll(templatesView);
        } catch (Exception e) {
            System.err.println("Could not load Template Settings");
            e.printStackTrace();
        }
    }



    /**
     * Generic method to load FXML screens into the content area
     * @param fxmlFile Path to the FXML file
     */
    private void loadScreen(String fxmlFile) {
        try {
            // FIXED: Use getClass().getResource() instead of Launcher.class.getResource()
            Parent screen = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(screen);
        } catch (IOException e) {
            System.err.println("Could not load " + fxmlFile);
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();

            // Show error in content area
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                    "Error loading screen: " + fxmlFile + "\n" + e.getMessage()
            );
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }
}