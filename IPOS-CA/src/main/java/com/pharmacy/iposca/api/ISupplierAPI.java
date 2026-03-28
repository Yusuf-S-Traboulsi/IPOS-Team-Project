package com.pharmacy.iposca.api;
import com.pharmacy.iposca.model.Order;
import com.pharmacy.iposca.model.Invoice;

public interface ISupplierAPI {
    // Note: Use Product[] if ProductList isn't a defined collection
    Object[] getProductCatalogue();
    boolean submitPurchaseOrder(Order order);
    String getDeliveryStatus(String orderID);
    Invoice[] getOutstandingInvoices();
}