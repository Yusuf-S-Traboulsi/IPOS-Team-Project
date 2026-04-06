package com.pharmacy.iposca.api;

import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Product;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IPOS-CA Inventory Controller
 *
 * This class provides access to the pharmacy inventory system for:
 * - Internal IPOS-CA modules (read/write access)
 * - External subsystems like IPOS-PU (read-only via IInventoryAPI)
 *
 * ARCHITECTURE NOTE:
 * - IInventoryAPI interface provides read-only access for external systems
 * - This class extends functionality for internal stock management
 * - Stock decrement for IPOS-PU sales should use InventoryRestAPI endpoint:
 *   POST /api/inventory/decrement
 */
public class CAController implements IInventoryAPI {

    private final InventoryController inventoryController;

    public CAController() {
        this.inventoryController = InventoryController.getInstance();
    }

    // ============================================================
    // IInventoryAPI Interface Implementation (Read-Only)
    // ============================================================

    @Override
    public Product[] searchStock(String criteria) {
        if (criteria == null || criteria.trim().isEmpty()) {
            return getAllProducts();
        }

        String searchCriteria = criteria.toLowerCase().trim();
        ObservableList<Product> allProducts = inventoryController.getProducts();

        if (allProducts == null) {
            return new Product[0];
        }

        List<Product> matchingProducts = new ArrayList<>();

        for (Product p : allProducts) {
            if (p == null) continue;

            String name = p.getName();
            boolean nameMatches = (name != null && name.toLowerCase().contains(searchCriteria));
            boolean idMatches = String.valueOf(p.getId()).contains(searchCriteria);

            if (nameMatches || idMatches) {
                matchingProducts.add(p);
            }
        }

        return matchingProducts.toArray(new Product[0]);
    }

    @Override
    public int getStockLevel(int itemID) {
        Product product = findProductById(itemID);
        return (product != null) ? product.getStock() : -1;
    }

    @Override
    public float getRetailPrice(int itemID) {
        Product product = findProductById(itemID);
        return (product != null) ? (float) product.getPrice() : -1.0f;
    }

    // ============================================================
    // Additional Public Methods for Internal Use
    // ============================================================

    /**
     * Get all products in inventory
     * @return Array of all Product objects
     */
    public Product[] getAllProducts() {
        ObservableList<Product> allProducts = inventoryController.getProducts();
        if (allProducts == null) {
            return new Product[0];
        }
        return allProducts.toArray(new Product[0]);
    }

    /**
     * Get product by ID
     * @param itemID Product ID
     * @return Product object or null if not found
     */
    public Product getProductById(int itemID) {
        return findProductById(itemID);
    }

    /**
     * Check if product has sufficient stock
     * @param itemID Product ID
     * @param quantity Required quantity
     * @return true if stock is sufficient
     */
    public boolean hasSufficientStock(int itemID, int quantity) {
        int currentStock = getStockLevel(itemID);
        return currentStock >= quantity;
    }

    /**
     * Get low stock products (below threshold)
     * @return Array of products with low stock
     */
    public Product[] getLowStockProducts() {
        ObservableList<Product> allProducts = inventoryController.getProducts();
        if (allProducts == null) {
            return new Product[0];
        }

        List<Product> lowStockProducts = new ArrayList<>();
        for (Product p : allProducts) {
            if (p != null && p.getStock() < p.getLowStockThreshold()) {
                lowStockProducts.add(p);
            }
        }

        return lowStockProducts.toArray(new Product[0]);
    }

    // ============================================================
    // Stock Management Methods (Internal Use Only)
    // ============================================================

    /**
     * Decrement stock for a product (used when sale is completed)
     *
     * SECURITY NOTE: This method should only be called internally.
     * For external systems (IPOS-PU), use the REST API endpoint:
     * POST http://localhost:4567/api/inventory/decrement
     *
     * @param productId Product ID
     * @param quantity Quantity to decrement
     * @param reason Reason for stock change (e.g., "POS Sale", "Damaged", "Expired")
     * @param saleId Optional sale transaction ID for audit trail
     * @return true if successful, false otherwise
     */
    public boolean decrementStock(int productId, int quantity, String reason, String saleId) {
        // Validate input
        if (productId <= 0 || quantity <= 0) {
            System.err.println("Invalid product ID or quantity");
            return false;
        }

        // Check current stock
        int currentStock = getStockLevel(productId);
        if (currentStock < quantity) {
            System.err.println("Insufficient stock. Current: " + currentStock + ", Requested: " + quantity);
            return false;
        }

        // Update database
        String sql = "UPDATE products SET stock = stock - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Log stock change
                logStockChange(productId, -quantity, reason, saleId);

                // Update local cache
                updateLocalCache(productId, -quantity);

                System.out.println("Stock decremented: Product " + productId + ", Quantity: " + quantity);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error decrementing stock: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Increment stock for a product (used when receiving supplier orders)
     * @param productId Product ID
     * @param quantity Quantity to increment
     * @param reason Reason for stock change (e.g., "Supplier Delivery", "Return")
     * @return true if successful
     */
    public boolean incrementStock(int productId, int quantity, String reason) {
        if (productId <= 0 || quantity <= 0) {
            System.err.println("Invalid product ID or quantity");
            return false;
        }

        String sql = "UPDATE products SET stock = stock + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Log stock change
                logStockChange(productId, quantity, reason, null);

                // Update local cache
                updateLocalCache(productId, quantity);

                System.out.println("Stock incremented: Product " + productId + ", Quantity: " + quantity);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error incrementing stock: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Set stock to specific value (used for stock takes/corrections)
     * @param productId Product ID
     * @param newStock New stock level
     * @param reason Reason for adjustment
     * @return true if successful
     */
    public boolean setStockLevel(int productId, int newStock, String reason) {
        if (productId <= 0) {
            System.err.println("Invalid product ID");
            return false;
        }

        int currentStock = getStockLevel(productId);
        int change = newStock - currentStock;

        String sql = "UPDATE products SET stock = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newStock);
            stmt.setInt(2, productId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Log stock change
                logStockChange(productId, change, reason, null);

                // Update local cache
                updateLocalCache(productId, change);

                System.out.println("Stock set: Product " + productId + ", New Level: " + newStock);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error setting stock level: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ============================================================
    // Private Helper Methods
    // ============================================================

    private Product findProductById(int itemID) {
        ObservableList<Product> products = inventoryController.getProducts();
        if (products == null) return null;

        for (Product p : products) {
            if (p != null && p.getId() == itemID) {
                return p;
            }
        }
        return null;
    }

    private void logStockChange(int productId, int changeAmount, String reason, String saleId) {
        String sql = "INSERT INTO stock_changes (product_id, change_amount, reason, sale_id, change_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setInt(2, changeAmount);
            stmt.setString(3, reason);
            stmt.setString(4, saleId);
            stmt.executeUpdate();

            System.out.println("Stock change logged: Product " + productId + ", Change: " + changeAmount);

        } catch (SQLException e) {
            System.err.println("Failed to log stock change: " + e.getMessage());
        }
    }

    private void updateLocalCache(int productId, int change) {
        try {
            ObservableList<Product> products = inventoryController.getProducts();
            if (products != null) {
                for (Product p : products) {
                    if (p != null && p.getId() == productId) {
                        p.setStock(p.getStock() + change);
                        System.out.println("Local cache updated: Product " + productId + ", New Stock: " + p.getStock());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating local cache: " + e.getMessage());
        }
    }

    // ============================================================
    // Integration Guide for IPOS-PU Team
    // ============================================================

    /**
     * INTEGRATION GUIDE FOR IPOS-PU TEAM
     *
     * To decrement stock when a sale is completed, use ONE of these methods:
     *
     * METHOD 1: REST API (Recommended for external systems)
     * -------------------------------------------------------
     * Endpoint: POST http://localhost:4567/api/inventory/decrement
     * Headers:
     *   Content-Type: application/json
     *   X-API-Key: ipos-ca-secret-key-2026
     * Body:
     *   {
     *     "productId": 1,
     *     "quantity": 2,
     *     "saleId": "SALE-2026-001234",
     *     "reason": "POS Sale"
     *   }
     *
     * METHOD 2: Direct Java Integration (If using same JVM)
     * ------------------------------------------------------
     * CAController controller = new CAController();
     * boolean success = controller.decrementStock(productId, quantity, "POS Sale", saleId);
     *
     * METHOD 3: Check Stock Before Sale
     * ----------------------------------
     * CAController controller = new CAController();
     * boolean hasStock = controller.hasSufficientStock(productId, quantity);
     * if (hasStock) {
     *     // Proceed with sale
     * } else {
     *     // Show out of stock message
     * }
     *
     * RESPONSE FORMAT (REST API):
     * Success:
     *   {
     *     "success": true,
     *     "message": "Stock decremented successfully",
     *     "productId": 1,
     *     "quantityDecrement": 2
     *   }
     *
     * Error (Insufficient Stock):
     *   {
     *     "success": false,
     *     "error": "Insufficient stock",
     *     "currentStock": 5,
     *     "requestedQuantity": 10
     *   }
     *
     * ERROR HANDLING:
     * - Always check "success" field in response
     * - Handle 401 Unauthorized (invalid API key)
     * - Handle 500 Internal Server Error (database issues)
     * - Retry failed requests with exponential backoff
     */
}