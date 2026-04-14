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
import java.time.format.DateTimeFormatter;

/**
 * This class handles all customer data that is loaded from and saved to the database
 * Includes discount plan management (Fixed & Flexible/variable plans)
 */
public class CustomerController {

    private static CustomerController instance;
    private final ObservableList<Customer> customerData = FXCollections.observableArrayList();
    private int invoiceCounter = 197362;

    private CustomerController() {
        loadCustomersFromDatabase();
    }

    public static synchronized CustomerController getInstance() {
        if (instance == null) {
            instance = new CustomerController();
        }
        return instance;
    }

    /**
     * Load all customers from database on startup
     */
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

                // Load date fields, (can be null)
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

            System.out.println("Loaded " + customerData.size() + " customers from database");

        } catch (SQLException e) {
            System.err.println("Error loading customers: " + e.getMessage());
            e.printStackTrace();
            loadMockCustomers();
        }
    }

    /**
     * Fallback mock data if database connection fails
     */
    private void loadMockCustomers() {
        customerData.addAll(
                new Customer(101, "Mr.", "John Smith", "john@email.com", "123 High St", "London", "SW1A 1AA", 500.00, 0.00),
                new Customer(102, "Mrs.", "Jane Doe", "jane@email.com", "456 Oak Rd", "Manchester", "M1 1AA", 1000.00, 150.00),
                new Customer(103, "Dr.", "Bob Johnson", "bob@email.com", "789 Pine Ln", "Birmingham", "B1 1AA", 750.00, 0.00)
        );
        customerData.get(1).setAccountStatus("Suspended");
        customerData.get(1).setStatus1stReminder("due");
    }

    public ObservableList<Customer> getCustomerData() {
        return customerData;
    }

    public Customer findCustomerById(int id) {
        for (Customer c : customerData) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    /**
     * Add customer and save to database
     */
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
                System.out.println("Customer added to database: " + name);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * this method updates customer field and saves it to database (Called from TableView edits)
     */
    public boolean updateCustomerField(Customer customer, String fieldName, Object newValue) {
        if (customer == null) return false;

        String sql = "UPDATE customers SET " + fieldName + " = ? WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (newValue instanceof String) {
                stmt.setString(1, (String) newValue);
            } else if (newValue instanceof Double) {
                stmt.setDouble(1, (Double) newValue);
            } else if (newValue instanceof Integer) {
                stmt.setInt(1, (Integer) newValue);
            }
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

    /**
     * updates all customer data AND saves it to the database
     */
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

            // Date fields
            if (customer.getDate1stReminder() != null) {
                stmt.setDate(12, Date.valueOf(customer.getDate1stReminder()));
            } else { stmt.setNull(12, Types.DATE); }

            if (customer.getDate2ndReminder() != null) {
                stmt.setDate(13, Date.valueOf(customer.getDate2ndReminder()));
            } else { stmt.setNull(13, Types.DATE); }

            if (customer.getOldestDebtDate() != null) {
                stmt.setDate(14, Date.valueOf(customer.getOldestDebtDate()));
            } else { stmt.setNull(14, Types.DATE); }

            // Discount fields
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

    /**
     * Method to delete customer from database
     */
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

    /**
     * Method to evaluate account status
     */
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
                if (statusChanged) {
                    updateCustomer(c);
                }
            }
        }
    }

    /**
     * Method to process debt reminders
     */
    public void processReminders(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return;

        LocalDate today = LocalDate.now();

        // Process 1st Reminder
        if ("due".equals(customer.getStatus1stReminder())) {
            generateFirstReminder(customer);
            customer.setStatus1stReminder("sent");
            customer.setDate2ndReminder(today.plusDays(15));
            updateCustomer(customer);
            System.out.println("1st reminder processed for: " + customer.getName());
        }

        // Process 2nd Reminder
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

    /**
     * Method to reset debt reminders when payment is received
     */
    public void resetRemindersOnPayment(Customer customer) {
        if (customer == null) return;

        // Per briefing: if account_status != 'in default', reset both reminders
        if (!"In Default".equals(customer.getAccountStatus())) {
            customer.setStatus1stReminder("no_need");
            customer.setStatus2ndReminder("no_need");
            updateCustomer(customer);
            System.out.println("Reminders reset for: " + customer.getName());
        }
    }

    /**
     * Updates customer debt and saves it to database
     */
    public boolean updateCustomerDebt(Customer customer) {
        return updateCustomer(customer);
    }


    //Discount Management Methods
    /**
     * Method to set discount plan for a customer and save it to database
     */
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

    /**
     * Reset all customers' monthly purchase totals, resets at the start of each month
     */
    public void resetAllMonthlyPurchaseTotals() {
        for (Customer c : customerData) {
            c.resetMonthlyPurchaseTotal();
            updateCustomer(c);
        }
        System.out.println("All monthly purchase totals reset");
    }

    /**
     * Add purchase amount to customer's monthly total
     */
    public void addPurchaseToMonthlyTotal(Customer customer, double amount) {
        if (customer != null && !"NONE".equals(customer.getDiscountPlanType())) {
            customer.addToMonthlyPurchaseTotal(amount);
            updateCustomer(customer);
            System.out.println("Added £" + amount + " to " + customer.getName() + "'s monthly total");
        }
    }


    //Report Methods

    /**
     * Method to generate a report of all customers including the html file
     */
    public File generateMonthlyStatement(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return null;

        int invoiceNum = invoiceCounter++;
        File file = new File("Statement_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();

        html.append("<html><head><title>Monthly Statement</title></head><body>");
        html.append("<h2>MONTHLY STATEMENT - INVOICE NO.: ").append(invoiceNum).append("</h2>");
        html.append("<p>Client: ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>");
        html.append("<p>Email: ").append(customer.getEmail()).append("</p>");
        html.append("<p>Total Amount Due: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</p>");
        html.append("<p>Payment due by: 15th of ").append(today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))).append("</p>");
        html.append("</body></html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Generates first reminder html file for overdue customer with a 7-day due date
     */
    public File generateFirstReminder(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return null;

        int invoiceNum = invoiceCounter++;
        File file = new File("Reminder1_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();
        LocalDate paymentDue = today.plusDays(7);

        html.append("<html><head><title>1st Reminder</title></head><body>");
        html.append("<h2>1ST REMINDER - INVOICE NO.: ").append(invoiceNum).append("</h2>");
        html.append("<p>Client: ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>");
        html.append("<p>Email: ").append(customer.getEmail()).append("</p>");
        html.append("<p>Total Amount: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</p>");
        html.append("<p>Payment due by: ").append(paymentDue.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))).append("</p>");
        html.append("</body></html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Generates second reminder html file for overdue customer with the unique invoice number
     */
    public File generateSecondReminder(Customer customer) {
        if (customer == null || customer.getCurrentDebt() <= 0) return null;

        int invoiceNum = invoiceCounter++;
        File file = new File("Reminder2_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();
        LocalDate today = LocalDate.now();
        LocalDate paymentDue = today.plusDays(7);

        html.append("<html><head><title>2nd Reminder</title></head><body>");
        html.append("<h2>2ND REMINDER - INVOICE NO.: ").append(invoiceNum).append("</h2>");
        html.append("<p>Client: ").append(customer.getTitle()).append(" ").append(customer.getName()).append("</p>");
        html.append("<p>Email: ").append(customer.getEmail()).append("</p>");
        html.append("<p>Total Amount: £").append(String.format("%.2f", customer.getCurrentDebt())).append("</p>");
        html.append("<p>Payment due by: ").append(paymentDue.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))).append("</p>");
        html.append("</body></html>");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }
}