package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.api.IPortalAPI;
import com.pharmacy.iposca.model.Customer;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a controller class for IPOS-CA-Templates and Communications.
 * Manages merchant identity and editable message templates.
 */
public class NotificationController {
    private IPortalAPI portal;

    //Merchant Identity Details
    private String pharmacyName = "CosyMed";
    private String pharmacyAddress = "20 High ST, London";
    private String pharmacyEmail = "cosymed@mail.com";

    //Template Storage
    private Map<String, String> templates = new HashMap<>();

    public NotificationController() {
        // Default Template for 1st Reminder
        templates.put("REMINDER_1", "Dear {name}, this is a friendly reminder that you have an outstanding balance of £{debt}. Please settle this soon.");

        // Default Template for 2nd Reminder
        templates.put("REMINDER_2", "URGENT: {name}, your account is now IN DEFAULT. Total debt: £{debt}. Please contact {pharmacyName} at {pharmacyEmail} immediately.");

        // Default Template for Receipts/Invoices
        templates.put("RECEIPT", "{pharmacyName}\n{pharmacyAddress}\n------------------\nTotal Sale: £{total}\nThank you for your custom.");
    }

    /**
     * Method to update templates, Manager can edit templates.
     */
    public void updateTemplate(String key, String newContent) {
        if (templates.containsKey(key)) {
            templates.put(key, newContent);
            System.out.println("Template " + key + " updated successfully.");
        }
    }

    /**
     * Method to update merchant identity details.
     */
    public void updateMerchantIdentity(String name, String address, String email) {
        this.pharmacyName = name;
        this.pharmacyAddress = address;
        this.pharmacyEmail = email;
    }

    /**
     * Sends reminder email to customer when portal and template are available
     */
    public void sendReminder(Customer customer, String templateKey) {
        if (portal != null && templates.containsKey(templateKey)) {
            String message = templates.get(templateKey)
                    .replace("{name}", customer.getName())
                    .replace("{debt}", String.valueOf(customer.getCurrentDebt()))
                    .replace("{pharmacyName}", pharmacyName)
                    .replace("{pharmacyEmail}", pharmacyEmail);

            //sends email and checks if it was successful
            boolean success = portal.sendEmail("customer_email_placeholder", "Account Notification", message);

            if (success) {
                System.out.println("Template " + templateKey + " sent to " + customer.getName());
            }
        }
    }

    /**
     * Method to generate formatted text for Receipts/Invoices.
     */
    public String generateReceiptText(double total) {
        return templates.get("RECEIPT")
                .replace("{pharmacyName}", pharmacyName)
                .replace("{pharmacyAddress}", pharmacyAddress)
                .replace("{total}", String.valueOf(total));
    }

    public void setPortal(IPortalAPI portal) {
        this.portal = portal;
    }
}