package com.pharmacy.iposca.model;
import javafx.beans.property.*;

/**
 * Class representing a user in the system.
 */
public class User {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty(); // NEW FIELD
    private final StringProperty password = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final BooleanProperty isActive = new SimpleBooleanProperty();

    public static final String Admin = "ADMIN";
    public static final String Manager = "MANAGER";
    public static final String Pharmacist = "PHARMACIST";

    public User(int id, String username, String fullName, String password, String role) {
        this.id.set(id);
        this.username.set(username);
        this.fullName.set(fullName);
        this.password.set(password);
        this.role.set(role);
        this.isActive.set(true);
    }

    //Getters and Setters
    public int getId() { return id.get(); }
    public String getUsername() { return username.get(); }
    public void setUsername(String username) { this.username.set(username); }
    public StringProperty usernameProperty() { return username; }

    public String getFullName() { return fullName.get(); }
    public void setFullName(String fullName) { this.fullName.set(fullName); }
    public StringProperty fullNameProperty() { return fullName; }

    public String getPassword() { return password.get(); }
    public void setPassword(String password) { this.password.set(password); }

    public String getRole() { return role.get(); }
    public void setRole(String role) { this.role.set(role); }

    public BooleanProperty isActiveProperty() { return isActive; }
    public boolean isActive() { return isActive.get(); }
    public void setActive(boolean active) { this.isActive.set(active); }

    public boolean isPharmacist() { return Pharmacist.equals(role.get()); }
    public boolean isAdmin() { return Admin.equals(role.get()); }
    public boolean isManager() { return Manager.equals(role.get()); }
}