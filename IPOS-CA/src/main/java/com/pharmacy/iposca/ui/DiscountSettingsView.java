package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.CustomerController;
import com.pharmacy.iposca.model.Customer;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXML;

/**
 * Discount Settings View - Manage Fixed and Flexible Discount Plans
 * Access: Manager and Pharmacist roles only
 */
public class DiscountSettingsView extends VBox {

    @FXML private ComboBox<Customer> customerCombo;
    @FXML private ComboBox<String> planTypeCombo;
    @FXML private TextField discountRateField;
    @FXML private Label infoLabel;

    private CustomerController customerController = CustomerController.getInstance();

    public DiscountSettingsView() {
        setSpacing(15);
        setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Discount Plan Management");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Separator separator1 = new Separator();

        // Customer Selection
        Label customerLabel = new Label("Select Customer:");
        customerLabel.setStyle("-fx-font-weight: bold;");
        customerCombo = new ComboBox<>();
        customerCombo.getItems().addAll(customerController.getCustomerData());
        customerCombo.setPromptText("Choose account holder...");
        customerCombo.setMaxWidth(400);

        // FIX: Display customer names properly instead of object references
        customerCombo.setCellFactory(lv -> new ListCell<Customer>() {
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

        customerCombo.setButtonCell(new ListCell<Customer>() {
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

        Label planLabel = new Label("Discount Plan Type:");
        planLabel.setStyle("-fx-font-weight: bold;");
        planTypeCombo = new ComboBox<>();
        planTypeCombo.getItems().addAll("NONE", "FIXED", "FLEXIBLE");
        planTypeCombo.setValue("NONE");
        planTypeCombo.setMaxWidth(400);

        // Discount Rate (for FIXED plan)
        Label rateLabel = new Label("Discount Rate (%):");
        rateLabel.setStyle("-fx-font-weight: bold;");
        discountRateField = new TextField("0");
        discountRateField.setPromptText("Enter percentage (e.g., 10 for 10%)");
        discountRateField.setMaxWidth(400);

        // Buttons
        Button applyButton = new Button("Apply Discount Plan");
        applyButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        applyButton.setPadding(new Insets(10, 20, 10, 20));
        applyButton.setOnAction(e -> handleApplyDiscount());

        Button resetButton = new Button("Reset Monthly Totals (New Month)");
        resetButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        resetButton.setPadding(new Insets(10, 20, 10, 20));
        resetButton.setOnAction(e -> handleResetMonthlyTotals());

        // Info Label
        infoLabel = new Label();
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: green; -fx-font-size: 13px;");

        // Help Text
        Label helpText = new Label(
                "FIXED Plan: Same discount rate for all purchases\n" +
                        "FLEXIBLE Plan: Discount varies by monthly spend (2%-15%)"
        );
        helpText.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        helpText.setWrapText(true);

        // Layout
        getChildren().addAll(
                titleLabel,
                separator1,
                customerLabel,
                customerCombo,
                planLabel,
                planTypeCombo,
                rateLabel,
                discountRateField,
                applyButton,
                resetButton,
                helpText,
                infoLabel
        );

        // Update info when customer selected
        customerCombo.setOnAction(e -> updateCustomerInfo());
    }

    /**
     * Update info display when customer is selected
     */
    private void updateCustomerInfo() {
        Customer selected = customerCombo.getValue();
        if (selected != null) {
            String info = String.format(
                    "Current Plan: %s\n" +
                            "Discount Rate: %.1f%%\n" +
                            "Monthly Purchases: £%.2f\n" +
                            "Effective Discount: %.1f%%",
                    selected.getDiscountPlanType(),
                    selected.getDiscountRate() * 100,
                    selected.getMonthlyPurchaseTotal(),
                    selected.calculateEffectiveDiscountRate() * 100
            );
            infoLabel.setText(info);
            discountRateField.setText(String.valueOf(selected.getDiscountRate() * 100));
            planTypeCombo.setValue(selected.getDiscountPlanType());
        }
    }

    /**
     * Apply discount plan to selected customer
     */
    @FXML
    private void handleApplyDiscount() {
        Customer selected = customerCombo.getValue();
        if (selected == null) {
            infoLabel.setText("Please select a customer");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String planType = planTypeCombo.getValue();
        double rate = 0.0;

        try {
            rate = Double.parseDouble(discountRateField.getText()) / 100.0;
        } catch (NumberFormatException e) {
            infoLabel.setText("Invalid discount rate format");
            infoLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean success = customerController.setDiscountPlan(selected, planType, rate);

        if (success) {
            infoLabel.setText("Discount plan applied successfully!");
            infoLabel.setStyle("-fx-text-fill: green;");
            updateCustomerInfo();
        } else {
            infoLabel.setText("Failed to apply discount plan");
            infoLabel.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Reset all monthly purchase totals (start of new month)
     */
    @FXML
    private void handleResetMonthlyTotals() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Monthly Totals");
        alert.setHeaderText("Reset all customers' monthly purchase totals?");
        alert.setContentText("This should be done at the start of each calendar month for flexible discount calculation.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            customerController.resetAllMonthlyPurchaseTotals();
            infoLabel.setText("All monthly totals reset for new month");
            infoLabel.setStyle("-fx-text-fill: green;");
            updateCustomerInfo();
        }
    }
}