package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.SupplierController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.fxml.FXML;

/**
 * IPOS-SA Supplier Login Screen
 * Authenticates against ipos_sa_users table in MySQL database
 */
public class SupplierLoginView extends VBox {

    private TextField usernameField;
    private PasswordField passwordField;
    private Label infoLabel;
    private SupplierController controller = SupplierController.getInstance();

    public SupplierLoginView() {
        setSpacing(20);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #f8f9fa;");

        // Title
        Label titleLabel = new Label("IPOS-SA Merchant Portal");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #2c3e50;");

        Label subtitleLabel = new Label("Enter your IPOS-SA credentials to access the supplier catalogue");
        subtitleLabel.setFont(Font.font("Segoe UI", 13));
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d;");

        // Login Form
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(Font.font("Segoe UI", 13));

        usernameField = new TextField();
        usernameField.setPromptText("Enter IPOS-SA username");
        usernameField.setPrefWidth(250);
        usernameField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(Font.font("Segoe UI", 13));

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter IPOS-SA password");
        passwordField.setPrefWidth(250);
        passwordField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        formGrid.add(usernameLabel, 0, 0);
        formGrid.add(usernameField, 1, 0);
        formGrid.add(passwordLabel, 0, 1);
        formGrid.add(passwordField, 1, 1);

        // Login Button
        Button loginButton = new Button("Login to IPOS-SA");
        loginButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 12 40;");
        loginButton.setOnAction(e -> handleLogin());

        // Info Label
        infoLabel = new Label();
        infoLabel.setFont(Font.font("Segoe UI", 13));
        infoLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        // Assemble
        getChildren().addAll(titleLabel, subtitleLabel, formGrid, loginButton, infoLabel);
    }

    /**
     * Handle login authentication against MySQL database
     */
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            infoLabel.setText("Please enter both username and password");
            infoLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            return;
        }

        // Authenticate with IPOS-SA (queries ipos_sa_users table)
        boolean authenticated = controller.authenticateWithIposSa(username, password);

        if (authenticated) {
            infoLabel.setText("Login successful! Loading supplier catalogue...");
            infoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

            // Switch to supplier catalogue view
            try {
                SupplierView supplierView = new SupplierView();
                this.getChildren().setAll(supplierView);
            } catch (Exception e) {
                e.printStackTrace();
                infoLabel.setText("Error loading supplier view: " + e.getMessage());
            }
        } else {
            infoLabel.setText("Invalid IPOS-SA credentials. Please try again.");
            infoLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
    }
}