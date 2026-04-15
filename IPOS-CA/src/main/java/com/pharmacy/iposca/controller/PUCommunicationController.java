package com.pharmacy.iposca.controller;
import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.OnlineOrder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;

public class PUCommunicationController {
    private static PUCommunicationController instance;

    private PUCommunicationController() {}

    public static synchronized PUCommunicationController getInstance() {
        if (instance == null) {
            instance = new PUCommunicationController();
        }
        return instance;
    }

    public ObservableList<OnlineOrder> getPortalOrders() {
        ObservableList<OnlineOrder> orders = FXCollections.observableArrayList();

        String sql = "SELECT o.OrderID, o.EmailAddress, o.Address, o.OrderStatus, o.CreatedAt, " +
                "o.Descriptions AS OrderDesc, o.DeliveryType, " +
                "COALESCE(SUM(oi.Quantity * oi.UnitPrice), 0) AS TotalAmount " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.OrderID = oi.OrderID " +
                "GROUP BY o.OrderID, o.EmailAddress, o.Address, o.OrderStatus, o.CreatedAt, o.Descriptions, o.DeliveryType " +
                "ORDER BY o.CreatedAt DESC";

        try (Connection conn = DatabaseConnector.getPUConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String orderId = rs.getString("OrderID");
                String email = rs.getString("EmailAddress");
                String address = rs.getString("Address");
                String status = rs.getString("OrderStatus");
                LocalDate date = rs.getTimestamp("CreatedAt").toLocalDateTime().toLocalDate();
                String description = rs.getString("OrderDesc");
                double totalAmount = rs.getDouble("TotalAmount");
                String paymentMethod = rs.getString("DeliveryType") != null ? rs.getString("DeliveryType") : "N/A";

                orders.add(new OnlineOrder(orderId, email, address, status, date, description, totalAmount, paymentMethod));
            }
            System.out.println("Loaded " + orders.size() + " orders from IPOS-PU");
        } catch (SQLException e) {
            System.err.println("Error fetching PU orders: " + e.getMessage());
            e.printStackTrace();
        }
        return orders;
    }

    public boolean updateOrderStatus(String orderId, String newStatus) {
        String sql = "UPDATE orders SET OrderStatus = ? WHERE OrderID = ?";
        try (Connection conn = DatabaseConnector.getPUConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, orderId);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Order: " + orderId + " updated to: " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating order: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean markAsDelivered(String orderId) {
        return updateOrderStatus(orderId, "Delivered");
    }
}