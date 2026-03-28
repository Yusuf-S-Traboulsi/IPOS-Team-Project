package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.controller.AdminController;
import com.pharmacy.iposca.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AdminControllerTest {

    private AdminController controller;

    @BeforeEach
    public void setUp() {
        controller = AdminController.getInstance();
    }

    @Test
    public void testGetUsers() {
        var users = controller.getUsers();
        assertNotNull(users, "Users list should not be null");
        assertTrue(users.size() >= 4, "Should have at least 4 default users");
    }

    @Test
    public void testCreateUser() {
        int initialSize = controller.getUsers().size();
        controller.createUser("TestUser","jeff", "test123", User.Pharmacist);
        assertEquals(initialSize + 1, controller.getUsers().size(), "User count should increase");
    }

    @Test
    public void testToggleStatus() {
        User user = controller.getUsers().get(0);
        boolean initialStatus = user.isActive();
        controller.toggleStatus(user);
        assertEquals(!initialStatus, user.isActive(), "Status should be toggled");
        controller.toggleStatus(user); // Reset
    }

    @Test
    public void testUserRoles() {
        User admin = controller.getUsers().get(0);
        User pharmacist = controller.getUsers().get(1);

        assertTrue(admin.isAdmin(), "First user should be admin");
        assertTrue(pharmacist.isPharmacist(), "Second user should be pharmacist");
    }
}