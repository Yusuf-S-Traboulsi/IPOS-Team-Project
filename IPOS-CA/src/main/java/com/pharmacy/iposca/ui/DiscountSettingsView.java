package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.CustomerController;
import com.pharmacy.iposca.model.Customer;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * This UI class handles the Discount Settings module
 */
public class DiscountSettingsView {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<String> planTypeCombo;
    @FXML private TextField discountRateField;
    @FXML private Label infoLabel;

    @FXML private Label summaryPlanLabel;
    @FXML private Label summaryCustomerLabel;
    @FXML private Label summaryRateLabel;
    @FXML private Label summaryMonthlyLabel;
    @FXML private Label summaryEffectiveLabel;

    private final CustomerController customerController = CustomerController.getInstance();

    @FXML
    public void initialize() {
        customerCombo.getItems().setAll(customerController.getCustomerData());

        customerCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getTitle() + " " + customer.getName() +
                            " (ID: " + customer.getId() + ")");
                }
            }
        });

        customerCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Customer customer, boolean empty) {
                super.updateItem(customer, empty);
                if (empty || customer == null) {
                    setText(null);
                } else {
                    setText(customer.getTitle() + " " + customer.getName() +
                            " (ID: " + customer.getId() + ")");
                }
            }
        });

        planTypeCombo.getItems().addAll("NONE", "FIXED", "FLEXIBLE");
        planTypeCombo.setValue("NONE");

        customerCombo.setOnAction(e -> updateCustomerInfo()); //Update info when customer changes


        planTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean disableRate = newVal == null || newVal.equalsIgnoreCase("NONE");
            discountRateField.setDisable(disableRate);

            if (disableRate) {
                discountRateField.setText("0");
            }
        });

        infoLabel.setText("");
    }

    private void updateCustomerInfo() {
        Customer selected = customerCombo.getValue();

        //Updating the UI for selected customer discount details
        if (selected != null) {
            String info = String.format(
                    "Current Plan: %s | Discount Rate: %.1f%% | Monthly Purchases: £%.2f | Effective Discount: %.1f%%",
                    selected.getDiscountPlanType(),
                    selected.getDiscountRate() * 100,
                    selected.getMonthlyPurchaseTotal(),
                    selected.calculateEffectiveDiscountRate() * 100
            );
            infoLabel.setText(info);
            infoLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: bold;");
            discountRateField.setText(String.valueOf(selected.getDiscountRate() * 100));
            planTypeCombo.setValue(selected.getDiscountPlanType());
            summaryPlanLabel.setText(selected.getDiscountPlanType());
            summaryRateLabel.setText(String.format("%.1f%%", selected.getDiscountRate() * 100));
            summaryMonthlyLabel.setText(String.format("£%.2f", selected.getMonthlyPurchaseTotal()));
            summaryEffectiveLabel.setText(String.format("%.1f%%", selected.calculateEffectiveDiscountRate() * 100));
            summaryCustomerLabel.setText(selected.getTitle() + " " + selected.getName() + "  ID: " + selected.getId());
        } else {
            infoLabel.setText("Select a customer to view and manage discount settings.");
            infoLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px; -fx-font-weight: bold;");

            discountRateField.setText("0");
            planTypeCombo.setValue("NONE");
            discountRateField.setDisable(true);

            summaryCustomerLabel.setText("No customer selected");
            summaryPlanLabel.setText("None selected");
            summaryRateLabel.setText("0.0%");
            summaryMonthlyLabel.setText("£0.00");
            summaryEffectiveLabel.setText("0.0%");
        }
    }

    @FXML
    private void handleApplyDiscount() {
        Customer selected = customerCombo.getValue();
        if (selected == null) {
            showError("Please select a customer.");
            return;
        }

        String planType = planTypeCombo.getValue();
        double rate = 0.0;

        try {
            if (!"NONE".equalsIgnoreCase(planType)) {
                rate = Double.parseDouble(discountRateField.getText()) / 100.0;
            }
        } catch(NumberFormatException e){
            showError("Invalid discount rate format.");
            return;
        }
        if (rate < 0.0 || rate > 1.0) {
            showError("Discount rate must be between 0% and 100%");
            return;
        }

        boolean success = customerController.setDiscountPlan(selected, planType, rate);

        if (success) {
            showSuccess("Discount plan applied successfully.");
            updateCustomerInfo();
        } else {
            showError("Failed to apply discount plan.");
        }
    }

    @FXML
    private void handleResetMonthlyTotals() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Monthly Totals");
        alert.setHeaderText("Reset all customers' monthly purchase totals?");
        alert.setContentText("This should be done at the start of each calendar month.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            customerController.resetAllMonthlyPurchaseTotals();
            showSuccess("All monthly totals reset for the new month.");
            updateCustomerInfo();
        }
    }

    private void showSuccess(String message) {
        infoLabel.setText(message);
        infoLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void showError(String message) {
        infoLabel.setText(message);
        infoLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}