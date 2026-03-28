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
import java.util.Random;

/**
 * Sales Controller - Handles all POS transactions
 * Supports account holders (with discounts) and walk-in customers
 * All sales logged to MySQL database
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

    /**
     * Load recent sales from database
     */
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

            // Get last sale ID for counter
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
     * Process sale with full validation
     */
    public String processSale(Customer c, String paymentType, String cardType,
                              String cardFirstFour, String cardLastFour, String cardExpiry) {
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

        double totalWithVat = totalAfterDiscount * (1 + inventoryController.getVatRate());

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

            double newDebt = c.getCurrentDebt() + totalWithVat;
            if (newDebt > c.getCreditLimit()) {
                return "ERROR: Transaction exceeds credit limit. Available: £" +
                        String.format("%.2f", c.getCreditLimit() - c.getCurrentDebt()) +
                        ", Required: £" + String.format("%.2f", totalWithVat);
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

        logSaleToDatabase(c, totalDue, discountRate, discountAmount, totalAfterDiscount, totalWithVat, paymentType);

        return "SUCCESS";
    }

    /**
     * Validate card details
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

    /**
     * Log sale to database
     */
    private void logSaleToDatabase(Customer c, double totalBeforeDiscount, double discountRate,
                                   double discountAmount, double totalAfterDiscount,
                                   double totalWithVat, String paymentType) {
        saleCounter++;

        Integer customerId = (c != null) ? c.getId() : null;
        String customerName = (c != null) ? c.getName() : "Walk-in Customer";
        boolean isAccountHolder = (c != null);

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
            stmt.setDouble(9, totalWithVat - totalAfterDiscount);
            stmt.setDouble(10, totalWithVat);
            stmt.setString(11, paymentType);
            stmt.setDate(12, Date.valueOf(LocalDate.now()));

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                salesLog.add(new SaleRecord(saleCounter, customerName, totalWithVat, LocalDate.now(), paymentType));
                System.out.println("✅ Sale #" + saleCounter + " logged to database");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error logging sale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate formal invoice/receipt
     */
    /**
     * Generate professional invoice/receipt (matching report style)
     */
    /**
     * Generate formal invoice/receipt
     */
    /**
     * Generate formal invoice matching the required template format
     * Per Briefing Page 40: Professional invoice with all required fields
     */
    public File generateFormalLetter(Customer c) {
        int invoiceNum = 100000 + new Random().nextInt(900000);
        File file = new File("Invoice_" + invoiceNum + ".html");
        StringBuilder html = new StringBuilder();

        double vatRate = inventoryController.getVatRate();
        double grossTotal = calculateTotal();
        double netTotal = grossTotal / (1 + vatRate);
        double vatAmount = grossTotal - netTotal;

        LocalDate now = LocalDate.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));

        // Get customer last name for salutation
        String customerLastName = "Customer";
        String customerTitle = "Mr.";
        if (c != null) {
            String[] nameParts = c.getName().trim().split("\\s+");
            customerLastName = nameParts[nameParts.length - 1];
            customerTitle = c.getTitle() != null ? c.getTitle() : "Mr.";
        }

        // I dont know i tried my best tho
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Invoice #" + invoiceNum + "</title>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { \n");
        html.append("      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; \n");
        html.append("      background: #f5f6fa; \n");
        html.append("      color: #2c3e50;\n");
        html.append("      line-height: 1.6;\n");
        html.append("      padding: 40px;\n");
        html.append("    }\n");
        html.append("    .invoice-container { \n");
        html.append("      max-width: 850px; \n");
        html.append("      margin: 0 auto; \n");
        html.append("      background: white;\n");
        html.append("      padding: 50px;\n");
        html.append("      box-shadow: 0 0 20px rgba(0,0,0,0.1);\n");
        html.append("      border-radius: 5px;\n");
        html.append("    }\n");
        html.append("    .header-section {\n");
        html.append("      display: flex;\n");
        html.append("      justify-content: space-between;\n");
        html.append("      margin-bottom: 40px;\n");
        html.append("    }\n");
        html.append("    .customer-address {\n");
        html.append("      width: 45%;\n");
        html.append("      font-size: 14px;\n");
        html.append("      line-height: 1.8;\n");
        html.append("    }\n");
        html.append("    .company-info {\n");
        html.append("      width: 45%;\n");
        html.append("      text-align: right;\n");
        html.append("      font-size: 14px;\n");
        html.append("      line-height: 1.8;\n");
        html.append("    }\n");
        html.append("    .invoice-date {\n");
        html.append("      text-align: right;\n");
        html.append("      margin-top: 20px;\n");
        html.append("      font-size: 14px;\n");
        html.append("    }\n");
        html.append("    .salutation {\n");
        html.append("      margin: 30px 0;\n");
        html.append("      font-size: 14px;\n");
        html.append("    }\n");
        html.append("    .invoice-title {\n");
        html.append("      text-align: center;\n");
        html.append("      font-size: 18px;\n");
        html.append("      font-weight: bold;\n");
        html.append("      margin: 30px 0;\n");
        html.append("      color: #2c3e50;\n");
        html.append("    }\n");
        html.append("    .account-no {\n");
        html.append("      font-size: 14px;\n");
        html.append("      margin-bottom: 20px;\n");
        html.append("    }\n");
        html.append("    table { \n");
        html.append("      width: 100%; \n");
        html.append("      border-collapse: collapse; \n");
        html.append("      margin: 30px 0;\n");
        html.append("      font-size: 14px;\n");
        html.append("    }\n");
        html.append("    th { \n");
        html.append("      background: #2c3e50; \n");
        html.append("      color: white; \n");
        html.append("      padding: 12px 15px; \n");
        html.append("      text-align: center;\n");
        html.append("      font-weight: 600;\n");
        html.append("      border: 2px solid #2c3e50;\n");
        html.append("    }\n");
        html.append("    td { \n");
        html.append("      padding: 10px 15px; \n");
        html.append("      border: 1px solid #2c3e50;\n");
        html.append("      text-align: center;\n");
        html.append("    }\n");
        html.append("    tr:nth-child(even) { \n");
        html.append("      background: #f8f9fa; \n");
        html.append("    }\n");
        html.append("    .totals-section {\n");
        html.append("      width: 40%;\n");
        html.append("      margin-left: auto;\n");
        html.append("      margin-top: -5px;\n");
        html.append("    }\n");
        html.append("    .totals-section td {\n");
        html.append("      padding: 8px 15px;\n");
        html.append("      border: 2px solid #2c3e50;\n");
        html.append("    }\n");
        html.append("    .totals-section .label {\n");
        html.append("      text-align: right;\n");
        html.append("      font-weight: 600;\n");
        html.append("      background: #f8f9fa;\n");
        html.append("    }\n");
        html.append("    .totals-section .value {\n");
        html.append("      text-align: center;\n");
        html.append("      font-weight: 600;\n");
        html.append("    }\n");
        html.append("    .totals-section .final {\n");
        html.append("      background: #2c3e50;\n");
        html.append("      color: white;\n");
        html.append("      font-size: 15px;\n");
        html.append("    }\n");
        html.append("    .footer-text {\n");
        html.append("      margin: 40px 0;\n");
        html.append("      font-size: 14px;\n");
        html.append("      line-height: 1.8;\n");
        html.append("    }\n");
        html.append("    .sign-off {\n");
        html.append("      margin-top: 50px;\n");
        html.append("      font-size: 14px;\n");
        html.append("      line-height: 2.5;\n");
        html.append("    }\n");
        html.append("    @media print {\n");
        html.append("      body { background: white; padding: 0; }\n");
        html.append("      .invoice-container { box-shadow: none; padding: 40px; }\n");
        html.append("    }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class='invoice-container'>\n");

        // Header Section - Customer Address (Left) & Company Info (Right) and im in the middle
        html.append("    <div class='header-section'>\n");
        html.append("      <div class='customer-address'>\n");
        if (c != null) {
            html.append("        " + customerTitle + ". " + c.getName() + ",<br>\n");
            html.append("        " + (c.getAddress() != null ? c.getAddress() : "") + ",<br>\n");
            html.append("        " + (c.getTown() != null ? c.getTown() : "") + ",<br>\n");
            html.append("        " + (c.getPostcode() != null ? c.getPostcode() : "") + "\n");
        } else {
            html.append("        Walk-in Customer,<br>\n");
            html.append("        N/A<br>\n");
        }
        html.append("      </div>\n");
        html.append("      <div class='company-info'>\n");
        html.append("        Cosymed Ltd.,<br>\n");
        html.append("        3, High Level Drive,<br>\n");
        html.append("        Sydenham,<br>\n");
        html.append("        SE26 3ET<br>\n");
        html.append("        Phone: 0208 778 0124<br>\n");
        html.append("        Fax: 0208 778 0125\n");
        html.append("      </div>\n");
        html.append("    </div>\n");

        // Date (Right aligned, below company info) - brilliant
        html.append("    <div class='invoice-date'>\n");
        html.append("      " + dateStr + "\n");
        html.append("    </div>\n");

        // Salutation - I know very posh of me
        html.append("    <div class='salutation'>\n");
        html.append("      Dear " + customerTitle + ". " + customerLastName + ",\n");
        html.append("    </div>\n");

        // Invoice Title (Centered, Bold) - roar
        html.append("    <div class='invoice-title'>\n");
        html.append("      INVOICE NO.: " + invoiceNum + "\n");
        html.append("    </div>\n");

        // Account No - yes who are you to tell me no
        if (c != null) {
            html.append("    <div class='account-no'>\n");
            html.append("      Account No: CSM" + String.format("%06d", c.getId()) + "\n");
            html.append("    </div>\n");
        }

        // Items Table - all i do is worry and slave and defend you - okay i'll take this table
        html.append("    <table>\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th>Item ID</th>\n");
        html.append("          <th>Packages</th>\n");
        html.append("          <th>Package Cost, £</th>\n");
        html.append("          <th>Amount, £</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        for (CartItem item : cart) {
            double netUnitPrice = item.getPrice() / (1 + vatRate);
            double lineAmount = netUnitPrice * item.getQuantity();

            html.append("        <tr>\n");
            html.append("          <td>" + item.getName() + "</td>\n");
            html.append("          <td>" + item.getQuantity() + "</td>\n");
            html.append("          <td>" + String.format("%.2f", netUnitPrice) + "</td>\n");
            html.append("          <td>" + String.format("%.2f", lineAmount) + "</td>\n");
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");

        // Totals Section (Bottom right of table, matching template) - thought it was left
        html.append("    <table class='totals-section'>\n");
        html.append("      <tr>\n");
        html.append("        <td class='label'>Total</td>\n");
        html.append("        <td class='value'>" + String.format("%.2f", netTotal) + "</td>\n");
        html.append("      </tr>\n");
        html.append("      <tr>\n");
        html.append("        <td class='label'>VAT @ " + String.format("%.1f", vatRate * 100) + "%</td>\n");
        html.append("        <td class='value'>" + String.format("%.2f", vatAmount) + "</td>\n");
        html.append("      </tr>\n");
        html.append("      <tr class='final'>\n");
        html.append("        <td class='label'>Amount Due</td>\n");
        html.append("        <td class='value'>" + String.format("%.2f", grossTotal) + "</td>\n");
        html.append("      </tr>\n");
        html.append("    </table>\n");

        // Footer Text
        html.append("    <div class='footer-text'>\n");
        html.append("      Thank you for your valued custom. We look forward to receiving your payment in due course.\n");
        html.append("    </div>\n");

        // Sign-off - sign-on
        html.append("    <div class='sign-off'>\n");
        html.append("      Yours sincerely,\n");
        html.append("      <br><br><br>\n");
        html.append("      J. Faith\n");
        html.append("    </div>\n");

        html.append("  </div>\n");
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

    // CartItem inner class - what matters is whats inside
    public static class CartItem {
        private final Product product;
        private final IntegerProperty quantity = new SimpleIntegerProperty(1);

        public CartItem(Product p) {
            this.product = p;
        }

        public String getName() { return product.getName(); }
        public double getPrice() { return product.getPrice(); }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int q) { this.quantity.set(q); }
        public IntegerProperty quantityProperty() { return quantity; }
        public Product getProduct() { return product; }
    }

    // SaleRecord class for reporting
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