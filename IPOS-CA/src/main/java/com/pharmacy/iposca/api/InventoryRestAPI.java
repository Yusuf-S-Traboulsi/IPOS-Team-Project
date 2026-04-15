package com.pharmacy.iposca.api;
import com.google.gson.*;
import com.pharmacy.iposca.db.DatabaseConnector;
import spark.Spark;
import java.sql.*;
import java.time.LocalDate;

public class InventoryRestAPI {
    private static final Gson gson = new Gson();

    public static void start(int port) {
        Spark.port(port);

        Spark.options("/*", (req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
            return " ";
        });

        Spark.after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type");
        });

        Spark.get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\": \"ok\", \"system\": \"IPOS-CA\"}";
        });

        Spark.get("/api/products", (req, res) -> {
            res.type("application/json");
            return executeQuery("SELECT id, name, price, stock, supplier_item_id FROM products ORDER BY name");
        });

        Spark.post("/api/inventory/decrement", (req, res) -> {
            res.type("application/json");
            return decrementStock(req.body());
        });

        System.out.println("IPOS-CA Inventory API Started on http://localhost:" + port);
        System.out.println("Endpoint: POST /api/inventory/decrement");
        System.out.println("Auth: DISABLED (Local Demo Mode)");
        System.out.println("Mapping: Uses supplier_item_id to find products");
    }


    private static String decrementStock(String jsonBody) {
        try {
            JsonObject data = JsonParser.parseString(jsonBody).getAsJsonObject();

            if (!data.has("productId") || !data.has("quantity")) {
                return createError("Missing required fields: productId, quantity");
            }

            String supplierItemId = String.valueOf(data.get("productId").getAsInt());
            int quantity = data.get("quantity").getAsInt();
            String saleId = data.has("saleId") ? data.get("saleId").getAsString() : "UNKNOWN";
            String reason = data.has("reason") ? data.get("reason").getAsString() : "POS Sale";

            System.out.println(" [API] Received Request: SupplierID=" + supplierItemId + ", Qty=" + quantity);

            String lookupSql = "SELECT id, name, stock FROM products WHERE supplier_item_id = ?";
            int internalId = -1;
            String productName = " ";
            int currentStock = 0;

            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(lookupSql)) {

                stmt.setString(1, supplierItemId);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    System.err.println("[API] Product with SupplierID " + supplierItemId + " NOT FOUND!");
                    return createError("Product not found (Supplier ID: " + supplierItemId + ")");
                }

                internalId = rs.getInt("id");
                productName = rs.getString("name");
                currentStock = rs.getInt("stock");

                System.out.println("[API] Mapped SupplierID " + supplierItemId + " -> Internal ID " + internalId);
            }

            if (currentStock < quantity) {
                System.err.println(" [API] Insufficient Stock: Have " + currentStock + ", Need " + quantity);
                JsonObject error = new JsonObject();
                error.addProperty("success", false);
                error.addProperty("error", "Insufficient stock");
                error.addProperty("currentStock", currentStock);
                error.addProperty("requestedQuantity", quantity);
                return gson.toJson(error);
            }

            String updateSql = "UPDATE products SET stock = stock - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection conn = DatabaseConnector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {

                stmt.setInt(1, quantity) ;
                stmt.setInt(2, internalId);

                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    logStockChange(internalId, -quantity, reason, saleId);
                    JsonObject response = new JsonObject();
                    response.addProperty("success", true);
                    response.addProperty("message", "Stock decremented successfully");
                    response.addProperty("productId", internalId);
                    response.addProperty("quantityDecrement", quantity);
                    return gson.toJson(response);
                } else {
                    return createError("Failed to update stock (0 rows affected)");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return createError(e.getMessage());
        }
    }

    private static void logStockChange(int pid, int change, String reason, String saleId) {
        String sql = "INSERT INTO stock_changes (product_id, change_amount, reason, sale_id, change_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pid);
            stmt.setInt(2,  change);
            stmt.setString(3, reason);
            stmt.setString(4, saleId);
            stmt.executeUpdate();
            System.out.println("Stock change logged: Product " + pid + ", Change: " + change);
        } catch (SQLException e) {
            System.err.println("Log failed: " + e.getMessage());
        }
    }

    private static String executeQuery(String sql) {
        JsonArray result = new JsonArray();
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                JsonObject row = new JsonObject();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.addProperty(meta.getColumnName(i), rs.getString(i));
                }
                result.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gson.toJson(result);
    }

    private static String createError(String msg) {
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("error", msg);
        return gson.toJson(err);
    }
}