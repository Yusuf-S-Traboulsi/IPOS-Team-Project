package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.AuthenticationService;
import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.model.Product;
import com.pharmacy.iposca.model.User;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Inventory View - Full Database Integration
 * All TableView edits save immediately to MySQL database
 */
public class InventoryView {

    @FXML private TextField searchField, vatField;
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, Integer> idCol, stockCol, thresholdCol;
    @FXML private TableColumn<Product, String> nameCol;
    @FXML private TableColumn<Product, Double> bulkCostCol, markupCol, priceCol;
    @FXML private Label informationLabel;

    private InventoryController logic = InventoryController.getInstance();

    @FXML
    public void initialize() {
        inventoryTable.setEditable(true);

        // --- VAT Listener ---
        vatField.setText(String.valueOf(logic.getVatRate()));
        vatField.textProperty().addListener((obs, old, newVal) -> {
            try {
                logic.setVatRate(Double.parseDouble(newVal));
                inventoryTable.refresh();
                informationLabel.setText("VAT rate updated to " + newVal);
                informationLabel.setStyle("-fx-text-fill: green;");
            } catch (Exception e) {
                informationLabel.setText("Invalid VAT rate");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        });

        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            String newName = e.getNewValue();
            p.setName(newName);
            logic.updateProductField(p.getId(), "name", newName);
            inventoryTable.refresh();
            informationLabel.setText("Product name updated: " + newName);
            informationLabel.setStyle("-fx-text-fill: green;");
        });

        bulkCostCol.setCellValueFactory(new PropertyValueFactory<>("bulkCost"));
        bulkCostCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        bulkCostCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            double newBulkCost = e.getNewValue();
            p.setBulkCost(newBulkCost);
            // Recalculate price based on new bulk cost
            p.setPrice(logic.calculateRetailPrice(p));
            // Save both bulk cost AND updated price to database
            logic.updateProductField(p.getId(), "bulk_cost", newBulkCost);
            logic.updateProductField(p.getId(), "price", p.getPrice());
            inventoryTable.refresh();
            informationLabel.setText("Bulk cost updated: £" + String.format("%.2f", newBulkCost));
            informationLabel.setStyle("-fx-text-fill: green;");
        });

        // Stock Column (editable - SAVES TO DATABASE)
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        stockCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            int newStock = e.getNewValue();
            p.setStock(newStock);
            logic.updateProductField(p.getId(), "stock", newStock);
            inventoryTable.refresh();
            informationLabel.setText("Stock updated: " + newStock);
            informationLabel.setStyle("-fx-text-fill: green;");
        });

        // Threshold Column (editable - SAVES TO DATABASE)
        thresholdCol.setCellValueFactory(new PropertyValueFactory<>("lowStockThreshold"));
        thresholdCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        thresholdCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            int newThreshold = e.getNewValue();
            p.setLowStockThreshold(newThreshold);
            logic.updateProductField(p.getId(), "low_stock_threshold", newThreshold);
            inventoryTable.refresh();
            informationLabel.setText("Stock threshold updated: " + newThreshold);
            informationLabel.setStyle("-fx-text-fill: green;");
        });

        // Markup Column (editable - SAVES TO DATABASE)
        markupCol.setCellValueFactory(new PropertyValueFactory<>("markupRate"));
        markupCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        markupCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            double newMarkup = e.getNewValue();
            p.setMarkupRate(newMarkup);
            p.setPrice(logic.calculateRetailPrice(p));
            logic.updateProductField(p.getId(), "markup_rate", newMarkup);
            logic.updateProductField(p.getId(), "price", p.getPrice());
            inventoryTable.refresh();
            informationLabel.setText("Markup updated: " + newMarkup);
            informationLabel.setStyle("-fx-text-fill: green;");
        });

        // Price Column (editable - SAVES TO DATABASE)
        setupPriceColumn();

        // --- Search and Styling ---
        setupSearchAndStyle();
    }

    private void setupPriceColumn() {
        DoubleStringConverter currencyConverter = new DoubleStringConverter() {
            @Override
            public String toString(Double value) {
                return (value == null) ? "£0.00" : String.format("£%.2f", value);
            }
            @Override
            public Double fromString(String value) {
                try {
                    return super.fromString(value.replace("£", "").trim());
                } catch (Exception e) { return 0.0; }
            }
        };

        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(TextFieldTableCell.forTableColumn(currencyConverter));
        priceCol.setOnEditCommit(e -> {
            Product p = e.getRowValue();
            double newPrice = e.getNewValue();
            p.setPrice(newPrice);
            logic.updateProductField(p.getId(), "price", newPrice);
            inventoryTable.refresh();
            informationLabel.setText("Price updated: £" + String.format("%.2f", newPrice));
            informationLabel.setStyle("-fx-text-fill: green;");
        });
    }

    private void setupSearchAndStyle() {
        FilteredList<Product> filteredData = new FilteredList<>(logic.getProducts(), p -> true);
        searchField.textProperty().addListener((obs, old, newVal) -> {
            filteredData.setPredicate(p -> newVal == null || newVal.isEmpty() ||
                    p.getName().toLowerCase().contains(newVal.toLowerCase()) ||
                    String.valueOf(p.getId()).equals(newVal));
        });
        inventoryTable.setItems(filteredData);

        inventoryTable.setRowFactory(tv -> new TableRow<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.getStock() <= item.getLowStockThreshold()) {
                    setStyle("-fx-background-color: #ffcccc;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    @FXML
    private void handleAddProduct() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Add New Product");
        alert.setHeaderText("Enter product details");

        DialogPane dialogPane = alert.getDialogPane();

        TextField idField = new TextField();
        idField.setPromptText("Product ID");
        TextField nameField = new TextField();
        nameField.setPromptText("Product Name");
        TextField bulkCostField = new TextField();
        bulkCostField.setPromptText("Bulk Cost");
        TextField markupField = new TextField();
        markupField.setPromptText("Markup Rate");
        TextField stockField = new TextField();
        stockField.setPromptText("Initial Stock");
        TextField thresholdField = new TextField();
        thresholdField.setPromptText("Low Stock Threshold");

        dialogPane.setContent(new VBox(10, idField, nameField, bulkCostField, markupField, stockField, thresholdField));
        alert.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                double bulkCost = Double.parseDouble(bulkCostField.getText());
                double markup = Double.parseDouble(markupField.getText());
                int stock = Integer.parseInt(stockField.getText());
                int threshold = Integer.parseInt(thresholdField.getText());

                boolean success = logic.addProduct(id, name, bulkCost, markup, stock, threshold);

                if (success) {
                    inventoryTable.refresh();
                    informationLabel.setText("Product added: " + name);
                    informationLabel.setStyle("-fx-text-fill: green;");
                } else {
                    informationLabel.setText("Failed to add product (ID may already exist)");
                    informationLabel.setStyle("-fx-text-fill: red;");
                }

            } catch (NumberFormatException e) {
                informationLabel.setText("Invalid number format");
                informationLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    private void handleDeleteProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Product");
            alert.setHeaderText("Are you sure you want to delete: " + selected.getName() + "?");

            if (alert.showAndWait().get() == ButtonType.OK) {
                boolean success = logic.deleteProduct(selected);

                if (success) {
                    inventoryTable.refresh();
                    informationLabel.setText("Product deleted: " + selected.getName());
                    informationLabel.setStyle("-fx-text-fill: green;");
                } else {
                    informationLabel.setText("Failed to delete product");
                    informationLabel.setStyle("-fx-text-fill: red;");
                }
            }
        } else {
            informationLabel.setText("Please select a product first");
            informationLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void generateLowStockReport() {
        List<Product> items = logic.getLowStockItems();
        String generatedBy = "Unknown User";
        String userRole = "";

        try {
            AuthenticationService authService = AuthenticationService.getCurrentInstance();
            if (authService != null && authService.getCurrentUser() != null) {
                User currentUser = authService.getCurrentUser();
                generatedBy = currentUser.getUsername();
                userRole = " (" + currentUser.getRole() + ")";
            }
        } catch (Exception e) { /* Fallback */ }

        File reportFile = new File("LowStockReport_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }\n");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("h3 { color: #34495e; margin-top: 30px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("th { background: #3498db; color: white; font-weight: 600; }\n");
        html.append("tr:nth-child(even) { background: #f8f9fa; }\n");
        html.append("tr:hover { background: #e8f4f8; }\n");
        html.append(".summary-box { background: #ecf0f1; padding: 20px; margin: 20px 0; border-left: 4px solid #3498db; border-radius: 5px; }\n");
        html.append("hr { border: none; border-top: 2px solid #bdc3c7; margin: 30px 0; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<hr>\n");
        html.append("<h1>Low Stock Report</h1>\n");
        html.append("<p>Generated: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .append(" | By: ").append(generatedBy).append(userRole).append("</p>\n");

        long criticalCount = items.stream().filter(p -> p.getStock() <= p.getLowStockThreshold() * 0.5).count();
        int totalUnitsToOrder = items.stream().mapToInt(p -> logic.calculateRecommendedOrder(p)).sum();

        html.append("<div class='summary-box'>\n");
        html.append("<h3>Summary</h3>\n");
        html.append("<table>\n");
        html.append("<tr><td>Low Stock Items:</td><td>").append(items.size()).append("</td></tr>\n");
        html.append("<tr><td>Critical Stock Items (≤50% threshold):</td><td>").append(criticalCount).append("</td></tr>\n");
        html.append("<tr><td><strong>Total Units to Order:</strong></td><td><strong>").append(totalUnitsToOrder).append("</strong></td></tr>\n");
        html.append("</table>\n");
        html.append("</div>\n");

        html.append("<h3>Stock Details</h3>\n");
        html.append("<table>\n");
        html.append("<tr><th>Item ID</th><th>Description</th><th>Current Stock</th><th>Min Threshold</th><th>Recommended Order</th></tr>\n");

        for (Product p : items) {
            int recommendedOrder = logic.calculateRecommendedOrder(p);
            String status = p.getStock() <= p.getLowStockThreshold() * 0.5 ? "CRITICAL" : "LOW";

            html.append("<tr>\n");
            html.append("<td>").append(String.format("%07d", p.getId())).append("</td>\n");
            html.append("<td>").append(p.getName().trim()).append("</td>\n");
            html.append("<td>").append(p.getStock()).append("</td>\n");
            html.append("<td>").append(p.getLowStockThreshold()).append("</td>\n");
            html.append("<td>").append(recommendedOrder).append(" units (").append(status).append(")</td>\n");
            html.append("</tr>\n");
        }
        html.append("</table>\n");

        html.append("<hr>\n");
        html.append("<p>Generated: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))).append("</p>\n");
        html.append("<p>By: ").append(generatedBy).append(" • InfoPharma Ltd.</p>\n");
        html.append("<p>Confidential - Internal Use Only</p>\n");
        html.append("</body>\n</html>\n");

        try (PrintWriter out = new PrintWriter(reportFile, StandardCharsets.UTF_8)) {
            out.println(html.toString());
            System.out.println("✅ Report generated: " + reportFile.getAbsolutePath());

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(reportFile.toURI());
            }

            if (informationLabel != null) {
                informationLabel.setText("Low stock report generated: " + items.size() + " items");
                informationLabel.setStyle("-fx-text-fill: green;");
            }

        } catch (Exception e) {
            if (informationLabel != null) {
                informationLabel.setText("Error generating report: " + e.getMessage());
                informationLabel.setStyle("-fx-text-fill: red;");
            }
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        logic.refreshProducts();
        inventoryTable.refresh();
        informationLabel.setText("Inventory refreshed from database");
        informationLabel.setStyle("-fx-text-fill: green;");
    }
}