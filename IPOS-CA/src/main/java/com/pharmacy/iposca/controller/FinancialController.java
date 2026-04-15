package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.model.Customer;
import java.time.LocalDate;

public class FinancialController {

    public void runMonthlyStatementProcess(Customer customer) {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();

        // Statements only allowed in the regulatory window (5th to 15th)
        if (day >= 5 && day <= 15) {
            if (customer.getCurrentDebt() > 0 && "no_need".equals(customer.getStatus1stReminder())) {
                customer.setStatus1stReminder("due");
            }
        }
    }

    public void processReminders(Customer customer) {
        LocalDate today = LocalDate.now();

        // 1st Reminder Logic
        if ("due".equals(customer.getStatus1stReminder())) {
            customer.setStatus1stReminder("sent");
            customer.setDate1stReminder(today);
            customer.setAccountStatus("Suspended");

            //Queue 2nd reminder
            customer.setStatus2ndReminder("due");
            customer.setDate2ndReminder(today.plusDays(15));
        }

        // 2nd Reminder Logic
        if ("due".equals(customer.getStatus2ndReminder())) {
            if (customer.getDate2ndReminder() != null && !today.isBefore(customer.getDate2ndReminder())) {
                customer.setStatus2ndReminder("sent");
                customer.setAccountStatus("In Default");
            }
        }
    }

    public void processFullPayment(Customer customer, double paymentAmount) {
        double remainingDebt = customer.getCurrentDebt() - paymentAmount;
        customer.setCurrentDebt(Math.max(0, remainingDebt));

        // If debt is fully cleared, reset flags to return to normal
        if (customer.getCurrentDebt() <= 0) {
            if (!"In Default".equals(customer.getAccountStatus())) {
                customer.setStatus1stReminder("no_need");
                customer.setStatus2ndReminder("no_need");
                customer.setAccountStatus("Normal");
            }
        }
    }
}