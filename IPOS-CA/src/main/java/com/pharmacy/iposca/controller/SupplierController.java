package com.pharmacy.iposca.controller;

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
 * ARCHITECTURE:
 * - IPOS-CA (Pharmacy): Direct database connection
 * - IPOS-SA (Supplier): REST API calls ONLY (http://localhost:4568/api)
 */
public class SupplierController {

    private static SupplierController instance;
    private ObservableList<SupplierCatalogueItem> catalogue;
    private ObservableList<SupplierOrder> orders;
    private final Gson gson = new Gson();

    // IPOS-SA API Configuration
    private static final String API_BASE_URL = "http://localhost:4568/api";
    private static final String API_KEY = "ipos-sa-secret-key-2026";

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
    // API COMMUNICATION METHODS (HTTP Calls - NO JDBC!)
    // ============================================================

    private void loadCatalogue() {
        String response = sendGetRequest("/catalogue");

        try {
            JsonArray items = JsonParser.parseString(response).getAsJsonArray();
            catalogue.clear();

            for (JsonElement item : items) {
                JsonObject obj = item.getAsJsonObject();
                catalogue.add(new SupplierCatalogueItem(
                        obj.get("item_id").getAsString(),
                        obj.get("description").getAsString(),
                        obj.get("package_type").getAsString(),
                        obj.get("unit").getAsString(),
                        obj.get("units_per_pack").getAsInt(),
                        obj.get("package_cost").getAsDouble(),
                        obj.get("availability").getAsInt(),
                        obj.get("stock_limit").getAsInt(),
                        obj.get("category").getAsString()
                ));
            }

            System.out.println("✅ Loaded " + catalogue.size() + " items from IPOS-SA API");
        } catch (Exception e) {
            System.err.println("❌ Error loading catalogue from API: " + e.getMessage());
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

            System.out.println("✅ Loaded " + orders.size() + " orders from IPOS-SA API");
        } catch (Exception e) {
            System.err.println("❌ Error loading orders from API: " + e.getMessage());
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
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();

        if (result.get("success").getAsBoolean()) {
            String orderId = result.get("orderId").getAsString();
            double totalAmount = result.get("totalAmount").getAsDouble();
            loadOrders();
            System.out.println("✅ Order placed via IPOS-SA API: " + orderId + " - Total: £" + totalAmount);
            return orderId;
        } else {
            return "ERROR: " + result.get("error").getAsString();
        }
    }

    public double getOutstandingBalance() {
        String response = sendGetRequest("/balance");
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();
        return result.get("outstandingBalance").getAsDouble();
    }

    public ObservableList<Invoice> getInvoices() {
        ObservableList<Invoice> invoices = FXCollections.observableArrayList();
        String response = sendGetRequest("/invoices");

        try {
            JsonArray invoiceList = JsonParser.parseString(response).getAsJsonArray();

            for (JsonElement invoice : invoiceList) {
                JsonObject obj = invoice.getAsJsonObject();
                invoices.add(new Invoice(
                        obj.get("invoice_id").getAsString(),
                        obj.get("order_id").getAsString(),
                        LocalDate.parse(obj.get("invoice_date").getAsString()),
                        obj.get("amount").getAsDouble(),
                        obj.get("paid_amount").getAsDouble(),
                        obj.get("outstanding_balance").getAsDouble(),
                        LocalDate.parse(obj.get("due_date").getAsString()),
                        obj.get("status").getAsString()
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading invoices from API: " + e.getMessage());
        }

        return invoices;
    }

    public boolean markOrderAsDelivered(String orderId) {
        System.out.println("📦 Marking order " + orderId + " as delivered via API...");

        String response = sendPutRequest("/orders/" + orderId + "/delivered", "{}");
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();

        if (result.get("success").getAsBoolean()) {
            updateInventoryFromOrder(orderId);
            loadOrders();
            System.out.println("✅ Order " + orderId + " marked as delivered via API");
            return true;
        }

        System.err.println("❌ Failed to mark order as delivered");
        return false;
    }

    public boolean markOrderAsPaid(String orderId) {
        System.out.println("💰 Marking order " + orderId + " as paid via API...");

        String response = sendPutRequest("/orders/" + orderId + "/paid", "{}");
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();

        if (result.get("success").getAsBoolean()) {
            loadOrders();
            System.out.println("✅ Order " + orderId + " marked as paid via API");
            return true;
        }

        System.err.println("❌ Failed to mark order as paid");
        return false;
    }

    public boolean authenticateWithIposSa(String username, String password) {
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);

        String response = sendPostRequest("/auth", credentials.toString());
        JsonObject result = JsonParser.parseString(response).getAsJsonObject();

        if (result.get("success").getAsBoolean()) {
            System.out.println("✅ IPOS-SA authentication successful: " + username);
            return true;
        }

        System.err.println("❌ IPOS-SA authentication failed");
        return false;
    }

    // ============================================================
    // HTTP CLIENT METHODS
    // ============================================================

    private String sendGetRequest(String endpoint) {
        try {
            URL url = new URL(API_BASE_URL + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key", API_KEY);
            conn.setRequestProperty("Accept", "application/json");

            return readResponse(conn);
        } catch (Exception e) {
            System.err.println("❌ API GET request failed: " + e.getMessage());
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
            System.err.println("❌ API POST request failed: " + e.getMessage());
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
            System.err.println("❌ API PUT request failed: " + e.getMessage());
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
        System.out.println("📦 Inventory update triggered for order: " + orderId);
    }

    // ============================================================
    // REPORT GENERATION (Keep your existing code)
    // ============================================================

    public File generateOrderForm(String orderId) {
        // Your existing report code here
        return new File("OrderForm_" + orderId + ".html");
    }

    public File generateOrdersSummaryReport(LocalDate startDate, LocalDate endDate) {
        // Your existing report code here
        return new File("OrdersSummaryReport.html");
    }

    public File generateDetailedOrderReport(LocalDate startDate, LocalDate endDate) {
        // Your existing report code here
        return new File("DetailedOrderReport.html");
    }

    public List<OrderItem> getOrderItems(String orderId) {
        return new ArrayList<>();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters
    public ObservableList<SupplierCatalogueItem> getCatalogue() {
        return catalogue;
    }

    public ObservableList<SupplierOrder> getOrders() {
        return orders;
    }

    public void refreshOrders() {
        loadOrders();
    }

    // Inner classes
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