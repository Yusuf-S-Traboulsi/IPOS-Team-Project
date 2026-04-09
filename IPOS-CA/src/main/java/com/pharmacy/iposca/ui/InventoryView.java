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

        // --- Column Setup ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Name Column (editable - SAVES TO DATABASE)
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

        //Search and Styling
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
                    //error when item has stock in the inventory
                    Alert alert2 = new Alert(Alert.AlertType.ERROR);
                    alert2.setTitle("ERROR");
                    alert2.setHeaderText("Cannot delete item with existing stock");
                    alert2.showAndWait();

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
        } catch (Exception e) {
            // Fallback to default
        }

        final String finalGeneratedBy = generatedBy;
        final String finalUserRole = userRole;

        File reportFile = new File("LowStockReport_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");

        StringBuilder html = new StringBuilder();

        // ===== PROFESSIONAL HTML HEADER WITH CSS =====
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Low Stock Report - InfoPharma Ltd</title>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body {\n");
        html.append("      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
        html.append("      background: #f5f6fa;\n");
        html.append("      color: #2c3e50;\n");
        html.append("      line-height: 1.6;\n");
        html.append("      padding: 30px;\n");
        html.append("    }\n");
        html.append("    .container {\n");
        html.append("      max-width: 1000px;\n");
        html.append("      margin: 0 auto;\n");
        html.append("      background: white;\n");
        html.append("      padding: 40px;\n");
        html.append("      box-shadow: 0 0 20px rgba(0,0,0,0.1);\n");
        html.append("      border-radius: 10px;\n");
        html.append("    }\n");
        html.append("    .header {\n");
        html.append("      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n");
        html.append("      color: white;\n");
        html.append("      padding: 30px;\n");
        html.append("      border-radius: 10px 10px 0 0;\n");
        html.append("      margin: -40px -40px 30px -40px;\n");
        html.append("    }\n");
        html.append("    .header h1 {\n");
        html.append("      font-size: 28px;\n");
        html.append("      margin-bottom: 10px;\n");
        html.append("    }\n");
        html.append("    .header p {\n");
        html.append("      opacity: 0.9;\n");
        html.append("      font-size: 14px;\n");
        html.append("    }\n");
        html.append("    .info-box {\n");
        html.append("      background: #f8f9fa;\n");
        html.append("      border-left: 4px solid #667eea;\n");
        html.append("      padding: 20px;\n");
        html.append("      margin: 20px 0;\n");
        html.append("      border-radius: 5px;\n");
        html.append("    }\n");
        html.append("    .info-box p {\n");
        html.append("      margin: 5px 0;\n");
        html.append("      font-size: 14px;\n");
        html.append("    }\n");
        html.append("    .stats-grid {\n");
        html.append("      display: grid;\n");
        html.append("      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n");
        html.append("      gap: 20px;\n");
        html.append("      margin: 30px 0;\n");
        html.append("    }\n");
        html.append("    .stat-card {\n");
        html.append("      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n");
        html.append("      color: white;\n");
        html.append("      padding: 20px;\n");
        html.append("      border-radius: 10px;\n");
        html.append("      text-align: center;\n");
        html.append("    }\n");
        html.append("    .stat-card h4 {\n");
        html.append("      font-size: 14px;\n");
        html.append("      opacity: 0.9;\n");
        html.append("      margin-bottom: 10px;\n");
        html.append("    }\n");
        html.append("    .stat-card .value {\n");
        html.append("      font-size: 32px;\n");
        html.append("      font-weight: bold;\n");
        html.append("    }\n");
        html.append("    table {\n");
        html.append("      width: 100%;\n");
        html.append("      border-collapse: collapse;\n");
        html.append("      margin: 30px 0;\n");
        html.append("      box-shadow: 0 2px 10px rgba(0,0,0,0.05);\n");
        html.append("    }\n");
        html.append("    th {\n");
        html.append("      background: #667eea;\n");
        html.append("      color: white;\n");
        html.append("      padding: 15px;\n");
        html.append("      text-align: left;\n");
        html.append("      font-weight: 600;\n");
        html.append("    }\n");
        html.append("    td {\n");
        html.append("      padding: 12px 15px;\n");
        html.append("      border-bottom: 1px solid #e0e0e0;\n");
        html.append("    }\n");
        html.append("    tr:hover {\n");
        html.append("      background: #f5f6fa;\n");
        html.append("    }\n");
        html.append("    tr:nth-child(even) {\n");
        html.append("      background: #fafbfc;\n");
        html.append("    }\n");
        html.append("    .stock-warning {\n");
        html.append("      background: #ffcccc;\n");
        html.append("      color: #c0392b;\n");
        html.append("      padding: 5px 10px;\n");
        html.append("      border-radius: 5px;\n");
        html.append("      font-weight: 600;\n");
        html.append("      display: inline-block;\n");
        html.append("    }\n");
        html.append("    .stock-critical {\n");
        html.append("      background: #e74c3c;\n");
        html.append("      color: white;\n");
        html.append("      padding: 5px 10px;\n");
        html.append("      border-radius: 5px;\n");
        html.append("      font-weight: 600;\n");
        html.append("      display: inline-block;\n");
        html.append("    }\n");
        html.append("    .footer {\n");
        html.append("      background: #2c3e50;\n");
        html.append("      color: white;\n");
        html.append("      padding: 20px 30px;\n");
        html.append("      border-radius: 0 0 10px 10px;\n");
        html.append("      margin: 30px -40px -40px -40px;\n");
        html.append("      text-align: center;\n");
        html.append("      font-size: 13px;\n");
        html.append("    }\n");
        html.append("    .footer p {\n");
        html.append("      margin: 5px 0;\n");
        html.append("    }\n");
        html.append("    @media print {\n");
        html.append("      body { background: white; padding: 0; }\n");
        html.append("      .container { box-shadow: none; padding: 30px; }\n");
        html.append("    }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class='container'>\n");

        // ===== HEADER =====
        html.append("    <div class='header'>\n");
        html.append("      <h1>Low Stock Report</h1>\n");
        html.append("      <p>InfoPharma Ltd | Inventory Management System</p>\n");
        html.append("    </div>\n");

        // ===== INFO BOX =====
        html.append("    <div class='info-box'>\n");
        html.append("      <p><strong>Generated:</strong> " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + "</p>\n");
        html.append("      <p><strong>Generated By:</strong> " + finalGeneratedBy + finalUserRole + "</p>\n");
        html.append("      <p><strong>Items Requiring Attention:</strong> " + items.size() + " product(s)</p>\n");
        html.append("    </div>\n");

        // ===== STATISTICS CARDS =====
        html.append("    <div class='stats-grid'>\n");
        html.append("      <div class='stat-card'>\n");
        html.append("        <h4>Low Stock Items</h4>\n");
        html.append("        <div class='value'>" + items.size() + "</div>\n");
        html.append("      </div>\n");
        html.append("      <div class='stat-card'>\n");
        html.append("        <h4>Critical Stock</h4>\n");
        html.append("        <div class='value'>" + items.stream().filter(p -> p.getStock() <= p.getLowStockThreshold() * 0.5).count() + "</div>\n");
        html.append("      </div>\n");
        html.append("      <div class='stat-card'>\n");
        html.append("        <h4>Total Units to Order</h4>\n");
        html.append("        <div class='value'>" + items.stream().mapToInt(p -> logic.calculateRecommendedOrder(p)).sum() + "</div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // ===== TABLE =====
        html.append("    <h2 style='margin: 30px 0 20px 0; color: #2c3e50;'>Stock Details</h2>\n");
        html.append("    <table>\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>Item ID</th>\n");
        html.append("          <th>Description</th>\n");
        html.append("          <th>Current Stock</th>\n");
        html.append("          <th>Min Threshold</th>\n");
        html.append("          <th>Status</th>\n");
        html.append("          <th>Recommended Order</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        for (Product p : items) {
            int recommendedOrder = logic.calculateRecommendedOrder(p);
            String statusClass = p.getStock() <= p.getLowStockThreshold() * 0.5 ? "stock-critical" : "stock-warning";
            String statusText = p.getStock() <= p.getLowStockThreshold() * 0.5 ? "Critical" : "Low";

            html.append("        <tr>\n");
            html.append("          <td><strong>").append(String.format("%07d", p.getId())).append("</strong></td>\n");
            html.append("          <td>").append(p.getName().trim()).append("</td>\n");
            html.append("          <td>").append(p.getStock()).append("</td>\n");
            html.append("          <td>").append(p.getLowStockThreshold()).append("</td>\n");
            html.append("          <td><span class='").append(statusClass).append("'>").append(statusText).append("</span></td>\n");
            html.append("          <td><strong>").append(recommendedOrder).append(" units</strong></td>\n");
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");

        // ===== FOOTER =====
        html.append("    <div class='footer'>\n");
        html.append("      <p><strong>InfoPharma Ltd</strong> | 19 High St., Ashford, Kent</p>\n");
        html.append("      <p>Phone: 0208 778 0124 | Fax: 0208 778 0125</p>\n");
        html.append("      <p style='margin-top: 15px; opacity: 0.8;'>Confidential - Internal Use Only | Generated by IPOS-CA System</p>\n");
        html.append("    </div>\n");

        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        try (PrintWriter out = new PrintWriter(reportFile, StandardCharsets.UTF_8)) {
            out.println(html.toString());

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(reportFile.toURI());
            }

            informationLabel.setText("Low stock report generated: " + items.size() + " items");
            informationLabel.setStyle("-fx-text-fill: green;");

        } catch (Exception e) {
            informationLabel.setText("Error generating report: " + e.getMessage());
            informationLabel.setStyle("-fx-text-fill: red;");
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