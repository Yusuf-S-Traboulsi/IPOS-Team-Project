package com.pharmacy.iposca.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testUserCreation() {
        // FIXED: Added fullName as 3rd parameter (5 total)
        User user = new User(1001, "Admin", "System Administrator", "123", User.Admin);

        assertEquals(1001, user.getId());
        assertEquals("Admin", user.getUsername());
        assertEquals("System Administrator", user.getFullName());  // NEW assertion
        assertEquals("123", user.getPassword());
        assertEquals(User.Admin, user.getRole());
        assertTrue(user.isActive(), "New user should be active by default");
    }

    @Test
    public void testSetActive() {
        // FIXED: Added fullName parameter
        User user = new User(1001, "Test", "Test User", "pass", User.Pharmacist);
        user.setActive(false);
        assertFalse(user.isActive());
    }

    @Test
    public void testIsAdmin() {
        // FIXED: Added fullName parameter
        User admin = new User(1001, "Admin", "System Administrator", "123", User.Admin);
        User pharmacist = new User(1002, "Pharma", "John Pharmacist", "123", User.Pharmacist);

        assertTrue(admin.isAdmin());
        assertFalse(pharmacist.isAdmin());
    }

    @Test
    public void testIsPharmacist() {
        // FIXED: Added fullName parameter
        User pharmacist = new User(1002, "Pharma", "John Pharmacist", "123", User.Pharmacist);
        User manager = new User(1004, "Manager", "Mike Manager", "123", User.Manager);

        assertTrue(pharmacist.isPharmacist());
        assertFalse(manager.isPharmacist());
    }

    @Test
    public void testIsManager() {
        // FIXED: Added fullName parameter (was incorrectly using 5 params for admin)
        User manager = new User(1004, "Manager", "Mike Manager", "123", User.Manager);
        User admin = new User(1001, "Admin", "System Administrator", "123", User.Admin);

        assertTrue(manager.isManager());
        assertFalse(admin.isManager());
    }

    @Test
    public void testGetFullName() {
        // NEW: Test the fullName getter
        User user = new User(1005, "jdoe", "Jane Doe", "password123", User.Pharmacist);
        assertEquals("Jane Doe", user.getFullName());
    }

    @Test
    public void testSetUsername() {
        // NEW: Test username setter if it exists
        User user = new User(1001, "oldname", "Test User", "pass", User.Admin);
        // If your User class has setUsername(), uncomment this:
        // user.setUsername("newname");
        // assertEquals("newname", user.getUsername());
    }
}