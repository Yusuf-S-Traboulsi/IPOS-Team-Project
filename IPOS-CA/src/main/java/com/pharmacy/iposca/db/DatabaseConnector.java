package com.pharmacy.iposca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * This class handles database connections.
 */
public class DatabaseConnector {

    // IPOS-CA Database
    private static final String CA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_ca?useSSL=false&allowPublicKeyRetrieval=true";

    private static final String CA_USER =
            System.getProperty("IPOS_CA_DB_USER", "root");
    private static final String CA_PASSWORD =
            System.getProperty("IPOS_CA_DB_PASSWORD", "Swim1234");

    // IPOS-SA Database
    private static final String SA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_sa?useSSL=false&allowPublicKeyRetrieval=true";

    private static final String SA_USER =
            System.getProperty("IPOS_SA_DB_USER", "root");
    private static final String SA_PASSWORD =
            System.getProperty("IPOS_SA_DB_PASSWORD", "Swim1234");

    static {
        testCADatabase();
    }

    /**
     * Get connection to IPOS-CA database (default)
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(CA_URL, CA_USER, CA_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    /**
     * Get connection to IPOS-SA database
     */
    public static Connection getSAConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(SA_URL, SA_USER, SA_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    /**
     * Test connection to IPOS-CA database on startup.
     * This avoids touching the IPOS-SA database unless explicitly requested.
     */
    private static void testCADatabase() {
        try {
            Connection conn = getConnection();
            System.out.println("Connected to IPOS-CA database at " + CA_URL);
            conn.close();
        } catch (SQLException e) {
            System.err.println("FAILED to connect to IPOS-CA database!");
            System.err.println("   Error: " + e.getMessage());
        }
    }
}