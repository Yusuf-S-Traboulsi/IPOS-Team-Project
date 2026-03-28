package com.pharmacy.iposca.model;

import java.time.LocalDate;

/**
 * Sale Record model for reporting and database logging
 */
public class SaleRecord {
    private final int id;
    private final String customerName;
    private final double amount;
    private final LocalDate date;
    private final String paymentType;

    // Discount fields (for detailed reporting)
    private final double discountRate;
    private final double discountAmount;
    private final double totalBeforeDiscount;
    private final double totalAfterDiscount;
    private final double vatAmount;
    private final boolean isAccountHolder;

    /**
     * Simple constructor (for basic sales log)
     */
    public SaleRecord(int id, String customerName, double amount, LocalDate date, String paymentType) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.date = date;
        this.paymentType = paymentType;
        this.discountRate = 0.0;
        this.discountAmount = 0.0;
        this.totalBeforeDiscount = amount;
        this.totalAfterDiscount = amount;
        this.vatAmount = amount * 0.20; // Default VAT
        this.isAccountHolder = false;
    }

    /**
     * Full constructor (for detailed sales with discount info)
     */
    public SaleRecord(int id, String customerName, double amount, LocalDate date,
                      String paymentType, double discountRate, double discountAmount,
                      double totalBeforeDiscount, double totalAfterDiscount,
                      double vatAmount, boolean isAccountHolder) {
        this.id = id;
        this.customerName = customerName;
        this.amount = amount;
        this.date = date;
        this.paymentType = paymentType;
        this.discountRate = discountRate;
        this.discountAmount = discountAmount;
        this.totalBeforeDiscount = totalBeforeDiscount;
        this.totalAfterDiscount = totalAfterDiscount;
        this.vatAmount = vatAmount;
        this.isAccountHolder = isAccountHolder;
    }

    // Getters
    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getPaymentType() { return paymentType; }
    public double getDiscountRate() { return discountRate; }
    public double getDiscountAmount() { return discountAmount; }
    public double getTotalBeforeDiscount() { return totalBeforeDiscount; }
    public double getTotalAfterDiscount() { return totalAfterDiscount; }
    public double getVatAmount() { return vatAmount; }
    public boolean isAccountHolder() { return isAccountHolder; }
}