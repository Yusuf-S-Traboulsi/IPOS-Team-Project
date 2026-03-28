package com.pharmacy.iposca.api;

import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.model.Product;
import javafx.collections.ObservableList;
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
}