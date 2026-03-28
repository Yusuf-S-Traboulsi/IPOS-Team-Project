package com.pharmacy.iposca.model;

import javafx.beans.property.*;

public class Invoice {

    private final StringProperty customerName = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty date = new SimpleStringProperty();

    public Invoice(String name, double amount, String date) {
        this.customerName.set(name);
        this.amount.set(amount);
        this.date.set(date);
    }

    public String getCustomerName() { return customerName.get(); }
    public double getAmount() { return amount.get(); }
    public String getDate() { return date.get(); }
}