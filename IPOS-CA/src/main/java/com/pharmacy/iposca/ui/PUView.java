package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.PUCommunicationController;
import com.pharmacy.iposca.model.OnlineSale;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.*;
import javafx.scene.text.Text;

public class PUView {

    @FXML private TableView<OnlineSale> onlineSalesTable;
    @FXML private TableColumn<OnlineSale, Integer> orderIdCol;
    @FXML private TableColumn<OnlineSale, String> customerNameCol;
    @FXML private TableColumn<OnlineSale, String> deliveryAddressCol;
    @FXML private TableColumn<OnlineSale, Double> totalAmountCol;
    @FXML private TableColumn<OnlineSale, String> statusCol;
    @FXML private TableColumn<OnlineSale, String> orderDateCol;

    @FXML private TextField customerNameField, deliveryAddressField, totalAmountField;
    @FXML private Label informationLabel;

    private final PUCommunicationController puCommController = PUCommunicationController.getInstance();

    @FXML
    public void initialize() {
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        deliveryAddressCol.setCellValueFactory(new PropertyValueFactory<>("deliveryAddress"));
        totalAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderDateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        onlineSalesTable.setItems(puCommController.getOnlineSales());
    }

    @FXML
    private void handleReceiveOnlineSale() {
        try{
            String customerName = customerNameField.getText();
            String deliveryAddress = deliveryAddressField.getText();
            double totalAmount = Double.parseDouble(totalAmountField.getText());

            boolean success = puCommController.receiveOnlineSale(customerName, deliveryAddress, totalAmount);

            if (success) {
                informationLabel.setText("Online sale received successfully!");
                informationLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                informationLabel.setText("Failed to receive online sale.");
                informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        } catch (NumberFormatException e) {
            informationLabel.setText("Invalid total amount format.");
            informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleAdvanceStatus() {
        OnlineSale selected = onlineSalesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            informationLabel.setText("Select an order first.");
            informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }
        boolean success = puCommController.advanceOrderStatus(selected);

        if (success) {
            onlineSalesTable.refresh();
            informationLabel.setText("Order: " + selected.getOrderId() + " status updated to " + selected.getStatus());
            informationLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            informationLabel.setText("Failed to update order status. Order is already delivered");
            informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleMarkAsDelivered() {
        OnlineSale selected = onlineSalesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            informationLabel.setText("Select an order first.");
            informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            return;
        }
        boolean success = puCommController.updateOrderStatus(selected, OnlineSale.DELIVERED);

        if (success) {
            onlineSalesTable.refresh();
            informationLabel.setText("Order: " + selected.getOrderId() + " delivered" + selected.getStatus());
            informationLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            informationLabel.setText("Failed to update order status.");
            informationLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    private void clearMethods() {
        customerNameField.clear();
        deliveryAddressField.clear();
        totalAmountField.clear();
    }
}
