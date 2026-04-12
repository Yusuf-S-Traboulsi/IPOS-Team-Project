package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.CustomerController;
import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.controller.SalesController;
import com.pharmacy.iposca.model.Customer;
import com.pharmacy.iposca.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.converter.IntegerStringConverter;
import java.io.File;
import java.time.LocalDate;

public class POSView {

    // Tab Controls
    @FXML private TabPane posTabPane;
    @FXML private Tab shoppingTab, debtTab;

    // Shopping Tab Controls
    @FXML private TextField searchField;
    @FXML private ListView<Product> catalogList;
    @FXML private TableView<SalesController.CartItem> cartTable;
    @FXML private TableColumn<SalesController.CartItem, String> cartItemCol;
    @FXML private TableColumn<SalesController.CartItem, Integer> cartQtyCol;
    @FXML private TableColumn<SalesController.CartItem, Double> cartPriceCol;
    @FXML private TextField customerIdField;
    @FXML private Label customerStatusLabel;
    @FXML private RadioButton cashRadio, cardRadio, creditRadio;
    @FXML private VBox cardDetailsBox;
    @FXML private TextField cardTypeField, cardFirstFour, cardLastFour, cardExpiry;
    @FXML private Text totalText;
    @FXML private Label informationLabel;

    // Debt Tab Controls
    @FXML private TextField debtCustomerIdField;
    @FXML private VBox debtPaymentBox;
    @FXML private Label debtCustomerLabel, debtLabel, debtStatusWarning;
    @FXML private TextField debtPaymentAmount;
    @FXML private RadioButton debtCashRadio, debtCardRadio;
    @FXML private VBox debtCardDetailsBox;
    @FXML private TextField debtCardTypeField, debtCardFirstFour, debtCardLastFour, debtCardExpiry;
    @FXML private Button payDebtButton;

    private InventoryController inventoryController;
    private CustomerController customerController;
    private SalesController salesController;
    private ObservableList<Product> catalog;
    private Customer currentCustomer;

    @FXML
    public void initialize() {
        inventoryController = InventoryController.getInstance();
        customerController = CustomerController.getInstance();
        salesController = new SalesController(inventoryController);

        // Initialize Product Catalog
        catalog = FXCollections.observableArrayList(inventoryController.getProducts());
        catalogList.setItems(catalog);
        catalogList.setCellFactory(lv -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s - £%.2f (Stock: %d)",
                            item.getName(), item.getPrice(), item.getStock()));
                }
            }
        });

        searchField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                catalogList.setItems(catalog);
            } else {
                ObservableList<Product> filtered = FXCollections.observableArrayList();
                for (Product p : catalog) {
                    if (p.getName().toLowerCase().contains(newVal.toLowerCase()) ||
                            String.valueOf(p.getId()).contains(newVal)) {
                        filtered.add(p);
                    }
                }
                catalogList.setItems(filtered);
            }
        });

        // Initialize Cart Table
        cartTable.setItems(salesController.getCart());
        cartTable.setEditable(true);
        cartItemCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cartQtyCol.setOnEditCommit(e -> {
            SalesController.CartItem item = e.getRowValue();
            item.setQuantity(e.getNewValue());
            updateTotal();
        });
        cartPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Payment Radio Buttons - Shopping Tab
        cashRadio.setOnAction(e -> {
            cardDetailsBox.setVisible(false);
            cardDetailsBox.setManaged(false);
            updateTotal();
        });
        cardRadio.setOnAction(e -> {
            cardDetailsBox.setVisible(true);
            cardDetailsBox.setManaged(true);
            updateTotal();
        });
        creditRadio.setOnAction(e -> {
            cardDetailsBox.setVisible(false);
            cardDetailsBox.setManaged(false);
            updateTotal();
        });

        customerIdField.setOnAction(e -> lookupCustomer());

        // Debt Payment Radio Buttons
        debtCashRadio.setOnAction(e -> {
            debtCardDetailsBox.setVisible(false);
            debtCardDetailsBox.setManaged(false);
        });
        debtCardRadio.setOnAction(e -> {
            debtCardDetailsBox.setVisible(true);
            debtCardDetailsBox.setManaged(true);
        });

        // Initialize UI
        cashRadio.setSelected(true);
        debtCashRadio.setSelected(true);
        cardDetailsBox.setVisible(false);
        cardDetailsBox.setManaged(false);
        debtCardDetailsBox.setVisible(false);
        debtCardDetailsBox.setManaged(false);
        debtPaymentBox.setVisible(false);
        debtPaymentBox.setManaged(false);
        customerStatusLabel.setText("");
        updateTotal();
    }

    // ==================== SHOPPING TAB METHODS ====================

    @FXML
    private void addToCart() {
        Product selected = catalogList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a product from the catalog.");
            return;
        }
        if (selected.getStock() <= 0) {
            showAlert("Product out of stock!");
            return;
        }

        for (SalesController.CartItem item : salesController.getCart()) {
            if (item.getProduct().getId() == selected.getId()) {
                if (item.getQuantity() < selected.getStock()) {
                    item.setQuantity(item.getQuantity() + 1);
                    updateTotal();
                    informationLabel.setText("Added: " + selected.getName());
                    informationLabel.setStyle("-fx-text-fill: green;");
                    return;
                } else {
                    showAlert("Cannot add more - stock limit reached.");
                    return;
                }
            }
        }

        salesController.addItemToCart(selected);
        updateTotal();
        informationLabel.setText("Added: " + selected.getName());
        informationLabel.setStyle("-fx-text-fill: green;");
        catalog.setAll(inventoryController.getProducts());
    }

    @FXML
    private void removeFromCart() {
        SalesController.CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            salesController.removeItemFromCart(selected);
            updateTotal();
            informationLabel.setText("Item removed");
            informationLabel.setStyle("-fx-text-fill: green;");
        } else {
            showAlert("Select an item to remove.");
        }
    }

    @FXML
    private void lookupCustomer() {
        try {
            int customerId = Integer.parseInt(customerIdField.getText());
            currentCustomer = customerController.findCustomerById(customerId);

            if (currentCustomer != null) {
                // ✅ PROTECTIVE MEASURE 1: Check Account Status
                String status = currentCustomer.getAccountStatus();

                if ("Suspended".equals(status)) {
                    customerStatusLabel.setText("⚠️ ACCOUNT SUSPENDED - Cannot purchase until debt is cleared");
                    customerStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    showAlert("⚠️ ACCOUNT SUSPENDED\n\nThis customer account is suspended due to outstanding debt.\n\n" +
                            "Current Debt: £" + String.format("%.2f", currentCustomer.getCurrentDebt()) + "\n" +
                            "Credit Limit: £" + String.format("%.2f", currentCustomer.getCreditLimit()) + "\n\n" +
                            "Please clear outstanding balance before allowing purchases.");
                    currentCustomer = null;
                    return;
                }

                if ("In Default".equals(status)) {
                    customerStatusLabel.setText("⚠️ ACCOUNT IN DEFAULT - Cannot purchase");
                    customerStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    showAlert("⚠️ ACCOUNT IN DEFAULT\n\nThis customer account is in default status.\n\n" +
                            "Current Debt: £" + String.format("%.2f", currentCustomer.getCurrentDebt()) + "\n" +
                            "Credit Limit: £" + String.format("%.2f", currentCustomer.getCreditLimit()) + "\n\n" +
                            "Manager approval required to restore account.");
                    currentCustomer = null;
                    return;
                }

                // ✅ PROTECTIVE MEASURE 2: Check Available Credit
                double availableCredit = currentCustomer.getCreditLimit() - currentCustomer.getCurrentDebt();
                if (availableCredit <= 0) {
                    customerStatusLabel.setText("⚠️ NO AVAILABLE CREDIT - Credit limit reached");
                    customerStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    showAlert("⚠️ CREDIT LIMIT REACHED\n\nThis customer has no available credit.\n\n" +
                            "Current Debt: £" + String.format("%.2f", currentCustomer.getCurrentDebt()) + "\n" +
                            "Credit Limit: £" + String.format("%.2f", currentCustomer.getCreditLimit()) + "\n" +
                            "Available Credit: £0.00\n\n" +
                            "Payment required before further purchases.");
                    currentCustomer = null;
                    return;
                }

                // Customer is valid
                customerStatusLabel.setText("✓ " + currentCustomer.getName() + " | Status: " + status +
                        " | Available Credit: £" + String.format("%.2f", availableCredit));
                customerStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

                // Force card/credit payment for account holders
                cashRadio.setSelected(false);
                cashRadio.setDisable(true);
                creditRadio.setDisable(false);
                cardRadio.setDisable(false);

                informationLabel.setText("Customer: " + currentCustomer.getName() + " (Account Holder)");
                informationLabel.setStyle("-fx-text-fill: blue;");
                updateTotal();

            } else {
                customerStatusLabel.setText("");
                showAlert("Customer ID not found. Proceeding as walk-in customer.");
                currentCustomer = null;
                informationLabel.setText("Walk-in Customer");
                informationLabel.setStyle("-fx-text-fill: orange;");
                cashRadio.setDisable(false);
                updateTotal();
            }
        } catch (NumberFormatException e) {
            customerStatusLabel.setText("");
            showAlert("Invalid Customer ID format.");
            currentCustomer = null;
            cashRadio.setDisable(false);
            updateTotal();
        }
    }

    @FXML
    private void processSale() {
        if (salesController.getCart().isEmpty()) {
            showAlert("Cart is empty!");
            return;
        }

        // Determine payment type
        String paymentType;
        if (cashRadio.isSelected()) {
            paymentType = "CASH";
        } else if (cardRadio.isSelected()) {
            paymentType = "CARD";
        } else {
            paymentType = "CREDIT";
        }

        // ✅ PROTECTIVE MEASURE 3: Account holders cannot pay with cash
        if (currentCustomer != null && "CASH".equals(paymentType)) {
            showAlert("❌ ACCOUNT HOLDERS CANNOT PAY WITH CASH\n\n" +
                    "Account holders must use Card or Credit payment method.");
            return;
        }

        // Validate card details if card selected
        if ("CARD".equals(paymentType)) {
            String cardError = validateCardDetails();
            if (cardError != null) {
                showAlert("Card Validation Error: " + cardError);
                return;
            }
        }

        // ✅ PROTECTIVE MEASURE 4: Re-check account status before processing
        if (currentCustomer != null) {
            String status = currentCustomer.getAccountStatus();
            if (!"Normal".equals(status)) {
                showAlert("❌ TRANSACTION DENIED\n\nAccount status is '" + status + "'.\n" +
                        "Please settle outstanding balance first.");
                return;
            }

            // Check if purchase would exceed credit limit
            double totalDue = salesController.calculateTotal();
            double discountRate = currentCustomer.calculateEffectiveDiscountRate();
            double totalAfterDiscount = totalDue - (totalDue * discountRate);
            double availableCredit = currentCustomer.getCreditLimit() - currentCustomer.getCurrentDebt();

            if (totalAfterDiscount > availableCredit) {
                showAlert("❌ TRANSACTION EXCEEDS CREDIT LIMIT\n\n" +
                        "Available Credit: £" + String.format("%.2f", availableCredit) + "\n" +
                        "Purchase Total: £" + String.format("%.2f", totalAfterDiscount) + "\n\n" +
                        "Please reduce cart total or make a payment first.");
                return;
            }
        }

        // Process the sale
        String result = salesController.processSale(
                currentCustomer,
                paymentType,
                cardTypeField.getText(),
                cardFirstFour.getText(),
                cardLastFour.getText(),
                cardExpiry.getText()
        );

        if ("SUCCESS".equals(result)) {
            File invoice = salesController.generateFormalLetter(currentCustomer);
            openInvoiceInBrowser(invoice);

            String discountInfo = "";
            if (currentCustomer != null) {
                double discountRate = currentCustomer.calculateEffectiveDiscountRate();
                if (discountRate > 0) {
                    discountInfo = "\nDiscount Applied: " + (discountRate * 100) + "%";
                }
            }

            showAlert("✅ SALE COMPLETED SUCCESSFULLY!\n\n" +
                    "Total: " + totalText.getText() +
                    discountInfo +
                    "\n\nInvoice opened in browser");

            // Reset for next customer
            salesController.getCart().clear();
            updateTotal();
            customerIdField.clear();
            customerStatusLabel.setText("");
            cardTypeField.clear();
            cardFirstFour.clear();
            cardLastFour.clear();
            cardExpiry.clear();
            currentCustomer = null;
            cashRadio.setSelected(true);
            cashRadio.setDisable(false);
            creditRadio.setDisable(false);
            cardRadio.setDisable(false);
            cardDetailsBox.setVisible(false);
            cardDetailsBox.setManaged(false);
            informationLabel.setText("Sale completed - Ready for next customer");
            informationLabel.setStyle("-fx-text-fill: green;");
            catalog.setAll(inventoryController.getProducts());
        } else {
            showAlert("❌ SALE FAILED:\n\n" + result);
            informationLabel.setText("Sale failed");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // ==================== DEBT REPAYMENT TAB METHODS ====================

    @FXML
    private void lookupCustomerForDebt() {
        try {
            int customerId = Integer.parseInt(debtCustomerIdField.getText());
            currentCustomer = customerController.findCustomerById(customerId);

            if (currentCustomer != null) {
                debtCustomerLabel.setText(currentCustomer.getTitle() + " " + currentCustomer.getName());
                debtLabel.setText("Outstanding Debt: £" + String.format("%.2f", currentCustomer.getCurrentDebt()));

                // Show status warning if applicable
                String status = currentCustomer.getAccountStatus();
                if ("Suspended".equals(status)) {
                    debtStatusWarning.setText("⚠️ Account is SUSPENDED. Payment required to restore to Normal status.");
                } else if ("In Default".equals(status)) {
                    debtStatusWarning.setText("⚠️ Account is IN DEFAULT. Manager approval may be required.");
                } else {
                    debtStatusWarning.setText("");
                }

                debtPaymentBox.setVisible(true);
                debtPaymentBox.setManaged(true);
                informationLabel.setText("Found: " + currentCustomer.getName());
                informationLabel.setStyle("-fx-text-fill: blue;");
            } else {
                debtCustomerLabel.setText("");
                debtLabel.setText("Outstanding Debt: £0.00");
                debtStatusWarning.setText("");
                debtPaymentBox.setVisible(false);
                debtPaymentBox.setManaged(false);
                showAlert("Customer not found. Please check Customer ID.");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Customer ID format.");
        }
    }

    @FXML
    private void processDebtPayment() {
        if (currentCustomer == null) {
            showAlert("No customer selected.");
            return;
        }

        double currentDebt = currentCustomer.getCurrentDebt();
        if (currentDebt <= 0) {
            showAlert("This customer has no outstanding debt.");
            return;
        }

        try {
            double paymentAmount = Double.parseDouble(debtPaymentAmount.getText());

            if (paymentAmount <= 0) {
                showAlert("Payment amount must be positive.");
                return;
            }

            if (paymentAmount > currentDebt) {
                showAlert("Payment amount (£" + String.format("%.2f", paymentAmount) +
                        ") exceeds outstanding debt (£" + String.format("%.2f", currentDebt) + ").");
                return;
            }

            // Validate card if card payment selected
            if (debtCardRadio.isSelected()) {
                String cardError = validateDebtCardDetails();
                if (cardError != null) {
                    showAlert("Card Validation Error: " + cardError);
                    return;
                }
            }

            // Process payment via CustomerController
            currentCustomer.setCurrentDebt(currentDebt - paymentAmount);

            // Reset reminders if debt cleared
            if (currentCustomer.getCurrentDebt() <= 0) {
                currentCustomer.setStatus1stReminder("no_need");
                currentCustomer.setStatus2ndReminder("no_need");

                // Auto-reset account status if not "In Default"
                if (!"In Default".equals(currentCustomer.getAccountStatus())) {
                    currentCustomer.setAccountStatus("Normal");
                }
            }

            // Save to database
            customerController.updateCustomer(currentCustomer);

            double newDebt = currentCustomer.getCurrentDebt();
            debtLabel.setText("Outstanding Debt: £" + String.format("%.2f", newDebt));

            if (newDebt <= 0) {
                debtStatusWarning.setText("✅ Debt cleared! Account status restored to Normal.");
                showAlert("✅ PAYMENT PROCESSED SUCCESSFULLY!\n\n" +
                        "Debt cleared in full!\n" +
                        "Account status has been restored to 'Normal'.\n" +
                        "Customer can now make purchases.");
            } else {
                debtStatusWarning.setText("Payment processed. Remaining debt: £" + String.format("%.2f", newDebt));
                showAlert("✅ PAYMENT PROCESSED!\n\n" +
                        "Payment Amount: £" + String.format("%.2f", paymentAmount) + "\n" +
                        "Remaining Debt: £" + String.format("%.2f", newDebt));
            }

            informationLabel.setText("Debt payment processed for " + currentCustomer.getName());
            informationLabel.setStyle("-fx-text-fill: green;");
            debtPaymentAmount.clear();

        } catch (NumberFormatException e) {
            showAlert("Invalid payment amount format.");
        }
    }

    private String validateCardDetails() {
        String cardType = cardTypeField.getText();
        String firstFour = cardFirstFour.getText();
        String lastFour = cardLastFour.getText();
        String expiry = cardExpiry.getText();

        if (cardType == null || !cardType.matches("^(Credit|Debit)$")) {
            return "Card type must be 'Credit' or 'Debit'";
        }
        if (firstFour == null || !firstFour.matches("^\\d{4}$")) {
            return "First 4 digits must be 4 numbers";
        }
        if (lastFour == null || !lastFour.matches("^\\d{4}$")) {
            return "Last 4 digits must be 4 numbers";
        }
        if (expiry == null || !expiry.matches("^\\d{2}/\\d{2}$")) {
            return "Expiry must be MM/YY format";
        }
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return "Card has expired";
            }
        } catch (Exception e) {
            return "Invalid expiry date";
        }
        return null;
    }

    private String validateDebtCardDetails() {
        String cardType = debtCardTypeField.getText();
        String firstFour = debtCardFirstFour.getText();
        String lastFour = debtCardLastFour.getText();
        String expiry = debtCardExpiry.getText();

        if (cardType == null || !cardType.matches("^(Credit|Debit)$")) {
            return "Card type must be 'Credit' or 'Debit'";
        }
        if (firstFour == null || !firstFour.matches("^\\d{4}$")) {
            return "First 4 digits must be 4 numbers";
        }
        if (lastFour == null || !lastFour.matches("^\\d{4}$")) {
            return "Last 4 digits must be 4 numbers";
        }
        if (expiry == null || !expiry.matches("^\\d{2}/\\d{2}$")) {
            return "Expiry must be MM/YY format";
        }
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return "Card has expired";
            }
        } catch (Exception e) {
            return "Invalid expiry date";
        }
        return null;
    }

    private void updateTotal() {
        double total = salesController.calculateTotal();
        if (currentCustomer != null) {
            double discountRate = currentCustomer.calculateEffectiveDiscountRate();
            if (discountRate > 0) {
                double discountAmount = total * discountRate;
                total = total - discountAmount;
            }
        }
        totalText.setText(String.format("£%.2f", total));
    }

    private void openInvoiceInBrowser(File invoiceFile) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(invoiceFile.toURI());
                    System.out.println("Invoice opened: " + invoiceFile.getAbsolutePath());
                } else {
                    showAlert("Invoice saved: " + invoiceFile.getAbsolutePath());
                }
            } else {
                showAlert("Invoice saved: " + invoiceFile.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert("Invoice saved: " + invoiceFile.getAbsolutePath());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("POS System");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}