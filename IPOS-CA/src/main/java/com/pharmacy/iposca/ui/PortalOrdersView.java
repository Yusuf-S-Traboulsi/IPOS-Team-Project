package com.pharmacy.iposca.ui;
import com.pharmacy.iposca.controller.PUCommunicationController;
import com.pharmacy.iposca.model.OnlineOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.format.DateTimeFormatter;

/**
 This UI class handles the Portal Orders module
 */
public class PortalOrdersView {
    @FXML private TableView<OnlineOrder> ordersTable;
    @FXML private TableColumn<OnlineOrder, String> colId;
    @FXML private TableColumn<OnlineOrder, String> colDate;
    @FXML private TableColumn<OnlineOrder, String> colCustomer;
    @FXML private TableColumn<OnlineOrder, String> colDescription;
    @FXML private TableColumn<OnlineOrder, String> colAddress;
    @FXML private TableColumn<OnlineOrder, Double> colAmount;
    @FXML private TableColumn<OnlineOrder, String> colStatus;
    @FXML private TableColumn<OnlineOrder, String> colPayment;
    @FXML private TableColumn<OnlineOrder, Void> colAction;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label infoLabel;

    private final PUCommunicationController puLogic = PUCommunicationController.getInstance();
    private ObservableList<OnlineOrder> allOrders = FXCollections.observableArrayList();
    private FilteredList<OnlineOrder> filteredOrders;

    @FXML
    public void initialize() {
        setupColumns();
        loadOrders();
        setupFilters();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("orderId"));

        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                )
        );

        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerEmail"));

        colDescription.setCellValueFactory(new PropertyValueFactory<>("orderDescription"));

        colAddress.setCellValueFactory(new PropertyValueFactory<>("deliveryAddress"));

        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        // Action Button Column (Fulfil Order)
        colAction.setCellFactory(param -> new TableCell<OnlineOrder, Void>() {
            private final Button fulfilBtn = new Button();

            {
                fulfilBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                fulfilBtn.setOnAction(event -> {
                    OnlineOrder order = getTableView().getItems().get(getIndex());
                    handleAdvanceOrderStatus(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                OnlineOrder order = getTableView().getItems().get(getIndex());
                String nextStatus = getNextStatus(order.getStatus());

                if (nextStatus == null) {
                    setGraphic(null);
                } else {
                    fulfilBtn.setText(getActionButtonText(order.getStatus()));
                    fulfilBtn.setDisable(false);
                    setGraphic(fulfilBtn);
                }
            }
        });
    }

    private void loadOrders() {
        allOrders.clear();
        allOrders.addAll(puLogic.getPortalOrders());
        filteredOrders = new FilteredList<>(allOrders, p -> true);
        ordersTable.setItems(filteredOrders);
        infoLabel.setText("Loaded " + allOrders.size() + " orders from Portal.");
    }

    private void setupFilters() {
        // Search Filter
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredOrders.setPredicate(order -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return order.getOrderId().contains(newVal) ||
                        (order.getCustomerEmail() != null && order.getCustomerEmail().toLowerCase().contains(lower)) ||
                        (order.getOrderDescription() != null && order.getOrderDescription().toLowerCase().contains(lower));
            });
        });

        // Status Filter
        statusFilter.getSelectionModel().select("ALL");
        statusFilter.setOnAction(e -> {
            String selected = statusFilter.getValue();
            filteredOrders.setPredicate(order -> {
                if ("ALL".equals(selected)) return true;
                return selected.equals(order.getStatus());
            });
        });
    }

    @FXML
    private void handleRefresh() {
        loadOrders();
        infoLabel.setText("Orders refreshed.");
    }

    /**
     Confirms delivery action then marks the order as delivered
     */
    private void handleAdvanceOrderStatus(OnlineOrder order) {
        String currentStatus = order.getStatus();
        String nextStatus = getNextStatus(currentStatus);

        if (nextStatus == null) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Order Complete");
            info.setHeaderText(null);
            info.setContentText("This order is already marked as Delivered.");
            info.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Update Order Status");
        confirm.setHeaderText("Update Order " + order.getOrderId().substring(0, 8) + " to " + nextStatus);
        confirm.setContentText("This will update the order status from '" + currentStatus + "' to '" + nextStatus + "'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = puLogic.updateOrderStatus(order.getOrderId(), nextStatus);

                if (success) {
                    order.setStatus(nextStatus);
                    ordersTable.refresh();

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText(null);
                    successAlert.setContentText("Order status updated to '" + nextStatus + "' successfully.");
                    successAlert.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setHeaderText(null);
                    error.setContentText("Failed to update order status in database.");
                    error.showAndWait();
                }
            }
        });
    }

    private String getNextStatus(String currentStatus) {
        if (currentStatus == null) return null;

        switch (currentStatus.trim().toUpperCase()) {
            case "PENDING":
                return "Ready for Shipment";
            case "READY FOR SHIPMENT":
                return "Shipped";
            case "SHIPPED":
                return "Delivered";
            case "DELIVERED":
                return null;
            default:
                return null;
        }
    }

    private String getActionButtonText(String currentStatus) {
        String nextStatus = getNextStatus(currentStatus);
        if (nextStatus == null) {
            return "Completed";
        }

        switch (nextStatus.toUpperCase()) {
            case "READY FOR SHIPMENT":
                return "Prepare Shipment";
            case "SHIPPED":
                return "Mark Shipped";
            case "DELIVERED":
                return "Mark Delivered";
            default:
                return "Update Status";
        }
    }
}