package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.model.OnlineSale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;

public class PUCommunicationController {
    private static PUCommunicationController instance;
    private final ObservableList<OnlineSale> onlineSales = FXCollections.observableArrayList();
    private int nextOrderId = 3001;


    private void loadMockOnlineSales() { onlineSales.addAll(
            new OnlineSale(3001, "John Smith", "12 Oak Street, Ashford, TN23 1AA", 45.00, OnlineSale.RECEIVED, LocalDate.now().minusDays(2)),
            new OnlineSale(3002, "Jane Doe", "34 Pine Avenue, Ashford, TN24 8BB", 32.40, OnlineSale.ACCEPTED, LocalDate.now().minusDays(1)),
            new OnlineSale(3003, "Bob Johnson", "56 Cedar Road, Canterbury, CT1 2AB", 96.00, OnlineSale.SHIPPED, LocalDate.now())
    );
        nextOrderId = 3004;
    }

    private PUCommunicationController() {
        loadMockOnlineSales();
    }

    public static synchronized PUCommunicationController getInstance() {
        if (instance == null) {
            instance = new PUCommunicationController();
        }
        return instance;
    }

    public ObservableList<OnlineSale> getOnlineSales() {
        return onlineSales;
    }

    public boolean receiveOnlineSale(String customerName, String deliveryAddress, double totalAmount) {
        if (customerName == null || deliveryAddress == null || totalAmount <= 0) {
            return false;
        }
        OnlineSale sale = new OnlineSale(nextOrderId++, customerName, deliveryAddress, totalAmount, OnlineSale.RECEIVED, LocalDate.now());

        onlineSales.add(sale);
        System.out.println("Received online sale: " + sale.getOrderId());
        return true;
    }

    public boolean updateOrderStatus(OnlineSale sale, String newStatus) {
        if (sale == null || newStatus == null) {
            return false;
        }
        sale.setStatus(newStatus);
        System.out.println("Status updated for sale " + sale.getOrderId() + " to " + newStatus);
        return true;
    }

    public boolean advanceOrderStatus(OnlineSale sale) {
        if (sale == null)
            return false;

        boolean advanced = sale.advanceStatus();
        if (advanced)
            System.out.println("Order: " + sale.getOrderId() + " marked as " + sale.getStatus());
        return advanced;
    }

    public ObservableList<OnlineSale> getOrderByStatus(String status) {
        ObservableList<OnlineSale> filteredSales = FXCollections.observableArrayList();
        for (OnlineSale sale : onlineSales) {
            if (sale.getStatus().equalsIgnoreCase(status)) {
                filteredSales.add(sale);
            }
        }
        return filteredSales;
    }

}
