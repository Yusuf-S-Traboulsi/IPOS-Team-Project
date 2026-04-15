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
 * Controller class for generating reports
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

    /**
     * Generates HTML turnover report for sales within the date range
     */
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

        //Summary section
        html.append("<div class='summary-box'>\n")
                .append("<h3>Summary</h3>\n")
                .append("<table>\n")
                .append("<tr><td>Cash Sales:</td><td>&pound;").append(String.format("%.2f", cashTotal)).append("</td></tr>\n")
                .append("<tr><td>Card Sales:</td><td>&pound;").append(String.format("%.2f", cardTotal)).append("</td></tr>\n")
                .append("<tr><td><strong>Grand Total:</strong></td><td><strong>&pound;").append(String.format("%.2f", grandTotal)).append("</strong></td></tr>\n")
                .append("</table>\n")
                .append("</div>\n");

        //Transaction details
        html.append("<h3>Transaction Details</h3>\n")
                .append("<table>\n")
                .append("<tr><th>ID</th><th>Date</th><th>Customer</th><th>Payment</th><th>Amount</th></tr>\n");

        for (SalesController.SaleRecord s : periodSales) {
            html.append("<tr>\n")
                    .append("<td>").append(s.getId()).append("</td>\n")
                    .append("<td>").append(s.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</td>\n")
                    .append("<td>").append(s.getCustomerName()).append("</td>\n")
                    .append("<td>").append(s.getPaymentType()).append("</td>\n")
                    .append("<td>&pound;").append(String.format("%.2f", s.getAmount())).append("</td>\n")
                    .append("</tr>\n");
        }
        html.append("</table>\n");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }

    /**
     * Generates stock report with VAT breakdowns and totals
     */
    public File generateStockReport() {
        File file = new File("StockReport_" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + ".html");
        StringBuilder html = new StringBuilder();
        double vatRate = inventory.getVatRate();

        buildReportHeader(html, "Stock Availability Report", LocalDate.now(), LocalDate.now());

        html.append("<table>\n")
                .append("<tr><th>Item ID</th><th>Description</th><th>Stock (packs)</th> ")
                .append("<th>Unit Price (&pound;)</th><th>Value Excl. VAT (&pound;)</th><th>VAT (&pound;)</th><th>Value Incl. VAT (&pound;)</th></tr>\n");

        double totalExclVat = 0, totalVat = 0, totalInclVat = 0;

        for (Product p : inventory.getProducts()) {
            double unitPriceExclVat = p.getPrice() / (1 + vatRate);
            double lineExclVat = unitPriceExclVat * p.getStock();
            double lineVat = lineExclVat * vatRate;
            double lineInclVat = lineExclVat + lineVat;

            totalExclVat += lineExclVat;
            totalVat += lineVat;
            totalInclVat += lineInclVat;

            html.append("<tr>\n")
                    .append("<td>").append(String.format("%07d", p.getId())).append("</td>\n")
                    .append("<td>").append(p.getName().trim()).append("</td>\n")
                    .append("<td>").append(p.getStock()).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", unitPriceExclVat)).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", lineExclVat)).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", lineVat)).append("</td>\n")
                    .append("<td>").append(String.format("%.2f", lineInclVat)).append("</td>\n")
                    .append("</tr>\n");
        }

        html.append("</table>\n")
                .append("<h3>Totals</h3>\n")
                .append("<table>\n")
                .append("<tr><td>Total Value (Excl. VAT):</td><td>&pound;").append(String.format("%.2f", totalExclVat)).append("</td></tr>\n")
                .append("<tr><td>Total VAT:</td><td>&pound;").append(String.format("%.2f", totalVat)).append("</td></tr>\n")
                .append("<tr><td><strong>Total Value (Incl. VAT):</strong></td><td><strong>&pound;").append(String.format("%.2f", totalInclVat)).append("</strong></td></tr>\n")
                .append("</table>\n");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }

    /**
     * Generates debt change HTML report with customer details
     */
    public File generateDebtChangeReport(LocalDate startDate, LocalDate endDate) {
        File file = new File("DebtChangeReport_" + startDate + "_to_" + endDate + ".html");
        StringBuilder html = new StringBuilder();

        // Calculate aggregated debt from all customers
        double openingDebt = customers.getCustomerData().stream()
                .mapToDouble(Customer::getCurrentDebt).sum();

        double paymentsReceived = 0;
        double closingDebt = openingDebt - paymentsReceived;

        buildReportHeader(html, "Aggregated Debt Change Report", startDate, endDate);

        html.append("<table>\n")
                .append("<tr><td>Aggregated Debt at Start of Period:</td><td>&pound;").append(String.format("%.2f", openingDebt)).append("</td></tr>\n")
                .append("<tr><td>Payments Received from Debtors:</td><td>&pound;").append(String.format("%.2f", paymentsReceived)).append("</td></tr>\n")
                .append("<tr><td><strong>Aggregated Debt at End of Period:</strong></td><td><strong>&pound;").append(String.format("%.2f", closingDebt)).append("</strong></td></tr>\n")
                .append("</table>\n");

        html.append("<h3>Account Holder Details</h3>\n")
                .append("<table>\n")
                .append("<tr><th>Account ID</th><th>Name</th><th>Current Debt</th><th>Status</th></tr>\n");

        for (Customer c : customers.getCustomerData()) {
            if (c.getCurrentDebt() > 0) {
                html.append("<tr>\n")
                        .append("<td>").append(String.format("%06d", c.getId())).append("</td>\n")
                        .append("<td>").append(c.getTitle()).append(" ").append(c.getName()).append("</td>\n")
                        .append("<td>&pound;").append(String.format("%.2f", c.getCurrentDebt())).append("</td>\n")
                        .append("<td>").append(c.getAccountStatus()).append("</td>\n")
                        .append("</tr>\n");
            }
        }
        html.append("</table>\n");

        buildReportFooter(html);
        writeToFile(file, html);
        return file;
    }

    private void buildReportHeader(StringBuilder html, String title, LocalDate startDate, LocalDate endDate) {
        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n<meta charset='UTF-8'>\n")
                .append("<style>\n")
                .append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #2c3e50; line-height: 1.6; }\n")
                .append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n")
                .append("h3 { color: #34495e; margin-top: 30px; }\n")
                .append("table { border-collapse: collapse; width: 100%; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n")
                .append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n")
                .append("th { background: #3498db; color: white; font-weight: 600; }\n")
                .append("tr:nth-child(even) { background: #f8f9fa; }\n")
                .append("tr:hover { background: #e8f4f8; }\n")
                .append(".summary-box { background: #ecf0f1; padding: 20px; margin: 20px 0; border-left: 4px solid #3498db; border-radius: 5px; }\n")
                .append("hr { border: none; border-top: 2px solid #bdc3c7; margin: 30px 0; }\n")
                .append("</style>\n</head>\n<body>\n")
                .append("<hr>\n")
                .append("<h1>").append(title).append("</h1>\n")
                .append("<p>Period: ").append(startDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                .append(" to ").append(endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))).append("</p>\n");
    }

    private void buildReportFooter(StringBuilder html) {
        html.append("<hr>\n")
                .append("<p>Generated: ").append(LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))).append("</p>\n")
                .append("<p>By: Manager &bull; InfoPharma Ltd.</p>\n")
                .append("<p>Confidential - Internal Use Only</p>\n")
                .append("</body>\n</html>\n");
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