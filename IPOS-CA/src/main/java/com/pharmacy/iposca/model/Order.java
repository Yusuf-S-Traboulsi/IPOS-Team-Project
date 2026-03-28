package com.pharmacy.iposca.model;

import javafx.beans.property.*;

public class Order {
    private final IntegerProperty orderId = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final DoubleProperty totalCost = new SimpleDoubleProperty();

    public Order(int id, double cost, String status) {
        this.orderId.set(id);
        this.totalCost.set(cost);
        this.status.set(status);
    }

    public int getOrderId() { return orderId.get(); }
}