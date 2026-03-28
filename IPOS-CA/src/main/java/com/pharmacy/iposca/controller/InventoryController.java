package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Inventory Controller
 * All product data is loaded from and saved to MySQL database
 */
public class InventoryController {

    private static InventoryController instance;
    private ObservableList<Product> masterInventory = FXCollections.observableArrayList();
    private double currentVatRate = 0.20;

    private InventoryController() {
        loadProductsFromDatabase();
    }

    public static synchronized InventoryController getInstance() {
        if (instance == null) {
            instance = new InventoryController();
        }
        return instance;
    }

    /**
     * Load all products from MySQL database on startup
     */
    private void loadProductsFromDatabase() {
        // ✅ Include supplier_item_id in query
        String sql = "SELECT id, name, bulk_cost, markup_rate, price, stock, low_stock_threshold, supplier_item_id FROM products";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            masterInventory.clear();
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("bulk_cost"),
                        rs.getDouble("markup_rate"),
                        rs.getInt("stock"),
                        rs.getInt("low_stock_threshold"),
                        rs.getString("supplier_item_id") // ✅ Load supplier_item_id
                );
                masterInventory.add(product);
            }

            System.out.println("Loaded " + masterInventory.size() + " products from database");
        } catch (SQLException e) {
            System.err.println("Error loading products from database: " + e.getMessage());
            e.printStackTrace();
            loadMockData();
        }
    }

    /**
     * Fallback mock data if database connection fails
     */
    private void loadMockData() {
        addProduct(1, "Paracetamol", 2.00, 0.25, 100, 20);
        addProduct(2, "Ibuprofen", 1.50, 0.30, 150, 25);
        addProduct(3, "Aspirin", 1.00, 0.35, 200, 30);
        addProduct(4, "Vitamin C", 3.00, 0.20, 75, 15);
        addProduct(5, "Cough Syrup", 4.50, 0.25, 50, 10);
    }

    /**
     * ADD product AND save to database
     */
    public boolean addProduct(int id, String name, double bulkCost, double markup, int stock, int threshold) {
        // Check if product already exists
        for (Product p : masterInventory) {
            if (p.getId() == id) {
                System.out.println("Product ID " + id + " already exists");
                return false;
            }
        }

        String sql = "INSERT INTO products (id, name, bulk_cost, markup_rate, price, stock, low_stock_threshold) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Product p = new Product(id, name, bulkCost, markup, stock, threshold);
            p.setPrice(calculateRetailPrice(p));

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setDouble(3, bulkCost);
            stmt.setDouble(4, markup);
            stmt.setDouble(5, p.getPrice());
            stmt.setInt(6, stock);
            stmt.setInt(7, threshold);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                masterInventory.add(p);
                System.out.println("Product added to database: " + name);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding product: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * UPDATE entire product AND save to database
     */
    public boolean updateProduct(Product product) {
        if (product == null) return false;

        String sql = "UPDATE products SET name = ?, bulk_cost = ?, markup_rate = ?, price = ?, stock = ?, low_stock_threshold = ? WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, product.getName());
            stmt.setDouble(2, product.getBulkCost());
            stmt.setDouble(3, product.getMarkupRate());
            stmt.setDouble(4, product.getPrice());
            stmt.setInt(5, product.getStock());
            stmt.setInt(6, product.getLowStockThreshold());
            stmt.setInt(7, product.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Product updated in database: " + product.getName());
                return true;
            } else {
                System.out.println("No rows updated for product ID: " + product.getId());
            }

        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * UPDATE specific field AND save to database (For TableView cell edits)
     */
    public boolean updateProductField(int productId, String fieldName, Object newValue) {
        String sql = "UPDATE products SET " + fieldName + " = ? WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (newValue instanceof String) {
                stmt.setString(1, (String) newValue);
            } else if (newValue instanceof Double) {
                stmt.setDouble(1, (Double) newValue);
            } else if (newValue instanceof Integer) {
                stmt.setInt(1, (Integer) newValue);
            } else if (newValue instanceof Float) {
                stmt.setFloat(1, (Float) newValue);
            }
            stmt.setInt(2, productId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Product field updated: " + fieldName + " = " + newValue);

                // Update local cache
                for (Product p : masterInventory) {
                    if (p.getId() == productId) {
                        switch (fieldName) {
                            case "name": p.setName((String) newValue); break;
                            case "bulk_cost": p.setBulkCost((Double) newValue); break;  // ✅ This handles bulk_cost
                            case "markup_rate": p.setMarkupRate((Double) newValue); break;
                            case "price": p.setPrice((Double) newValue); break;
                            case "stock": p.setStock((Integer) newValue); break;
                            case "low_stock_threshold": p.setLowStockThreshold((Integer) newValue); break;
                        }
                        break;
                    }
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating product field: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * DELETE product from database
     */
    public boolean deleteProduct(Product p) {
        if (p == null) return false;

        String sql = "DELETE FROM products WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, p.getId());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                masterInventory.remove(p);
                System.out.println("Product deleted from database: " + p.getName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting product: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * ✅ Decrement stock in database AND local cache (For sales)
     */
    public boolean decrementLocalStock(int itemID, int quantity) {
        String sql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, quantity);
            stmt.setInt(2, itemID);
            stmt.setInt(3, quantity);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Stock decremented in database for product " + itemID);

                for (Product p : masterInventory) {
                    if (p.getId() == itemID) {
                        p.setStock(p.getStock() - quantity);
                        logStockChange(itemID, -quantity, "Sale", 0);
                        break;
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println(" Error decrementing stock: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Log stock change to database (audit trail)
     */
    private void logStockChange(int productId, int changeAmount, String reason, int userId) {
        String sql = "INSERT INTO stock_changes (product_id, change_amount, reason, user_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setInt(2, changeAmount);
            stmt.setString(3, reason);
            stmt.setInt(4, userId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error logging stock change: " + e.getMessage());
        }
    }

    public List<Product> getLowStockItems() {
        return masterInventory.stream()
                .filter(p -> p.getStock() <= p.getLowStockThreshold())
                .collect(Collectors.toList());
    }

    public int calculateRecommendedOrder(Product p) {
        int targetStock = (int) Math.ceil(p.getLowStockThreshold() * 1.10);
        int recommended = targetStock - p.getStock();
        return Math.max(0, recommended);
    }

    public double calculateRetailPrice(Product p) {
        double priceBeforeVat = p.getBulkCost() * (1 + p.getMarkupRate());
        return priceBeforeVat * (1 + currentVatRate);
    }

    public void refreshAllPrices() {
        for (Product p : masterInventory) {
            p.setPrice(calculateRetailPrice(p));
            updateProduct(p);
        }
    }

    public void setVatRate(double rate) {
        this.currentVatRate = rate;
        refreshAllPrices();
    }

    public double getVatRate() {
        return currentVatRate;
    }

    public ObservableList<Product> getProducts() {
        return masterInventory;
    }

    public void refreshProducts() {
        masterInventory.clear();
        loadProductsFromDatabase();
    }
}