package com.pharmacy.iposca.model;

import javafx.beans.property.*;

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

    public String getItemId() { return itemId.get(); }
    public String getDescription() { return description.get(); }
    public String getPackageType() { return packageType.get(); }
    public String getUnit() { return unit.get(); }
    public int getUnitsPerPack() { return unitsPerPack.get(); }
    public double getPackageCost() { return packageCost.get(); }
    public int getAvailability() { return availability.get(); }
    public int getStockLimit() { return stockLimit.get(); }
    public String getCategory() { return category.get(); }
}