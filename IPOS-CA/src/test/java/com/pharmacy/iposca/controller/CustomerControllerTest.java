package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

public class CustomerControllerTest {

    private CustomerController controller;

    @BeforeEach
    public void setUp() {
        controller = CustomerController.getInstance();
    }

    @Test
    public void testFindCustomerById() {
        controller.addCustomer("Mr.", "John Smith", "johnsmith@mail.com", "123 Main St", "London", "SW1A 1AA", 1000.0);
        Customer lastCustomer = controller.getCustomerData().get(controller.getCustomerData().size() - 1);
        Customer found = controller.findCustomerById(lastCustomer.getId());

        assertNotNull(found, "Customer should be found");
        assertEquals("John Smith", found.getName(), "Name should match");
    }

    @Test
    public void testFindCustomerByIdNotFound() {
        Customer found = controller.findCustomerById(9999);
        assertNull(found, "Should return null for non-existent customer");
    }

    @Test
    public void testAddCustomer() {
        int initialSize = controller.getCustomerData().size();
        controller.addCustomer("Dr.", "Jane Doe", "janedoe@mail.com", "456 High St", "Manchester", "M1 1AA", 500.0);
        assertEquals(initialSize + 1, controller.getCustomerData().size(), "Customer count should increase");
    }

    @Test
    public void testDeleteCustomerWithNoDebt() {
        controller.addCustomer("Mr.", "Test User", "testuser@mail.com","789 Road", "Birmingham", "B1 1AA", 200.0);
        Customer lastCustomer = controller.getCustomerData().get(controller.getCustomerData().size() - 1);

        boolean deleted = controller.deleteCustomer(lastCustomer);
        assertTrue(deleted, "Should delete customer with no debt");
    }

    @Test
    public void testDeleteCustomerWithDebt() {
        controller.addCustomer("Mrs.", "Debt User", "debtuser@mail.com","321 Lane", "Leeds", "LS1 1AA", 300.0);
        Customer lastCustomer = controller.getCustomerData().get(controller.getCustomerData().size() - 1);
        lastCustomer.setCurrentDebt(50.0);

        boolean deleted = controller.deleteCustomer(lastCustomer);
        assertFalse(deleted, "Should NOT delete customer with active debt");
    }

    @Test
    public void testEvaluateAccountStatusesSuspended() {
        controller.addCustomer("Mr.", "Test Debtor", "testdebtor@mail.com","100 Debt St", "London", "E1 1AA", 500.0);
        Customer debtor = controller.getCustomerData().get(controller.getCustomerData().size() - 1);
        debtor.setCurrentDebt(100.0);
        debtor.setOldestDebtDate(LocalDate.now().minusDays(35));

        controller.evaluateAccountStatuses(LocalDate.now());
        assertEquals("Suspended", debtor.getAccountStatus(), "Account should be suspended (30-60 days)");
    }

    @Test
    public void testEvaluateAccountStatusesInDefault() {
        controller.addCustomer("Mr.", "Default Debtor", "defaultdebtor@mail.com", "200 Debt St", "London", "E2 2AA", 500.0);
        Customer debtor = controller.getCustomerData().get(controller.getCustomerData().size() - 1);
        debtor.setCurrentDebt(100.0);
        debtor.setOldestDebtDate(LocalDate.now().minusDays(70));

        controller.evaluateAccountStatuses(LocalDate.now());
        assertEquals("In Default", debtor.getAccountStatus(), "Account should be in default (60+ days)");
    }

    @Test
    public void testEvaluateAccountStatusesNormal() {
        controller.addCustomer("Mr.", "Good Customer", "goodcustomer@mail.com", "300 Good St", "London", "E3 3AA", 500.0);
        Customer goodCustomer = controller.getCustomerData().get(controller.getCustomerData().size() - 1);
        goodCustomer.setCurrentDebt(50.0);
        goodCustomer.setOldestDebtDate(LocalDate.now().minusDays(10));

        controller.evaluateAccountStatuses(LocalDate.now());
        assertEquals("Normal", goodCustomer.getAccountStatus(), "Account should remain normal (< 30 days)");
    }
}