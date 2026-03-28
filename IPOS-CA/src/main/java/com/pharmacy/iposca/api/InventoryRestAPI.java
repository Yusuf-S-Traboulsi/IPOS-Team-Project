package com.pharmacy.iposca.api;

import com.google.gson.Gson;
import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.model.Product;
import spark.Spark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.halt;

public class InventoryRestAPI {

    private static final IInventoryAPI inventoryAPI = new CAController();
    private static final InventoryController inventoryController = InventoryController.getInstance();
    private static final Gson gson = new Gson();
    private static final String API_KEY = "ipos-ca-secret-key-2026";

    public static void start(int port) {
        Spark.port(port);

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, X-API-Key");
        });

        Spark.options("/*", (request, response) -> {
            response.status(200);
            return "";
        });

        Spark.before("/api/*", (request, response) -> {
            String apiKey = request.headers("X-API-Key");
            if (apiKey == null || !apiKey.equals(API_KEY)) {
                response.status(401);
                response.type("application/json");
                halt(gson.toJson(createError("Unauthorized: Invalid API Key")));
            }
        });

        // READ Endpoints
        Spark.get("/api/health", (req, res) -> {
            res.type("application/json");
            return gson.toJson(new MapResponse("status", "OK", "timestamp", System.currentTimeMillis()));
        });

        Spark.get("/api/products", (req, res) -> {
            Product[] allProducts = ((CAController) inventoryAPI).getAllProducts();
            res.type("application/json");
            return gson.toJson(new SearchResponse(allProducts));
        });

        Spark.get("/api/products/search", (req, res) -> {
            String criteria = req.queryParams("criteria");
            if (criteria == null || criteria.trim().isEmpty()) {
                res.status(400);
                return gson.toJson(createError("Missing 'criteria' parameter"));
            }
            Product[] results = inventoryAPI.searchStock(criteria);
            res.type("application/json");
            return gson.toJson(new SearchResponse(results));
        });

        Spark.get("/api/products/:id", (req, res) -> {
            try {
                int itemID = Integer.parseInt(req.params("id"));
                Product[] results = inventoryAPI.searchStock(String.valueOf(itemID));
                if (results.length == 0) {
                    res.status(404);
                    return gson.toJson(createError("Product not found"));
                }
                res.type("application/json");
                return gson.toJson(new ProductDTO(results[0]));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(createError("Invalid ID format"));
            }
        });

        Spark.get("/api/products/:id/stock", (req, res) -> {
            try {
                int itemID = Integer.parseInt(req.params("id"));
                int stock = inventoryAPI.getStockLevel(itemID);
                if (stock == -1) {
                    res.status(404);
                    return gson.toJson(createError("Product not found: " + itemID));
                }
                res.type("application/json");
                return gson.toJson(new MapResponse("productId", itemID, "stockLevel", stock));
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(createError("Invalid product ID"));
            }
        });

        Spark.get("/api/products/:id/price", (req, res) -> {
            try {
                int itemID = Integer.parseInt(req.params("id"));
                float price = inventoryAPI.getRetailPrice(itemID);
                if (price == -1.0f) {
                    res.status(404);
                    return gson.toJson(createError("Product not found: " + itemID));
                }
                res.type("application/json");
                return gson.toJson(new MapResponse("productId", itemID, "price", price));
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(createError("Invalid product ID"));
            }
        });

        // WRITE Endpoints
        Spark.post("/api/inventory/decrement", (req, res) -> {
            try {
                Map<String, Object> requestBody = gson.fromJson(req.body(), Map.class);
                if (requestBody == null) {
                    res.status(400);
                    return gson.toJson(createError("Request body is required"));
                }
                int itemID = ((Double) requestBody.get("itemID")).intValue();
                int qty = ((Double) requestBody.get("quantity")).intValue();

                if (itemID <= 0 || qty <= 0) {
                    res.status(400);
                    return gson.toJson(createError("itemID and quantity must be positive"));
                }

                int currentStock = inventoryAPI.getStockLevel(itemID);
                if (currentStock == -1) {
                    res.status(404);
                    return gson.toJson(createError("Product not found: " + itemID));
                }

                if (currentStock < qty) {
                    res.status(400);
                    return gson.toJson(createError("Insufficient stock. Available: " + currentStock));
                }

                inventoryController.decrementLocalStock(itemID, qty);

                res.type("application/json");
                return gson.toJson(new MapResponse(
                        "success", true,
                        "message", "Stock decremented successfully",
                        "productId", itemID,
                        "decrementedBy", qty,
                        "newStockLevel", currentStock - qty
                ));
            } catch (Exception e) {
                System.err.println("ERROR in /api/inventory/decrement:");
                e.printStackTrace();
                res.status(500);
                return gson.toJson(createError("Internal server error: " + e.getMessage()));
            }
        });

        System.out.println("IPOS-CA Inventory REST API Started!");
        System.out.println("Base URL: http://localhost:" + port + "/api");
        System.out.println("API Key: " + API_KEY);
    }

    private static Map<String, Object> createError(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    static class ProductDTO {
        int id;
        String name;
        double price;
        int stock;
        double bulkCost;
        double markupRate;
        int lowStockThreshold;

        ProductDTO(Product p) {
            this.id = p.getId();
            this.name = p.getName();
            this.price = p.getPrice();
            this.stock = p.getStock();
            this.bulkCost = p.getBulkCost();
            this.markupRate = p.getMarkupRate();
            this.lowStockThreshold = p.getLowStockThreshold();
        }
    }

    static class SearchResponse {
        int count;
        List<ProductDTO> products;

        SearchResponse(Product[] rawProducts) {
            this.count = rawProducts.length;
            this.products = new ArrayList<>();
            for (Product p : rawProducts) {
                if (p != null) {
                    this.products.add(new ProductDTO(p));
                }
            }
        }
    }

    static class MapResponse extends HashMap<String, Object> {
        MapResponse(Object... keyValues) {
            for (int i = 0; i < keyValues.length; i += 2) {
                put(keyValues[i].toString(), keyValues[i + 1]);
            }
        }
    }
}