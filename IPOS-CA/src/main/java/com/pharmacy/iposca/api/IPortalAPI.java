package com.pharmacy.iposca.api;

/**
 * Interface for the Online Portal (IPOS-PU).
 * This allows the Merchant's system (IPOS-CA) to sync stock and send emails.
 */
public interface IPortalAPI {

    /**
     * Requirement: Online purchases should be deducted from the merchant's stock.
     * @param itemID The unique ID of the pharmaceutical item.
     * @param qty The quantity sold online to be removed from local inventory.
     */
    void decrementStock(int itemID, int qty);

    /**
     * Requirement: Integration with email services (e.g., Google SMTP).
     * @param recipient The customer's email address.
     * @param subject The subject line of the notification.
     * @param message The body text of the email.
     * @return true if the email was successfully handed off to the mail server.
     */
    boolean sendEmail(String recipient, String subject, String message);

    /**
     * Requirement: The platform should maintain and provide the status of the order.
     * @param messageID The unique identifier for the communication or order.
     * @return The current delivery status (e.g., "Dispatched", "Delivered").
     */
    String getDeliveryStatus(String messageID);
}