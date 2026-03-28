package com.pharmacy.iposca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connector for Local MySQL Server
 * All teams connect to this same database
 */
public class DatabaseConnector {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/ipos_ca?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Swim1234"; // Your MySQL root password

    // Test connection on class load - i think
    static {
        testConnection();
    }

    /**
     * Establishes a connection to the local MySQL server.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    /**
     * Test connection on startup
     */
    public static void testConnection() {
        try {
            Connection conn = getConnection();
            System.out.println("Connected to MySQL database at " + URL);
            System.out.println("Database: ipos_ca");
            System.out.println("User: " + USER);
            conn.close();
        } catch (SQLException e) {
            System.err.println("FAILED to connect to MySQL database!");
            System.err.println("Error: " + e.getMessage());
            System.err.println("URL: " + URL);
            System.err.println("User: " + USER);
            System.err.println("Check: 1) MySQL running, 2) Password correct, 3) Database exists");
        }
    }
}