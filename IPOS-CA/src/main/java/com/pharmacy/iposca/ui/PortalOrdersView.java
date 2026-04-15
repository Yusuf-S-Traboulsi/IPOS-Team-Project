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
 * This UI class handles the Portal Orders module
 */
public class PortalOrdersView {

    @FXML private TableView<OnlineOrder> ordersTable;
    @FXML private TableColumn<OnlineOrder, Integer> colId;
    @FXML private TableColumn<OnlineOrder, String> colDate;
    @FXML private TableColumn<OnlineOrder, String> colCustomer;
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

        colCustomer.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCustomerName() != null ?
                                data.getValue().getCustomerName() : data.getValue().getCustomerEmail()
                )
        );

        colAddress.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDeliveryAddress()
                )
        );

        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        // Action Button Column (Fulfil Order)
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button fulfilBtn = new Button("Fulfil Order");

            {
                fulfilBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-cursor: hand;");
                fulfilBtn.setOnAction(event -> {
                    OnlineOrder order = getTableView().getItems().get(getIndex());
                    handleMarkDelivered(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OnlineOrder order = getTableView().getItems().get(getIndex());

                    //Showing button for 'Received', 'Dispatched', 'Shipped'. Hidden for 'Delivered'.
                    if (!"Delivered".equals(order.getStatus())) {
                        fulfilBtn.setVisible(true);
                        fulfilBtn.setDisable(false);
                    } else {
                        fulfilBtn.setVisible(false);
                        fulfilBtn.setDisable(true);
                    }
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
        //Search Filter
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredOrders.setPredicate(order -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return String.valueOf(order.getOrderId()).contains(newVal) ||
                        (order.getCustomerEmail() != null && order.getCustomerEmail().toLowerCase().contains(lower)) ||
                        (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(lower));
            });
        });

        //Status Filter
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
     * Confirms delivery action then marks the order as delivered
     */
    private void handleMarkDelivered(OnlineOrder order) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Fulfil Order");
        confirm.setHeaderText("Mark Order #" + order.getOrderId() + " as Delivered");
        confirm.setContentText("This will update the order status to 'Delivered'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (puLogic.markAsDelivered(order.getOrderId())) {
                    order.setStatus("Delivered");
                    ordersTable.refresh();

                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Success");
                    success.setContentText("Order: " + order.getOrderId() + " marked as delivered successfully.");
                    success.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Error");
                    error.setContentText("Failed to update order status in database.");
                    error.showAndWait();
                }
            }
        });
    }
}