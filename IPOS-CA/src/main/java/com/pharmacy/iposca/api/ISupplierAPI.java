package com.pharmacy.iposca.api;

import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;

/**
 * IPOS-SA Supplier API Interface
 * This interface defines the contract for communicating with the IPOS-SA
 * Supplier Ordering System via REST API.
 */
public interface ISupplierAPI {

    /**
     * Get full supplier product catalogue
     * API Endpoint: GET /api/catalogue
     * @return Array of catalogue items
     */
    SupplierCatalogueItem[] getProductCatalogue();

    /**
     * Submit a new purchase order to IPOS-SA
     * API Endpoint: POST /api/orders
     * @param order Order object containing items
     * @return true if order submitted successfully
     */
    boolean submitPurchaseOrder(SupplierOrder order);

    /**
     * Get delivery status for an order
     * API Endpoint: GET /api/orders
     * @param orderID Order ID to check
     * @return Status string (Ordered, Dispatched, Delivered)
     */
    String getDeliveryStatus(String orderID);

    /**
     * Get all outstanding invoices from IPOS-SA
     * API Endpoint: GET /api/invoices
     * @return Array of invoices
     */
    com.pharmacy.iposca.controller.SupplierController.Invoice[] getOutstandingInvoices();

    /**
     * Get outstanding balance from IPOS-SA
     * API Endpoint: GET /api/balance
     * @return Outstanding balance amount
     */
    double getOutstandingBalance();

    /**
     * Mark an order as delivered
     * API Endpoint: PUT /api/orders/{orderID}/delivered
     * @param orderID Order ID to mark as delivered
     * @return true if successful
     */
    boolean markOrderAsDelivered(String orderID);

    /**
     * Mark an order as paid
     * API Endpoint: PUT /api/orders/{orderID}/paid
     * @param orderID Order ID to mark as paid
     * @return true if successful
     */
    boolean markOrderAsPaid(String orderID);

    /**
     * Authenticate with IPOS-SA supplier portal
     * API Endpoint: POST /api/auth
     * @param username Username
     * @param password Password
     * @return true if authentication successful
     */
    boolean authenticate(String username, String password);
}