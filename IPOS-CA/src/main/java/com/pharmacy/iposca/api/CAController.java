package com.pharmacy.iposca.api;

import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Product;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CAController implements IInventoryAPI {

    private final InventoryController inventoryController;

    public CAController() {
        this.inventoryController = InventoryController.getInstance();
    }

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


    public Product[] getAllProducts() {
        ObservableList<Product> allProducts = inventoryController.getProducts();
        if (allProducts == null) {
            return new Product[0];
        }
        return allProducts.toArray(new Product[0]);
    }

    public Product getProductById(int itemID) {
        return findProductById(itemID);
    }

    public boolean hasSufficientStock(int itemID, int quantity) {
        int currentStock = getStockLevel(itemID);
        return currentStock >= quantity;
    }

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

        String sql = "UPDATE products SET stock = stock - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, productId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {

                logStockChange(productId, -quantity, reason, saleId);

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
                logStockChange(productId, quantity, reason, null);

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
}