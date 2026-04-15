package com.pharmacy.iposca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *  This class handles database connections.
 */
public class DatabaseConnector {

    // IPOS-CA Database
    private static final String CA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_ca?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String CA_USER = "root";
    private static final String CA_PASSWORD = "Swim1234";

    // IPOS-SA Database
    private static final String SA_URL = "jdbc:mysql://127.0.0.1:3306/ipos_sa?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String SA_USER = "root";
    private static final String SA_PASSWORD = "Swim1234";

    // IPOS-PU Database
    private static final String PU_URL = "jdbc:mysql://127.0.0.1:3306/ipos_pu?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String PU_USER = "root";
    private static final String PU_PASSWORD = "Swim1234";

    static {
        testCADatabase();
        testSADatabase();
        testPUDatabase();
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(CA_URL, CA_USER, CA_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

    /**
     *  Get connection to IPOS-SA database
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
     *  Get connection to IPOS-PU database
     */
    public static Connection getPUConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(PU_URL, PU_USER, PU_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
    }

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

    //Test methods

    private static void testSADatabase() {
        try {
            Connection conn = getSAConnection();
            System.out.println("Connected to IPOS-SA database at " + SA_URL);
            conn.close();
        } catch (SQLException e) {
            System.err.println("FAILED to connect to IPOS-SA database!");
            System.err.println("   Error: " + e.getMessage());
        }
    }

    private static void testPUDatabase() {
        try (Connection conn = getPUConnection()) {
            System.out.println("Connected to IPOS-PU database");
        } catch (SQLException e) {
            System.err.println("FAILED to connect to IPOS-PU: " + e.getMessage());
        }
    }
}