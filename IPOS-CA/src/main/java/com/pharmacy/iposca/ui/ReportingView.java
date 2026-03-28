package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;

public class ReportingView {
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private Label statusLabel;

    private ReportController reportLogic;
    private InventoryController inventory = InventoryController.getInstance();
    private CustomerController customers = CustomerController.getInstance();
    private SalesController sales = new SalesController(inventory);

    @FXML
    public void initialize() {
        reportLogic = new ReportController(inventory, customers, sales);

        // Set default date range: current month
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today);
    }

    @FXML
    private void generateTurnoverReport() {
        runReport(() -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            if (start == null || end == null || start.isAfter(end)) {
                throw new IllegalArgumentException("Please select a valid date range");
            }
            return reportLogic.generateTurnoverReport(start, end);
        }, "Turnover Report");
    }

    @FXML
    private void generateStockReport() {
        runReport(reportLogic::generateStockReport, "Stock Report");
    }

    @FXML
    private void generateDebtReport() {
        runReport(() -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            if (start == null || end == null || start.isAfter(end)) {
                throw new IllegalArgumentException("Please select a valid date range");
            }
            return reportLogic.generateDebtChangeReport(start, end);
        }, "Debt Change Report");
    }

    @FXML
    private void generateAllReports() {
        try {
            generateTurnoverReport();
            generateStockReport();
            generateDebtReport();
            showStatus("All reports generated successfully.", false);
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
        }
    }

    private void runReport(ReportGenerator generator, String reportName) {
        try {
            File report = generator.generate();
            if (report != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(report.toURI());
                showStatus(reportName + " opened in browser.", false);
            } else {
                showStatus("No data available for selected period.", true);
            }
        } catch (Exception e) {
            showStatus("Error generating " + reportName + ": " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green") + ";");
        }
    }

    @FunctionalInterface
    private interface ReportGenerator {
        File generate() throws Exception;
    }
}