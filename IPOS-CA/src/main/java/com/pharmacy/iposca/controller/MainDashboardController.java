package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.Launcher;
import com.pharmacy.iposca.model.User;
import com.pharmacy.iposca.ui.DiscountSettingsView;
import com.pharmacy.iposca.ui.MerchantSettingsView;
import com.pharmacy.iposca.ui.SupplierLoginView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;

/**
 * This class handles navigation and role-based permissions
 * */
public class MainDashboardController {

    @FXML private VBox sidebar;
    @FXML private VBox navButtons;
    @FXML private StackPane contentArea;
    @FXML private Label titleLabel;
    @FXML private Label userLabel;

    // Navigation Buttons
    @FXML private Button posBtn;
    @FXML private Button inventoryBtn;
    @FXML private Button customerBtn;
    @FXML private Button supplierBtn;
    @FXML private Button discountBtn;
    @FXML private Button templatesBtn;
    @FXML private Button adminBtn;
    @FXML private Button reportBtn;
    @FXML private Button onlineOrderBtn; // Added for PU Orders
    @FXML private Button logoutBtn;

    private User currentUser;

    /**
     * Helper method for section labels in sidebar
     */
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("sidebar-section-label");
        return label;
    }

    /**
     * Apply role-based permissions to navigation buttons
     */
    public void applyPermissions(User user) {
        this.currentUser = user;
        if (user == null) return;

        // Update user label
        if (userLabel != null) {
            userLabel.setText("User: " + user.getUsername() + " (" + user.getRole() + ")");
        }

        // Clear navigation buttons
        navButtons.getChildren().clear();

        // Role-specific button visibility
        if (user.isAdmin()) {
            // For Admin permissions
            navButtons.getChildren().add(createSectionLabel("SYSTEM"));
            if (adminBtn != null) navButtons.getChildren().add(adminBtn);
            if (titleLabel != null) titleLabel.setText("Admin Panel");

        } else if (user.isManager()) {
            // For Manager permissions
            navButtons.getChildren().add(createSectionLabel("OPERATIONS"));
            if (posBtn != null) navButtons.getChildren().add(posBtn);
            if (inventoryBtn != null) navButtons.getChildren().add(inventoryBtn);
            if (supplierBtn != null) navButtons.getChildren().add(supplierBtn);

            //Online Orders added for Manager
            if (onlineOrderBtn != null) {
                navButtons.getChildren().add(createSectionLabel("ONLINE SALES"));
                navButtons.getChildren().add(onlineOrderBtn);
            }

            navButtons.getChildren().add(createSectionLabel("ACCOUNT HOLDERS"));
            if (customerBtn != null) navButtons.getChildren().add(customerBtn);
            if (discountBtn != null) navButtons.getChildren().add(discountBtn);

            navButtons.getChildren().add(createSectionLabel("MANAGEMENT"));
            if (templatesBtn != null) navButtons.getChildren().add(templatesBtn);
            if (reportBtn != null) navButtons.getChildren().add(reportBtn);

            if (titleLabel != null) titleLabel.setText("Manager Dashboard");

        } else if (user.isPharmacist()) {
            // For Pharmacist permissions
            navButtons.getChildren().add(createSectionLabel("OPERATIONS"));
            if (posBtn != null) navButtons.getChildren().add(posBtn);
            if (inventoryBtn != null) navButtons.getChildren().add(inventoryBtn);
            if (supplierBtn != null) navButtons.getChildren().add(supplierBtn);

            //Online orders added for Pharmacist
            if (onlineOrderBtn != null) {
                navButtons.getChildren().add(createSectionLabel("ONLINE SALES"));
                navButtons.getChildren().add(onlineOrderBtn);
            }

            navButtons.getChildren().add(createSectionLabel("ACCOUNT HOLDERS"));
            if (customerBtn != null) navButtons.getChildren().add(customerBtn);
            if (discountBtn != null) navButtons.getChildren().add(discountBtn);

            if (titleLabel != null) titleLabel.setText("Pharmacist Dashboard");
        }
    }

    /**
     * Load a view into the content area safely
     */
    private void loadView(String fxmlPath, String title) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("Resource not found: " + fxmlPath);
            }
            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
            if (titleLabel != null) {
                titleLabel.setText(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading: " + fxmlPath + "\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-wrap-text: true;");
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    //Navigation Handlers
    @FXML
    private void showPOS() {
        loadView("/com/pharmacy/iposca/POSView.fxml", "POS / Sales");
    }

    @FXML
    private void showInventory() {
        loadView("/com/pharmacy/iposca/InventoryView.fxml", "Inventory Management");
    }

    @FXML
    private void showAdmin() {
        loadView("/com/pharmacy/iposca/AdminView.fxml", "Admin Panel");
    }

    @FXML
    private void showCustomers() {
        loadView("/com/pharmacy/iposca/CustomerView.fxml", "Customer Management");
    }

    @FXML
    private void showReports() {
        //Check if the user has manager permissions
        if (currentUser != null && currentUser.isManager()) {
            loadView("/com/pharmacy/iposca/ReportingView.fxml", "Reports");
        } else {
            contentArea.getChildren().setAll(new Label("Access Denied: Reports are for Managers only."));
        }
    }

    @FXML
    private void showDiscountSettings() {
        //Checks if the user has manager or pharmacist permissions
        if (currentUser != null && (currentUser.isManager() || currentUser.isPharmacist())) {
            loadView("/com/pharmacy/iposca/discount_settings.fxml", "Discount Settings");
        } else {
            contentArea.getChildren().setAll(new Label("Access Denied."));
        }
    }

    @FXML
    private void showTemplates() {
        //Checks if the user has manager permissions
        if (currentUser != null && currentUser.isManager()) {
            try {
                MerchantSettingsView templatesView = new MerchantSettingsView();
                contentArea.getChildren().setAll(templatesView);
                if (titleLabel != null) titleLabel.setText("Templates & Merchant Settings");
            } catch (Exception e) {
                e.printStackTrace();
                contentArea.getChildren().setAll(new Label("Error: " + e.getMessage()));
            }
        } else {
            contentArea.getChildren().setAll(new Label("Access Denied: Templates are for Managers only."));
        }
    }

    @FXML
    private void showSuppliers() {
        //Shows the supplier login screen first
        if (currentUser != null && (currentUser.isManager() || currentUser.isPharmacist())) {
            try {
                SupplierLoginView loginView = new SupplierLoginView();
                contentArea.getChildren().setAll(loginView);
                if (titleLabel != null) titleLabel.setText("IPOS-SA Supplier Portal");
            } catch (Exception e) {
                e.printStackTrace();
                contentArea.getChildren().setAll(new Label("Error: " + e.getMessage()));
            }
        } else {
            contentArea.getChildren().setAll(new Label("Access Denied."));
        }
    }

    /**
     * Loads online portal view for authorized roles
     */
    @FXML
    private void showOnlineOrders() {
        if (currentUser != null && currentUser.isManager()) {
            loadView("/com/pharmacy/iposca/PortalOrdersView.fxml", "Online Orders");
        } else {
            contentArea.getChildren().setAll(new Label("Access Denied: Portal are for Pharmacists and Managers only."));
        }
    }

    @FXML
    private void handleLogout() {
        //Switches to login screen after logout
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/iposca/LoginView.fxml"));
            Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) sidebar.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, Launcher.WIDTH, Launcher.HEIGHT);
            stage.setScene(scene);
            stage.setTitle("IPOS-CA - System Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}