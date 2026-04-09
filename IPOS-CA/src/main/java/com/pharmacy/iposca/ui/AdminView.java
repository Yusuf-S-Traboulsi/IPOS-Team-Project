package com.pharmacy.iposca.ui;
import com.pharmacy.iposca.controller.AdminController;
import com.pharmacy.iposca.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;

public class AdminView {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> fullNameCol;  // Match FXML
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Boolean> activeCol;

    @FXML private TextField newUsernameField;
    @FXML private TextField newFullNameField;  // Match FXML
    @FXML private PasswordField newPasswordField;  // Match FXML (lowercase 'w')
    @FXML private ComboBox<String> roleDropdown;
    @FXML private ComboBox<String> roleUpdateDropdown; //For promoting/demoting user roles

    @FXML private Label informationLabel;

    private AdminController adminController = AdminController.getInstance();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Username column (editable)
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        usernameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        usernameCol.setOnEditCommit(e -> {
            e.getRowValue().setUsername(e.getNewValue());
            informationLabel.setText("Username updated");
        });

        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        fullNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        fullNameCol.setOnEditCommit(e -> {
            e.getRowValue().setFullName(e.getNewValue());
            informationLabel.setText("Full name updated");
        });

        // Role column
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Active status column (color-coded)
        activeCol.setCellValueFactory(cellData -> cellData.getValue().isActiveProperty());
        activeCol.setCellFactory(col -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Active" : "Inactive");
                    setTextFill(item ? Color.GREEN : Color.RED);
                }
            }
        });

        userTable.setItems(adminController.getUsers());
        userTable.setEditable(true);
        roleDropdown.getItems().addAll(User.Admin, User.Pharmacist, User.Manager);
        roleUpdateDropdown.getItems().addAll(User.Admin, User.Pharmacist, User.Manager);
    }
    @FXML
    private void handleUpdateRole() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        String selectedRole = roleUpdateDropdown.getValue();

        if (selected == null) {
            informationLabel.setText("No user selected");
            informationLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        if (selectedRole == null) {
            informationLabel.setText("Please select a role");
            informationLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        boolean success = adminController.updateRole(selected, selectedRole);
        if (success) {
            userTable.refresh();
            informationLabel.setText("Role updated for " + selected.getUsername() + " to " + selectedRole);
            informationLabel.setStyle("-fx-text-fill: green;");
        } else {
            informationLabel.setText("Failed to update role");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }


    @FXML
    private void toggleUserStatusLogic() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            informationLabel.setText("ERROR: No user selected");
            informationLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        adminController.toggleStatus(selected);
        userTable.refresh();
        informationLabel.setText("Status updated for " + selected.getUsername());
        informationLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    private void deleteUserLogic() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            informationLabel.setText("ERROR: No user selected");
            informationLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete user: " + selected.getUsername() + "?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                adminController.deleteUser(selected);
                userTable.refresh();
                informationLabel.setText("User deleted");
                informationLabel.setStyle("-fx-text-fill: green;");
            }
        });
    }

    @FXML
    private void createUserLogic() {
        String username = newUsernameField.getText();
        String fullName = newFullNameField.getText();  // Get fullName
        String password = newPasswordField.getText();  // Match FXML field name
        String role = roleDropdown.getValue();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || role == null) {
            informationLabel.setText("Fill in all fields");
            informationLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        adminController.createUser(username, fullName, password, role);
        informationLabel.setText("User created: " + username);
        informationLabel.setStyle("-fx-text-fill: green;");

        // Clear fields
        newUsernameField.clear();
        newFullNameField.clear();
        newPasswordField.clear();
        roleDropdown.setValue(null);
        userTable.refresh();
    }
}