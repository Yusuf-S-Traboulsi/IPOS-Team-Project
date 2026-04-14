package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.Customer;
import com.pharmacy.iposca.model.Product;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class handles all POS sales transactions.
 * Supports account holders (with discounts) and walk-in customers
 */
public class SalesController {
    private ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private InventoryController inventoryController;
    private ObservableList<SaleRecord> salesLog = FXCollections.observableArrayList();
    private int saleCounter = 50000;

    public SalesController(InventoryController ic) {
        this.inventoryController = ic;
        loadSalesFromDatabase();
    }

    private void loadSalesFromDatabase() {
        String sql = "SELECT id, customer_name, total_with_vat, sale_date, payment_type FROM sales ORDER BY id DESC LIMIT 100";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            salesLog.clear();
            while (rs.next()) {
                salesLog.add(new SaleRecord(
                        rs.getInt("id"),
                        rs.getString("customer_name"),
                        rs.getDouble("total_with_vat"),
                        rs.getDate("sale_date").toLocalDate(),
                        rs.getString("payment_type")
                ));
            }

            String maxSql = "SELECT MAX(id) FROM sales";
            try (Statement stmt2 = conn.createStatement();
                 ResultSet rs2 = stmt2.executeQuery(maxSql)) {
                if (rs2.next()) {
                    saleCounter = rs2.getInt(1);
                }
            }

            System.out.println("Loaded " + salesLog.size() + " sales records from database");
        } catch (SQLException e) {
            System.err.println("Error loading sales: " + e.getMessage());
        }
    }

    public ObservableList<CartItem> getCart() {
        return cart;
    }

    public void addItemToCart(Product p) {
        for (CartItem item : cart) {
            if (item.getProduct().getId() == p.getId()) {
                item.setQuantity(item.getQuantity() + 1);
                return;
            }
        }
        cart.add(new CartItem(p));
    }

    public void removeItemFromCart(CartItem item) {
        cart.remove(item);
    }

    /**
     * Method to process a sale and update inventory
     * */
    public String processSale(Customer c, String paymentType, String cardType, String cardFirstFour, String cardLastFour, String cardExpiry) {
        if (cart.isEmpty()) {
            return "ERROR: Cart is empty. Cannot process sale.";
        }

        double totalDue = calculateTotal();

        // Calculate discount for account holders
        double discountRate = 0.0;
        double discountAmount = 0.0;
        double totalAfterDiscount = totalDue;

        if (c != null) {
            discountRate = c.calculateEffectiveDiscountRate();
            discountAmount = totalDue * discountRate;
            totalAfterDiscount = totalDue - discountAmount;
        }

        double finalAmountPayable = totalAfterDiscount;

        // Account holder validation
        if (c != null) {
            if ("CASH".equalsIgnoreCase(paymentType)) {
                return "ERROR: Account holders must pay by card. Cash payments are not permitted for account customers.";
            }

            String cardError = validateCardDetails(cardType, cardFirstFour, cardLastFour, cardExpiry);
            if (cardError != null) {
                return "ERROR: " + cardError;
            }

            if (!"Normal".equals(c.getAccountStatus())) {
                return "ERROR: Transaction denied. Account status is '" + c.getAccountStatus() + "'. Please settle outstanding balance first.";
            }

            double newDebt = c.getCurrentDebt() + finalAmountPayable;
            if (newDebt > c.getCreditLimit()) {
                return "ERROR: Transaction exceeds credit limit. Available: £" +
                        String.format("%.2f", c.getCreditLimit() - c.getCurrentDebt()) +
                        ", Required: £" + String.format("%.2f", finalAmountPayable);
            }

            c.setCurrentDebt(newDebt);
            if (c.getOldestDebtDate() == null) {
                c.setOldestDebtDate(LocalDate.now());
            }

            CustomerController.getInstance().addPurchaseToMonthlyTotal(c, totalAfterDiscount);
            CustomerController.getInstance().updateCustomerDebt(c);
        } else {
            if ("CARD".equalsIgnoreCase(paymentType)) {
                String cardError = validateCardDetails(cardType, cardFirstFour, cardLastFour, cardExpiry);
                if (cardError != null) {
                    return "ERROR: " + cardError;
                }
            }
        }

        // Decrement stock for all items
        for (CartItem item : cart) {
            int availableStock = inventoryController.getProducts().stream()
                    .filter(p -> p.getId() == item.getProduct().getId())
                    .findFirst()
                    .map(Product::getStock)
                    .orElse(0);

            if (item.getQuantity() > availableStock) {
                return "ERROR: Insufficient stock for " + item.getName();
            }

            inventoryController.decrementLocalStock(item.getProduct().getId(), item.getQuantity());
        }

        logSaleToDatabase(c, totalDue, discountRate, discountAmount, totalAfterDiscount, finalAmountPayable, paymentType);
        return "SUCCESS";
    }

    /**
     * Validate card details for account holders
     */
    private String validateCardDetails(String cardType, String firstFour, String lastFour, String expiry) {
        if (cardType == null || cardType.trim().isEmpty()) {
            return "Card type is required.";
        }
        if (!cardType.matches("^(Credit|Debit)$")) {
            return "Invalid card type. Must be Credit OR Debit.";
        }
        if (firstFour == null || !firstFour.matches("^\\d{4}$")) {
            return "First four digits must be exactly 4 numeric digits.";
        }
        if (lastFour == null || !lastFour.matches("^\\d{4}$")) {
            return "Last four digits must be exactly 4 numeric digits.";
        }
        if (expiry == null || !expiry.matches("^\\d{2}/\\d{2}$")) {
            return "Expiry date must be in MM/YY format (e.g., 12/25).";
        }

        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]);
            int currentYear = LocalDate.now().getYear();
            int currentMonth = LocalDate.now().getMonthValue();

            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return "Card has expired. Please use a valid card.";
            }
        } catch (Exception e) {
            return "Invalid expiry date format.";
        }
        return null;
    }

    private void logSaleToDatabase(Customer c, double totalBeforeDiscount, double discountRate,
                                   double discountAmount, double totalAfterDiscount,
                                   double finalAmount, String paymentType) {
        saleCounter++;
        Integer customerId = (c != null) ? c.getId() : null;
        String customerName = (c != null) ? c.getName() : "Walk-in Customer";
        boolean isAccountHolder = (c != null);

        // Extract VAT component from gross amount for accounting
        double vatRate = inventoryController.getVatRate();
        double netAmount = finalAmount / (1 + vatRate);
        double vatAmount = finalAmount - netAmount;

        String sql = "INSERT INTO sales (id, customer_id, customer_name, is_account_holder, " +
                "total_before_discount, discount_rate, discount_amount, total_after_discount, " +
                "vat_amount, total_with_vat, payment_type, sale_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, saleCounter);

            if (customerId != null) {
                stmt.setInt(2, customerId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, customerName);
            stmt.setBoolean(4, isAccountHolder);
            stmt.setDouble(5, totalBeforeDiscount);
            stmt.setDouble(6, discountRate);
            stmt.setDouble(7, discountAmount);
            stmt.setDouble(8, totalAfterDiscount);
            stmt.setDouble(9, vatAmount);
            stmt.setDouble(10, finalAmount);
            stmt.setString(11, paymentType);
            stmt.setDate(12, Date.valueOf(LocalDate.now()));

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                salesLog.add(new SaleRecord(saleCounter, customerName, finalAmount, LocalDate.now(), paymentType));
                System.out.println("Sale: " + saleCounter + " logged to database");
            }
        } catch (SQLException e) {
            System.err.println("Error logging sale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates formal file for invoices/receipts for POS sales
     */
    public File generateFormalLetter(Customer c) {
        int invoiceNum = 100000 + new Random().nextInt(900000);
        File file = new File("Invoice_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();

        double vatRate = inventoryController.getVatRate();
        double grossTotal = calculateTotal();  // Total from POS (VAT-inclusive)
        double netTotal = grossTotal / (1 + vatRate);  // Extract net from gross
        double vatAmount = grossTotal - netTotal;  // Calculate VAT amount

        LocalDate now = LocalDate.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));

        String customerLastName = "Customer";
        String customerTitle = "Mr.";
        if (c != null) {
            String[] nameParts = c.getName().trim().split("\\s+");
            customerLastName = nameParts[nameParts.length - 1];
            customerTitle = c.getTitle() != null ? c.getTitle() : "Mr.";
        }

        MerchantSettingsController settingsController = MerchantSettingsController.getInstance();
        com.pharmacy.iposca.model.MerchantSettings settings = settingsController.getMerchantSettings();

        // HTML Header and body for the invoice
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Invoice #" + invoiceNum + "</title>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f6fa; color: #2c3e50; line-height: 1.6; padding: 40px; }\n");
        html.append("    .invoice-container { max-width: 850px; margin: 0 auto; background: white; padding: 50px; box-shadow: 0 0 20px rgba(0,0,0,0.1); border-radius: 5px; }\n");
        html.append("    .header-section { display: flex; justify-content: space-between; margin-bottom: 40px; }\n");
        html.append("    .customer-address { width: 45%; font-size: 14px; line-height: 1.8; }\n");
        html.append("    .company-info { width: 45%; text-align: right; font-size: 14px; line-height: 1.8; }\n");
        html.append("    .invoice-date { text-align: right; margin-top: 20px; font-size: 14px; }\n");
        html.append("    .salutation { margin: 30px 0; font-size: 14px; }\n");
        html.append("    .invoice-title { text-align: center; font-size: 18px; font-weight: bold; margin: 30px 0; color: #2c3e50; }\n");
        html.append("    .account-no { font-size: 14px; margin-bottom: 20px; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; margin: 30px 0; font-size: 14px; }\n");
        html.append("    th { background: #2c3e50; color: white; padding: 12px 15px; text-align: center; font-weight: 600; border: 2px solid #2c3e50; }\n");
        html.append("    td { padding: 10px 15px; border: 1px solid #2c3e50; text-align: center; }\n");
        html.append("    tr:nth-child(even) { background: #f8f9fa; }\n");
        html.append("    .totals-section { width: 40%; margin-left: auto; }\n");
        html.append("    .totals-section td { padding: 8px 15px; border: 2px solid #2c3e50; }\n");
        html.append("    .totals-section .label { text-align: right; font-weight: 600; background: #f8f9fa; }\n");
        html.append("    .totals-section .value { text-align: center; font-weight: 600; }\n");
        html.append("    .totals-section .final { background: #2c3e50; color: white; font-size: 15px; }\n");
        html.append("    .footer-text { margin: 40px 0; font-size: 14px; line-height: 1.8; }\n");
        html.append("    .sign-off { margin-top: 50px; font-size: 14px; line-height: 2.5; }\n");
        html.append("    @media print { body { background: white; padding: 0; } .invoice-container { box-shadow: none; padding: 40px; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<div class='invoice-container'>\n");

        // Header Section
        html.append("  <div class='header-section'>\n");
        html.append("    <div class='customer-address'>\n");
        if (c != null) {
            html.append("      " + customerTitle + ". " + c.getName() + ",<br>\n");
            html.append("      " + (c.getAddress() != null ? c.getAddress() : "") + ",<br>\n");
            html.append("      " + (c.getTown() != null ? c.getTown() : "") + ",<br>\n");
            html.append("      " + (c.getPostcode() != null ? c.getPostcode() : "") + "\n");
        } else {
            html.append("      Walk-in Customer,<br>\n");
            html.append("      N/A\n");
        }
        html.append("    </div>\n");
        html.append("    <div class='company-info'>\n");
        html.append("      " + (settings != null && settings.getCompanyName() != null ? settings.getCompanyName() : "InfoPharma Ltd.") + ",<br>\n");
        html.append("      " + (settings != null && settings.getAddressLine1() != null ? settings.getAddressLine1() : "20 High St.") + ",<br>\n");
        html.append("      " + (settings != null && settings.getCity() != null ? settings.getCity() : "London") + ",<br>\n");
        html.append("      " + (settings != null && settings.getPostcode() != null ? settings.getPostcode() : "Kent") + "<br>\n");
        html.append("      Phone: " + (settings != null && settings.getPhone() != null ? settings.getPhone() : "0208 778 0124") + "<br>\n");
        html.append("      Fax: " + (settings != null && settings.getFax() != null ? settings.getFax() : "0208 778 0125") + "\n");
        html.append("    </div>\n");
        html.append("  </div>\n");

        // Date
        html.append("  <div class='invoice-date'>\n");
        html.append("    " + dateStr + "\n");
        html.append("  </div>\n");

        // Salutation
        html.append("  <div class='salutation'>\n");
        html.append("    Dear " + customerTitle + ". " + customerLastName + ",\n");
        html.append("  </div>\n");

        // Invoice Title
        html.append("  <div class='invoice-title'>\n");
        html.append("    INVOICE NO.: " + invoiceNum + "\n");
        html.append("  </div>\n");

        // Account No
        if (c != null) {
            html.append("  <div class='account-no'>\n");
            html.append("    Account No: CSM" + String.format("%06d", c.getId()) + "\n");
            html.append("  </div>\n");
        }

        // Items Table, show VAT-inclusive prices to match POS
        html.append("  <table>\n");
        html.append("    <thead>\n");
        html.append("      <tr>\n");
        html.append("        <th>Item ID</th>\n");
        html.append("        <th>Packages</th>\n");
        html.append("        <th>Package Cost, £</th>\n");
        html.append("        <th>Amount, £</th>\n");
        html.append("      </tr>\n");
        html.append("    </thead>\n");
        html.append("    <tbody>\n");

        for (CartItem item : cart) {
            double lineAmount = item.getPrice() * item.getQuantity();
            html.append("      <tr>\n");
            html.append("        <td>" + item.getName() + "</td>\n");
            html.append("        <td>" + item.getQuantity() + "</td>\n");
            html.append("        <td>" + String.format("%.2f", item.getPrice()) + "</td>\n");
            html.append("        <td>" + String.format("%.2f", lineAmount) + "</td>\n");
            html.append("      </tr>\n");
        }

        html.append("    </tbody>\n");
        html.append("  </table>\n");

        html.append("  <table class='totals-section'>\n");
        html.append("    <tr>\n");
        html.append("      <td class='label'>Total</td>\n");
        html.append("      <td class='value'>" + String.format("%.2f", grossTotal) + "</td>\n");
        html.append("    </tr>\n");
        html.append("    <tr>\n");
        html.append("      <td class='label'>VAT @ " + String.format("%.1f", vatRate * 100) + "%</td>\n");
        html.append("      <td class='value'>" + String.format("%.2f", vatAmount) + "</td>\n");
        html.append("    </tr>\n");
        html.append("    <tr class='final'>\n");
        html.append("      <td class='label'>Amount Due</td>\n");
        html.append("      <td class='value'>" + String.format("%.2f", grossTotal) + "</td>\n");
        html.append("    </tr>\n");
        html.append("  </table>\n");

        // Footer text, uses the template from database
        html.append("  <div class='footer-text'>\n");
        com.pharmacy.iposca.model.DocumentTemplate invoiceTemplate = settingsController.getTemplateByType("INVOICE");
        if (invoiceTemplate != null && invoiceTemplate.getFooterTemplate() != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("company_name", settings != null && settings.getCompanyName() != null ? settings.getCompanyName() : "InfoPharma Ltd.");
            placeholders.put("director_name", settings != null && settings.getDirectorName() != null ? settings.getDirectorName() : "A. Petite");
            html.append(settingsController.processTemplate(invoiceTemplate.getFooterTemplate(), placeholders));
        } else {
            html.append("    Thank you for your valued custom. We look forward to receiving your payment in due course.\n");
        }
        html.append("  </div>\n");

        // Sign-off
        html.append("  <div class='sign-off'>\n");
        html.append("    Yours sincerely,\n");
        html.append("    <br><br>\n");
        html.append("    " + (settings != null && settings.getDirectorName() != null ? settings.getDirectorName() : "A. Petite") + "\n");
        html.append("  </div>\n");

        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
            System.out.println("Invoice generated: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error generating invoice: " + e.getMessage());
            e.printStackTrace();
        }

        cart.clear();
        return file;
    }

    public double calculateTotal() {
        return cart.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
    }

    public ObservableList<SaleRecord> getSalesLog() {
        return salesLog;
    }

    public static class CartItem {
        private final Product product;
        private final IntegerProperty quantity = new SimpleIntegerProperty(1);

        public CartItem(Product p) {
            this.product = p;
        }

        //getters and setters
        public String getName() { return product.getName(); }
        public double getPrice() { return product.getPrice(); }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int q) { this.quantity.set(q); }
        public IntegerProperty quantityProperty() { return quantity; }
        public Product getProduct() { return product; }
    }

    public static class SaleRecord {
        private final int id;
        private final String customerName;
        private final double amount;
        private final LocalDate date;
        private final String paymentType;

        public SaleRecord(int id, String name, double amount, LocalDate date, String type) {
            this.id = id;
            this.customerName = name;
            this.amount = amount;
            this.date = date;
            this.paymentType = type;
        }

        public int getId() { return id; }
        public String getCustomerName() { return customerName; }
        public double getAmount() { return amount; }
        public LocalDate getDate() { return date; }
        public String getPaymentType() { return paymentType; }
    }
}