package com.pharmacy.iposca.api;

public interface IPortalAPI {
    void decrementStock(int itemID, int qty);

    boolean sendEmail(String recipient, String subject, String message);

    String getDeliveryStatus(String messageID);
}