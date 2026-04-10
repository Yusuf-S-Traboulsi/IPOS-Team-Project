package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.model.*;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Report Controller - Generates various business reports
 * All reports use data from ipos_ca database
 */
public class ReportController {

    private InventoryController inventory;
    private CustomerController customers;
    private SalesController sales;

    public ReportController(InventoryController inv, CustomerController cust, SalesController sal) {
        this.inventory = inv;
        this.customers = cust;
        this.sales = sal;
    }

    public File generateTurnoverReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("TurnoverReport_" + startDate + "_to_" + endDate + ".html");
        StringBuilder html = new StringBuilder();

        // Filter sales within date range
        ObservableList<SalesController.SaleRecord> allSales = sales.getSalesLog();
        List<SalesController.SaleRecord> periodSales = allSales.stream()
                .filter(s -> !s.getDate().isBefore(startDate) && !s.getDate().isAfter(endDate))
                .collect(Collectors.toList());

        double cashTotal = periodSales.stream()
                .filter(s -> "CASH".equals(s.getPaymentType()))
                .mapToDouble(SalesController.SaleRecord::getAmount).sum();

        double cardTotal = periodSales.stream()
                .filter(s -> "CARD".equals(s.getPaymentType()))
                .mapToDouble(SalesController.SaleRecord::getAmount).sum();

        double grandTotal = cashTotal + cardTotal;

        buildReportHeader(html, "Turnover Report", startDate, endDate);

        // Summary Section
        html.append("<div class='summary-box'>")
                .append("<h3>Summary</h3>")
                .append("<table>")
                .append("<tr><td>Cash Sales:</td><td>£").append(String.format("%.2f", cashTotal)).append("</td></tr>")
                .append("<tr><td>Card Sales:</td><td>£").append(String.format("%.2f", cardTotal)).append("</td></tr>")
                .append("<tr><td><strong>Grand Total:</strong></td><td><strong>£").append(String.format("%.2f", grandTotal)).append("</strong></td></tr>")
                .append("</table>")
                .append("</div>");

        // Transaction Details
        html.append("<h3>Transaction Details</h3>")
                .append("<table>")
                .append("<tr><th>ID</th><th>Date</th><th>Customer</th><th>Payment</th><th>Amount</th></tr>");

        for (SalesController.SaleRecord s : periodSales) {
            html.append("<tr>")
                    .append("<td>").append(s.getId()).append("</td>")
                    .append("<td>").append(s.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>")
                    .append("<td>").append(s.getCustomerName()).append("</td>")
                    .append("<td>").append(s.getPaymentType()).append("</td>")
                    .append("<td>£").append(String.format("%.2f", s.getAmount())).append("</td>")
                    .append("</tr>");
        }
        html.append("</table>");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }

    public File generateStockReport() {
        File file = new File("StockReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();
        double vatRate = inventory.getVatRate();

        buildReportHeader(html, "Stock Availability Report", LocalDate.now(), LocalDate.now());

        html.append("<table>")
                .append("<tr><th>Item ID</th><th>Description</th><th>Stock (packs)</th>")
                .append("<th>Unit Price (£)</th><th>Value Excl. VAT (£)</th><th>VAT (£)</th><th>Value Incl. VAT (£)</th></tr>");

        double totalExclVat = 0, totalVat = 0, totalInclVat = 0;

        for (Product p : inventory.getProducts()) {
            double unitPriceExclVat = p.getPrice() / (1 + vatRate);
            double lineExclVat = unitPriceExclVat * p.getStock();
            double lineVat = lineExclVat * vatRate;
            double lineInclVat = lineExclVat + lineVat;

            totalExclVat += lineExclVat;
            totalVat += lineVat;
            totalInclVat += lineInclVat;

            html.append("<tr>")
                    .append("<td>").append(String.format("%07d", p.getId())).append("</td>")
                    .append("<td>").append(p.getName().trim()).append("</td>")
                    .append("<td>").append(p.getStock()).append("</td>")
                    .append("<td>").append(String.format("%.2f", unitPriceExclVat)).append("</td>")
                    .append("<td>").append(String.format("%.2f", lineExclVat)).append("</td>")
                    .append("<td>").append(String.format("%.2f", lineVat)).append("</td>")
                    .append("<td>").append(String.format("%.2f", lineInclVat)).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>")
                .append("<h3>Totals</h3>")
                .append("<table>")
                .append("<tr><td>Total Value (Excl. VAT):</td><td>£").append(String.format("%.2f", totalExclVat)).append("</td></tr>")
                .append("<tr><td>Total VAT:</td><td>£").append(String.format("%.2f", totalVat)).append("</td></tr>")
                .append("<tr><td><strong>Total Value (Incl. VAT):</strong></td><td><strong>£").append(String.format("%.2f", totalInclVat)).append("</strong></td></tr>")
                .append("</table>");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }

    public File generateDebtChangeReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("DebtChangeReport_" + startDate + "_to_" + endDate + ".html");
        StringBuilder html = new StringBuilder();

        // Calculate aggregated debt from all customers
        double openingDebt = customers.getCustomerData().stream()
                .mapToDouble(Customer::getCurrentDebt).sum();

        double paymentsReceived = 0;
        double closingDebt = openingDebt - paymentsReceived;

        buildReportHeader(html, "Aggregated Debt Change Report", startDate, endDate);

        html.append("<table>")
                .append("<tr><td>Aggregated Debt at Start of Period:</td><td>£").append(String.format("%.2f", openingDebt)).append("</td></tr>")
                .append("<tr><td>Payments Received from Debtors:</td><td>£").append(String.format("%.2f", paymentsReceived)).append("</td></tr>")
                .append("<tr><td><strong>Aggregated Debt at End of Period:</strong></td><td><strong>£").append(String.format("%.2f", closingDebt)).append("</strong></td></tr>")
                .append("</table>");

        html.append("<h3>Account Holder Details</h3>")
                .append("<table>")
                .append("<tr><th>Account ID</th><th>Name</th><th>Current Debt</th><th>Status</th></tr>");

        for (Customer c : customers.getCustomerData()) {
            if (c.getCurrentDebt() > 0) {
                html.append("<tr>")
                        .append("<td>").append(String.format("%06d", c.getId())).append("</td>")
                        .append("<td>").append(c.getTitle()).append(" ").append(c.getName()).append("</td>")
                        .append("<td>£").append(String.format("%.2f", c.getCurrentDebt())).append("</td>")
                        .append("<td>").append(c.getAccountStatus()).append("</td>")
                        .append("</tr>");
            }
        }
        html.append("</table>");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }


    private void buildReportHeader(StringBuilder html, String title, LocalDate startDate, LocalDate endDate) {
        html.append("<!DOCTYPE html>")
                .append("<html><head><title>").append(title).append("</title>")
                .append("<style>")
                .append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }")
                .append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }")
                .append("h3 { color: #34495e; margin-top: 30px; }")
                .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
                .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }")
                .append("th { background: #3498db; color: white; font-weight: 600; }")
                .append("tr:nth-child(even) { background: #f8f9fa; }")
                .append("tr:hover { background: #e8f4f8; }")
                .append(".summary-box { background: #ecf0f1; padding: 20px; margin: 20px 0; border-left: 4px solid #3498db; border-radius: 5px; }")
                .append("hr { border: none; border-top: 2px solid #bdc3c7; margin: 30px 0; }")
                .append("</style>")
                .append("</head><body>")
                .append("<h1>").append(title).append("</h1>")
                .append("<p><strong>Period:</strong> ").append(startDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .append(" to ").append(endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))).append("</p>");
    }

    private void buildReportFooter(StringBuilder html) {
        html.append("<hr>")
                .append("<p><strong>Generated:</strong> ").append(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))).append("</p>")
                .append("<p><strong>By:</strong> Manager • InfoPharma Ltd.</p>")
                .append("<p><em>Confidential - Internal Use Only</em></p>")
                .append("</body></html>");
    }

    private void writeToFile(File file, StringBuilder html) {
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(html.toString());
            System.out.println("Report generated: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error writing report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}