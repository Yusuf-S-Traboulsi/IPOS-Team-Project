package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Customer Controller - Full MySQL Database Integration
 */
public class CustomerController {
    private static CustomerController instance;
    private final ObservableList<Customer> customerData = FXCollections.observableArrayList();
    private final ObservableList<PaymentRecord> paymentHistory = FXCollections.observableArrayList();
    private int invoiceCounter = 197362;

    private CustomerController() {
        loadCustomersFromDatabase();
        loadPaymentHistory();
    }

    public static synchronized CustomerController getInstance() {
        if (instance == null) {
            instance = new CustomerController();
        }
        return instance;
    }

    private void loadCustomersFromDatabase() {
        String sql = "SELECT * FROM customers ORDER BY id";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            customerData.clear();
            while (rs.next()) {
                Customer c = new Customer(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("town"),
                        rs.getString("postcode"),
                        rs.getDouble("credit_limit"),
                        rs.getDouble("current_debt")
                );
                c.setAccountStatus(rs.getString("account_status"));
                c.setStatus1stReminder(rs.getString("status_1st_reminder"));
                c.setStatus2ndReminder(rs.getString("status_2nd_reminder"));
                c.setDiscountPlanType(rs.getString("discount_plan_type"));
                c.setDiscountRate(rs.getDouble("discount_rate"));
                c.setMonthlyPurchaseTotal(rs.getDouble("monthly_purchase_total"));

                try {
                    Date d1 = rs.getDate("date_1st_reminder");
                    Date d2 = rs.getDate("date_2nd_reminder");
                    Date d3 = rs.getDate("oldest_debt_date");

                    c.setDate1stReminder(d1 != null ? d1.toLocalDate() : null);
                    c.setDate2ndReminder(d2 != null ? d2.toLocalDate() : null);
                    c.setOldestDebtDate(d3 != null ? d3.toLocalDate() : null);
                } catch (Exception e) {
                    // Date fields can be null
                }

                customerData.add(c);
            }

            System.out.println("✅ Loaded " + customerData.size() + " customers from database");
        } catch (SQLException e) {
            System.err.println("❌ Error loading customers: " + e.getMessage());
            e.printStackTrace();
            loadMockCustomers();
        }
    }

    private void loadPaymentHistory() {
        String sql = "SELECT * FROM customer_payments ORDER BY payment_date DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            paymentHistory.clear();
            while (rs.next()) {
                paymentHistory.add(new PaymentRecord(
                        rs.getInt("id"),
                        rs.getInt("customer_id"),
                        rs.getDate("payment_date").toLocalDate(),
                        rs.getDouble("amount"),
                        rs.getString("payment_type"),
                        rs.getString("payment_details"),
                        rs.getString("reference")
                ));
            }

            System.out.println("✅ Loaded " + paymentHistory.size() + " payment records");
        } catch (SQLException e) {
            System.err.println("⚠️ Payment history table may not exist: " + e.getMessage());
        }
    }

    private void loadMockCustomers() {
        customerData.addAll(
                new Customer(101, "Mr.", "John Smith", "john@email.com", "123 High St", "London", "SW1A 1AA", 500.00, 0.00),
                new Customer(102, "Mrs.", "Jane Doe", "jane@email.com", "456 Oak Rd", "Manchester", "M1 1AA", 1000.00, 150.00),
                new Customer(103, "Dr.", "Bob Johnson", "bob@email.com", "789 Pine Ln", "Birmingham", "B1 1AA", 750.00, 0.00)
        );
        customerData.get(1).setAccountStatus("Suspended");
        customerData.get(1).setStatus1stReminder("due");
    }

    public ObservableList<Customer> getCustomerData() { return customerData; }
    public ObservableList<PaymentRecord> getPaymentHistory() { return paymentHistory; }

    public Customer findCustomerById(int id) {
        for (Customer c : customerData) { if (c.getId() == id) return c; }
        return null;
    }

    public ObservableList<PaymentRecord> getCustomerPaymentHistory(int customerId) {
        ObservableList<PaymentRecord> history = FXCollections.observableArrayList();
        for (PaymentRecord record : paymentHistory) {
            if (record.getCustomerId() == customerId) { history.add(record); }
        }
        return history;
    }

    public boolean addCustomer(String title, String name, String email, String address,
                               String town, String postcode, double limit) {
        int nextId = customerData.stream().mapToInt(Customer::getId).max().orElse(100) + 1;
        String sql = "INSERT INTO customers (id, title, name, email, address, town, postcode, " +
                "credit_limit, current_debt, account_status, status_1st_reminder, status_2nd_reminder, " +
                "discount_plan_type, discount_rate, monthly_purchase_total) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, nextId);
            stmt.setString(2, title);
            stmt.setString(3, name);
            stmt.setString(4, email);
            stmt.setString(5, address);
            stmt.setString(6, town);
            stmt.setString(7, postcode);
            stmt.setDouble(8, limit);
            stmt.setDouble(9, 0.0);
            stmt.setString(10, "Normal");
            stmt.setString(11, "no_need");
            stmt.setString(12, "no_need");
            stmt.setString(13, "NONE");
            stmt.setDouble(14, 0.0);
            stmt.setDouble(15, 0.0);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                Customer newCustomer = new Customer(nextId, title, name, email, address, town, postcode, limit, 0.0);
                customerData.add(newCustomer);
                System.out.println("✅ Customer added to database: " + name);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error adding customer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateCustomerField(Customer customer, String fieldName, Object newValue) {
        if (customer == null) return false;
        String sql = "UPDATE customers SET " + fieldName + " = ? WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (newValue instanceof String) stmt.setString(1, (String) newValue);
            else if (newValue instanceof Double) stmt.setDouble(1, (Double) newValue);
            else if (newValue instanceof Integer) stmt.setInt(1, (Integer) newValue);
            stmt.setInt(2, customer.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Customer field updated: " + fieldName + " = " + newValue);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating customer field: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateCustomer(Customer customer) {
        if (customer == null) return false;
        String sql = "UPDATE customers SET title = ?, name = ?, email = ?, address = ?, town = ?, postcode = ?, " +
                "credit_limit = ?, current_debt = ?, account_status = ?, status_1st_reminder = ?, " +
                "status_2nd_reminder = ?, date_1st_reminder = ?, date_2nd_reminder = ?, oldest_debt_date = ?, " +
                "discount_plan_type = ?, discount_rate = ?, monthly_purchase_total = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getTitle());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getAddress());
            stmt.setString(5, customer.getTown());
            stmt.setString(6, customer.getPostcode());
            stmt.setDouble(7, customer.getCreditLimit());
            stmt.setDouble(8, customer.getCurrentDebt());
            stmt.setString(9, customer.getAccountStatus());
            stmt.setString(10, customer.getStatus1stReminder());
            stmt.setString(11, customer.getStatus2ndReminder());

            if (customer.getDate1stReminder() != null) stmt.setDate(12, Date.valueOf(customer.getDate1stReminder()));
            else stmt.setNull(12, Types.DATE);

            if (customer.getDate2ndReminder() != null) stmt.setDate(13, Date.valueOf(customer.getDate2ndReminder()));
            else stmt.setNull(13, Types.DATE);

            if (customer.getOldestDebtDate() != null) stmt.setDate(14, Date.valueOf(customer.getOldestDebtDate()));
            else stmt.setNull(14, Types.DATE);

            stmt.setString(15, customer.getDiscountPlanType());
            stmt.setDouble(16, customer.getDiscountRate());
            stmt.setDouble(17, customer.getMonthlyPurchaseTotal());
            stmt.setInt(18, customer.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Customer updated in database: " + customer.getName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating customer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteCustomer(Customer c) {
        if (c == null) return false;
        if (c.getCurrentDebt() > 0) {
            System.out.println("Cannot delete customer with outstanding debt");
            return false;
        }
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, c.getId());
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                customerData.remove(c);
                System.out.println("Customer deleted from database: " + c.getName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting customer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean recordPayment(int customerId, double amount, String paymentType,
                                 String paymentDetails, String reference) {
        String sql = "INSERT INTO customer_payments (customer_id, payment_date, amount, " +
                "payment_type, payment_details, reference) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setDouble(3, amount);
            stmt.setString(4, paymentType);
            stmt.setString(5, paymentDetails);
            stmt.setString(6, reference);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                loadPaymentHistory();
                System.out.println("✅ Payment recorded for customer ID: " + customerId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error recording payment: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean processDebtPayment(Customer customer, double amount) {
        if (customer == null || amount <= 0) return false;
        double currentDebt = customer.getCurrentDebt();
        if (amount > currentDebt) amount = currentDebt;

        double newDebt = currentDebt - amount;
        customer.setCurrentDebt(newDebt);

        if (newDebt <= 0.001) {
            customer.setStatus1stReminder("no_need");
            customer.setStatus2ndReminder("no_need");
            customer.setDate1stReminder(null);
            customer.setDate2ndReminder(null);
            if (!"In Default".equals(customer.getAccountStatus())) {
                customer.setAccountStatus("Normal");
            }
            System.out.println("✅ Debt cleared for " + customer.getName() + " - Status reset to Normal");
        }
        return updateCustomer(customer);
    }

    public boolean canUnsuspendCustomer(Customer customer) {
        if (customer == null) return false;
        if (customer.getCurrentDebt() > 0 && "Suspended".equals(customer.getAccountStatus())) {
            System.out.println("⚠️ Cannot unsuspend " + customer.getName() + " - Outstanding debt: £" + customer.getCurrentDebt());
            return false;
        }
        return true;
    }

    public void evaluateAccountStatuses(LocalDate simulatedToday) {
        for (Customer c : customerData) {
            if (c.getCurrentDebt() > 0 && c.getOldestDebtDate() != null) {
                LocalDate debtMonth = c.getOldestDebtDate().withDayOfMonth(1);
                LocalDate nextMonth15th = debtMonth.plusMonths(1).withDayOfMonth(15);
                LocalDate endOfNextMonth = debtMonth.plusMonths(2).minusDays(1);
                boolean statusChanged = false;

                if (simulatedToday.isAfter(endOfNextMonth) && !"In Default".equals(c.getAccountStatus())) {
                    c.setAccountStatus("In Default");
                    c.setStatus2ndReminder("due");
                    statusChanged = true;
                } else if (simulatedToday.isAfter(nextMonth15th) && "Normal".equals(c.getAccountStatus())) {
                    c.setAccountStatus("Suspended");
                    c.setStatus1stReminder("due");
                    statusChanged = true;
                }

                if (statusChanged) updateCustomer(c);
            }
        }
    }

    public void processReminders(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return;
        LocalDate today = LocalDate.now();
        if ("due".equals(customer.getStatus1stReminder())) {
            generateFirstReminder(customer);
            customer.setStatus1stReminder("sent");
            customer.setDate2ndReminder(today.plusDays(15));
            updateCustomer(customer);
            System.out.println("1st reminder processed for: " + customer.getName());
        }
        if ("due".equals(customer.getStatus2ndReminder())) {
            LocalDate date2nd = customer.getDate2ndReminder();
            if (date2nd != null && !date2nd.isAfter(today)) {
                generateSecondReminder(customer);
                customer.setStatus2ndReminder("sent");
                updateCustomer(customer);
                System.out.println("2nd reminder processed for: " + customer.getName());
            }
        }
    }

    public void resetRemindersOnPayment(Customer customer) {
        if (customer == null) return;
        if (!"In Default".equals(customer.getAccountStatus())) {
            customer.setStatus1stReminder("no_need");
            customer.setStatus2ndReminder("no_need");
            updateCustomer(customer);
            System.out.println("Reminders reset for: " + customer.getName());
        }
    }

    public boolean setDiscountPlan(Customer customer, String planType, double rate) {
        if (customer == null) return false;
        if (!"NONE".equals(planType) && !"FIXED".equals(planType) && !"FLEXIBLE".equals(planType)) {
            System.out.println("Invalid discount plan type");
            return false;
        }
        customer.setDiscountPlanType(planType);
        customer.setDiscountRate(rate);
        updateCustomer(customer);
        System.out.println("Discount plan set for " + customer.getName() + ": " + planType + " (" + (rate * 100) + "%)");
        return true;
    }

    public void resetAllMonthlyPurchaseTotals() {
        for (Customer c : customerData) {
            c.resetMonthlyPurchaseTotal();
            updateCustomer(c);
        }
        System.out.println("All monthly purchase totals reset");
    }

    public void addPurchaseToMonthlyTotal(Customer customer, double amount) {
        if (customer != null && !"NONE".equals(customer.getDiscountPlanType())) {
            customer.addToMonthlyPurchaseTotal(amount);
            updateCustomer(customer);
            System.out.println("Added £" + amount + " to " + customer.getName() + "'s monthly total");
        }
    }

    public boolean updateCustomerDebt(Customer customer) {
        return updateCustomer(customer);
    }

    public File generateMonthlyStatement(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return null;
        int invoiceNum = invoiceCounter++;
        File file = new File("Statement_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();

        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n<title>Monthly Statement</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(".statement-container { max-width: 800px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("h1 { color: #2c3e50; text-align: center; margin-bottom: 30px; }\n");
        html.append(".info-section { margin: 20px 0; padding: 15px; background: #ecf0f1; border-radius: 5px; }\n");
        html.append(".amount { font-size: 24px; font-weight: bold; color: #e74c3c; text-align: center; margin: 20px 0; }\n");
        html.append(".footer { text-align: center; margin-top: 30px; color: #7f8c8d; font-size: 12px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class='statement-container'>\n");
        html.append("<h1>MONTHLY STATEMENT</h1>\n");
        html.append("<div class='info-section'>\n");
        html.append("<p><strong>Invoice No.:</strong> ").append(invoiceNum).append("</p>\n");
        html.append("<p><strong>Client:</strong> ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>\n");
        html.append("<p><strong>Email:</strong> ").append(customer.getEmail()).append("</p>\n");
        html.append("<p><strong>Account ID:</strong> CSM").append(String.format("%06d", customer.getId())).append("</p>\n");
        html.append("</div>\n");
        html.append("<div class='amount'>Total Amount Due: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</div>\n");
        html.append("<p style='text-align: center;'>Payment due by: 15th of ").append(today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append("</p>\n");
        html.append("<div class='footer'>\n");
        html.append("<p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>\n");
        html.append("<p>This is an automated statement. Please contact us if you have any questions.</p>\n");
        html.append("</div>\n");
        html.append("</div>\n</body>\n</html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Generate 1st Reminder - REMOVED DEBT CHECK FOR DEMO PURPOSES
     */
    public File generateFirstReminder(Customer customer) {
        if (customer == null) return null;
        int invoiceNum = invoiceCounter++;
        File file = new File("Reminder1_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();
        LocalDate paymentDue = today.plusDays(7);

        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n<title>1st Reminder</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(".reminder-container { max-width: 800px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-left: 5px solid #f39c12; }\n");
        html.append("h1 { color: #e67e22; text-align: center; margin-bottom: 30px; }\n");
        html.append(".info-section { margin: 20px 0; padding: 15px; background: #fef9e7; border-radius: 5px; }\n");
        html.append(".amount { font-size: 24px; font-weight: bold; color: #e74c3c; text-align: center; margin: 20px 0; }\n");
        html.append(".urgent { background: #ffeaa7; padding: 10px; border-radius: 5px; margin: 20px 0; text-align: center; font-weight: bold; }\n");
        html.append(".footer { text-align: center; margin-top: 30px; color: #7f8c8d; font-size: 12px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class='reminder-container'>\n");
        html.append("<h1>1ST PAYMENT REMINDER</h1>\n");
        html.append("<div class='urgent'>⚠️ This is your first reminder regarding outstanding payment</div>\n");
        html.append("<div class='info-section'>\n");
        html.append("<p><strong>Invoice No.:</strong> ").append(invoiceNum).append("</p>\n");
        html.append("<p><strong>Client:</strong> ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>\n");
        html.append("<p><strong>Email:</strong> ").append(customer.getEmail()).append("</p>\n");
        html.append("<p><strong>Account ID:</strong> CSM").append(String.format("%06d", customer.getId())).append("</p>\n");
        html.append("</div>\n");
        html.append("<div class='amount'>Total Amount: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</div>\n");
        html.append("<p style='text-align: center;'>Payment due by: ").append(paymentDue.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))).append("</p>\n");
        html.append("<div class='footer'>\n");
        html.append("<p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>\n");
        html.append("<p>Please arrange payment immediately to avoid further action.</p>\n");
        html.append("</div>\n");
        html.append("</div>\n</body>\n</html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Generate 2nd Reminder - REMOVED DEBT CHECK FOR DEMO PURPOSES
     */
    public File generateSecondReminder(Customer customer) {
        if (customer == null) return null;

        int invoiceNum = invoiceCounter++;
        File file = new File("Reminder2_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();
        LocalDate paymentDue = today.plusDays(7);

        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n<title>2nd Reminder</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(".reminder-container { max-width: 800px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); border-left: 5px solid #e74c3c; }\n");
        html.append("h1 { color: #c0392b; text-align: center; margin-bottom: 30px; }\n");
        html.append(".info-section { margin: 20px 0; padding: 15px; background: #fdedec; border-radius: 5px; }\n");
        html.append(".amount { font-size: 24px; font-weight: bold; color: #e74c3c; text-align: center; margin: 20px 0; }\n");
        html.append(".urgent { background: #fadbd8; padding: 15px; border-radius: 5px; margin: 20px 0; text-align: center; font-weight: bold; font-size: 16px; }\n");
        html.append(".warning { background: #ffcccc; padding: 10px; border-radius: 5px; margin: 15px 0; text-align: center; }\n");
        html.append(".footer { text-align: center; margin-top: 30px; color: #7f8c8d; font-size: 12px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class='reminder-container'>\n");
        html.append("<h1>2ND AND FINAL PAYMENT REMINDER</h1>\n");
        html.append("<div class='urgent'>⚠️ URGENT: Final notice before account suspension</div>\n");
        html.append("<div class='warning'>⚠️ Your account will be suspended if payment is not received</div>\n");
        html.append("<div class='info-section'>\n");
        html.append("<p><strong>Invoice No.:</strong> ").append(invoiceNum).append("</p>\n");
        html.append("<p><strong>Client:</strong> ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>\n");
        html.append("<p><strong>Email:</strong> ").append(customer.getEmail()).append("</p>\n");
        html.append("<p><strong>Account ID:</strong> CSM").append(String.format("%06d", customer.getId())).append("</p>\n");
        html.append("</div>\n");
        html.append("<div class='amount'>Total Amount: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</div>\n");
        html.append("<p style='text-align: center;'>Payment due by: ").append(paymentDue.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))).append("</p>\n");
        html.append("<div class='footer'>\n");
        html.append("<p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>\n");
        html.append("<p>IMMEDIATE ACTION REQUIRED - Contact us immediately to arrange payment.</p>\n");
        html.append("</div>\n");
        html.append("</div>\n</body>\n</html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Generate purchase history report for a customer
     */
    public File generatePurchaseHistoryReport(Customer customer) {
        if (customer == null) return null;
        File file = new File("PurchaseHistory_" + customer.getId() + "_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();
        ObservableList<PaymentRecord> history = getCustomerPaymentHistory(customer.getId());

        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n<title>Purchase History</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }\n");
        html.append(".report-container { max-width: 1000px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("h1 { color: #2c3e50; text-align: center; margin-bottom: 30px; }\n");
        html.append(".customer-info { background: #ecf0f1; padding: 20px; border-radius: 5px; margin-bottom: 30px; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("th { background: #3498db; color: white; font-weight: bold; }\n");
        html.append("tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append(".footer { text-align: center; margin-top: 30px; color: #7f8c8d; font-size: 12px; }\n");
        html.append("</style>\n</head>\n<body>\n");
        html.append("<div class='report-container'>\n");
        html.append("<h1>PURCHASE HISTORY REPORT</h1>\n");
        html.append("<div class='customer-info'>\n");
        html.append("<p><strong>Customer:</strong> ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>\n");
        html.append("<p><strong>Account ID:</strong> CSM").append(String.format("%06d", customer.getId())).append("</p>\n");
        html.append("<p><strong>Current Debt:</strong> £").append(String.format("%.2f", customer.getCurrentDebt())).append("</p>\n");
        html.append("<p><strong>Account Status:</strong> ").append(customer.getAccountStatus()).append("</p>\n");
        html.append("</div>\n");
        html.append("<h2>Payment History</h2>\n");
        html.append("<table>\n<tr>\n<th>Date</th>\n<th>Amount</th>\n<th>Payment Type</th>\n<th>Details</th>\n<th>Reference</th>\n</tr>\n");

        if (history.isEmpty()) {
            html.append("<tr><td colspan='5' style='text-align: center;'>No payment records found</td></tr>\n");
        } else {
            for (PaymentRecord record : history) {
                html.append("<tr>\n");
                html.append("<td>").append(record.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n");
                html.append("<td>£ ").append(String.format("%.2f", record.getAmount())).append("</td>\n");
                html.append("<td>").append(record.getPaymentType() != null ? record.getPaymentType() : "-").append("</td>\n");
                html.append("<td>").append(record.getPaymentDetails() != null ? record.getPaymentDetails() : "-").append("</td>\n");
                html.append("<td>").append(record.getReference() != null ? record.getReference() : "-").append("</td>\n");
                html.append("</tr>\n");
            }
        }
        html.append("</table>\n");

        html.append("<div class='footer'>\n");
        html.append("<p>Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>\n");
        html.append("<p>This report shows all payment transactions for this customer account.</p>\n");
        html.append("</div>\n");
        html.append("</div>\n</body>\n</html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
            System.out.println("Purchase history report generated: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
        return file;
    }

    public static class PaymentRecord {
        private final int id;
        private final int customerId;
        private final LocalDate date;
        private final double amount;
        private final String paymentType;
        private final String paymentDetails;
        private final String reference;

        public PaymentRecord(int id, int customerId, LocalDate date, double amount,
                             String paymentType, String paymentDetails, String reference) {
            this.id = id;
            this.customerId = customerId;
            this.date = date;
            this.amount = amount;
            this.paymentType = paymentType;
            this.paymentDetails = paymentDetails;
            this.reference = reference;
        }

        public int getId() { return id; }
        public int getCustomerId() { return customerId; }
        public LocalDate getDate() { return date; }
        public double getAmount() { return amount; }
        public String getPaymentType() { return paymentType; }
        public String getPaymentDetails() { return paymentDetails; }
        public String getReference() { return reference; }
    }
}