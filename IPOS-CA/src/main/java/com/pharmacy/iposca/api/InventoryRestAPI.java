package com.pharmacy.iposca.api;

import com.google.gson.*;
import com.pharmacy.iposca.db.DatabaseConnector;
import spark.*;

import java.sql.*;
import java.time.LocalDate;

/**
 * IPOS-CA Inventory REST API
 * Runs on port 4567
 * Uses ipos_ca database
 */
public class InventoryRestAPI {

    private static final String API_KEY =
            System.getenv("IPOS_CA_API_KEY") != null
                    ? System.getenv("IPOS_CA_API_KEY")
                    : "ipos-ca-secret-key-2026";

    private static final Gson gson = new Gson();

    public static void start(int port) {
        // ⚠️ MUST BE FIRST - Set port BEFORE any routes
        Spark.port(port);

        // Handle CORS preflight (OPTIONS requests)
        Spark.options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
            return "";
        });

        // API Key Authentication (skip for health check and OPTIONS)
        Spark.before((request, response) -> {
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

        // CORS Headers on ALL responses (including errors)
        Spark.after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        });

        // Health Check
        Spark.get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\": \"ok\", \"system\": \"IPOS-CA\", \"timestamp\": \"" + LocalDate.now() + "\"}";
        });

        // Get Products
        Spark.get("/api/products", (req, res) -> {
            res.type("application/json");
            return getProducts();
        });

        // Get Single Product
        Spark.get("/api/products/:id", (req, res) -> {
            res.type("application/json");
            return getProductById(req.params(":id"));
        });

        // Update Stock (Direct)
        Spark.put("/api/products/:id/stock", (req, res) -> {
            res.type("application/json");
            return updateStock(req.params(":id"), req.body());
        });

        // ✅ NEW: Decrement Stock (for IPOS-PU team)
        Spark.post("/api/inventory/decrement", (req, res) -> {
            res.type("application/json");
            return decrementStock(req.body());
        });

        // ✅ NEW: Get Stock Level
        Spark.get("/api/inventory/stock/:productId", (req, res) -> {
            res.type("application/json");
            return getStockLevel(req.params(":productId"));
        });

        System.out.println("🚀 IPOS-CA Inventory REST API Started on port " + port);
        System.out.println("   Base URL: http://localhost:" + port + "/api");
        System.out.println("   Auth: Enabled (API Key required)");
    }

    private static String getProducts() {
        return executeQuery("SELECT * FROM products ORDER BY name");
    }

    private static String getProductById(String productId) {
        return executeQuery("SELECT * FROM products WHERE id = ?", productId);
    }

    private static String updateStock(String productId, String jsonBody) {
        try {
            JsonObject data = JsonParser.parseString(jsonBody).getAsJsonObject();
            int newStock = data.get("stock").getAsInt();
            String reason = data.has("reason") ? data.get("reason").getAsString() : "Manual update";

            String sql = "UPDATE products SET stock = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            int rows = executeUpdate(sql, newStock, productId);

            // Log stock change
            if (rows > 0) {
                logStockChange(Integer.parseInt(productId), newStock, reason, null);
            }

            JsonObject response = new JsonObject();
            response.addProperty("success", rows > 0);
            response.addProperty("message", rows > 0 ? "Stock updated" : "Product not found");
            return gson.toJson(response);

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    // ✅ NEW METHOD: Decrement Stock for IPOS-PU
    private static String decrementStock(String jsonBody) {
        try {
            JsonObject data = JsonParser.parseString(jsonBody).getAsJsonObject();

            // Validate required fields
            if (!data.has("productId") || !data.has("quantity")) {
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Missing required fields: productId, quantity");
                return gson.toJson(error);
            }

            int productId = data.get("productId").getAsInt();  // ✅ Get as int
            int quantity = data.get("quantity").getAsInt();
            String saleId = data.has("saleId") ? data.get("saleId").getAsString() : null;
            String reason = data.has("reason") ? data.get("reason").getAsString() : "POS Sale";

            // Validate quantity
            if (quantity <= 0) {
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Quantity must be positive");
                return gson.toJson(error);
            }

            // Check current stock
            String checkSql = "SELECT stock, name FROM products WHERE id = ?";
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(checkSql)) {

                stmt.setInt(1, productId);  // ✅ Set as int
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    JsonObject error = new JsonObject();
                    error.addProperty("success", false);
                    error.addProperty("error", "Product not found");
                    error.addProperty("productId", productId);
                    return gson.toJson(error);
                }

                int currentStock = rs.getInt("stock");
                String productName = rs.getString("name");

                // Check if sufficient stock
                if (currentStock < quantity) {
                    JsonObject error = new JsonObject();
                    error.addProperty("success", false);
                    error.addProperty("error", "Insufficient stock");
                    error.addProperty("currentStock", currentStock);
                    error.addProperty("requestedQuantity", quantity);
                    error.addProperty("productId", productId);
                    error.addProperty("productName", productName);
                    return gson.toJson(error);
                }
            }

            // Decrement stock
            String updateSql = "UPDATE products SET stock = stock - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            int rows = executeUpdate(updateSql, quantity, productId);  // ✅ productId is int

            if (rows > 0) {
                // ✅ Log stock change with correct parameter types
                logStockChange(productId, -quantity, reason, saleId);

                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "Stock decremented successfully");
                response.addProperty("productId", productId);
                response.addProperty("quantityDecrement", quantity);
                response.addProperty("saleId", saleId != null ? saleId : "N/A");
                return gson.toJson(response);
            } else {
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Failed to update stock");
                return gson.toJson(error);
            }

        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", e.getMessage());
            e.printStackTrace();
            return gson.toJson(error);
        }
    }

    // ✅ NEW METHOD: Get Stock Level
    private static String getStockLevel(String productId) {
        try {
            String sql = "SELECT id, name, stock, low_stock_threshold FROM products WHERE id = ?";
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, Integer.parseInt(productId));
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("productId", rs.getInt("id"));
                    response.addProperty("productName", rs.getString("name"));
                    response.addProperty("stockLevel", rs.getInt("stock"));
                    response.addProperty("lowStockThreshold", rs.getInt("low_stock_threshold"));
                    response.addProperty("isLowStock", rs.getInt("stock") < rs.getInt("low_stock_threshold"));
                    return gson.toJson(response);
                } else {
                    JsonObject error = new JsonObject();
                    error.addProperty("success", false);
                    error.addProperty("error", "Product not found");
                    return gson.toJson(error);
                }
            }
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    // ✅ Helper method to log stock changes - CORRECT SIGNATURE
    private static void logStockChange(int productId, int changeAmount, String reason, String saleId) {
        String sql = "INSERT INTO stock_changes (product_id, change_amount, reason, sale_id, change_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);       // ✅ int
            stmt.setInt(2, changeAmount);    // ✅ int
            stmt.setString(3, reason);       // ✅ String
            stmt.setString(4, saleId);       // ✅ String (can be null)
            stmt.executeUpdate();

            System.out.println("Stock change logged: Product " + productId + ", Change: " + changeAmount + ", Reason: " + reason);

        } catch (SQLException e) {
            System.err.println("Failed to log stock change: " + e.getMessage());
        }
    }

    private static String executeQuery(String sql, Object... params) {
        JsonArray result = new JsonArray();
        try (Connection conn = DatabaseConnector.getConnection();
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
        try (Connection conn = DatabaseConnector.getConnection();
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
}