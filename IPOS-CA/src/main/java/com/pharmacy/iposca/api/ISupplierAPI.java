package com.pharmacy.iposca.api;
import com.pharmacy.iposca.model.SupplierCatalogueItem;
import com.pharmacy.iposca.model.SupplierOrder;

public interface ISupplierAPI {
    SupplierCatalogueItem[] getProductCatalogue();

    boolean submitPurchaseOrder(SupplierOrder order);

    String getDeliveryStatus(String orderID);

    com.pharmacy.iposca.controller.SupplierController.Invoice[] getOutstandingInvoices();

    double getOutstandingBalance();

    boolean markOrderAsDelivered(String orderID);

    boolean markOrderAsPaid(String orderID);

    boolean authenticate(String username, String password);
}