package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.SupplierController;
import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Supplier Catalogue View - IPOS-SA Ordering System
 */
public class SupplierCatalogueView extends VBox {

    private SupplierController controller = SupplierController.getInstance();
    private ObservableList<OrderCartItem> orderCart = FXCollections.observableArrayList();

    // Catalogue Table
    private TableView<SupplierCatalogueItem> catalogueTable;
    private TextField searchField;
    private ComboBox<String> categoryFilter;
    private Label infoLabel;

    // Cart Table
    private TableView<OrderCartItem> cartTable;
    private Label totalLabel;

    // Orders Table
    private TableView<SupplierOrder> ordersTable;
    private Label ordersInfoLabel;

    // Invoices Table
    private TableView<SupplierController.Invoice> invoicesTable;
    private Label balanceLabel;
    private Label balanceInfoLabel;

    public SupplierCatalogueView() {
        setSpacing(15);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f8f9fa;");

        // Create tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab 1: Catalogue
        Tab catalogueTab = new Tab("Supplier Catalogue");
        catalogueTab.setContent(createCataloguePanel());
        catalogueTab.setClosable(false);

        // Tab 2: Current Order
        Tab orderTab = new Tab("Current Order");
        orderTab.setContent(createOrderPanel());
        orderTab.setClosable(false);

        // Tab 3: Order History
        Tab historyTab = new Tab("Order History");
        historyTab.setContent(createHistoryPanel());
        historyTab.setClosable(false);

        // Tab 4: Outstanding Balance
        Tab balanceTab = new Tab("Outstanding Balance");
        balanceTab.setContent(createBalancePanel());
        balanceTab.setClosable(false);

        tabPane.getTabs().addAll(catalogueTab, orderTab, historyTab, balanceTab);

        getChildren().add(tabPane);
    }

    /**
     * Create Catalogue Panel
     */
    private VBox createCataloguePanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titleLabel = new Label("IPOS-SA Supplier Catalogue");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        // Search & Filter
        HBox filterBox = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Search by Item ID or Description...");
        searchField.setPrefWidth(300);

        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("All", "Pain Relief", "Antiseptics", "Cold Relief", "Antibiotics", "Vitamins");
        categoryFilter.setValue("All");
        categoryFilter.setPrefWidth(150);

        filterBox.getChildren().addAll(new Label("Search:"), searchField, new Label("Category:"), categoryFilter);

        // Catalogue Table
        catalogueTable = new TableView<>();
        catalogueTable.setItems(controller.getCatalogue());
        VBox.setVgrow(catalogueTable, Priority.ALWAYS);

        TableColumn<SupplierCatalogueItem, String> itemIdCol = new TableColumn<>("Item ID");
        itemIdCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        itemIdCol.setPrefWidth(100);

        TableColumn<SupplierCatalogueItem, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionCol.setPrefWidth(200);

        TableColumn<SupplierCatalogueItem, String> packageTypeCol = new TableColumn<>("Package Type");
        packageTypeCol.setCellValueFactory(new PropertyValueFactory<>("packageType"));
        packageTypeCol.setPrefWidth(100);

        TableColumn<SupplierCatalogueItem, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(60);

        TableColumn<SupplierCatalogueItem, Integer> unitsPerPackCol = new TableColumn<>("Units/Pack");
        unitsPerPackCol.setCellValueFactory(new PropertyValueFactory<>("unitsPerPack"));
        unitsPerPackCol.setPrefWidth(80);

        TableColumn<SupplierCatalogueItem, Double> packageCostCol = new TableColumn<>("Package Cost");
        packageCostCol.setCellValueFactory(new PropertyValueFactory<>("packageCost"));
        packageCostCol.setPrefWidth(120);

        TableColumn<SupplierCatalogueItem, Integer> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(new PropertyValueFactory<>("availability"));
        availabilityCol.setPrefWidth(100);

        catalogueTable.getColumns().addAll(itemIdCol, descriptionCol, packageTypeCol, unitCol,
                unitsPerPackCol, packageCostCol, availabilityCol);

        // Filter functionality
        searchField.textProperty().addListener((obs, old, newVal) -> filterCatalogue());
        categoryFilter.setOnAction(e -> filterCatalogue());

        // Single Add Button
        Button addToOrderButton = new Button("Add to Order");
        addToOrderButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        addToOrderButton.setOnAction(e -> addToOrder());

        infoLabel = new Label("Select an item from the catalogue and click 'Add to Order'");
        infoLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        panel.getChildren().addAll(titleLabel, filterBox, catalogueTable, addToOrderButton, infoLabel);

        return panel;
    }

    /**
     * Create Order Panel
     */
    private VBox createOrderPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titleLabel = new Label("Current Order");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        // Cart Table with EDITABLE quantity
        cartTable = new TableView<>();
        cartTable.setItems(orderCart);
        cartTable.setEditable(true);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        TableColumn<OrderCartItem, String> cartItemIdCol = new TableColumn<>("Item ID");
        cartItemIdCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        cartItemIdCol.setPrefWidth(100);

        TableColumn<OrderCartItem, String> cartDescCol = new TableColumn<>("Description");
        cartDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        cartDescCol.setPrefWidth(200);

        TableColumn<OrderCartItem, Integer> cartQtyCol = new TableColumn<>("Quantity");
        cartQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cartQtyCol.setOnEditCommit(e -> {
            OrderCartItem item = e.getRowValue();
            item.quantity.set(e.getNewValue());
            item.total.set(e.getNewValue() * item.unitCost.get());
            updateCartDisplay();
        });
        cartQtyCol.setPrefWidth(100);

        TableColumn<OrderCartItem, Double> cartCostCol = new TableColumn<>("Unit Cost");
        cartCostCol.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        cartCostCol.setPrefWidth(100);

        TableColumn<OrderCartItem, Double> cartTotalCol = new TableColumn<>("Total");
        cartTotalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        cartTotalCol.setPrefWidth(100);

        cartTable.getColumns().addAll(cartItemIdCol, cartDescCol, cartQtyCol, cartCostCol, cartTotalCol);

        // Single Remove Button
        Button removeFromOrderButton = new Button("Remove from Order");
        removeFromOrderButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        removeFromOrderButton.setOnAction(e -> removeFromOrder());

        // Total and Submit
        HBox totalBox = new HBox(20);
        totalBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        totalLabel = new Label("Grand Total: 0.00");
        totalLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        totalLabel.setStyle("-fx-text-fill: #2c3e50;");

        Button submitButton = new Button("Submit Order to IPOS-SA");
        submitButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        submitButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 12 30;");
        submitButton.setOnAction(e -> submitOrder());

        Button clearButton = new Button("Clear Order");
        clearButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        clearButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 12 30;");
        clearButton.setOnAction(e -> {
            orderCart.clear();
            updateCartDisplay();
        });

        totalBox.getChildren().addAll(totalLabel, submitButton, clearButton);

        panel.getChildren().addAll(titleLabel, cartTable, removeFromOrderButton, totalBox);

        return panel;
    }

    /**
     * Create Order History Panel
     */
    private VBox createHistoryPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titleLabel = new Label("Order History");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        ordersTable = new TableView<>();
        ordersTable.setItems(controller.getOrders());
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        TableColumn<SupplierOrder, String> orderIdCol = new TableColumn<>("Order ID");
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        orderIdCol.setPrefWidth(100);

        TableColumn<SupplierOrder, LocalDate> orderDateCol = new TableColumn<>("Ordered");
        orderDateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderDateCol.setPrefWidth(100);

        TableColumn<SupplierOrder, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        amountCol.setPrefWidth(100);

        TableColumn<SupplierOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        // Action Column for Delivery
        TableColumn<SupplierOrder, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(param -> new TableCell<SupplierOrder, Void>() {
            private final Button deliverButton = new Button("Mark as Delivered");
            {
                deliverButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                deliverButton.setOnAction(e -> {
                    SupplierOrder order = getTableView().getItems().get(getIndex());
                    if (!"Delivered".equals(order.getStatus())) {
                        boolean success = controller.markOrderAsDelivered(order.getOrderId());
                        if (success) {
                            showAlert("Order " + order.getOrderId() + " marked as delivered!\nInventory updated.");
                            controller.refreshOrders();
                            ordersTable.setItems(controller.getOrders());
                        } else {
                            showAlert("Failed to mark order as delivered.");
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SupplierOrder order = getTableView().getItems().get(getIndex());
                    if ("Delivered".equals(order.getStatus())) {
                        deliverButton.setText("✓ Delivered");
                        deliverButton.setDisable(true);
                        deliverButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
                    } else {
                        deliverButton.setText("Mark as Delivered");
                        deliverButton.setDisable(false);
                        deliverButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
                    }
                    setGraphic(deliverButton);
                }
            }
        });

        ordersTable.getColumns().addAll(orderIdCol, orderDateCol, amountCol, statusCol, actionCol);

        // Report Buttons
        HBox reportButtonBox = new HBox(10);

        Button refreshButton = new Button("Refresh Orders");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> {
            controller.refreshOrders();
            ordersTable.setItems(controller.getOrders());
        });

        // Order Form Button (Appendix 9.2)
        Button orderFormButton = new Button("Order Form");
        orderFormButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        orderFormButton.setOnAction(e -> {
            SupplierOrder selected = ordersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                controller.generateOrderForm(selected.getOrderId());
                showAlert("Order Form generated and opened in browser.");
            } else {
                showAlert("Please select an order first.");
            }
        });

        // Orders Summary Report Button (Appendix 9.4)
        Button summaryReportButton = new Button("Orders Summary");
        summaryReportButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        summaryReportButton.setOnAction(e -> generateOrdersSummaryReport());

        // Detailed Order Report Button (Appendix 9.5)
        Button detailedReportButton = new Button("Detailed Report");
        detailedReportButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        detailedReportButton.setOnAction(e -> generateDetailedOrderReport());

        // Mark as Paid Button
        Button markAsPaidButton = new Button("Mark as Paid");
        markAsPaidButton.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        markAsPaidButton.setOnAction(e -> markSelectedAsPaid());

        reportButtonBox.getChildren().addAll(refreshButton, markAsPaidButton, orderFormButton, summaryReportButton, detailedReportButton);

        ordersInfoLabel = new Label();
        ordersInfoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        panel.getChildren().addAll(titleLabel, ordersTable, reportButtonBox, ordersInfoLabel);

        return panel;
    }

    /**
     * Create Outstanding Balance Panel
     */
    private VBox createBalancePanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        Label titleLabel = new Label("Outstanding Balance Query");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        // Balance Display
        VBox balanceBox = new VBox(10);
        balanceBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-border-color: #e74c3c; -fx-border-width: 2;");

        balanceLabel = new Label("Total Outstanding: 0.00");
        balanceLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        balanceLabel.setStyle("-fx-text-fill: #e74c3c;");

        Label infoText = new Label("This represents unpaid invoices from IPOS-SA");
        infoText.setStyle("-fx-text-fill: #7f8c8d;");

        balanceBox.getChildren().addAll(balanceLabel, infoText);

        // Invoices Table
        Label invoicesLabel = new Label("Invoice Details");
        invoicesLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        invoicesTable = new TableView<>();
        invoicesTable.setItems(controller.getInvoices());
        invoicesTable.setPrefHeight(300);

        TableColumn<SupplierController.Invoice, String> invoiceIdCol = new TableColumn<>("Invoice ID");
        invoiceIdCol.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        invoiceIdCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, String> invoiceOrderIdCol = new TableColumn<>("Order ID");
        invoiceOrderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        invoiceOrderIdCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, LocalDate> invoiceDateCol = new TableColumn<>("Invoice Date");
        invoiceDateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        invoiceDateCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, Double> invoiceAmountCol = new TableColumn<>("Amount");
        invoiceAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        invoiceAmountCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, Double> paidAmountCol = new TableColumn<>("Paid");
        paidAmountCol.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        paidAmountCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, Double> balanceCol = new TableColumn<>("Outstanding");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("outstandingBalance"));
        balanceCol.setPrefWidth(100);

        TableColumn<SupplierController.Invoice, String> invoiceStatusCol = new TableColumn<>("Status");
        invoiceStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        invoiceStatusCol.setPrefWidth(100);

        invoicesTable.getColumns().addAll(invoiceIdCol, invoiceOrderIdCol, invoiceDateCol,
                invoiceAmountCol, paidAmountCol, balanceCol, invoiceStatusCol);

        HBox buttonBox = new HBox(10);

        Button refreshButton = new Button("Refresh Balance");
        refreshButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        refreshButton.setOnAction(e -> {
            invoicesTable.setItems(controller.getInvoices());
            updateBalanceDisplay();
            balanceInfoLabel.setText("Balance refreshed");
            balanceInfoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        });

        Button payButton = new Button("Pay Selected Invoice");
        payButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        payButton.setOnAction(e -> paySelectedInvoice());

        buttonBox.getChildren().addAll(refreshButton, payButton);

        balanceInfoLabel = new Label();
        balanceInfoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        panel.getChildren().addAll(titleLabel, balanceBox, invoicesLabel, invoicesTable, buttonBox, balanceInfoLabel);

        updateBalanceDisplay();

        return panel;
    }

    //ACTION METHODS

    private void addToOrder() {
        SupplierCatalogueItem selected = catalogueTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an item from the catalogue first.");
            infoLabel.setText("No item selected");
            infoLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            return;
        }

        for (OrderCartItem cartItem : orderCart) {
            if (cartItem.itemId.get().equals(selected.getItemId())) {
                cartItem.quantity.set(cartItem.quantity.get() + 1);
                cartItem.total.set(cartItem.quantity.get() * cartItem.unitCost.get());
                updateCartDisplay();
                infoLabel.setText("Quantity updated for: " + selected.getDescription());
                infoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                return;
            }
        }

        orderCart.add(new OrderCartItem(
                selected.getItemId(),
                selected.getDescription(),
                1,
                selected.getPackageCost(),
                selected.getPackageCost()
        ));
        updateCartDisplay();
        infoLabel.setText("Added to order: " + selected.getDescription());
        infoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void removeFromOrder() {
        OrderCartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an item from the order cart first.");
            return;
        }

        orderCart.remove(selected);
        updateCartDisplay();
        infoLabel.setText("Removed from order: " + selected.description.get());
        infoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }

    private void updateCartDisplay() {
        if (cartTable != null) {
            cartTable.refresh();
        }
        double total = orderCart.stream().mapToDouble(item -> item.total.get()).sum();
        if (totalLabel != null) {
            totalLabel.setText("Grand Total: " + String.format("%.2f", total));
        }
    }

    private void filterCatalogue() {
        String searchText = searchField.getText().toLowerCase();
        String category = categoryFilter.getValue();

        ObservableList<SupplierCatalogueItem> filtered = FXCollections.observableArrayList();
        for (SupplierCatalogueItem item : controller.getCatalogue()) {
            boolean matchesSearch = item.getItemId().toLowerCase().contains(searchText) ||
                    item.getDescription().toLowerCase().contains(searchText);
            boolean matchesCategory = "All".equals(category) || item.getCategory().equals(category);
            if (matchesSearch && matchesCategory) {
                filtered.add(item);
            }
        }
        catalogueTable.setItems(filtered);
    }

    private void submitOrder() {
        if (orderCart.isEmpty()) {
            showAlert("Order cart is empty!");
            return;
        }

        List<SupplierController.OrderItem> orderItems = new ArrayList<>();
        for (OrderCartItem item : orderCart) {
            orderItems.add(new SupplierController.OrderItem(
                    item.itemId.get(),
                    item.description.get(),
                    item.quantity.get(),
                    item.unitCost.get(),
                    item.total.get()
            ));
        }

        String result = controller.placeOrder(orderItems);
        if (result.startsWith("IP")) {
            showAlert("Order Placed Successfully!\nOrder ID: " + result +
                    "\nTotal: " + totalLabel.getText() +
                    "\n\nOrder has been sent to IPOS-SA for processing.");
            orderCart.clear();
            updateCartDisplay();
            controller.refreshOrders();
        } else {
            showAlert("Order Failed: " + result);
        }
    }

    private void markSelectedAsPaid() {
        SupplierOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an order first.");
            return;
        }

        if ("Paid".equals(selected.getPaymentStatus())) {
            showAlert("This order is already marked as paid.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Payment");
        confirmAlert.setHeaderText("Mark Order as Paid?");
        confirmAlert.setContentText("Order ID: " + selected.getOrderId() + "\nAmount: £" +
                String.format("%.2f", selected.getTotalAmount()) +
                "\n\nThis will mark the invoice as paid in IPOS-SA.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = controller.markOrderAsPaid(selected.getOrderId());
                if (success) {
                    showAlert("Payment confirmed!\nOrder " + selected.getOrderId() + " marked as paid.");
                    controller.refreshOrders();
                    ordersTable.setItems(controller.getOrders());
                    updateBalanceDisplay();
                } else {
                    showAlert("Failed to mark order as paid.");
                }
            }
        });
    }

    private void paySelectedInvoice() {
        SupplierController.Invoice selected = invoicesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an invoice first.");
            return;
        }

        if ("Paid".equals(selected.getStatus())) {
            showAlert("This invoice is already paid.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Payment");
        confirmAlert.setHeaderText("Pay Invoice?");
        confirmAlert.setContentText("Invoice: " + selected.getInvoiceId() + "\nAmount: £" +
                String.format("%.2f", selected.getOutstandingBalance()) +
                "\n\nThis will process payment through IPOS-SA.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = controller.markOrderAsPaid(selected.getOrderId());
                if (success) {
                    showAlert("Payment processed!\nInvoice " + selected.getInvoiceId() + " marked as paid.");
                    invoicesTable.setItems(controller.getInvoices());
                    updateBalanceDisplay();
                    controller.refreshOrders();
                    ordersTable.setItems(controller.getOrders());
                } else {
                    showAlert("Payment failed.");
                }
            }
        });
    }

    private void updateBalanceDisplay() {
        double outstandingBalance = controller.getOutstandingBalance();
        if (balanceLabel != null) {
            balanceLabel.setText("Total Outstanding: " + String.format("%.2f", outstandingBalance));
        }
    }

    private void generateOrdersSummaryReport() {
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        File reportFile = controller.generateOrdersSummaryReport(startDate, endDate);
        showAlert("Orders Summary Report generated:\n" + reportFile.getName());
    }

    private void generateDetailedOrderReport() {
        SupplierOrder selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showAlert("Please select an order to view details");
            return;
        }

        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        File reportFile = controller.generateDetailedOrderReport(startDate, endDate);
        showAlert("Detailed Order Report generated:\n" + reportFile.getName());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Supplier System");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===== INNER CLASS: OrderCartItem =====

    public static class OrderCartItem {
        public final StringProperty itemId;
        public final StringProperty description;
        public final IntegerProperty quantity;
        public final DoubleProperty unitCost;
        public final DoubleProperty total;

        public OrderCartItem(String itemId, String description, int quantity, double unitCost, double total) {
            this.itemId = new SimpleStringProperty(itemId);
            this.description = new SimpleStringProperty(description);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.unitCost = new SimpleDoubleProperty(unitCost);
            this.total = new SimpleDoubleProperty(total);
        }

        public String getItemId() { return itemId.get(); }
        public String getDescription() { return description.get(); }
        public int getQuantity() { return quantity.get(); }
        public double getUnitCost() { return unitCost.get(); }
        public double getTotal() { return total.get(); }
    }
}