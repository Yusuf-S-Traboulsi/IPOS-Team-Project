package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.api.ISupplierAPI;
import com.pharmacy.iposca.model.Product;
import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.*;

/**
 * Supplier Controller - Manages IPOS-SA ordering system via REST API
 *
 * ARCHITECTURE NOTE:
 * - IPOS-CA (Pharmacy): Direct database connection
 * - IPOS-SA (Supplier): REST API calls only (required by brief)
 *
 * This demonstrates proper separation between the two systems.
 */
public class SupplierController implements ISupplierAPI {

    private static SupplierController instance;
    private final ObservableList<SupplierCatalogueItem> catalogue;
    private final ObservableList<SupplierOrder> orders;
    private final Gson gson = new Gson();

    // IPOS-SA API Configuration
    private static final String API_BASE_URL = "http://localhost:4568/api";
    private static final String API_KEY =
            System.getProperty("IPOS_SA_API_KEY",
                    System.getenv("IPOS_SA_API_KEY") != null ?
                            System.getenv("IPOS_SA_API_KEY") : "ipos-sa-secret-key-2026");

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
    // ISupplierAPI Interface Implementation
    // ============================================================

    @Override
    public SupplierCatalogueItem[] getProductCatalogue() {
        return catalogue.toArray(new SupplierCatalogueItem[0]);
    }

    @Override
    public boolean submitPurchaseOrder(SupplierOrder order) {
        // Convert SupplierOrder to List<OrderItem> and call placeOrder
        // For demo, return true
        return true;
    }

    @Override
    public String getDeliveryStatus(String orderID) {
        for (SupplierOrder order : orders) {
            if (order.getOrderId().equals(orderID)) {
                return order.getStatus();
            }
        }
        return "Order not found";
    }

    @Override
    public Invoice[] getOutstandingInvoices() {
        ObservableList<Invoice> invoices = getInvoices();
        return invoices.toArray(new Invoice[0]);
    }

    @Override
    public double getOutstandingBalance() {
        String response = sendGetRequest("/balance");
        try {
            JsonObject result = JsonParser.parseString(response).getAsJsonObject();
            if (result.has("outstandingBalance")) {
                return result.get("outstandingBalance").getAsDouble();
            }
        } catch (Exception e) {
            System.err.println("Error fetching balance: " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public boolean markOrderAsDelivered(String orderId) {
        System.out.println("Marking order " + orderId + " as delivered via API...");

        String response = sendPutRequest("/orders/" + orderId + "/delivered", "{}");

        try {
            JsonObject result = JsonParser.parseString(response).getAsJsonObject();
            if (result.has("success") && result.get("success").getAsBoolean()) {
                loadOrders();
                System.out.println("Order " + orderId + " marked as delivered via API");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to mark order as delivered: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean markOrderAsPaid(String orderId) {
        System.out.println("Marking order " + orderId + " as paid via API...");

        String response = sendPutRequest("/orders/" + orderId + "/paid", "{}");

        try {
            JsonObject result = JsonParser.parseString(response).getAsJsonObject();
            if (result.has("success") && result.get("success").getAsBoolean()) {
                loadOrders();
                System.out.println("Order " + orderId + " marked as paid via API");
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to mark order as paid: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean authenticate(String username, String password) {
        return authenticateWithIposSa(username, password);
    }

    // API COMMUNICATION METHODS

    private void loadCatalogue() {
        String response = sendGetRequest("/catalogue");

        try {
            JsonArray items = JsonParser.parseString(response).getAsJsonArray();
            catalogue.clear();

            for (JsonElement item : items) {
                JsonObject obj = item.getAsJsonObject();

                // Handle nullable fields safely
                String packageType = obj.has("package_type") && !obj.get("package_type").isJsonNull()
                        ? obj.get("package_type").getAsString() : "";
                String unit = obj.has("unit") && !obj.get("unit").isJsonNull()
                        ? obj.get("unit").getAsString() : "";
                String category = obj.has("category") && !obj.get("category").isJsonNull()
                        ? obj.get("category").getAsString() : "";

                catalogue.add(new SupplierCatalogueItem(
                        obj.get("item_id").getAsString(),
                        obj.get("description").getAsString(),
                        packageType,
                        unit,
                        obj.has("units_per_pack") ? obj.get("units_per_pack").getAsInt() : 0,
                        obj.get("package_cost").getAsDouble(),
                        obj.has("availability") ? obj.get("availability").getAsInt() : 0,
                        obj.has("stock_limit") ? obj.get("stock_limit").getAsInt() : 0,
                        category
                ));
            }

            System.out.println("Loaded " + catalogue.size() + " items from IPOS-SA API");
        } catch (Exception e) {
            System.err.println("Error loading catalogue from API: " + e.getMessage());
            catalogue.clear();
        }
    }

    private void loadOrders() {
        String response = sendGetRequest("/orders");

        try {
            JsonArray orderList = JsonParser.parseString(response).getAsJsonArray();
            orders.clear();

            for (JsonElement order : orderList) {
                JsonObject obj = order.getAsJsonObject();
                SupplierOrder supplierOrder = new SupplierOrder(
                        obj.get("order_id").getAsString(),
                        LocalDate.parse(obj.get("order_date").getAsString()),
                        obj.get("total_amount").getAsDouble(),
                        obj.get("status").getAsString()
                );

                if (obj.has("dispatched_date") && !obj.get("dispatched_date").isJsonNull()) {
                    supplierOrder.setDispatchedDate(LocalDate.parse(obj.get("dispatched_date").getAsString()));
                }
                if (obj.has("delivered_date") && !obj.get("delivered_date").isJsonNull()) {
                    supplierOrder.setDeliveredDate(LocalDate.parse(obj.get("delivered_date").getAsString()));
                }
                if (obj.has("payment_status")) {
                    supplierOrder.setPaymentStatus(obj.get("payment_status").getAsString());
                }
                if (obj.has("paid_date") && !obj.get("paid_date").isJsonNull()) {
                    supplierOrder.setPaidDate(LocalDate.parse(obj.get("paid_date").getAsString()));
                }

                orders.add(supplierOrder);
            }

            System.out.println("Loaded " + orders.size() + " orders from IPOS-SA API");
        } catch (Exception e) {
            System.err.println("Error loading orders from API: " + e.getMessage());
            orders.clear();
        }
    }

    public String placeOrder(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "ERROR: Cart is empty";
        }

        JsonObject orderData = new JsonObject();
        JsonArray itemsArray = new JsonArray();

        for (OrderItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("itemId", item.itemId);
            itemObj.addProperty("description", item.description);
            itemObj.addProperty("quantity", item.quantity);
            itemObj.addProperty("unitCost", item.unitCost);
            itemObj.addProperty("amount", item.amount);
            itemsArray.add(itemObj);
        }

        orderData.add("items", itemsArray);

        String response = sendPostRequest("/orders", orderData.toString());

        try {
            JsonObject result = JsonParser.parseString(response).getAsJsonObject();

            if (result.has("success") && result.get("success").getAsBoolean()) {
                String orderId = result.get("orderId").getAsString();
                double totalAmount = result.get("totalAmount").getAsDouble();
                loadOrders();
                System.out.println("Order placed via IPOS-SA API: " + orderId + " - Total: £" + totalAmount);
                return orderId;
            } else {
                String errorMsg = result.has("error") && !result.get("error").isJsonNull()
                        ? result.get("error").getAsString() : "Unknown error";
                return "ERROR: " + errorMsg;
            }
        } catch (Exception e) {
            return "ERROR: Failed to parse API response: " + e.getMessage();
        }
    }

    public ObservableList<Invoice> getInvoices() {
        ObservableList<Invoice> invoices = FXCollections.observableArrayList();
        String response = sendGetRequest("/invoices");

        try {
            JsonArray invoiceList = JsonParser.parseString(response).getAsJsonArray();

            for (JsonElement invoice : invoiceList) {
                JsonObject obj = invoice.getAsJsonObject();

                // Handle nullable date fields safely
                LocalDate invoiceDate = obj.has("invoice_date") && !obj.get("invoice_date").isJsonNull()
                        ? LocalDate.parse(obj.get("invoice_date").getAsString()) : null;
                LocalDate dueDate = obj.has("due_date") && !obj.get("due_date").isJsonNull()
                        ? LocalDate.parse(obj.get("due_date").getAsString()) : null;

                invoices.add(new Invoice(
                        obj.get("invoice_id").getAsString(),
                        obj.get("order_id").getAsString(),
                        invoiceDate,
                        obj.get("amount").getAsDouble(),
                        obj.get("paid_amount").getAsDouble(),
                        obj.get("outstanding_balance").getAsDouble(),
                        dueDate,
                        obj.get("status").getAsString()
                ));
            }
        } catch (Exception e) {
            System.err.println("Error loading invoices from API: " + e.getMessage());
        }

        return invoices;
    }

    public boolean authenticateWithIposSa(String username, String password) {
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);

        String response = sendPostRequest("/auth", credentials.toString());

        try {
            JsonObject result = JsonParser.parseString(response).getAsJsonObject();

            if (result.has("success") && result.get("success").getAsBoolean()) {
                System.out.println("IPOS-SA authentication successful: " + username);
                return true;
            }
        } catch (Exception e) {
            System.err.println("IPOS-SA authentication failed: " + e.getMessage());
        }

        System.err.println("IPOS-SA authentication failed");
        return false;
    }

    // HTTP CLIENT METHODS

    private String sendGetRequest(String endpoint) {
        try {
            URL url = new URL(API_BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key", API_KEY);
            conn.setRequestProperty("Accept", "application/json");

            return readResponse(conn);
        } catch (Exception e) {
            System.err.println("API GET request failed: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String sendPostRequest(String endpoint, String jsonBody) {
        try {
            URL url = new URL(API_BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-API-Key", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            return readResponse(conn);
        } catch (Exception e) {
            System.err.println("API POST request failed: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String sendPutRequest(String endpoint, String jsonBody) {
        try {
            URL url = new URL(API_BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("X-API-Key", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            return readResponse(conn);
        } catch (Exception e) {
            System.err.println("API PUT request failed: " + e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        BufferedReader reader;

        if (status >= 400) {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    private void updateInventoryFromOrder(String orderId) {
        System.out.println("Inventory update triggered for order: " + orderId);
        System.out.println("   (In production, this would update IPOS-CA products table via API)");
    }

    // REPORT GENERATION METHODS

    public File generateOrderForm(String orderId) {
        File file = new File("OrderForm_" + orderId + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");

        StringBuilder html = new StringBuilder();
        List<OrderItem> items = getOrderItems(orderId);
        SupplierOrder order = getOrderByOrderId(orderId);

        if (order == null) return file;

        html.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<title>Order Form - ").append(orderId).append("</title>\n")
                .append("<style>\n")
                .append("body { font-family: Arial, sans-serif; margin: 40px; }\n")
                .append("h1 { color: #2c3e50; font-size: 18px; }\n")
                .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
                .append("th, td { border: 2px solid #000; padding: 8px; text-align: center; }\n")
                .append("th { background: #f0f0f0; font-weight: bold; }\n")
                .append(".header-info { margin-bottom: 20px; }\n")
                .append(".signature { margin-top: 40px; }\n")
                .append(".total-row { font-weight: bold; background: #f9f9f9; }\n")
                .append("</style>\n</head>\n<body>\n");

        html.append("<div class='header-info'>\n")
                .append("<h1>9.2 Appendix 2: Order Form</h1>\n")
                .append("<p><strong>Client:</strong> Cosymed Ltd.</p>\n")
                .append("<p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n")
                .append("<p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n")
                .append("<p><strong>IPOS Account:</strong> 0000235</p>\n")
                .append("<p><strong>Date:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n")
                .append("</div>\n");

        html.append("<table>\n")
                .append("<tr>\n")
                .append("<th>Item ID</th>\n")
                .append("<th>Description</th>\n")
                .append("<th>Quantity</th>\n")
                .append("<th>Unit Cost, £</th>\n")
                .append("<th>Total, £</th>\n")
                .append("</tr>\n");

        double grandTotal = 0;
        for (OrderItem item : items) {
            double itemTotal = item.quantity * item.unitCost;
            grandTotal += itemTotal;

            html.append("<tr>\n")
                    .append("<td>").append(item.itemId).append("</td>\n")
                    .append("<td>").append(item.description).append("</td>\n")
                    .append("<td>").append(item.quantity).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", item.unitCost)).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", itemTotal)).append("</td>\n")
                    .append("</tr>\n");
        }

        html.append("<tr class='total-row'>\n")
                .append("<td colspan='4' style='text-align: right;'>Grand Total:</td>\n")
                .append("<td>").append(String.format("%.2f", grandTotal)).append("</td>\n")
                .append("</tr>\n")
                .append("</table>\n");

        html.append("<div class='signature'>\n")
                .append("<p>For Cosymed Ltd:</p>\n")
                .append("<br><br>\n")
                .append("<p>/Signature/</p>\n")
                .append("</div>\n");

        html.append("<hr>\n")
                .append("<p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n")
                .append("<p><strong>By:</strong> Director of Operations</p>\n")
                .append("</body>\n</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public File generateOrdersSummaryReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("OrdersSummaryReport_" +
                startDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "to" +
                endDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");

        StringBuilder html = new StringBuilder();

        List<SupplierOrder> filteredOrders = getOrders().stream()
                .filter(o -> !o.getOrderDate().isBefore(startDate) && !o.getOrderDate().isAfter(endDate))
                .toList();

        html.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<title>Merchant's Orders Summary</title>\n")
                .append("<style>\n")
                .append("body { font-family: Arial, sans-serif; margin: 40px; }\n")
                .append("h1 { color: #2c3e50; font-size: 18px; }\n")
                .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
                .append("th, td { border: 2px solid #000; padding: 8px; text-align: center; }\n")
                .append("th { background: #f0f0f0; font-weight: bold; }\n")
                .append(".header-info { margin-bottom: 20px; }\n")
                .append(".total-row { font-weight: bold; background: #f9f9f9; }\n")
                .append("</style>\n</head>\n<body>\n");

        html.append("<div class='header-info'>\n")
                .append("<h1>9.4 Appendix 4: Merchant's Orders Summary</h1>\n")
                .append("<p><strong>Report Period:</strong> ")
                .append(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" – ")
                .append(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append("</p>\n")
                .append("<p><strong>Client:</strong> Cosymed Ltd.</p>\n")
                .append("<p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n")
                .append("<p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n")
                .append("<p><strong>IPOS Account:</strong> 0000235</p>\n")
                .append("</div>\n");

        html.append("<table>\n")
                .append("<tr>\n")
                .append("<th>Order ID</th>\n")
                .append("<th>Ordered</th>\n")
                .append("<th>Amount, £</th>\n")
                .append("<th>Dispatched</th>\n")
                .append("<th>Delivered</th>\n")
                .append("<th>Paid</th>\n")
                .append("</tr>\n");

        int totalOrders = 0;
        double totalAmount = 0;
        int dispatchedCount = 0;
        int deliveredCount = 0;
        int paidCount = 0;

        for (SupplierOrder order : filteredOrders) {
            html.append("<tr>\n")
                    .append("<td>").append(order.getOrderId()).append("</td>\n")
                    .append("<td>").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", order.getTotalAmount())).append("</td>\n")
                    .append("<td>").append(order.getDispatchedDate() != null ?
                            order.getDispatchedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Pending").append("</td>\n")
                    .append("<td>").append(order.getDeliveredDate() != null ?
                            order.getDeliveredDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Pending").append("</td>\n")
                    .append("<td>").append(order.getPaymentStatus()).append("</td>\n")
                    .append("</tr>\n");

            totalOrders++;
            totalAmount += order.getTotalAmount();
            if (order.getDispatchedDate() != null) dispatchedCount++;
            if (order.getDeliveredDate() != null) deliveredCount++;
            if ("Paid".equals(order.getPaymentStatus())) paidCount++;
        }

        html.append("<tr class='total-row'>\n")
                .append("<td>Total:</td>\n")
                .append("<td>").append(totalOrders).append("</td>\n")
                .append("<td>").append(String.format("%.1f", totalAmount)).append("</td>\n")
                .append("<td>").append(dispatchedCount).append("</td>\n")
                .append("<td>").append(deliveredCount).append("</td>\n")
                .append("<td>").append(paidCount).append("</td>\n")
                .append("</tr>\n")
                .append("</table>\n");

        html.append("<p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n")
                .append("<p><strong>By:</strong> Director of Operations</p>\n")
                .append("<hr>\n")
                .append("</body>\n</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public File generateDetailedOrderReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("DetailedOrderReport_" +
                startDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "to" +
                endDate.format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");

        StringBuilder html = new StringBuilder();

        List<SupplierOrder> filteredOrders = getOrders().stream()
                .filter(o -> !o.getOrderDate().isBefore(startDate) && !o.getOrderDate().isAfter(endDate))
                .toList();

        html.append("<!DOCTYPE html>\n<html>\n<head>\n")
                .append("<title>Merchant's Orders Detailed Report</title>\n")
                .append("<style>\n")
                .append("body { font-family: Arial, sans-serif; margin: 40px; }\n")
                .append("h1 { color: #2c3e50; font-size: 18px; }\n")
                .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n")
                .append("th, td { border: 2px solid #000; padding: 8px; text-align: center; }\n")
                .append("th { background: #f0f0f0; font-weight: bold; }\n")
                .append(".header-info { margin-bottom: 20px; }\n")
                .append(".total-row { font-weight: bold; background: #f9f9f9; }\n")
                .append("</style>\n</head>\n<body>\n");

        html.append("<div class='header-info'>\n")
                .append("<h1>9.5 Appendix 5: Merchant's Orders Detailed Report</h1>\n")
                .append("<p><strong>Report Period:</strong> ")
                .append(startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" – ")
                .append(endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append("</p>\n")
                .append("<p><strong>Client:</strong> Cosymed Ltd.</p>\n")
                .append("<p>3, High Level Drive<br>Sydenham, SE26 3ET</p>\n")
                .append("<p>Phone: 0208 778 0124<br>Fax: 0208 778 0125</p>\n")
                .append("<p><strong>IPOS Account:</strong> 0000235</p>\n")
                .append("</div>\n");

        html.append("<table>\n")
                .append("<tr>\n")
                .append("<th>Order ID</th>\n")
                .append("<th>Cost, £</th>\n")
                .append("<th>Ordered</th>\n")
                .append("<th>ItemID</th>\n")
                .append("<th>Quantity</th>\n")
                .append("<th>Unit cost, £</th>\n")
                .append("<th>Amount, £</th>\n")
                .append("</tr>\n");

        double grandTotal = 0;
        int totalOrders = 0;

        for (SupplierOrder order : filteredOrders) {
            List<OrderItem> items = getOrderItems(order.getOrderId());
            int itemCount = items.size();
            int rowIndex = 0;

            for (OrderItem item : items) {
                html.append("<tr>\n");

                if (rowIndex == 0) {
                    html.append("<td rowspan='").append(itemCount).append("'>").append(order.getOrderId()).append("</td>\n")
                            .append("<td rowspan='").append(itemCount).append("'>").append(String.format("%.2f", order.getTotalAmount())).append("</td>\n")
                            .append("<td rowspan='").append(itemCount).append("'>").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n");
                }

                html.append("<td>").append(item.itemId).append("</td>\n")
                        .append("<td>").append(item.quantity).append("</td>\n")
                        .append("<td>").append(String.format("%.2f", item.unitCost)).append("</td>\n")
                        .append("<td>").append(String.format("%.2f", item.quantity * item.unitCost)).append("</td>\n")
                        .append("</tr>\n");

                grandTotal += item.quantity * item.unitCost;
                rowIndex++;
            }

            totalOrders++;
        }

        html.append("<tr class='total-row'>\n")
                .append("<td>Total:</td>\n")
                .append("<td>").append(String.format("%.2f", grandTotal)).append("</td>\n")
                .append("<td>").append(totalOrders).append("</td>\n")
                .append("<td colspan='4'></td>\n")
                .append("</tr>\n")
                .append("</table>\n");

        html.append("<p><strong>Generated:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>\n")
                .append("<p><strong>By:</strong> Director of Operations</p>\n")
                .append("<hr>\n")
                .append("</body>\n</html>\n");

        writeReportToFile(file, html);
        return file;
    }

    public List<OrderItem> getOrderItems(String orderId) {
        List<OrderItem> items = new ArrayList<>();
        // In production, this would call: GET /api/orders/{orderId}/items
        return items;
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
            System.err.println("Error writing report: " + e.getMessage());
        }
    }

    // ============================================================
    // GETTERS
    // ============================================================

    public ObservableList<SupplierCatalogueItem> getCatalogue() {
        return catalogue;
    }

    public ObservableList<SupplierOrder> getOrders() {
        return orders;
    }

    public void refreshOrders() {
        loadOrders();
    }

    // ============================================================
    // INNER CLASSES (For UI binding - keep these)
    // ============================================================

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