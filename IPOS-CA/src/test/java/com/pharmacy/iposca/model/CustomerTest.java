package com.pharmacy.iposca.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomerTest {

    @Test
    public void testCustomerCreation() {
        Customer customer = new Customer(101, "Mr.", "John Smith", "johnsmith@mail.com","123 Main St", "London", "SW1A 1AA", 1000.0, 0.0);

        assertEquals(101, customer.getId());
        assertEquals("Mr.", customer.getTitle());
        assertEquals("John Smith", customer.getName());
        assertEquals("123 Main St", customer.getAddress());
        assertEquals("London", customer.getTown());
        assertEquals("SW1A 1AA", customer.getPostcode());
        assertEquals(1000.0, customer.getCreditLimit(), 0.01);
        assertEquals(0.0, customer.getCurrentDebt(), 0.01);
        assertEquals("Normal", customer.getAccountStatus());
    }

    @Test
    public void testSetCurrentDebt() {
        Customer customer = new Customer(101, "Mr.", "Test", "Mail", "Addr", "Town", "Post", 500.0, 0.0);
        customer.setCurrentDebt(150.0);
        assertEquals(150.0, customer.getCurrentDebt(), 0.01);
    }

    @Test
    public void testSetAccountStatus() {
        Customer customer = new Customer(101, "Mr.", "Test", "Mail", "Addr", "Town", "Post", 500.0, 0.0);
        customer.setAccountStatus("Suspended");
        assertEquals("Suspended", customer.getAccountStatus());
    }

    @Test
    public void testCreditLimitExceeded() {
        Customer customer = new Customer(101, "Mr.", "Test", "Mail", "Addr", "Town", "Post", 500.0, 0.0);
        customer.setCurrentDebt(600.0);
        assertTrue(customer.getCurrentDebt() > customer.getCreditLimit(), "Should exceed credit limit");
    }
}