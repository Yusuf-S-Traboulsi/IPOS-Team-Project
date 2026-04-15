package com.pharmacy.iposca.api;
import com.google.gson.*;
import com.pharmacy.iposca.db.DatabaseConnector;
import spark.*;
import java.sql.*;
import java.time.LocalDate;

public class SupplierRestAPI {
    private static final String API_KEY = System.getProperty("IPOS_SA_API_KEY", "ipos-sa-secret-key-2026");
    private static final Gson gson = new Gson();

    public static void start(int port) {
        Service saService = spark.Service.ignite();
        saService.port(port);

        saService.options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
            return " ";
        });

        saService.before((request, response) -> {
            if (request.pathInfo().equals("/api/health")) {
                return;
            }
            if (request.requestMethod().equalsIgnoreCase("OPTIONS")) {
                return;
            }
            String apiKey = request.headers("X-API-Key");
            if (apiKey == null || !apiKey.equals(API_KEY)) {
                response.header("Access-Control-Allow-Origin", "*");
                Spark.halt(401, "{\"success\": false, \"error\": \"Unauthorized - Invalid API Key\"}");
            }
        });

        saService.after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        });

        saService.get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\": \"ok\", \"system\": \"IPOS-SA\", \"timestamp\": \"" + LocalDate.now() + "\"}";
        });

        saService.get("/api/catalogue", (req, res) -> {
            res.type("application/json");
            return getCatalogue();
        });

        saService.get("/api/orders", (req, res) -> {
            res.type("application/json");
            return getOrders();
        });

        saService.post("/api/orders", (req, res) -> {
            res.type("application/json");
            return placeOrder(req.body());
        });

        saService.put("/api/orders/:id/delivered", (req, res) -> {
            res.type("application/json");
            return markOrderAsDelivered(req.params(":id"));
        });

        saService.put("/api/orders/:id/paid", (req, res) -> {
            res.type("application/json");
            return markOrderAsPaid(req.params(":id"));
        });

        saService.get("/api/balance", (req, res) -> {
            res.type("application/json");
            return getOutstandingBalance();
        });

        saService.get("/api/invoices", (req, res) -> {
            res.type("application/json");
            return getInvoices();
        });

        saService.post("/api/auth", (req, res) -> {
            res.type("application/json");
            return authenticate(req.body());
        });

        System.out.println("IPOS-SA REST API Started on port " + port);
        System.out.println("   Base URL: http://localhost:" + port + "/api");
        System.out.println("   Auth: Enabled (API Key required)");
    }

    private static String getCatalogue() {
        return executeQuery("SELECT * FROM supplier_catalogue ORDER BY category, item_id");
    }

    private static String getOrders() {
        return executeQuery("SELECT * FROM supplier_orders ORDER BY order_date DESC");
    }

    private static String placeOrder(String jsonBody) {
        try {
            JsonObject orderData = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonArray items = orderData.getAsJsonArray("items");

            String orderId = generateOrderId();
            LocalDate orderDate = LocalDate.now();
            double totalAmount = 0;

            for (JsonElement item : items) {
                JsonObject itemObj = item.getAsJsonObject();
                totalAmount += itemObj.get("quantity").getAsInt() * itemObj.get("unitCost").getAsDouble();
            }

            try (Connection conn = DatabaseConnector.getSAConnection()) {
                conn.setAutoCommit(false);

                String orderSql = "INSERT INTO supplier_orders (order_id, order_date, total_amount, status) VALUES (?, ?, ?, 'Ordered')";
                try (PreparedStatement orderStmt = conn.prepareStatement(orderSql)) {
                    orderStmt.setString(1, orderId);
                    orderStmt.setDate(2, Date.valueOf(orderDate));
                    orderStmt.setDouble(3, totalAmount);
                    orderStmt.executeUpdate();
                }

                String itemSql = "INSERT INTO supplier_order_items (order_id, item_id, description, quantity, unit_cost, amount) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                    for (JsonElement item : items) {
                        JsonObject itemObj = item.getAsJsonObject();
                        itemStmt.setString(1, orderId);
                        itemStmt.setString(2, itemObj.get("itemId").getAsString());
                        itemStmt.setString(3, itemObj.get("description").getAsString());
                        itemStmt.setInt(4, itemObj.get("quantity").getAsInt());
                        itemStmt.setDouble(5, itemObj.get("unitCost").getAsDouble());
                        itemStmt.setDouble(6, itemObj.get("quantity").getAsInt() * itemObj.get("unitCost").getAsDouble());
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
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("orderId", orderId);
            response.addProperty("totalAmount", totalAmount);
            return gson.toJson(response);

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    private static String markOrderAsDelivered(String orderId) {
        String sql = "UPDATE supplier_orders SET status = 'Delivered', delivered_date = ? WHERE order_id = ?";
        int rows = executeUpdate(sql, Date.valueOf(LocalDate.now()), orderId);
        JsonObject response = new JsonObject();
        if (rows > 0) {
            response.addProperty("success", true);
            response.addProperty("message", "Order marked as delivered");
        } else {
            response.addProperty("success", false);
            response.addProperty("error", "Order not found");
        }
        return gson.toJson(response);
    }

    private static String markOrderAsPaid(String orderId) {
        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement invoiceStmt = null;
        JsonObject response = new JsonObject();

        try {
            conn = DatabaseConnector.getSAConnection();
            conn.setAutoCommit(false);

            String orderSql = "UPDATE supplier_orders SET payment_status = 'Paid', paid_date = ? WHERE order_id = ?";
            orderStmt = conn.prepareStatement(orderSql);
            orderStmt.setDate(1, Date.valueOf(LocalDate.now()));
            orderStmt.setString(2, orderId);
            int orderRows = orderStmt.executeUpdate();

            if (orderRows == 0) {
                conn.rollback();
                response.addProperty("success", false);
                response.addProperty("error", "Order not found");
            } else {
                String invoiceSql = "UPDATE supplier_invoices " +
                        "SET status = 'Paid', " +
                        "    paid_amount = paid_amount + outstanding_balance, " +
                        "    outstanding_balance = 0 " +
                        "WHERE order_id = ?";
                invoiceStmt = conn.prepareStatement(invoiceSql);
                invoiceStmt.setString(1, orderId);
                invoiceStmt.executeUpdate();

                conn.commit();
                response.addProperty("success", true);
                response.addProperty("message", "Order marked as paid");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            response.addProperty("success", false);
            response.addProperty("error", "Failed to mark order as paid");
        } finally {
            try { if (invoiceStmt != null) invoiceStmt.close(); } catch (SQLException ignored) {}
            try { if (orderStmt != null) orderStmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (SQLException ignored) {}
        }

        return gson.toJson(response);
    }

    private static String getOutstandingBalance() {
        String sql = "SELECT SUM(outstanding_balance) as total FROM supplier_invoices WHERE status = 'Unpaid'";
        String result = executeQuery(sql);
        JsonObject response = new JsonObject();
        try {
            JsonArray arr = JsonParser.parseString(result).getAsJsonArray();
            if (!arr.isEmpty()) {
                response.addProperty("outstandingBalance", arr.get(0).getAsJsonObject().get("total").getAsDouble());
            } else {
                response.addProperty("outstandingBalance", 0.0);
            }
        } catch (Exception e) {
            response.addProperty("outstandingBalance", 0.0);
        }
        response.addProperty("success", true);
        return gson.toJson(response);
    }

    private static String getInvoices() {
        return executeQuery("SELECT * FROM supplier_invoices ORDER BY invoice_date DESC");
    }

    private static String authenticate(String jsonBody) {
        try {
            JsonObject credentials = JsonParser.parseString(jsonBody).getAsJsonObject();
            String username = credentials.get("username").getAsString();
            String password = credentials.get("password").getAsString();

            String sql = "SELECT * FROM ipos_sa_users WHERE username = ? AND password = ? AND active = TRUE";
            String result = executeQuery(sql, username, password);

            JsonArray users = JsonParser.parseString(result).getAsJsonArray();

            JsonObject response = new JsonObject();
            if (!users.isEmpty()) {
                response.addProperty("success", true);
                response.addProperty("message", "Authentication successful");
                response.addProperty("username", username);
            } else {
                response.addProperty("success", false);
                response.addProperty("error", "Invalid credentials");
            }
            return gson.toJson(response);

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    private static String executeQuery(String sql, Object... params) {
        JsonArray result = new JsonArray();
        try (Connection conn = DatabaseConnector.getSAConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                JsonObject row = new JsonObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    if (value instanceof Date) {
                        row.addProperty(columnName, ((Date) value).toLocalDate().toString());
                    } else if (value instanceof Timestamp) {
                        row.addProperty(columnName, ((Timestamp) value).toLocalDateTime().toString());
                    } else if (value != null) {
                        row.addProperty(columnName, value.toString());
                    }
                }
                result.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gson.toJson(result);
    }

    private static int executeUpdate(String sql, Object... params) {
        try (Connection conn = DatabaseConnector.getSAConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static String generateOrderId() {
        String sql = "SELECT MAX(CAST(SUBSTRING(order_id, 3) AS UNSIGNED)) as max_id FROM supplier_orders";
        String result = executeQuery(sql);
        try {
            JsonArray arr = JsonParser.parseString(result).getAsJsonArray();
            if (!arr.isEmpty() && arr.get(0).getAsJsonObject().has("max_id")) {
                int maxId = arr.get(0).getAsJsonObject().get("max_id").getAsInt();
                return "IP" + String.format("%04d", maxId + 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "IP" + String.format("%04d", 3000 + (int)(Math.random() * 1000));
    }
}