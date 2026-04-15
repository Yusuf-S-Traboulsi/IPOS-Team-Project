package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.AdminController;
import com.pharmacy.iposca.controller.AuthenticationService;
import com.pharmacy.iposca.controller.MainDashboardController;
import com.pharmacy.iposca.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * This UI class handles the Login module
 */
public class LoginView {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    //Connecting to the same list the Admin uses
    private AdminController adminController = AdminController.getInstance();
    private AuthenticationService authService = new AuthenticationService(adminController);

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        if (authService.login(user, pass)) {
            navigateToDashboard(authService.getAuthenticatedUser());
        } else {
            errorLabel.setText("Invalid Login or Inactive Account.");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Method to navigate to the MainDashboard
     * Loads the MainDashboard with the appropriate permissions and changes views accordingly
     */
    private void navigateToDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pharmacy/iposca/MainDashboard.fxml"));
            Parent root = loader.load();

            MainDashboardController dashboard = loader.getController();
            dashboard.applyPermissions(user);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("IPOS-CA - " + user.getRole() + ": " + user.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}