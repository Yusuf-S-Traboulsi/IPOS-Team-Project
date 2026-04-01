package com.pharmacy.iposca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connector for IPOS-CA and IPOS-SA
 *
 * IPOS-CA: Pharmacy system (ipos_ca database)
 * IPOS-SA: Supplier system (ipos_sa database) - SEPARATE!
 */
public class DatabaseConnector {

    // IPOS-CA Database (Pharmacy System)
    private static final String CA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_ca?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String CA_USER = "root";
    private static final String CA_PASSWORD = "Swim1234";

    // IPOS-SA Database (Supplier System) - SEPARATE DATABASE!
    private static final String SA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_sa?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String SA_USER = "root";
    private static final String SA_PASSWORD = "Swim1234";

    static {
        testConnection();
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
     * Get connection to IPOS-SA database (SEPARATE!)
     * ONLY used by SupplierRestAPI
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
     * Test connection on startup
     */
    public static void testConnection() {
        try {
            Connection conn = getConnection();
            System.out.println("✅ Connected to IPOS-CA database at " + CA_URL);
            conn.close();

            Connection saConn = getSAConnection();
            System.out.println("✅ Connected to IPOS-SA database at " + SA_URL);
            saConn.close();
        } catch (SQLException e) {
            System.err.println("❌ FAILED to connect to database!");
            System.err.println("   Error: " + e.getMessage());
        }
    }
}