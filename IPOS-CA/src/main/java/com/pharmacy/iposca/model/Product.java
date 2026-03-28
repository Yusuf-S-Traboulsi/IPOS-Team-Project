package com.pharmacy.iposca.model;

import com.google.gson.annotations.Expose;
import javafx.beans.property.*;

/**
 * Product model for the pharmacy inventory system.
 * Uses JavaFX properties for UI binding and @Expose for API serialization.
 */
public class Product {
    @Expose private final IntegerProperty id = new SimpleIntegerProperty();
    @Expose private final StringProperty name = new SimpleStringProperty();
    @Expose private final DoubleProperty bulkCost = new SimpleDoubleProperty();
    @Expose private final DoubleProperty markupRate = new SimpleDoubleProperty();
    @Expose private final DoubleProperty price = new SimpleDoubleProperty();
    @Expose private final IntegerProperty stock = new SimpleIntegerProperty();
    @Expose private final IntegerProperty lowStockThreshold = new SimpleIntegerProperty();
    @Expose private final StringProperty supplierItemId = new SimpleStringProperty(); // ✅ NEW

    public Product(int id, String name, double bulkCost, double markupRate, int stock, int lowStockThreshold) {
        this.id.set(id);
        this.name.set(name);
        this.bulkCost.set(bulkCost);
        this.markupRate.set(markupRate);
        this.price.set(calculatePrice(bulkCost, markupRate));
        this.stock.set(stock);
        this.lowStockThreshold.set(lowStockThreshold);
        this.supplierItemId.set("");
    }

    // ✅ NEW: Constructor with supplier_item_id
    public Product(int id, String name, double bulkCost, double markupRate, int stock,
                   int lowStockThreshold, String supplierItemId) {
        this.id.set(id);
        this.name.set(name);
        this.bulkCost.set(bulkCost);
        this.markupRate.set(markupRate);
        this.price.set(calculatePrice(bulkCost, markupRate));
        this.stock.set(stock);
        this.lowStockThreshold.set(lowStockThreshold);
        this.supplierItemId.set(supplierItemId);
    }

    // ===== PROPERTY GETTERS (for JavaFX UI) =====
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public DoubleProperty bulkCostProperty() { return bulkCost; }
    public DoubleProperty markupRateProperty() { return markupRate; }
    public DoubleProperty priceProperty() { return price; }
    public IntegerProperty stockProperty() { return stock; }
    public IntegerProperty lowStockThresholdProperty() { return lowStockThreshold; }
    public StringProperty supplierItemIdProperty() { return supplierItemId; } // ✅ NEW

    // ===== PLAIN VALUE GETTERS (for Gson/REST API) =====
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public double getBulkCost() { return bulkCost.get(); }
    public double getMarkupRate() { return markupRate.get(); }
    public double getPrice() { return price.get(); }
    public int getStock() { return stock.get(); }
    public int getLowStockThreshold() { return lowStockThreshold.get(); }
    public String getSupplierItemId() { return supplierItemId.get(); } // ✅ NEW

    // ===== SETTERS =====
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setBulkCost(double bulkCost) { this.bulkCost.set(bulkCost); }
    public void setMarkupRate(double markupRate) { this.markupRate.set(markupRate); }
    public void setPrice(double price) { this.price.set(price); }
    public void setStock(int stock) { this.stock.set(stock); }
    public void setLowStockThreshold(int threshold) { this.lowStockThreshold.set(threshold); }
    public void setSupplierItemId(String supplierItemId) { this.supplierItemId.set(supplierItemId); } // ✅ NEW

    private double calculatePrice(double bulkCost, double markupRate) {
        return bulkCost * (1 + markupRate) * 1.20;
    }
}