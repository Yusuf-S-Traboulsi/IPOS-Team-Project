package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.api.IPortalAPI;
import com.pharmacy.iposca.model.Customer;
import java.util.HashMap;
import java.util.Map;

/**
 * Fully updated controller for IPOS-CA-Templates and Communications.
 * Manages merchant identity and editable message templates.
 */
public class NotificationController {
    private IPortalAPI portal;

    // --- Merchant Identity Details ---
    private String pharmacyName = "Your Pharmacy Name";
    private String pharmacyAddress = "123 Pharmacy Street, London";
    private String pharmacyEmail = "billing@yourpharmacy.com";

    // --- Template Storage ---
    private Map<String, String> templates = new HashMap<>();

    public NotificationController() {
        // Default Template A: 1st Reminder (Friendly nudge)
        templates.put("REMINDER_1", "Dear {name}, this is a friendly reminder that you have an outstanding balance of £{debt}. Please settle this soon.");

        // Default Template B: 2nd Reminder (Final Notice)
        templates.put("REMINDER_2", "URGENT: {name}, your account is now IN DEFAULT. Total debt: £{debt}. Please contact {pharmacyName} at {pharmacyEmail} immediately.");

        // Default Template C: Receipt/Invoice
        templates.put("RECEIPT", "{pharmacyName}\n{pharmacyAddress}\n------------------\nTotal Sale: £{total}\nThank you for your custom.");
    }

    /**
     * Requirement: Allow Manager to edit templates.
     */
    public void updateTemplate(String key, String newContent) {
        if (templates.containsKey(key)) {
            templates.put(key, newContent);
            System.out.println("Template " + key + " updated successfully.");
        }
    }

    /**
     * Requirement: Update Merchant Identity details.
     */
    public void updateMerchantIdentity(String name, String address, String email) {
        this.pharmacyName = name;
        this.pharmacyAddress = address;
        this.pharmacyEmail = email;
    }

    /**
     * Requirement: Send reminders using the IPortalAPI signature.
     * Injects Merchant Identity and Customer data into the templates.
     */
    public void sendReminder(Customer customer, String templateKey) {
        if (portal != null && templates.containsKey(templateKey)) {
            String message = templates.get(templateKey)
                    .replace("{name}", customer.getName())
                    .replace("{debt}", String.valueOf(customer.getCurrentDebt()))
                    .replace("{pharmacyName}", pharmacyName)
                    .replace("{pharmacyEmail}", pharmacyEmail);

            // Exactly matches: sendEmail(String, String, String) : boolean
            boolean success = portal.sendEmail("customer_email_placeholder", "Account Notification", message);

            if (success) {
                System.out.println("Template " + templateKey + " sent to " + customer.getName());
            }
        }
    }

    /**
     * Requirement: Generate formatted text for Receipts/Invoices.
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