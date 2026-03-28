package com.pharmacy.iposca.api;

import com.pharmacy.iposca.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryAPITest {

    private IInventoryAPI api;

    @BeforeEach
    public void setUp() {
        api = new CAController();
    }

    @Test
    public void testSearchStockByName() {
        Product[] results = api.searchStock("Paracetamol");
        assertTrue(results.length > 0, "Should find Paracetamol");
        assertEquals("Paracetamol", results[0].getName().trim(), "First result should be Paracetamol");
    }

    @Test
    public void testSearchStockById() {
        Product[] results = api.searchStock("1");
        assertTrue(results.length > 0, "Should find product with ID 1");
    }

    @Test
    public void testSearchStockNotFound() {
        Product[] results = api.searchStock("XYZNonExistent");
        assertEquals(0, results.length, "Should return empty array for non-existent product");
    }

    @Test
    public void testGetStockLevel() {
        int stock = api.getStockLevel(1);
        assertTrue(stock >= 0, "Stock level should be non-negative");
    }

    @Test
    public void testGetStockLevelNotFound() {
        int stock = api.getStockLevel(9999);
        assertEquals(-1, stock, "Should return -1 for non-existent product");
    }

    @Test
    public void testGetRetailPrice() {
        float price = api.getRetailPrice(1);
        assertTrue(price > 0, "Price should be positive");
    }

    @Test
    public void testGetRetailPriceNotFound() {
        float price = api.getRetailPrice(9999);
        assertEquals(-1.0f, price, 0.01f, "Should return -1.0 for non-existent product");
    }
}