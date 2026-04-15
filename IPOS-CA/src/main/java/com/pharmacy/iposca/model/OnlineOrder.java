package com.pharmacy.iposca.model;

import java.time.LocalDate;

/**
 * Model class representing an order received from the IPOS-PU Portal.
 */
public class OnlineOrder {
    private final int orderId;
    private final String customerEmail;
    private final String customerName;
    private final String deliveryAddress;
    private final String postcode;
    private final double totalAmount;
    private String status;// Mutable for updates
    private final LocalDate orderDate;
    private final String paymentMethod;

    /**
     * Constructor matching the 9 columns from database.
     */
    public OnlineOrder(int orderId, String email, String name, String address,
                       String postcode, double amount, String status,
                       LocalDate date, String paymentMethod) {
        this.orderId = orderId;
        this.customerEmail = email;
        this.customerName = name;
        this.deliveryAddress = address;
        this.postcode = postcode;
        this.totalAmount = amount;
        this.status = status;
        this.orderDate = date;
        this.paymentMethod = paymentMethod;
    }

    //Getter Methods
    public int getOrderId() {
        return orderId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getPostcode() {
        return postcode;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    //Setters, only for updating status
    public void setStatus(String status) {
        this.status = status;
    }
}