package com.pharmacy.iposca.controller;
import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.controlsfx.control.tableview2.filter.filtereditor.SouthFilter;
import javax.print.DocFlavor;
import java.sql.*;

public class AdminController {
    private static AdminController instance;
    private ObservableList<User> users = FXCollections.observableArrayList();

    private AdminController() {
        loadUsersFromDatabase();
    }

    public static synchronized AdminController getInstance() {
        if (instance == null) {
            instance = new AdminController();
        }
        return instance;
    }

    private void loadUsersFromDatabase() {
        String sql = "SELECT id, username, full_name, password, role, active FROM users";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            users.clear();
            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("password"),
                        rs.getString("role")
                );
                user.setActive(rs.getBoolean("active"));
                users.add(user);
            }

            System.out.println("Loaded " + users.size() + " users from database");
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
            loadMockUsers();
        }
    }

    private void loadMockUsers() {
        users.addAll(
                new User(1001, "admin", "System Administrator", "admin123", User.Admin),
                new User(1002, "pharmacist", "John Pharmacist", "pharm123", User.Pharmacist),
                new User(1003, "manager", "Mike Manager", "mgr123", User.Manager)
        );
        System.out.println("Using mock user data (database connection failed)");
    }

    public ObservableList<User> getUsers() {
        return users;
    }

    public boolean createUser(String username, String fullName, String password, String role) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                System.out.println("Username already exists: " + username);
                return false;
            }
        }

        String sql = "INSERT INTO users (username, full_name, password, role, active) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, fullName);
            stmt.setString(3, password);
            stmt.setString(4, role);
            stmt.setBoolean(5, true);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                int newId = 0;
                if (rs.next()) {
                    newId = rs.getInt(1);
                }

                User newUser = new User(newId, username, fullName, password, role);
                users.add(newUser);

                System.out.println("User created: " + username);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(User user) {
        if (user == null) return false;

        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user.getId());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                users.remove(user);
                System.out.println("User deleted: " + user.getUsername());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateRole(User user, String newRole) {
        if (user == null) {
            return false;
        }

        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setInt(2, user.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                user.setRole(newRole);
                System.out.println("User role updated: " + user.getUsername() + " = " + newRole);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean toggleStatus(User user) {
        if (user == null) return false;

        String sql = "UPDATE users SET active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            boolean newStatus = !user.isActive();
            stmt.setBoolean(1, newStatus);
            stmt.setInt(2, user.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                user.setActive(newStatus);
                System.out.println("User status updated: " + user.getUsername() + " = " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}