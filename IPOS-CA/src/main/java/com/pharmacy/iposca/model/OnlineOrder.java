package com.pharmacy.iposca.model;

import java.time.LocalDate;

/**
 * Model representing an order received from the IPOS-PU Portal.
 * Maps directly to the 'online_sales' table in the 'ipos_pu' database.
 *
 * Columns mapped:
 * - id
 * - sale_reference (mapped to customerEmail for display/search)
 * - customer_type (mapped to customerName for display)
 * - delivery_address
 * - delivery_town (combined with address in UI logic)
 * - delivery_postcode
 * - total_amount
 * - order_status
 * - sale_date
 * - payment_method
 */
public class OnlineOrder {
    private final int orderId;
    private final String customerEmail;      // Mapped from 'sale_reference' or 'customer_id'
    private final String customerName;       // Mapped from 'customer_type'
    private final String deliveryAddress;    // Combined Address + Town
    private final String postcode;
    private final double totalAmount;
    private String status;                   // Mutable for updates
    private final LocalDate orderDate;       // Mapped from 'sale_date'
    private final String paymentMethod;

    /**
     * Constructor matching the 9 columns selected from the database.
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

    // --- Getters ---

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

    // --- Setters (Only Status needs to be mutable for the "Mark as Delivered" feature) ---

    public void setStatus(String status) {
        this.status = status;
    }
}