package com.pharmacy.iposca.model;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * Class for representing a supplier order.
 * */
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

    // Getters and Setters
    public String getOrderId() { return orderId.get(); }
    public void setOrderId(String orderId) { this.orderId.set(orderId); }
    public StringProperty orderIdProperty() { return orderId; }

    public LocalDate getOrderDate() { return orderDate.get(); }
    public void setOrderDate(LocalDate date) { this.orderDate.set(date); }
    public ObjectProperty<LocalDate> orderDateProperty() { return orderDate; }

    public double getTotalAmount() { return totalAmount.get(); }
    public void setTotalAmount(double amount) { this.totalAmount.set(amount); }
    public DoubleProperty totalAmountProperty() { return totalAmount; }

    public String getStatus() { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    public LocalDate getDispatchedDate() { return dispatchedDate.get(); }
    public void setDispatchedDate(LocalDate date) { this.dispatchedDate.set(date); }
    public ObjectProperty<LocalDate> dispatchedDateProperty() { return dispatchedDate; }

    public LocalDate getDeliveredDate() { return deliveredDate.get(); }
    public void setDeliveredDate(LocalDate date) { this.deliveredDate.set(date); }
    public ObjectProperty<LocalDate> deliveredDateProperty() { return deliveredDate; }

    public String getPaymentStatus() { return paymentStatus.get(); }
    public void setPaymentStatus(String status) { this.paymentStatus.set(status); }
    public StringProperty paymentStatusProperty() { return paymentStatus; }

    public LocalDate getPaidDate() { return paidDate.get(); }
    public void setPaidDate(LocalDate date) { this.paidDate.set(date); }
    public ObjectProperty<LocalDate> paidDateProperty() { return paidDate; }
}