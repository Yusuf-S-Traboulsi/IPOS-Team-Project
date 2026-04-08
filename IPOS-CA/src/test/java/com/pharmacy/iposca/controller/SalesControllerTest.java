package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.controller.InventoryController;
import com.pharmacy.iposca.controller.SalesController;
import com.pharmacy.iposca.model.Customer;
import com.pharmacy.iposca.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SalesControllerTest {

    private SalesController salesController;
    private InventoryController inventoryController;

    @BeforeEach
    public void setUp() {
        inventoryController = InventoryController.getInstance();
        salesController = new SalesController(inventoryController);
    }

    @Test
    public void testEmptyCart() {
        double total = salesController.calculateTotal();
        assertEquals(0.0, total, 0.01, "Empty cart should have zero total");
    }

    @Test
    public void testAddItemToCart() {
        Product product = inventoryController.getProducts().get(0);
        salesController.addItemToCart(product);
        assertEquals(1, salesController.getCart().size(), "Cart should have 1 item");
    }

    @Test
    public void testAddSameItemIncreasesQuantity() {
        Product product = inventoryController.getProducts().get(0);
        salesController.addItemToCart(product);
        salesController.addItemToCart(product);
        assertEquals(1, salesController.getCart().size(), "Cart should have 1 unique item");
        assertEquals(2, salesController.getCart().get(0).getQuantity(), "Quantity should be 2");
    }

    @Test
    public void testRemoveItemFromCart() {
        Product product = inventoryController.getProducts().get(0);
        salesController.addItemToCart(product);
        salesController.removeItemFromCart(salesController.getCart().get(0));
        assertEquals(0, salesController.getCart().size(), "Cart should be empty");
    }

    @Test
    public void testProcessSaleWithEmptyCart() {
        String result = salesController.processSale(null, "CASH", "", "", "", "");
        assertTrue(result.contains("empty"), "Should reject empty cart");
    }

    @Test
    public void testProcessSaleWithSuspendedAccount() {
        System.out.println("Products count: " + inventoryController.getProducts().size());

        Customer customer = new Customer(101, "Mr.", "Test User", "testuser@mail.com","123 St", "London", "SW1", 1000.0, 0.0);
        customer.setAccountStatus("Suspended");

        salesController.addItemToCart(inventoryController.getProducts().get(0));

        // FIXED: Use future expiry date (December 2027)
        String result = salesController.processSale(customer, "CARD", "Debit", "1234", "5678", "12/27");

        System.out.println("Result: " + result);
        assertTrue(result.toLowerCase().contains("denied"), "Should reject suspended account");
    }
}