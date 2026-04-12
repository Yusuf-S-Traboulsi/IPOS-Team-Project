package com.pharmacy.iposca.model;

import javafx.beans.property.*;

import java.time.LocalDate;

public class OnlineSale {
    private final IntegerProperty orderId = new SimpleIntegerProperty();
    private final StringProperty customerName = new SimpleStringProperty();
    private final StringProperty deliveryAddress = new SimpleStringProperty();
    private final DoubleProperty totalAmount = new SimpleDoubleProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> orderDate = new SimpleObjectProperty<>();

    public static final String ACCEPTED = "Accepted";
    public static final String RECEIVED = "Received";
    public static final String READY_FOR_SHIPMENT = "Ready for Shipment";
    public static final String SHIPPED = "Shipped";
    public static final String DELIVERED = "Delivered";

    public OnlineSale(int orderId, String customerName, String deliveryAddress, double totalAmount, String status, LocalDate orderDate) {
        this.orderId.set(orderId);
        this.customerName.set(customerName);
        this.deliveryAddress.set(deliveryAddress);
        this.totalAmount.set(totalAmount);
        this.status.set(status);
    }

    //Getters
    public int getOrderId() { return orderId.get(); }
    public String getCustomerName() { return customerName.get(); }
    public String getDeliveryAddress() { return deliveryAddress.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public String getStatus() { return status.get(); }
    public LocalDate getOrderDate() { return orderDate.get(); }

    //Setters
    public void setOrderId(int orderId) { this.orderId.set(orderId); }
    public void setCustomerName(String customerName) { this.customerName.set(customerName); }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress.set(deliveryAddress); }
    public void setTotalAmount(double totalAmount) { this.totalAmount.set(totalAmount); }
    public void setStatus(String status) { this.status.set(status); }
    public void setOrderDate(LocalDate orderDate) { this.orderDate.set(orderDate); }

    public IntegerProperty orderIdProperty() { return orderId; }
    public StringProperty customerNameProperty() { return customerName; }
    public StringProperty deliveryAddressProperty() { return deliveryAddress; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public StringProperty statusProperty() { return status; }
    public ObjectProperty<LocalDate> orderDateProperty() { return orderDate; }

    public boolean advanceStatus() {
        switch (getStatus()) {
            case RECEIVED:
                setStatus(ACCEPTED);
                return true;
            case ACCEPTED:
                setStatus(READY_FOR_SHIPMENT);
                return true;
            case READY_FOR_SHIPMENT:
                setStatus(SHIPPED);
                return true;
            case SHIPPED:
                setStatus(DELIVERED);
                return true;
            case DELIVERED:
            default:
                return false;
        }
    }
}