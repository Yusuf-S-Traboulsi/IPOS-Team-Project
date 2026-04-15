package com.pharmacy.iposca.controller;
import com.pharmacy.iposca.model.User;

public class AuthenticationService {
    private AdminController adminController;
    private User authenticatedUser;
    private static AuthenticationService currentInstance;

    public AuthenticationService(AdminController adminController) {
        this.adminController = adminController;
        currentInstance = this;
    }

    public static AuthenticationService getCurrentInstance() {
        return currentInstance;
    }

    public User getCurrentUser() {
        return authenticatedUser;
    }

    public boolean login(String username, String password) {
        System.out.println("Login attempt for username: " + username);
        for (User user : adminController.getUsers()) {
            System.out.println("   Checking user: " + user.getUsername());
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {

                if (!user.isActive()) {
                    System.out.println("Login Denied: Account is Inactive for user: " + username);
                    return false;
                }

                this.authenticatedUser = user;
                System.out.println("Login successful for: " + username + " (Role: " + user.getRole() + ")");
                return true;
            }
        }
        System.out.println("Login failed: Invalid username or password for: " + username);
        return false;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void logout() {
        System.out.println("User logged out: " + (authenticatedUser != null ? authenticatedUser.getUsername() : "Unknown"));
        this.authenticatedUser = null;
        System.out.println("User session cleared. Ready for next user.");
    }
}