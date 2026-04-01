package com.pharmacy.iposca.api;

import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;
import com.pharmacy.iposca.model.Invoice;

/**
 * IPOS-SA Supplier API Interface
 * This interface defines the contract for communicating with the IPOS-SA
 * Supplier Ordering System via REST API.
 */
public interface ISupplierAPI {

    /**
     * Get full supplier product catalogue
     * API Endpoint: GET /api/catalogue
     */
    SupplierCatalogueItem[] getProductCatalogue();

    /**
     * Submit a new purchase order to IPOS-SA
     * API Endpoint: POST /api/orders
     */
    boolean submitPurchaseOrder(SupplierOrder order);

    /**
     * Get delivery status for an order
     * API Endpoint: GET /api/orders/{orderID}/status
     */
    String getDeliveryStatus(String orderID);

    /**
     * Get all outstanding invoices from IPOS-SA
     * API Endpoint: GET /api/invoices
     */
    Invoice[] getOutstandingInvoices();

    /**
     * Get outstanding balance from IPOS-SA
     * API Endpoint: GET /api/balance
     */
    double getOutstandingBalance();

    /**
     * Mark an order as delivered
     * API Endpoint: PUT /api/orders/{orderID}/delivered
     */
    boolean markOrderAsDelivered(String orderID);

    /**
     * Mark an order as paid
     * API Endpoint: PUT /api/orders/{orderID}/paid
     */
    boolean markOrderAsPaid(String orderID);

    /**
     * Authenticate with IPOS-SA supplier portal
     * API Endpoint: POST /api/auth
     */
    boolean authenticate(String username, String password);
}