package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.Launcher;
import com.pharmacy.iposca.model.User;
import com.pharmacy.iposca.ui.DiscountSettingsView;
import com.pharmacy.iposca.ui.MerchantSettingsView;
import com.pharmacy.iposca.ui.SupplierLoginView;
import com.pharmacy.iposca.ui.SupplierView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main Dashboard Controller - Handles navigation and role-based permissions
 *
 * Role Permissions (Per Briefing Requirements):
 * - Admin: ONLY Admin module
 * - Manager: Everything EXCEPT Admin (including Discount Settings, Templates, Reports & Suppliers)
 * - Pharmacist: Everything EXCEPT Admin AND Reports AND Templates (including Discount Settings & Suppliers)
 */
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
    @FXML private Button financeBtn;
    @FXML private Button supplierBtn;
    @FXML private Button discountBtn;
    @FXML private Button templatesBtn;
    @FXML private Button adminBtn;
    @FXML private Button reportBtn;
    @FXML private Button logoutBtn;
    @FXML private Button onlinePortalBtn;

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
            // Admin: ONLY Admin module
            navButtons.getChildren().add(createSectionLabel("Admin"));
            if (adminBtn != null) {
                navButtons.getChildren().add(adminBtn);
            }
            if (titleLabel != null) {
                titleLabel.setText("Admin Panel");
            }

        } else if (user.isManager()) {
            // Manager: Everything EXCEPT Admin
            navButtons.getChildren().add(createSectionLabel("OPERATIONS"));
            if (posBtn != null) navButtons.getChildren().add(posBtn);
            if (inventoryBtn != null) navButtons.getChildren().add(inventoryBtn);
            if (supplierBtn != null) navButtons.getChildren().add(supplierBtn);
            if (onlinePortalBtn != null) navButtons.getChildren().add(onlinePortalBtn);

            navButtons.getChildren().add(createSectionLabel("ACCOUNT HOLDERS"));
            if (customerBtn != null) navButtons.getChildren().add(customerBtn);
            if (discountBtn != null) navButtons.getChildren().add(discountBtn);

            navButtons.getChildren().add(createSectionLabel("MANAGEMENT"));
            if (templatesBtn != null) navButtons.getChildren().add(templatesBtn);
            if (reportBtn != null) navButtons.getChildren().add(reportBtn);

            if (titleLabel != null) {
                titleLabel.setText("Manager Dashboard");
            }

        } else if (user.isPharmacist()) {
            // Pharmacist: Everything EXCEPT Admin AND Reports AND Templates
            navButtons.getChildren().add(createSectionLabel("OPERATIONS"));
            if (posBtn != null) navButtons.getChildren().add(posBtn);
            if (inventoryBtn != null) navButtons.getChildren().add(inventoryBtn);
            if (supplierBtn != null) navButtons.getChildren().add(supplierBtn);
            if (onlinePortalBtn != null) navButtons.getChildren().add(onlinePortalBtn);

            navButtons.getChildren().add(createSectionLabel("ACCOUNT HOLDERS"));
            if (customerBtn != null) navButtons.getChildren().add(customerBtn);
            if (discountBtn != null) navButtons.getChildren().add(discountBtn);
            // NO Templates button for Pharmacist
            // NO Reports button for Pharmacist

            if (titleLabel != null) {
                titleLabel.setText("Pharmacist Dashboard");
            }
        }
    }

    /**
     * Load a view into the content area
     */
    private void loadView(String fxmlPath, String title) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
            if (titleLabel != null) {
                titleLabel.setText(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(
                    new Label("Error loading: " + fxmlPath)
            );
        }
    }

    //nav below

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
        // Security check: Only Manager can access reports
        if (currentUser != null && currentUser.isManager()) {
            loadView("/com/pharmacy/iposca/ReportingView.fxml", "Reports");
        } else {
            contentArea.getChildren().setAll(
                    new Label("Access Denied: Reports module is for Manager role only.")
            );
        }
    }

    @FXML
    private void showDiscountSettings() {
        // Security check: Manager and Pharmacist can access Discount Settings
        if (currentUser != null && (currentUser.isManager() || currentUser.isPharmacist())) {
            loadView("/com/pharmacy/iposca/discount_settings.fxml", "Discount Settings");
        } else {
            contentArea.getChildren().setAll(
                    new Label("Access Denied: Discount Settings is for Manager and Pharmacist roles only.")
            );
        }
    }

    @FXML
    private void showTemplates() {
        // Security check: ONLY Manager can access Templates
        if (currentUser != null && currentUser.isManager()) {
            try {
                MerchantSettingsView templatesView = new MerchantSettingsView();
                contentArea.getChildren().setAll(templatesView);
                if (titleLabel != null) {
                    titleLabel.setText("Templates & Merchant Settings");
                }
            } catch (Exception e) {
                e.printStackTrace();
                contentArea.getChildren().setAll(
                        new Label("Error loading Templates: " + e.getMessage())
                );
            }
        } else {
            contentArea.getChildren().setAll(
                    new Label("Access Denied: Templates module is for Manager role only.")
            );
        }
    }

    @FXML
    private void showSuppliers() {
        if (currentUser != null && (currentUser.isManager() || currentUser.isPharmacist())) {
            try {
                // Show IPOS-SA login first
                SupplierLoginView loginView = new SupplierLoginView();
                contentArea.getChildren().setAll(loginView);
                if (titleLabel != null) {
                    titleLabel.setText("IPOS-SA Supplier Portal");
                }
            } catch (Exception e) {
                e.printStackTrace();
                contentArea.getChildren().setAll(
                        new Label("Error loading Supplier System: " + e.getMessage())
                );
            }
        } else {
            contentArea.getChildren().setAll(
                    new Label("Access Denied: Supplier ordering is for Manager and Pharmacist roles only.")
            );
        }
    }

    @FXML
    private void showOnlinePortal() {
        if (currentUser != null && (currentUser.isManager() || currentUser.isPharmacist())) {
            loadView("/com/pharmacy/iposca/PUView.fxml", "Online Portal");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/iposca/LoginView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) sidebar.getScene().getWindow();
            Scene scene = new Scene(root, Launcher.WIDTH, Launcher.HEIGHT);
            stage.setScene(scene);
            stage.setTitle("IPOS-CA - System Login");
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}