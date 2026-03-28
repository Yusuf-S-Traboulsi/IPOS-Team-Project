package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InventoryControllerTest {

    private InventoryController controller;

    @BeforeEach
    public void setUp() {
        controller = InventoryController.getInstance();
    }

    @Test
    public void testGetVatRate() {
        double vat = controller.getVatRate();
        assertEquals(0.20, vat, 0.01, "VAT rate should be 20%");
    }

    @Test
    public void testSetVatRate() {
        controller.setVatRate(0.25);
        assertEquals(0.25, controller.getVatRate(), 0.01, "VAT rate should be updated");
        controller.setVatRate(0.20); // Reset
    }

    @Test
    public void testGetProducts() {
        var products = controller.getProducts();
        assertNotNull(products, "Products list should not be null");
        assertTrue(products.size() > 0, "Should have at least one product");
    }

    @Test
    public void testCalculateRetailPrice() {
        Product testProduct = new Product(999, "Test Item", 10.00, 0.30, 50, 10);
        double retailPrice = controller.calculateRetailPrice(testProduct);
        // Price = BulkCost * (1 + Markup) * (1 + VAT) = 10.00 * 1.30 * 1.20 = 15.60
        assertEquals(15.60, retailPrice, 0.01, "Retail price calculation incorrect");
    }

    @Test
    public void testGetLowStockItems() {
        var lowStockItems = controller.getLowStockItems();
        assertNotNull(lowStockItems, "Low stock list should not be null");
    }

    @Test
    public void testAddProduct() {
        int initialSize = controller.getProducts().size();
        controller.addProduct(999, "New Product", 5.00, 0.20, 100, 20);
        assertEquals(initialSize + 1, controller.getProducts().size(), "Product count should increase");
    }

    @Test
    public void testDecrementLocalStock() {
        Product product = controller.getProducts().get(0);
        int initialStock = product.getStock();
        controller.decrementLocalStock(product.getId(), 5);
        assertEquals(initialStock - 5, product.getStock(), "Stock should be decremented");
    }
}