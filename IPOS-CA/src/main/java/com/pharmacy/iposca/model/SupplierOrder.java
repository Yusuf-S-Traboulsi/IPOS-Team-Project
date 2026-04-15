package com.pharmacy.iposca.model;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * Class representing a supplier order.
 */
public class SupplierOrder {
    private final StringProperty orderId = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> orderDate = new SimpleObjectProperty<>();
    private final DoubleProperty totalAmount = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty("Ordered");
    private final ObjectProperty<LocalDate> dispatchedDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> deliveredDate = new SimpleObjectProperty<>();
    private final StringProperty paymentStatus = new SimpleStringProperty("Pending");
    private final ObjectProperty<LocalDate> paidDate = new SimpleObjectProperty<>();

    public SupplierOrder(String orderId, LocalDate orderDate, double totalAmount, String status) {
        this.orderId.set(orderId);
        this.orderDate.set(orderDate);
        this.totalAmount.set(totalAmount);
        this.status.set(status);
    }

    //Getters and setters
    public String getOrderId() { return orderId.get(); }
    public LocalDate getOrderDate() { return orderDate.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }

    public LocalDate getDispatchedDate() { return dispatchedDate.get(); }
    public void setDispatchedDate(LocalDate date) { this.dispatchedDate.set(date); }

    public LocalDate getDeliveredDate() { return deliveredDate.get(); }
    public void setDeliveredDate(LocalDate date) { this.deliveredDate.set(date); }

    public String getPaymentStatus() { return paymentStatus.get(); }
    public void setPaymentStatus(String status) { this.paymentStatus.set(status); }

    public LocalDate getPaidDate() { return paidDate.get(); }
    public void setPaidDate(LocalDate date) { this.paidDate.set(date); }
}