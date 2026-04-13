package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.OnlineOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

/**
 * Handles communication between IPOS-CA and IPOS-PU databases.
 * Fulfills requirement: Receive online sales & Maintain status.
 *
 * UPDATED TO MATCH: ipos_pu.online_sales schema
 */
public class PUCommunicationController {

    private static PUCommunicationController instance;

    private PUCommunicationController() {}

    public static synchronized PUCommunicationController getInstance() {
        if (instance == null) instance = new PUCommunicationController();
        return instance;
    }

    /**
     * Fetches all orders from the IPOS-PU database.
     * Table: online_sales
     * Columns: id, sale_reference, customer_type, customer_id, sale_date, total_amount, order_status, delivery_address, delivery_town, delivery_postcode, payment_method
     */
    public ObservableList<OnlineOrder> getPortalOrders() {
        ObservableList<OnlineOrder> orders = FXCollections.observableArrayList();

        // CORRECTED SQL: Matches 'online_sales' table schema exactly
        String sql = "SELECT id, sale_reference, customer_type, customer_id, sale_date, " +
                "subtotal, discount_amount, delivery_charge, total_amount, " +
                "payment_method, payment_status, delivery_address, delivery_town, " +
                "delivery_postcode, order_status, tracking_link " +
                "FROM online_sales ORDER BY sale_date DESC";

        try (Connection conn = DatabaseConnector.getPUConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Combine address fields for display in the UI
                String fullAddress = rs.getString("delivery_address");
                String town = rs.getString("delivery_town");
                String postcode = rs.getString("delivery_postcode");

                if (town != null && !town.isEmpty()) {
                    fullAddress += ", " + town;
                }
                if (postcode != null && !postcode.isEmpty()) {
                    fullAddress += ", " + postcode;
                }

                // Map customer_type (ENUM) to a readable name for the UI
                String customerDisplay = rs.getString("customer_type");
                if ("NON_COMMERCIAL".equals(customerDisplay)) {
                    customerDisplay = "Non-Commercial (ID: " + rs.getInt("customer_id") + ")";
                } else if ("COMMERCIAL".equals(customerDisplay)) {
                    customerDisplay = "Commercial (ID: " + rs.getInt("customer_id") + ")";
                }

                // PASSING 9 ARGUMENTS TO MATCH THE UPDATED OnlineOrder CONSTRUCTOR
                orders.add(new OnlineOrder(
                        rs.getInt("id"),                        // 1. Order ID
                        rs.getString("sale_reference"),         // 2. Email/Ref (using sale_reference)
                        customerDisplay,                        // 3. Name (using mapped customer_type)
                        fullAddress,                            // 4. Address (combined)
                        rs.getString("delivery_postcode"),      // 5. Postcode
                        rs.getDouble("total_amount"),           // 6. Amount
                        rs.getString("order_status"),           // 7. Status
                        rs.getTimestamp("sale_date").toLocalDateTime().toLocalDate(), // 8. Date (handle DATETIME)
                        rs.getString("payment_method")          // 9. Payment Method
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
     * Used when CA dispatches/delivers an order.
     */
    public boolean updateOrderStatus(int orderId, String newStatus) {
        // CORRECTED SQL: Matches 'online_sales' table
        String sql = "UPDATE online_sales SET order_status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnector.getPUConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Order #" + orderId + " updated to: " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating order: " + e.getMessage());
        }
        return false;
    }

    /**
     * Marks an order as Delivered and generates a mock tracking link.
     */
    public boolean markAsDelivered(int orderId) {
        String tracking = "TRK-" + System.currentTimeMillis();

        // First update status to DELIVERED
        if (updateOrderStatus(orderId, "Delivered")) {
            // Then update tracking link (column exists in your schema)
            String sqlTrack = "UPDATE online_sales SET tracking_link = ? WHERE id = ?";
            try (Connection conn = DatabaseConnector.getPUConnection();
                 PreparedStatement stmt = conn.prepareStatement(sqlTrack)) {
                stmt.setString(1, tracking);
                stmt.setInt(2, orderId);
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}