package com.pharmacy.iposca.model;

import javafx.beans.property.*;

/**
 * Supplier Catalogue Item Model
 * Matches Briefing Section 9.1 Catalogue Layout
 */
public class SupplierCatalogueItem {

    private final StringProperty itemId = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty packageType = new SimpleStringProperty();
    private final StringProperty unit = new SimpleStringProperty();
    private final IntegerProperty unitsPerPack = new SimpleIntegerProperty();
    private final DoubleProperty packageCost = new SimpleDoubleProperty();
    private final IntegerProperty availability = new SimpleIntegerProperty();
    private final IntegerProperty stockLimit = new SimpleIntegerProperty();
    private final StringProperty category = new SimpleStringProperty();

    public SupplierCatalogueItem(String itemId, String description, String packageType,
                                 String unit, int unitsPerPack, double packageCost,
                                 int availability, int stockLimit, String category) {
        this.itemId.set(itemId);
        this.description.set(description);
        this.packageType.set(packageType);
        this.unit.set(unit);
        this.unitsPerPack.set(unitsPerPack);
        this.packageCost.set(packageCost);
        this.availability.set(availability);
        this.stockLimit.set(stockLimit);
        this.category.set(category);
    }

    // Getters and Setters
    public String getItemId() { return itemId.get(); }
    public void setItemId(String itemId) { this.itemId.set(itemId); }
    public StringProperty itemIdProperty() { return itemId; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public String getPackageType() { return packageType.get(); }
    public void setPackageType(String packageType) { this.packageType.set(packageType); }
    public StringProperty packageTypeProperty() { return packageType; }

    public String getUnit() { return unit.get(); }
    public void setUnit(String unit) { this.unit.set(unit); }
    public StringProperty unitProperty() { return unit; }

    public int getUnitsPerPack() { return unitsPerPack.get(); }
    public void setUnitsPerPack(int units) { this.unitsPerPack.set(units); }
    public IntegerProperty unitsPerPackProperty() { return unitsPerPack; }

    public double getPackageCost() { return packageCost.get(); }
    public void setPackageCost(double cost) { this.packageCost.set(cost); }
    public DoubleProperty packageCostProperty() { return packageCost; }

    public int getAvailability() { return availability.get(); }
    public void setAvailability(int availability) { this.availability.set(availability); }
    public IntegerProperty availabilityProperty() { return availability; }

    public int getStockLimit() { return stockLimit.get(); }
    public void setStockLimit(int limit) { this.stockLimit.set(limit); }
    public IntegerProperty stockLimitProperty() { return stockLimit; }

    public String getCategory() { return category.get(); }
    public void setCategory(String category) { this.category.set(category); }
    public StringProperty categoryProperty() { return category; }
}