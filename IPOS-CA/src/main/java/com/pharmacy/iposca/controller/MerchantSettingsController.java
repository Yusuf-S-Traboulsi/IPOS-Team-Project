package com.pharmacy.iposca.controller;

import com.pharmacy.iposca.db.DatabaseConnector;
import com.pharmacy.iposca.model.DocumentTemplate;
import com.pharmacy.iposca.model.MerchantSettings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Merchant Settings Controller - Manages templates and company identity
 * Access: Manager role only
 */
public class MerchantSettingsController {

    private static MerchantSettingsController instance;
    private MerchantSettings merchantSettings;
    private ObservableList<DocumentTemplate> templates;
    private Map<String, DocumentTemplate> templateMap;

    private MerchantSettingsController() {
        templates = FXCollections.observableArrayList();
        templateMap = new HashMap<>();
        loadMerchantSettings();
        loadDocumentTemplates();
    }

    public static synchronized MerchantSettingsController getInstance() {
        if (instance == null) {
            instance = new MerchantSettingsController();
        }
        return instance;
    }

    /**
     * Load merchant settings from database
     */
    private void loadMerchantSettings() {
        String sql = "SELECT * FROM merchant_settings LIMIT 1";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                merchantSettings = new MerchantSettings();
                merchantSettings.setId(rs.getInt("id"));
                merchantSettings.setCompanyName(rs.getString("company_name"));
                merchantSettings.setAddressLine1(rs.getString("address_line1"));
                merchantSettings.setAddressLine2(rs.getString("address_line2"));
                merchantSettings.setCity(rs.getString("city"));
                merchantSettings.setPostcode(rs.getString("postcode"));
                merchantSettings.setPhone(rs.getString("phone"));
                merchantSettings.setFax(rs.getString("fax"));
                merchantSettings.setEmail(rs.getString("email"));
                merchantSettings.setWebsite(rs.getString("website"));
                merchantSettings.setLogoPath(rs.getString("logo_path"));
                merchantSettings.setRegistrationNumber(rs.getString("registration_number"));
                merchantSettings.setVatNumber(rs.getString("vat_number"));

                System.out.println("Loaded merchant settings: " + merchantSettings.getCompanyName());
            } else {
                // Create default settings
                merchantSettings = new MerchantSettings();
                merchantSettings.setCompanyName("InfoPharma Ltd.");
                merchantSettings.setAddressLine1("19 High St.");
                merchantSettings.setCity("Ashford");
                merchantSettings.setPostcode("Kent");
                merchantSettings.setPhone("0208 778 0124");
                merchantSettings.setFax("0208 778 0125");
                merchantSettings.setEmail("accounts@infopharma.co.uk");
                merchantSettings.setDirectorName("A. Petite");
            }

        } catch (SQLException e) {
            System.err.println("Error loading merchant settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load document templates from database
     */
    private void loadDocumentTemplates() {
        String sql = "SELECT * FROM document_templates WHERE is_active = TRUE";
        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            templates.clear();
            templateMap.clear();

            while (rs.next()) {
                DocumentTemplate template = new DocumentTemplate();
                template.setId(rs.getInt("id"));
                template.setTemplateName(rs.getString("template_name"));
                template.setTemplateType(rs.getString("template_type"));
                template.setSubjectTemplate(rs.getString("subject_template"));
                template.setBodyTemplate(rs.getString("body_template"));
                template.setFooterTemplate(rs.getString("footer_template"));
                template.setActive(rs.getBoolean("is_active"));

                templates.add(template);
                templateMap.put(template.getTemplateType(), template);
            }

            System.out.println("Loaded " + templates.size() + " document templates");
        } catch (SQLException e) {
            System.err.println("Error loading templates: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save merchant settings to database
     */
    public boolean saveMerchantSettings(MerchantSettings settings) {
        String sql;
        if (settings.getId() > 0) {
            sql = "UPDATE merchant_settings SET company_name = ?, address_line1 = ?, address_line2 = ?, " +
                    "city = ?, postcode = ?, phone = ?, fax = ?, email = ?, website = ?, " +
                    "logo_path = ?, registration_number = ?, vat_number = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO merchant_settings (company_name, address_line1, address_line2, city, postcode, " +
                    "phone, fax, email, website, logo_path, registration_number, vat_number) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, settings.getCompanyName());
            stmt.setString(2, settings.getAddressLine1());
            stmt.setString(3, settings.getAddressLine2());
            stmt.setString(4, settings.getCity());
            stmt.setString(5, settings.getPostcode());
            stmt.setString(6, settings.getPhone());
            stmt.setString(7, settings.getFax());
            stmt.setString(8, settings.getEmail());
            stmt.setString(9, settings.getWebsite());
            stmt.setString(10, settings.getLogoPath());
            stmt.setString(11, settings.getRegistrationNumber());
            stmt.setString(12, settings.getVatNumber());

            if (settings.getId() > 0) {
                stmt.setInt(13, settings.getId());
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                this.merchantSettings = settings;
                System.out.println("Merchant settings saved");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving merchant settings: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Save document template to database
     */
    public boolean saveTemplate(DocumentTemplate template) {
        String sql;
        if (template.getId() > 0) {
            sql = "UPDATE document_templates SET subject_template = ?, body_template = ?, " +
                    "footer_template = ?, is_active = ? WHERE id = ?";
        } else {
            sql = "INSERT INTO document_templates (template_name, template_type, subject_template, " +
                    "body_template, footer_template, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (template.getId() > 0) {
                stmt.setString(1, template.getSubjectTemplate());
                stmt.setString(2, template.getBodyTemplate());
                stmt.setString(3, template.getFooterTemplate());
                stmt.setBoolean(4, template.isActive());
                stmt.setInt(5, template.getId());
            } else {
                stmt.setString(1, template.getTemplateName());
                stmt.setString(2, template.getTemplateType());
                stmt.setString(3, template.getSubjectTemplate());
                stmt.setString(4, template.getBodyTemplate());
                stmt.setString(5, template.getFooterTemplate());
                stmt.setBoolean(6, template.isActive());
            }

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                loadDocumentTemplates(); // Reload templates
                System.out.println("Template saved: " + template.getTemplateName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving template: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get template by type
     */
    public DocumentTemplate getTemplateByType(String type) {
        return templateMap.get(type);
    }

    /**
     * Process template placeholders with actual values
     */
    public String processTemplate(String template, Map<String, String> placeholders) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * Get merchant settings
     */
    public MerchantSettings getMerchantSettings() {
        return merchantSettings;
    }

    /**
     * Get all templates
     */
    public ObservableList<DocumentTemplate> getTemplates() {
        return templates;
    }

    /**
     * Refresh templates from database
     */
    public void refreshTemplates() {
        loadDocumentTemplates();
    }
}