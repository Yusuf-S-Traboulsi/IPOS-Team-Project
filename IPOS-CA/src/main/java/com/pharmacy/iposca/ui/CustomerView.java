package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.CustomerController;
import com.pharmacy.iposca.model.Customer;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DoubleStringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Customer View - Full Database Integration
 * All TableView edits save immediately to MySQL database
 */
public class CustomerView {

    @FXML private TextField customerSearch;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idCol;
    @FXML private TableColumn<Customer, String> titleCol, nameCol, emailCol, addressCol, townCol, postcodeCol, statusCol, rem1Col, rem2Col;
    @FXML private TableColumn<Customer, Double> limitCol, debtCol;
    @FXML private ComboBox<String> titleCombo;
    @FXML private TextField nameInput, emailInput, addressInput, townInput, postcodeInput, limitInput;
    @FXML private Label informationLabel;

    private CustomerController logic = CustomerController.getInstance();

    @FXML
    public void initialize() {
        customerTable.setEditable(true);
        titleCombo.getItems().addAll("Mr.", "Ms.", "Mrs.", "Dr.");

        // --- Column Setup ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        statusCol.setCellValueFactory(new PropertyValueFactory<>("accountStatus"));
        debtCol.setCellValueFactory(new PropertyValueFactory<>("currentDebt"));
        rem1Col.setCellValueFactory(new PropertyValueFactory<>("status1stReminder"));
        rem2Col.setCellValueFactory(new PropertyValueFactory<>("status2ndReminder"));

        // Configure Editable String Columns (SAVE TO DATABASE ON EDIT)
        setupEditableStringColumn(titleCol, "title");
        setupEditableStringColumn(nameCol, "name");
        setupEditableStringColumn(emailCol, "email");  // ✅ EMAIL IS EDITABLE
        setupEditableStringColumn(addressCol, "address");
        setupEditableStringColumn(townCol, "town");
        setupEditableStringColumn(postcodeCol, "postcode");

        // Credit Limit Column (editable numeric - SAVE TO DATABASE ON EDIT)
        limitCol.setCellValueFactory(new PropertyValueFactory<>("creditLimit"));
        limitCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        limitCol.setOnEditCommit(e -> {
            Customer c = e.getRowValue();
            c.setCreditLimit(e.getNewValue());
            logic.updateCustomerField(c, "credit_limit", e.getNewValue());
        });

        // --- Search Filter ---
        FilteredList<Customer> filteredData = new FilteredList<>(logic.getCustomerData(), p -> true);
        customerSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(c -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return c.getName().toLowerCase().contains(lower) ||
                        c.getEmail().toLowerCase().contains(lower) ||
                        String.valueOf(c.getId()).contains(newVal);
            });
        });
        customerTable.setItems(filteredData);
    }

    /**
     * Setup editable string column with database save on edit commit
     */
    private void setupEditableStringColumn(TableColumn<Customer, String> col, String property) {
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(e -> {
            Customer c = e.getRowValue();
            String newValue = e.getNewValue();

            // Update local object
            switch (property) {
                case "title": c.setTitle(newValue); break;
                case "name": c.setName(newValue); break;
                case "email": c.setEmail(newValue); break;
                case "address": c.setAddress(newValue); break;
                case "town": c.setTown(newValue); break;
                case "postcode": c.setPostcode(newValue); break;
            }

            // ✅ SAVE TO DATABASE
            logic.updateCustomerField(c, property, newValue);

            customerTable.refresh();
        });
    }

    @FXML
    private void handleAddCustomer() {
        try {
            String title = titleCombo.getValue();
            String name = nameInput.getText();
            String email = emailInput.getText();  // ✅ GET EMAIL - huh who knew u could use emojis
            String address = addressInput.getText();
            String town = townInput.getText();
            String postcode = postcodeInput.getText();
            double limit = Double.parseDouble(limitInput.getText());

            if (title == null || name.isEmpty() || address.isEmpty() || town.isEmpty() || postcode.isEmpty()) {
                informationLabel.setText("Please fill in all fields.");
                informationLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // ✅ ADD TO DATABASE
            boolean success = logic.addCustomer(title, name, email, address, town, postcode, limit);

            if (success) {
                clearInputs();
                customerTable.refresh();
                informationLabel.setText("Customer added successfully.");
                informationLabel.setStyle("-fx-text-fill: green;");
            } else {
                informationLabel.setText("Failed to add customer. Check database connection.");
                informationLabel.setStyle("-fx-text-fill: red;");
            }

        } catch (NumberFormatException e) {
            informationLabel.setText("Invalid credit limit format.");
            informationLabel.setStyle("-fx-text-fill: red;");
        } catch (Exception e) {
            informationLabel.setText("Error adding customer: " + e.getMessage());
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleDeleteCustomer() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // ✅ DELETE FROM DATABASE
            if (logic.deleteCustomer(selected)) {
                customerTable.refresh();
                informationLabel.setText("Customer deleted.");
                informationLabel.setStyle("-fx-text-fill: green;");
            } else {
                informationLabel.setText("Cannot delete account with active debt.");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            informationLabel.setText("Please select a customer first.");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void triggerSystemAutoCheck() {
        logic.evaluateAccountStatuses(LocalDate.now().plusDays(45));
        customerTable.refresh();
        informationLabel.setText("Compliance check complete.");
        informationLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    private void generateMonthlyStatements() {
        int day = LocalDate.now().getDayOfMonth();
        if (day >= 5 && day <= 15) {
            int count = 0;
            for (Customer c : logic.getCustomerData()) {
                if (c.getCurrentDebt() > 0) {
                    File statement = logic.generateMonthlyStatement(c);
                    if (statement != null) count++;
                }
            }
            customerTable.refresh();
            informationLabel.setText(count + " statement(s) generated.");
            informationLabel.setStyle("-fx-text-fill: green;");
        } else {
            informationLabel.setText("Statements can only be generated between 5th-15th of month.");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleGenerateFirstReminder() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getCurrentDebt() > 0 && "due".equals(selected.getStatus1stReminder())) {
                File reminder = logic.generateFirstReminder(selected);
                if (reminder != null) {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().browse(reminder.toURI());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    customerTable.refresh();
                    informationLabel.setText("First reminder generated for " + selected.getName());
                    informationLabel.setStyle("-fx-text-fill: green;");
                }
            } else if (selected.getCurrentDebt() <= 0) {
                informationLabel.setText("Customer has no outstanding debt.");
                informationLabel.setStyle("-fx-text-fill: red;");
            } else {
                informationLabel.setText("First reminder not due for this customer.");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            informationLabel.setText("Please select a customer first.");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleGenerateSecondReminder() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getCurrentDebt() > 0 && "due".equals(selected.getStatus2ndReminder())) {
                LocalDate date2nd = selected.getDate2ndReminder();
                if (date2nd != null && !date2nd.isAfter(LocalDate.now())) {
                    File reminder = logic.generateSecondReminder(selected);
                    if (reminder != null) {
                        try {
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop.getDesktop().browse(reminder.toURI());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        customerTable.refresh();
                        informationLabel.setText("Second reminder generated for " + selected.getName());
                        informationLabel.setStyle("-fx-text-fill: green;");
                    }
                } else {
                    informationLabel.setText("Second reminder not yet due (scheduled: " +
                            (date2nd != null ? date2nd.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "unknown") + ")");
                    informationLabel.setStyle("-fx-text-fill: red;");
                }
            } else if (selected.getCurrentDebt() <= 0) {
                informationLabel.setText("Customer has no outstanding debt.");
                informationLabel.setStyle("-fx-text-fill: red;");
            } else {
                informationLabel.setText("Second reminder not due for this customer.");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            informationLabel.setText("Please select a customer first.");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void handleProcessAllReminders() {
        int count = 0;
        for (Customer c : logic.getCustomerData()) {
            logic.processReminders(c);
            if ("sent".equals(c.getStatus1stReminder()) || "sent".equals(c.getStatus2ndReminder())) {
                count++;
            }
        }
        customerTable.refresh();
        informationLabel.setText("Processed reminders for " + count + " customer(s).");
        informationLabel.setStyle("-fx-text-fill: green;");
    }

    @FXML
    private void handlePayment() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getCurrentDebt() > 0) {
                // Record payment - reset debt and reminder statuses
                selected.setCurrentDebt(0.0);
                // ✅ SAVE TO DATABASE
                logic.updateCustomer(selected);
                logic.resetRemindersOnPayment(selected);
                customerTable.refresh();
                informationLabel.setText("Payment recorded for " + selected.getName() + ". Statuses reset.");
                informationLabel.setStyle("-fx-text-fill: green;");
            } else {
                informationLabel.setText("No outstanding debt to pay.");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        } else {
            informationLabel.setText("Please select an account first.");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void clearInputs() {
        nameInput.clear();
        emailInput.clear();
        addressInput.clear();
        townInput.clear();
        postcodeInput.clear();
        limitInput.clear();
        titleCombo.setValue(null);
    }
}