package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.CustomerController;
import com.pharmacy.iposca.controller.CustomerController.PaymentRecord;
import com.pharmacy.iposca.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
 * UI class for Customer Management
 */
public class CustomerView {

    @FXML private TextField customerSearch;
    @FXML private TableView<Customer> customerTable;
    @FXML private TableColumn<Customer, Integer> idCol;
    @FXML private TableColumn<Customer, String> titleCol, nameCol, emailCol, addressCol, townCol, postcodeCol, statusCol, rem1Col, rem2Col;
    @FXML private TableColumn<Customer, Double> limitCol, debtCol;

    //Registration Controls
    @FXML private ComboBox<String> titleCombo;
    @FXML private TextField nameInput, emailInput, addressInput, townInput, postcodeInput, limitInput;

    //Feedback & Actions
    @FXML private Label informationLabel;
    @FXML private Button unsuspendButton;

    //Payment History Controls
    @FXML private Label selectedCustomerLabel;
    @FXML private TableView<PaymentRecord> paymentHistoryTable;

    //Payment History Columns
    @FXML private TableColumn<PaymentRecord, LocalDate> payDateCol;
    @FXML private TableColumn<PaymentRecord, Double> payAmountCol;
    @FXML private TableColumn<PaymentRecord, String> payTypeCol;
    @FXML private TableColumn<PaymentRecord, String> payDetailsCol;
    @FXML private TableColumn<PaymentRecord, String> payRefCol;

    private final CustomerController logic = CustomerController.getInstance();
    private String currentUserRole = "MANAGER";

    @FXML
    public void initialize() {
        if (titleCombo != null) {
            titleCombo.getItems().addAll("Mr.", "Ms.", "Mrs.", "Dr.");
        }

        //Customer Table Columns
        if (idCol != null) idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (titleCol != null) titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (emailCol != null) emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (addressCol != null) addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (townCol != null) townCol.setCellValueFactory(new PropertyValueFactory<>("town"));
        if (postcodeCol != null) postcodeCol.setCellValueFactory(new PropertyValueFactory<>("postcode"));
        if (limitCol != null) limitCol.setCellValueFactory(new PropertyValueFactory<>("creditLimit"));
        if (debtCol != null) debtCol.setCellValueFactory(new PropertyValueFactory<>("currentDebt"));
        if (statusCol != null) statusCol.setCellValueFactory(new PropertyValueFactory<>("accountStatus"));
        if (rem1Col != null) rem1Col.setCellValueFactory(new PropertyValueFactory<>("status1stReminder"));
        if (rem2Col != null) rem2Col.setCellValueFactory(new PropertyValueFactory<>("status2ndReminder"));

        //Configuring editable columns
        if (customerTable != null) {
            customerTable.setEditable(true);
            setupEditableColumns();
            customerTable.setItems(logic.getCustomerData());
            setupSearchFilter();

            //Link selection to payment history
            customerTable.getSelectionModel().selectedItemProperty()
                    .addListener((obs, oldVal, newVal) -> {
                        if (newVal != null) loadPaymentHistory(newVal);
                    });
        }

        setupPaymentHistoryTable();
        setupRoleBasedControls();
        System.out.println("CustomerView initialized with " + logic.getCustomerData().size() + " customers");
    }


    private void setupEditableColumns() {
        //update local object
        setupEditableStringColumn(titleCol, "title");
        setupEditableStringColumn(nameCol, "name");
        setupEditableStringColumn(emailCol, "email");
        setupEditableStringColumn(addressCol, "address");
        setupEditableStringColumn(townCol, "town");
        setupEditableStringColumn(postcodeCol, "postcode");

        if (limitCol != null) {
            limitCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            limitCol.setOnEditCommit(e -> {
                Customer c = e.getRowValue();
                c.setCreditLimit(e.getNewValue());
                logic.updateCustomerField(c, "credit_limit", e.getNewValue());
                customerTable.refresh(); //Refreshes the table to reflect the updated limit
            });
        }
    }

    /**
     * Method to setup editable string columns
     */
    private void setupEditableStringColumn(TableColumn<Customer, String> col, String property) {
        if (col == null) return;
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(e -> {
            Customer c = e.getRowValue();
            String newValue = e.getNewValue();
            switch (property) {
                case "title": c.setTitle(newValue); break;
                case "name": c.setName(newValue); break;
                case "email": c.setEmail(newValue); break;
                case "address": c.setAddress(newValue); break;
                case "town": c.setTown(newValue); break;
                case "postcode": c.setPostcode(newValue); break;
            }
            logic.updateCustomerField(c, property, newValue); //Update the database with the new value
            customerTable.refresh();
        });
    }

    private void setupSearchFilter() {
        if (customerTable == null || customerSearch == null) return;
        FilteredList<Customer> filtered = new FilteredList<>(logic.getCustomerData(), p -> true);
        customerSearch.textProperty().addListener((obs, old, newVal) -> {
            filtered.setPredicate(c -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return c.getName().toLowerCase().contains(lower) ||
                        c.getEmail().toLowerCase().contains(lower) ||
                        String.valueOf(c.getId()).contains(newVal);
            });
        });
        customerTable.setItems(filtered);
    }

    private void setupPaymentHistoryTable() {
        if (paymentHistoryTable == null) return;
        if (payDateCol != null) payDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        if (payAmountCol != null) payAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        if (payTypeCol != null) payTypeCol.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        if (payDetailsCol != null) payDetailsCol.setCellValueFactory(new PropertyValueFactory<>("paymentDetails"));
        if (payRefCol != null) payRefCol.setCellValueFactory(new PropertyValueFactory<>("reference"));
    }

    private void setupRoleBasedControls() {
        if (unsuspendButton != null && !"MANAGER".equals(currentUserRole)) {
            unsuspendButton.setVisible(false);
            unsuspendButton.setManaged(false);
        }
    }

    private void loadPaymentHistory(Customer customer) {
        if (selectedCustomerLabel != null) {
            selectedCustomerLabel.setText("Payment History: " + customer.getTitle() + " " + customer.getName());
        }
        if (paymentHistoryTable != null) {
            paymentHistoryTable.setItems(logic.getCustomerPaymentHistory(customer.getId()));
        }
    }


    @FXML
    private void handleAddCustomer() {
        try {
            String title = titleCombo != null ? titleCombo.getValue() : null;
            String name = nameInput != null ? nameInput.getText() : "";
            String email = emailInput != null ? emailInput.getText() : "";
            String address = addressInput != null ? addressInput.getText() : "";
            String town = townInput != null ? townInput.getText() : "";
            String postcode = postcodeInput != null ? postcodeInput.getText() : "";
            double limit = limitInput != null ? Double.parseDouble(limitInput.getText()) : 0;

            if (title == null || name.isEmpty() || address.isEmpty() || town.isEmpty() || postcode.isEmpty()) {
                showInfo("Please fill in all required fields.", true);
                return;
            }

            //Adding customer to database
            if (logic.addCustomer(title, name, email, address, town, postcode, limit)) {
                clearInputs();
                customerTable.refresh();
                showInfo("Customer added successfully.", false);
            } else {
                showInfo("Failed to add customer.", true);
            }
        } catch (NumberFormatException e) {
            showInfo("Invalid credit limit format.", true);
        } catch (Exception e) {
            showInfo("Error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDeleteCustomer() {
        //Deleting customer from database
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showInfo("Please select a customer first.", true);
            return;
        }
        if (logic.deleteCustomer(selected)) {
            customerTable.refresh();
            showInfo("Customer deleted.", false);
        } else {
            showInfo("Cannot delete account with active debt.", true);
        }
    }

    @FXML
    private void handleUnsuspendCustomer() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showInfo("Please select a customer first.", true);
            return;
        }
        if (!"MANAGER".equals(currentUserRole)) {
            showInfo("Only Manager can unsuspend accounts.", true);
            return;
        }

        if (selected.getCurrentDebt() > 0.001) {
            showInfo("Cannot Unsuspend: Customer has outstanding debt of £" +
                    String.format("%.2f", selected.getCurrentDebt()) + ".\nThey must pay in full first.", true);
            return;
        }

        selected.setAccountStatus("Normal");
        logic.updateCustomer(selected);
        customerTable.refresh();
        showInfo("Account unsuspended: " + selected.getName(), false);
    }

    @FXML
    private void handleViewPurchaseHistory() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showInfo("Please select a customer first.", true);
            return;
        }
        File report = logic.generatePurchaseHistoryReport(selected);
        if (report != null) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(report.toURI());
                }
                showInfo("Purchase history report generated.", false);
            } catch (Exception e) {
                showInfo("Error opening report: " + e.getMessage(), true);
            }
        }
    }

    @FXML
    private void handleRefreshPaymentHistory() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected != null) {
            loadPaymentHistory(selected);
            showInfo("Payment history refreshed", false);
        }
    }

    @FXML
    private void triggerSystemAutoCheck() {
        logic.evaluateAccountStatuses(LocalDate.now().plusDays(45));
        customerTable.refresh();
        showInfo("Compliance check complete.", false);
    }

    @FXML
    private void generateMonthlyStatements() {
        int day = LocalDate.now().getDayOfMonth();
        if (day < 5 || day > 15) {
            showInfo("Statements can only be generated between 5th-15th of month.", true);
            return;
        }
        int count = 0;
        for (Customer c : logic.getCustomerData()) {
            if (c.getCurrentDebt() > 0 && logic.generateMonthlyStatement(c) != null) count++;
        }
        customerTable.refresh();
        showInfo(count + " statement(s) generated.", false);
    }

    @FXML
    private void handleGenerateFirstReminder() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) { showInfo("Select a customer.", true); return; }
        if (selected.getCurrentDebt() <= 0) { showInfo("No debt to remind about.", true); return; }
        if (!"due".equals(selected.getStatus1stReminder())) { showInfo("1st reminder not due yet.", true); return; }

        File reminder = logic.generateFirstReminder(selected);
        openFile(reminder, selected.getName() + " - 1st Reminder");
    }

    @FXML
    private void handleGenerateSecondReminder() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) { showInfo("Select a customer.", true); return; }
        if (selected.getCurrentDebt() <= 0) { showInfo("No debt to remind about.", true); return; }
        if (!"due".equals(selected.getStatus2ndReminder())) { showInfo("2nd reminder not due yet.", true); return; }

        LocalDate date2nd = selected.getDate2ndReminder();
        if (date2nd == null || date2nd.isAfter(LocalDate.now())) { showInfo("2nd reminder date not reached.", true); return; }

        File reminder = logic.generateSecondReminder(selected);
        openFile(reminder, selected.getName() + " - 2nd Reminder");
    }

    @FXML
    private void handleProcessAllReminders() {
        int count = 0;
        for (Customer c : logic.getCustomerData()) {
            logic.processReminders(c);
            if ("sent".equals(c.getStatus1stReminder()) || "sent".equals(c.getStatus2ndReminder())) count++;
        }
        customerTable.refresh();
        showInfo("Processed reminders for " + count + " customer(s).", false);
    }

    @FXML
    private void handleDemoFirstReminder() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showInfo("Select a customer to demo.", true);
            return;
        }

        File reminder = logic.generateFirstReminder(selected);
        if (reminder != null) {
            openFile(reminder, selected.getName() + " - Demo 1st Reminder");
            showInfo("Demo: 1st Reminder generated", false);
        } else {
            showInfo("Failed to generate demo reminder.", true);
        }
    }

    @FXML
    private void handleDemoSecondReminder() {
        Customer selected = customerTable != null ? customerTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showInfo("Select a customer to demo.", true);
            return;
        }

        File reminder = logic.generateSecondReminder(selected);
        if (reminder != null) {
            openFile(reminder, selected.getName() + " - DEMO 2nd Reminder");
            showInfo("DEMO: 2nd Reminder generated", false);
        } else {
            showInfo("Failed to generate demo reminder.", true);
        }
    }

    private void openFile(File file, String description) {
        if (file != null) {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(file.toURI());
                }
                customerTable.refresh();
            } catch (Exception e) {
                showInfo("Error opening " + description + ": " + e.getMessage(), true);
            }
        }
    }

    private void clearInputs() {
        if (nameInput != null) nameInput.clear();
        if (emailInput != null) emailInput.clear();
        if (addressInput != null) addressInput.clear();
        if (townInput != null) townInput.clear();
        if (postcodeInput != null) postcodeInput.clear();
        if (limitInput != null) limitInput.clear();
        if (titleCombo != null) titleCombo.setValue(null);
    }

    private void showInfo(String message, boolean isError) {
        if (informationLabel != null) {
            informationLabel.setText(message);
            informationLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green") + "; -fx-font-weight: bold;");
        }
    }

    public void setCurrentUserRole(String role) {
        this.currentUserRole = role;
        setupRoleBasedControls();
    }
}