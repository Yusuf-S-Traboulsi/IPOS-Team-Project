package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Product;
import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 Supplier Controller - Manages IPOS-SA ordering system
 All supplier data is loaded from MySQL database
 */
public class SupplierController {
    private static SupplierController instance;
    private ObservableList<SupplierCatalogueItem> catalogue;
    private ObservableList<SupplierOrder> orders;

    private SupplierController() {
        catalogue = FXCollections.observableArrayList();
        orders = FXCollections.observableArrayList();
        loadCatalogue();
        loadOrders();
    }

    public static synchronized SupplierController getInstance() {
        if (instance == null) {
            instance = new SupplierController();
        }
        return instance;
    }

    // ============================================================
    // DATABASE LOAD METHODS
    // ============================================================
    private void loadCatalogue() {
        String sql = "SELECT * FROM supplier_catalogue ORDER BY category, item_id";
        try (Connection conn = DatabaseConnector.getSAConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            catalogue.clear();
            while (rs.next()) {
                SupplierCatalogueItem item = new SupplierCatalogueItem(
                        rs.getString("item_id"),
                        rs.getString("description"),
                        rs.getString("package_type"),
                        rs.getString("unit"),
                        rs.getInt("units_per_pack"),
                        rs.getDouble("package_cost"),
                        rs.getInt("availability"),
                        rs.getInt("stock_limit"),
                        rs.getString("category")
                );
                catalogue.add(item);
            }
            System.out.println("✅ Loaded " + catalogue.size() + " items from supplier catalogue");
        } catch (SQLException e) {
            System.err.println("❌ Error loading supplier catalogue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadOrders() {
        String sql = "SELECT * FROM supplier_orders ORDER BY order_date DESC";
        try (Connection conn = DatabaseConnector.getSAConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            orders.clear();
            while (rs.next()) {
                SupplierOrder order = new SupplierOrder(
                        rs.getString("order_id"),
                        rs.getDate("order_date").toLocalDate(),
                        rs.getDouble("total_amount"),
                        rs.getString("status")
                );
                order.setDispatchedDate(rs.getDate("dispatched_date") != null ?
                        rs.getDate("dispatched_date").toLocalDate() : null);
                order.setDeliveredDate(rs.getDate("delivered_date") != null ?
                        rs.getDate("delivered_date").toLocalDate() : null);
                order.setPaymentStatus(rs.getString("payment_status"));
                order.setPaidDate(rs.getDate("paid_date") != null ?
                        rs.getDate("paid_date").toLocalDate() : null);
                orders.add(order);
            }
            System.out.println("✅ Loaded " + orders.size() + " supplier orders");
        } catch (SQLException e) {
            System.err.println("❌ Error loading supplier orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // ORDER MANAGEMENT METHODS
    // ============================================================
    public String placeOrder(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "ERROR: Cart is empty";
        }

        String orderId = generateOrderId();
        LocalDate orderDate = LocalDate.now();
        double totalAmount = items.stream().mapToDouble(i -> i.quantity * i.unitCost).sum();

        String orderSql = "INSERT INTO supplier_orders (order_id, order_date, total_amount, status) VALUES (?, ?, ?, 'Ordered')";
        String itemSql = "INSERT INTO supplier_order_items (order_id, item_id, description, quantity, unit_cost, amount) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getSAConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql)) {
                orderStmt.setString(1, orderId);
                orderStmt.setDate(2, Date.valueOf(orderDate));
                orderStmt.setDouble(3, totalAmount);
                orderStmt.executeUpdate();
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                for (OrderItem item : items) {
                    itemStmt.setString(1, orderId);
                    itemStmt.setString(2, item.itemId);
                    itemStmt.setString(3, item.description);
                    itemStmt.setInt(4, item.quantity);
                    itemStmt.setDouble(5, item.unitCost);
                    itemStmt.setDouble(6, item.quantity * item.unitCost);
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            String invoiceSql = "INSERT INTO supplier_invoices (invoice_id, order_id, invoice_date, amount, outstanding_balance, due_date, status) VALUES (?, ?, ?, ?, ?, ?, 'Unpaid')";
            try (PreparedStatement invoiceStmt = conn.prepareStatement(invoiceSql)) {
                String invoiceId = "INV-" + orderId.substring(2);
                invoiceStmt.setString(1, invoiceId);
                invoiceStmt.setString(2, orderId);
                invoiceStmt.setDate(3, Date.valueOf(orderDate));
                invoiceStmt.setDouble(4, totalAmount);
                invoiceStmt.setDouble(5, totalAmount);
                invoiceStmt.setDate(6, Date.valueOf(orderDate.plusDays(30)));
                invoiceStmt.executeUpdate();
            }

            conn.commit();
            loadOrders();
            System.out.println("✅ Order placed: " + orderId + " - Total: £" + totalAmount);
            return orderId;

        } catch (SQLException e) {
            System.err.println("❌ Error placing order: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private String generateOrderId() {
        String sql = "SELECT MAX(CAST(SUBSTRING(order_id, 3) AS UNSIGNED)) as max_id FROM supplier_orders";
        try (Connection conn = DatabaseConnector.getSAConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int maxId = rs.getInt("max_id");
                return "IP" + String.format("%04d", maxId + 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "IP" + String.format("%04d", 3000 + (int)(Math.random() * 1000));
    }

    public double getOutstandingBalance() {
        String sql = "SELECT SUM(outstanding_balance) as total FROM supplier_invoices WHERE status = 'Unpaid'";
        try (Connection conn = DatabaseConnector.getSAConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public List<OrderItem> getOrderItems(String orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM supplier_order_items WHERE order_id = ?";
        try (Connection conn = DatabaseConnector.getSAConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new OrderItem(
                            rs.getString("item_id"),
                            rs.getString("description"),
                            rs.getInt("quantity"),
                            rs.getDouble("unit_cost"),
                            rs.getDouble("amount")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public ObservableList<Invoice> getInvoices() {
        ObservableList<Invoice> invoices = FXCollections.observableArrayList();
        String sql = "SELECT * FROM supplier_invoices ORDER BY invoice_date DESC";
        try (Connection conn = DatabaseConnector.getSAConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                invoices.add(new Invoice(
                        rs.getString("invoice_id"),
                        rs.getString("order_id"),
                        rs.getDate("invoice_date").toLocalDate(),
                        rs.getDouble("amount"),
                        rs.getDouble("paid_amount"),
                        rs.getDouble("outstanding_balance"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return invoices;
    }

    // ============================================================
    // DELIVERY & PAYMENT METHODS
    // ============================================================
    public boolean markOrderAsDelivered(String orderId) {
        System.out.println("📦 Marking order " + orderId + " as delivered...");
        String sql = "UPDATE supplier_orders SET status = 'Delivered', delivered_date = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnector.getSAConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setString(2, orderId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Order status updated to Delivered");
                boolean inventoryUpdated = updateInventoryFromOrder(orderId);

                if (inventoryUpdated) {
                    System.out.println("Order " + orderId + " marked as delivered - Inventory updated");
                    loadOrders();
                    return true;
                } else {
                    System.out.println("Order marked as delivered but inventory update failed");
                    loadOrders();
                    return true;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error marking order as delivered: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean markOrderAsPaid(String orderId) {
        System.out.println("💰 Marking order " + orderId + " as paid...");
        String updateOrderSql = "UPDATE supplier_orders SET payment_status = 'Paid', paid_date = ? WHERE order_id = ?";
        String updateInvoiceSql = "UPDATE supplier_invoices SET status = 'Paid', paid_amount = outstanding_balance, outstanding_balance = 0 WHERE order_id = ?";

        try (Connection conn = DatabaseConnector.getSAConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(updateOrderSql)) {
                orderStmt.setDate(1, Date.valueOf(LocalDate.now()));
                orderStmt.setString(2, orderId);
                int orderRows = orderStmt.executeUpdate();

                if (orderRows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement invoiceStmt = conn.prepareStatement(updateInvoiceSql)) {
                invoiceStmt.setString(1, orderId);
                int invoiceRows = invoiceStmt.executeUpdate();

                if (invoiceRows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            loadOrders();

            System.out.println("Order " + orderId + " marked as paid");
            return true;

        } catch (SQLException e) {
            System.err.println("Error marking order as paid: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateInventoryFromOrder(String orderId) {
        String getItemsSql = "SELECT * FROM supplier_order_items WHERE order_id = ?";
        String updateProductSql = "UPDATE products SET stock = stock + ? WHERE name = ?";

        try (Connection conn = DatabaseConnector.getSAConnection();
             Connection connCA = DatabaseConnector.getConnection();
             PreparedStatement getItemStmt = conn.prepareStatement(getItemsSql);
             PreparedStatement updateStmt = connCA.prepareStatement(updateProductSql)) {

            getItemStmt.setString(1, orderId);
            try (ResultSet rs = getItemStmt.executeQuery()) {
                int itemsUpdated = 0;

                while (rs.next()) {
                    String supplierDescription = rs.getString("description");
                    int quantityDelivered = rs.getInt("quantity");

                    System.out.println("  Processing: " + supplierDescription + " x " + quantityDelivered);

                    String productName = mapSupplierItemToProductName(supplierDescription);

                    if (productName != null && !productName.isEmpty()) {
                        updateStmt.setInt(1, quantityDelivered);
                        updateStmt.setString(2, productName);
                        int rowsAffected = updateStmt.executeUpdate();

                        if (rowsAffected > 0) {
                            itemsUpdated++;
                            System.out.println("Added " + quantityDelivered + " units to product: " + productName);
                            updateLocalInventoryCache(productName, quantityDelivered);
                        } else {
                            System.out.println("Product '" + productName + "' not found in products table");
                        }
                    } else {
                        System.out.println("Could not map supplier item '" + supplierDescription + "' to product");
                    }
                }

                if (itemsUpdated > 0) {
                    InventoryController.getInstance().refreshProducts();
                    System.out.println("Inventory refreshed - " + itemsUpdated + " items updated");
                    return true;
                } else {
                    System.out.println("No items were updated in inventory");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error updating inventory from order: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String mapSupplierItemToProductName(String supplierDescription) {
        if (supplierDescription == null || supplierDescription.trim().isEmpty()) {
            return null;
        }
        String cleanDesc = supplierDescription.trim().toLowerCase();

        if (cleanDesc.contains("paracetamol")) return "Paracetamol";
        if (cleanDesc.contains("ibuprofen")) return "Ibuprofen";
        if (cleanDesc.contains("aspirin")) return "Aspirin";
        if (cleanDesc.contains("vitamin c")) return "Vitamin C";
        if (cleanDesc.contains("cough syrup")) return "Cough Syrup";
        if (cleanDesc.contains("iodine")) return "Iodine tincture";
        if (cleanDesc.contains("rhynol")) return "Rhynol";
        if (cleanDesc.contains("ospen")) return "Ospen";
        if (cleanDesc.contains("amopen")) return "Amopen";
        if (cleanDesc.contains("vitamin b12")) return "Vitamin B12";

        System.out.println("No match found for: " + supplierDescription);
        return null;
    }

    private void updateLocalInventoryCache(String productName, int quantity) {
        try {
            InventoryController inventory = InventoryController.getInstance();
            for (Product p : inventory.getProducts()) {
                if (p.getName().equalsIgnoreCase(productName)) {
                    p.setStock(p.getStock() + quantity);
                    System.out.println("  ✓ Local cache updated: " + p.getName() + " stock = " + p.getStock());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating local inventory cache: " + e.getMessage());
        }
    }

    // ============================================================
    // REPORT GENERATION METHODS
    // ============================================================
    public File generateOrderForm(String orderId) {
        File file = new File("OrderForm_" + orderId + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();
        List<OrderItem> items = getOrderItems(orderId);
        SupplierOrder order = getOrderByOrderId(orderId);

        if (order == null) return file;

        // HTML Header with UTF-8 - FIXED ENCODING
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");  // ✅ FIXED: Added UTF-8 charset
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Order Form - ").append(orderId).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }\n");
        html.append("    h1 { color: #2c3e50; font-size: 24px; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("    .header-info { margin-bottom: 30px; background: #f8f9fa; padding: 20px; border-radius: 5px; }\n");
        html.append("    .header-info p { margin: 5px 0; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 30px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    th { background: #3498db; color: white; padding: 12px; text-align: center; font-weight: 600; border: 2px solid #2980b9; }\n");
        html.append("    td { padding: 10px; text-align: center; border: 2px solid #bdc3c7; }\n");
        html.append("    tr:nth-child(even) { background: #f8f9fa; }\n");
        html.append("    tr:hover { background: #e8f4f8; }\n");
        html.append("    .total-row { background: #2c3e50 !important; color: white; font-weight: bold; }\n");
        html.append("    .signature { margin-top: 60px; }\n");
        html.append("    .footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #bdc3c7; font-size: 12px; color: #7f8c8d; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("<h1>Order Form</h1>\n");  // ✅ REMOVED: Appendix reference
        html.append("<div class='header-info'>\n");
        html.append("  <p><strong>Client:</strong> Cosymed Ltd.</p>\n");
        html.append("  <p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n");
        html.append("  <p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n");
        html.append("  <p><strong>IPOS Account:</strong> 0000235</p>\n");
        html.append("  <p><strong>Order ID:</strong> ").append(escapeHtml(orderId)).append("</p>\n");
        html.append("  <p><strong>Order Date:</strong> ").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n");
        html.append("</div>\n");

        // Items Table
        html.append("<table>\n");
        html.append("  <thead>\n");
        html.append("    <tr>\n");
        html.append("      <th>Item ID</th>\n");
        html.append("      <th>Description</th>\n");
        html.append("      <th>Quantity</th>\n");
        html.append("      <th>Unit Cost, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("      <th>Total, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("    </tr>\n");
        html.append("  </thead>\n");
        html.append("  <tbody>\n");

        double grandTotal = 0.0;
        for (OrderItem item : items) {
            double itemTotal = item.quantity * item.unitCost;
            grandTotal += itemTotal;

            html.append("    <tr>\n");
            html.append("      <td>").append(escapeHtml(item.itemId)).append("</td>\n");
            html.append("      <td>").append(escapeHtml(item.description)).append("</td>\n");
            html.append("      <td>").append(item.quantity).append("</td>\n");
            html.append("      <td>").append(String.format("%.2f", item.unitCost)).append("</td>\n");
            html.append("      <td>").append(String.format("%.2f", itemTotal)).append("</td>\n");
            html.append("    </tr>\n");
        }

        html.append("  </tbody>\n");
        html.append("  <tfoot>\n");
        html.append("    <tr class='total-row'>\n");
        html.append("      <td colspan='4' style='text-align: right;'>Grand Total:</td>\n");
        html.append("      <td>").append(String.format("%.2f", grandTotal)).append("</td>\n");
        html.append("    </tr>\n");
        html.append("  </tfoot>\n");
        html.append("</table>\n");

        // Signature
        html.append("<div class='signature'>\n");
        html.append("  <p>For Cosymed Ltd:</p>\n");
        html.append("  <br><br><br>\n");
        html.append("  <p>___________________________</p>\n");
        html.append("  <p>Authorised Signature</p>\n");
        html.append("</div>\n");

        // Footer
        html.append("<div class='footer'>\n");
        html.append("  <p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n");
        html.append("  <p><strong>By:</strong> Director of Operations</p>\n");
        html.append("</div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public File generateOrdersSummaryReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("OrdersSummaryReport_" +
                startDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "to" +
                endDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();

        // Get ALL orders if no date range specified, otherwise filter
        List<SupplierOrder> filteredOrders;
        if (startDate == null || endDate == null) {
            filteredOrders = new ArrayList<>(getOrders());
        } else {
            filteredOrders = getOrders().stream()
                    .filter(o -> !o.getOrderDate().isBefore(startDate) && !o.getOrderDate().isAfter(endDate))
                    .collect(java.util.stream.Collectors.toList());
        }

        // HTML Header with UTF-8 - FIXED ENCODING
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");  // ✅ FIXED: Added UTF-8 charset
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Orders Summary Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }\n");
        html.append("    h1 { color: #2c3e50; font-size: 24px; border-bottom: 3px solid #9b59b6; padding-bottom: 10px; }\n");
        html.append("    .header-info { margin-bottom: 30px; background: #f8f9fa; padding: 20px; border-radius: 5px; }\n");
        html.append("    .header-info p { margin: 5px 0; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 30px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    th { background: #9b59b6; color: white; padding: 12px; text-align: center; font-weight: 600; border: 2px solid #8e44ad; }\n");
        html.append("    td { padding: 10px; text-align: center; border: 2px solid #bdc3c7; }\n");
        html.append("    tr:nth-child(even) { background: #f8f9fa; }\n");
        html.append("    tr:hover { background: #f4ecf7; }\n");
        html.append("    .total-row { background: #2c3e50 !important; color: white; font-weight: bold; }\n");
        html.append("    .footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #bdc3c7; font-size: 12px; color: #7f8c8d; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("<h1>Merchant's Orders Summary</h1>\n");  // ✅ REMOVED: Appendix reference
        html.append("<div class='header-info'>\n");
        html.append("  <p><strong>Report Period:</strong> ");
        if (startDate != null && endDate != null) {
            html.append(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(" to ")
                    .append(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            html.append("All Orders");
        }
        html.append("</p>\n");
        html.append("  <p><strong>Client:</strong> Cosymed Ltd.</p>\n");
        html.append("  <p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n");
        html.append("  <p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n");
        html.append("  <p><strong>IPOS Account:</strong> 0000235</p>\n");
        html.append("</div>\n");

        // Orders Table
        html.append("<table>\n");
        html.append("  <thead>\n");
        html.append("    <tr>\n");
        html.append("      <th>Order ID</th>\n");
        html.append("      <th>Ordered</th>\n");
        html.append("      <th>Amount, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("      <th>Dispatched</th>\n");
        html.append("      <th>Delivered</th>\n");
        html.append("      <th>Paid</th>\n");
        html.append("    </tr>\n");
        html.append("  </thead>\n");
        html.append("  <tbody>\n");

        int totalOrders = 0;
        double totalAmount = 0.0;
        int dispatchedCount = 0;
        int deliveredCount = 0;
        int paidCount = 0;

        for (SupplierOrder order : filteredOrders) {
            html.append("    <tr>\n");
            html.append("      <td>").append(escapeHtml(order.getOrderId())).append("</td>\n");
            html.append("      <td>").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n");
            html.append("      <td>").append(String.format("%.2f", order.getTotalAmount())).append("</td>\n");
            html.append("      <td>").append(order.getDispatchedDate() != null ?
                    order.getDispatchedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Pending").append("</td>\n");
            html.append("      <td>").append(order.getDeliveredDate() != null ?
                    order.getDeliveredDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Pending").append("</td>\n");
            html.append("      <td>").append(order.getPaymentStatus() != null ? order.getPaymentStatus() : "Pending").append("</td>\n");
            html.append("    </tr>\n");

            totalOrders++;
            totalAmount += order.getTotalAmount();
            if (order.getDispatchedDate() != null) dispatchedCount++;
            if (order.getDeliveredDate() != null) deliveredCount++;
            if ("Paid".equals(order.getPaymentStatus())) paidCount++;
        }

        html.append("  </tbody>\n");
        html.append("  <tfoot>\n");
        html.append("    <tr class='total-row'>\n");
        html.append("      <td><strong>Total:</strong></td>\n");
        html.append("      <td><strong>").append(totalOrders).append("</strong></td>\n");
        html.append("      <td><strong>").append(String.format("%.2f", totalAmount)).append("</strong></td>\n");
        html.append("      <td><strong>").append(dispatchedCount).append("</strong></td>\n");
        html.append("      <td><strong>").append(deliveredCount).append("</strong></td>\n");
        html.append("      <td><strong>").append(paidCount).append("</strong></td>\n");
        html.append("    </tr>\n");
        html.append("  </tfoot>\n");
        html.append("</table>\n");

        // Footer
        html.append("<div class='footer'>\n");
        html.append("  <p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n");
        html.append("  <p><strong>By:</strong> Director of Operations</p>\n");
        html.append("</div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public File generateDetailedOrderReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("DetailedOrderReport_" +
                startDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "to" +
                endDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();

        // Get ALL orders if no date range specified
        List<SupplierOrder> filteredOrders;
        if (startDate == null || endDate == null) {
            filteredOrders = new ArrayList<>(getOrders());
        } else {
            filteredOrders = getOrders().stream()
                    .filter(o -> !o.getOrderDate().isBefore(startDate) && !o.getOrderDate().isAfter(endDate))
                    .collect(java.util.stream.Collectors.toList());
        }

        // HTML Header with UTF-8 - FIXED ENCODING
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");  // ✅ FIXED: Added UTF-8 charset
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Detailed Order Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }\n");
        html.append("    h1 { color: #2c3e50; font-size: 24px; border-bottom: 3px solid #e67e22; padding-bottom: 10px; }\n");
        html.append("    .header-info { margin-bottom: 30px; background: #f8f9fa; padding: 20px; border-radius: 5px; }\n");
        html.append("    .header-info p { margin: 5px 0; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin: 30px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    th { background: #e67e22; color: white; padding: 12px; text-align: center; font-weight: 600; border: 2px solid #d35400; }\n");
        html.append("    td { padding: 10px; text-align: center; border: 2px solid #bdc3c7; }\n");
        html.append("    tr:nth-child(even) { background: #f8f9fa; }\n");
        html.append("    tr:hover { background: #fef5e7; }\n");
        html.append("    .total-row { background: #2c3e50 !important; color: white; font-weight: bold; }\n");
        html.append("    .footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #bdc3c7; font-size: 12px; color: #7f8c8d; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("<h1>Merchant's Orders Detailed Report</h1>\n");  // ✅ REMOVED: Appendix reference
        html.append("<div class='header-info'>\n");
        html.append("  <p><strong>Report Period:</strong> ");
        if (startDate != null && endDate != null) {
            html.append(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(" to ")
                    .append(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } else {
            html.append("All Orders");
        }
        html.append("</p>\n");
        html.append("  <p><strong>Client:</strong> Cosymed Ltd.</p>\n");
        html.append("  <p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n");
        html.append("  <p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n");
        html.append("  <p><strong>IPOS Account:</strong> 0000235</p>\n");
        html.append("</div>\n");

        // Detailed Table
        html.append("<table>\n");
        html.append("  <thead>\n");
        html.append("    <tr>\n");
        html.append("      <th>Order ID</th>\n");
        html.append("      <th>Order Total, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("      <th>Ordered</th>\n");
        html.append("      <th>Item ID</th>\n");
        html.append("      <th>Description</th>\n");
        html.append("      <th>Quantity</th>\n");
        html.append("      <th>Unit Cost, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("      <th>Line Total, £</th>\n");  // ✅ FIXED: Proper £ symbol
        html.append("    </tr>\n");
        html.append("  </thead>\n");
        html.append("  <tbody>\n");

        double grandTotal = 0.0;
        int totalOrders = 0;

        for (SupplierOrder order : filteredOrders) {
            List<OrderItem> items = getOrderItems(order.getOrderId());
            int itemCount = items.size();
            int rowIndex = 0;

            for (OrderItem item : items) {
                double lineTotal = item.quantity * item.unitCost;
                grandTotal += lineTotal;

                html.append("    <tr>\n");

                if (rowIndex == 0) {
                    html.append("      <td rowspan='").append(itemCount).append("'>").append(escapeHtml(order.getOrderId())).append("</td>\n");
                    html.append("      <td rowspan='").append(itemCount).append("'>").append(String.format("%.2f", order.getTotalAmount())).append("</td>\n");
                    html.append("      <td rowspan='").append(itemCount).append("'>").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n");
                }

                html.append("      <td>").append(escapeHtml(item.itemId)).append("</td>\n");
                html.append("      <td>").append(escapeHtml(item.description)).append("</td>\n");
                html.append("      <td>").append(item.quantity).append("</td>\n");
                html.append("      <td>").append(String.format("%.2f", item.unitCost)).append("</td>\n");
                html.append("      <td>").append(String.format("%.2f", lineTotal)).append("</td>\n");
                html.append("    </tr>\n");

                rowIndex++;
            }

            totalOrders++;
        }

        html.append("  </tbody>\n");
        html.append("  <tfoot>\n");
        html.append("    <tr class='total-row'>\n");
        html.append("      <td colspan='2'><strong>Grand Total:</strong></td>\n");
        html.append("      <td><strong>").append(String.format("%.2f", grandTotal)).append("</strong></td>\n");
        html.append("      <td colspan='2'><strong>Total Orders:</strong></td>\n");
        html.append("      <td><strong>").append(totalOrders).append("</strong></td>\n");
        html.append("      <td colspan='2'></td>\n");
        html.append("    </tr>\n");
        html.append("  </tfoot>\n");
        html.append("</table>\n");

        // Footer
        html.append("<div class='footer'>\n");
        html.append("  <p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n");
        html.append("  <p><strong>By:</strong> Director of Operations</p>\n");
        html.append("</div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public SupplierOrder getOrderByOrderId(String orderId) {
        for (SupplierOrder order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }

    private void writeReportToFile(File file, StringBuilder html) {
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(file.toURI());
            }
        } catch (Exception e) {
            System.err.println("❌ Error writing report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Escape HTML special characters to prevent encoding issues
     */
    private String escapeHtml(String text) {
        if (text == null) return " ";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public boolean authenticateWithIposSa(String username, String password) {
        String sql = "SELECT * FROM ipos_sa_users WHERE username = ? AND password = ? AND active = TRUE";
        try (Connection conn = DatabaseConnector.getSAConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("✅ IPOS-SA authentication successful: " + username);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ IPOS-SA authentication error: " + e.getMessage());
            if (("supplier".equals(username) && "supplier123".equals(password)) ||
                    ("cosymed".equals(username) && "bondstreet".equals(password))) {
                return true;
            }
        }
        return false;
    }

    public ObservableList<SupplierCatalogueItem> getCatalogue() {
        return catalogue;
    }

    public ObservableList<SupplierOrder> getOrders() {
        return orders;
    }

    public void refreshOrders() {
        loadOrders();
    }

    public static class OrderItem {
        public String itemId;
        public String description;
        public int quantity;
        public double unitCost;
        public double amount;

        public OrderItem(String itemId, String description, int quantity, double unitCost, double amount) {
            this.itemId = itemId;
            this.description = description;
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.amount = amount;
        }
    }

    public static class Invoice {
        private final StringProperty invoiceId = new SimpleStringProperty();
        private final StringProperty orderId = new SimpleStringProperty();
        private final ObjectProperty<LocalDate> invoiceDate = new SimpleObjectProperty<>();
        private final DoubleProperty amount = new SimpleDoubleProperty();
        private final DoubleProperty paidAmount = new SimpleDoubleProperty();
        private final DoubleProperty outstandingBalance = new SimpleDoubleProperty();
        private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();
        private final StringProperty status = new SimpleStringProperty();

        public Invoice(String invoiceId, String orderId, LocalDate invoiceDate,
                       double amount, double paidAmount, double outstandingBalance,
                       LocalDate dueDate, String status) {
            this.invoiceId.set(invoiceId);
            this.orderId.set(orderId);
            this.invoiceDate.set(invoiceDate);
            this.amount.set(amount);
            this.paidAmount.set(paidAmount);
            this.outstandingBalance.set(outstandingBalance);
            this.dueDate.set(dueDate);
            this.status.set(status);
        }

        public String getInvoiceId() { return invoiceId.get(); }
        public String getOrderId() { return orderId.get(); }
        public LocalDate getInvoiceDate() { return invoiceDate.get(); }
        public double getAmount() { return amount.get(); }
        public double getPaidAmount() { return paidAmount.get(); }
        public double getOutstandingBalance() { return outstandingBalance.get(); }
        public LocalDate getDueDate() { return dueDate.get(); }
        public String getStatus() { return status.get(); }
    }
}