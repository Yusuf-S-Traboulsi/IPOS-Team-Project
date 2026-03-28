package com.pharmacy.iposca.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

    @Test
    public void testProductCreation() {
        Product product = new Product(1, "Paracetamol", 2.00, 0.25, 100, 20);

        assertEquals(1, product.getId());
        assertEquals("Paracetamol", product.getName().trim());
        assertEquals(2.00, product.getBulkCost(), 0.01);
        assertEquals(0.25, product.getMarkupRate(), 0.01);
        assertEquals(100, product.getStock());
        assertEquals(20, product.getLowStockThreshold());
    }

    @Test
    public void testSetPrice() {
        Product product = new Product(1, "Test", 5.00, 0.20, 50, 10);
        product.setPrice(7.50);
        assertEquals(7.50, product.getPrice(), 0.01);
    }

    @Test
    public void testSetStock() {
        Product product = new Product(1, "Test", 5.00, 0.20, 50, 10);
        product.setStock(30);
        assertEquals(30, product.getStock());
    }

    @Test
    public void testLowStockThreshold() {
        Product product = new Product(1, "Test", 5.00, 0.20, 15, 20);
        assertTrue(product.getStock() <= product.getLowStockThreshold(), "Should be below threshold");
    }
}