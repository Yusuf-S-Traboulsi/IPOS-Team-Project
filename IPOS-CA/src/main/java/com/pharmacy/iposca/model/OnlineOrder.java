package com.pharmacy.iposca.model;
import java.time.LocalDate;

public class OnlineOrder {
    private final String orderId;
    private final String customerEmail;
    private final String deliveryAddress;
    private String status;
    private final LocalDate orderDate;
    private final String orderDescription;
    private final double totalAmount;
    private final String paymentMethod;
    public OnlineOrder(String orderId, String customerEmail, String deliveryAddress, String status,
                       LocalDate orderDate, String orderDescription, double totalAmount, String paymentMethod) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.orderDate = orderDate;
        this.orderDescription = orderDescription;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
    }

    public String getOrderId() { return orderId; }
    public String getCustomerEmail() { return customerEmail; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOrderDate() { return orderDate; }
    public String getOrderDescription() { return orderDescription; }
    public double getTotalAmount() { return totalAmount; }
    public String getPaymentMethod() { return paymentMethod; }
}