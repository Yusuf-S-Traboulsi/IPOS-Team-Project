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

/**
 * This UI class is responsible for displaying and managing the Point-of-Sale(POS) view.
 * Handles sales for both account holders and walk-in customers
 * Opens invoice in browser after successful transaction
 */
public class POSView {

    @FXML private TextField searchField;
    @FXML private ListView<Product> catalogList;
    @FXML private TableView<SalesController.CartItem> cartTable;
    @FXML private TableColumn<SalesController.CartItem, String> cartItemCol;
    @FXML private TableColumn<SalesController.CartItem, Integer> cartQtyCol;
    @FXML private TableColumn<SalesController.CartItem, Double> cartPriceCol;
    @FXML private TextField customerIdField;
    @FXML private RadioButton cashRadio;
    @FXML private RadioButton cardRadio;
    @FXML private VBox cardDetailsBox;
    @FXML private TextField cardTypeField;
    @FXML private TextField cardFirstFour;
    @FXML private TextField cardLastFour;
    @FXML private TextField cardExpiry;
    @FXML private Text totalText;
    @FXML private Label informationLabel;

    private InventoryController inventoryController;
    private CustomerController customerController;
    private SalesController salesController;
    private ObservableList<Product> catalog;
    private Customer currentCustomer;

    @FXML
    public void initialize() {
        // Initialize controllers
        inventoryController = InventoryController.getInstance();
        customerController = CustomerController.getInstance();
        salesController = new SalesController(inventoryController);

        // Initialize catalog
        catalog = FXCollections.observableArrayList(inventoryController.getProducts());

        // Setup catalog list
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

        // Search functionality
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

        // Setup cart table
        cartTable.setItems(salesController.getCart());
        cartTable.setEditable(true);

        cartItemCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartItemCol.setSortable(false);

        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cartQtyCol.setOnEditCommit(e -> {
            SalesController.CartItem item = e.getRowValue();
            item.setQuantity(e.getNewValue());
            updateTotal();
        });

        cartPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        cartPriceCol.setCellFactory(column -> new TableCell<SalesController.CartItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                setText(empty || item == null ? null : String.format("£%.2f", item));
            }
        });

        // Toggle card details visibility
        cashRadio.setOnAction(e -> {
            cardDetailsBox.setVisible(false);
            updateTotal();
        });
        cardRadio.setOnAction(e -> {
            cardDetailsBox.setVisible(true);
            updateTotal();
        });

        // Customer lookup
        customerIdField.setOnAction(e -> lookupCustomer());

        // Set default payment method
        cashRadio.setSelected(true);
        cardDetailsBox.setVisible(false);

        updateTotal();
    }

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

        // Check if already in cart
        for (SalesController.CartItem item : salesController.getCart()) {
            if (item.getProduct().getId() == selected.getId()) {
                if (item.getQuantity() < selected.getStock()) {
                    item.setQuantity(item.getQuantity() + 1);
                    updateTotal();
                    informationLabel.setText("Added to cart: " + selected.getName());
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
        informationLabel.setText("Added to cart: " + selected.getName());
        informationLabel.setStyle("-fx-text-fill: green;");

        // Refresh catalog to show updated stock
        catalog.setAll(inventoryController.getProducts());
    }

    @FXML
    private void removeFromCart() {
        SalesController.CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            salesController.removeItemFromCart(selected);
            updateTotal();
            informationLabel.setText("Item removed from cart");
            informationLabel.setStyle("-fx-text-fill: green;");
        } else {
            showAlert("Please select an item to remove.");
        }
    }

    /**
     * Lookup customer by ID
     */
    private void lookupCustomer() {
        try {
            int customerId = Integer.parseInt(customerIdField.getText());
            currentCustomer = customerController.findCustomerById(customerId);

            if (currentCustomer != null) {
                showAlert("Customer found: " + currentCustomer.getName() +
                        "\nStatus: " + currentCustomer.getAccountStatus() +
                        "\nCredit Limit: £" + currentCustomer.getCreditLimit() +
                        "\nCurrent Debt: £" + currentCustomer.getCurrentDebt());

                // Account holders must pay by card
                cashRadio.setSelected(false);
                cardRadio.setSelected(true);
                cardDetailsBox.setVisible(true);

                informationLabel.setText("Customer: " + currentCustomer.getName() + " (Account Holder)");
                informationLabel.setStyle("-fx-text-fill: blue;");
            } else {
                showAlert("Customer ID not found. Proceeding as walk-in customer.");
                currentCustomer = null;
                informationLabel.setText("Walk-in Customer");
                informationLabel.setStyle("-fx-text-fill: orange;");
            }
        } catch (NumberFormatException e) {
            showAlert("Invalid Customer ID format.");
            currentCustomer = null;
        }
    }

    @FXML
    private void processSale() {
        if (salesController.getCart().isEmpty()) {
            showAlert("Cart is empty!");
            return;
        }

        String paymentType = cashRadio.isSelected() ? "CASH" : "CARD";

        // Validate payment method for account holders
        if (currentCustomer != null && "CASH".equals(paymentType)) {
            showAlert("ERROR: Account holders must pay by card.");
            return;
        }

        // Validate card details if paying by card
        if ("CARD".equals(paymentType)) {
            String cardError = validateCardDetails();
            if (cardError != null) {
                showAlert("Card Validation Error: " + cardError);
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
            // Generate invoice
            File invoice = salesController.generateFormalLetter(currentCustomer);

            // OPEN INVOICE IN BROWSER
            openInvoiceInBrowser(invoice);

            String discountInfo = "";
            if (currentCustomer != null) {
                double discountRate = currentCustomer.calculateEffectiveDiscountRate();
                if (discountRate > 0) {
                    discountInfo = "\nDiscount Applied: " + (discountRate * 100) + "%";
                }
            }

            showAlert("Sale Completed Successfully!" +
                    "\nTotal: " + totalText.getText() +
                    discountInfo +
                    "\nInvoice opened in browser");

            // Clear cart
            salesController.getCart().clear();
            updateTotal();
            customerIdField.clear();
            cardTypeField.clear();
            cardFirstFour.clear();
            cardLastFour.clear();
            cardExpiry.clear();
            currentCustomer = null;
            informationLabel.setText("Sale completed - Ready for next customer");
            informationLabel.setStyle("-fx-text-fill: green;");

            // Refresh catalog to show updated stock
            catalog.setAll(inventoryController.getProducts());
        } else {
            showAlert("Sale Failed:\n" + result);
            informationLabel.setText("Sale failed");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Opens the invoice HTML file in the default browser
     */
    private void openInvoiceInBrowser(File invoiceFile) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(invoiceFile.toURI());
                    System.out.println("Invoice opened in browser: " + invoiceFile.getAbsolutePath());
                } else {
                    System.out.println("Desktop BROWSE action not supported");
                    showAlert("Invoice saved to: " + invoiceFile.getAbsolutePath());
                }
            } else {
                System.out.println("Desktop not supported");
                showAlert("Invoice saved to: " + invoiceFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error opening invoice in browser: " + e.getMessage());
            e.printStackTrace();
            showAlert("Invoice saved to: " + invoiceFile.getAbsolutePath());
        }
    }

    /**
     * Method to validate card details
     */
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

        // Check expiry not in the past
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

    /**
     * Update total display with discount applied
     */
    private void updateTotal() {
        double total = salesController.calculateTotal();

        // Apply discount if account holder
        if (currentCustomer != null) {
            double discountRate = currentCustomer.calculateEffectiveDiscountRate();
            if (discountRate > 0) {
                double discountAmount = total * discountRate;
                total = total - discountAmount;
            }
        }

        totalText.setText(String.format("£%.2f", total));
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("POS System");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}