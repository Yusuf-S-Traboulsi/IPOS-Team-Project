package com.pharmacy.iposca.api;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.OnlineOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

/**
 * Handles communication between IPOS-CA and IPOS-PU databases.
 * Acts as the internal API to fetch portal orders and update their status.
 *
 * Requirement Fulfilled:
 * - Receive online sales (details + delivery address)
 * - Maintain online sale status ("PAID", "DISPATCHED", "DELIVERED")
 */
public class PortalAPI {

    private static PortalAPI instance;

    private PortalAPI() {}

    public static synchronized PortalAPI getInstance() {
        if (instance == null) {
            instance = new PortalAPI();
        }
        return instance;
    }

    /**
     * Fetches all online orders from the IPOS-PU database.
     * Assumes a table named 'portal_orders' exists in ipos_pu.
     *
     * Expected Schema in ipos_pu.portal_orders:
     * id (INT), customer_email (VARCHAR), customer_name (VARCHAR),
     * delivery_address (VARCHAR), delivery_postcode (VARCHAR),
     * total_amount (DOUBLE), order_status (VARCHAR), order_date (DATE),
     * payment_method (VARCHAR)
     *
     * NOTE: tracking_link REMOVED as per sample data constraints.
     */
    public ObservableList<OnlineOrder> getPortalOrders() {
        ObservableList<OnlineOrder> orders = FXCollections.observableArrayList();

        String sql = "SELECT id, customer_email, customer_name, delivery_address, delivery_postcode, " +
                "total_amount, order_status, order_date, payment_method " +
                "FROM portal_orders ORDER BY order_date DESC";

        try (Connection conn = DatabaseConnector.getPUConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // PASSING 9 ARGUMENTS TO MATCH THE UPDATED CONSTRUCTOR
                orders.add(new OnlineOrder(
                        rs.getInt("id"),                    // 1. ID
                        rs.getString("customer_email"),     // 2. Email
                        rs.getString("customer_name"),      // 3. Name
                        rs.getString("delivery_address"),   // 4. Address
                        rs.getString("delivery_postcode"),  // 5. Postcode
                        rs.getDouble("total_amount"),       // 6. Amount
                        rs.getString("order_status"),       // 7. Status
                        rs.getDate("order_date").toLocalDate(), // 8. Date
                        rs.getString("payment_method")      // 9. Payment Method
                ));
            }
            System.out.println("✅ Loaded " + orders.size() + " orders from IPOS-PU");

        } catch (SQLException e) {
            System.err.println("❌ Error fetching PU orders: " + e.getMessage());
            e.printStackTrace();
        }
        return orders;
    }

    /**
     * Updates the status of an order in the PU database.
     * Used when CA dispatches an order.
     */
    public boolean updateOrderStatus(int orderId, String newStatus) {
        String sql = "UPDATE portal_orders SET order_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnector.getPUConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Order #" + orderId + " status updated to: " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating order status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Dispatches an order: Sets status to 'DISPATCHED'.
     * No tracking link generation needed for this demo.
     */
    public boolean dispatchOrder(int orderId) {
        // Simply update the status to DISPATCHED
        return updateOrderStatus(orderId, "DISPATCHED");
    }
}