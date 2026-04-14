package com.pharmacy.iposca.ui;

import com.pharmacy.iposca.controller.MerchantSettingsController;
import com.pharmacy.iposca.model.DocumentTemplate;
import com.pharmacy.iposca.model.MerchantSettings;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * This UI class represents the Merchant Settings view.
 * Accessible only by Manager role
 */
public class MerchantSettingsView extends TabPane {

    private MerchantSettingsController controller = MerchantSettingsController.getInstance();

    // Merchant Settings Fields
    private TextField companyNameField, address1Field, address2Field, cityField, postcodeField;
    private TextField phoneField, faxField, emailField, websiteField;
    private TextField regNumberField, vatNumberField, directorNameField;
    private TextField logoPathField;
    private ImageView logoImageView;
    private Label infoLabel;

    // Template Fields
    private ComboBox<String> templateTypeCombo;
    private TextArea subjectArea, bodyArea, footerArea;

    public MerchantSettingsView() {
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        // Create tabs
        Tab merchantTab = new Tab("Merchant Identity");
        merchantTab.setContent(createMerchantSettingsPanel());
        merchantTab.setClosable(false);

        Tab invoiceTab = new Tab("Invoice Template");
        invoiceTab.setContent(createTemplatePanel("INVOICE"));
        invoiceTab.setClosable(false);

        Tab statementTab = new Tab("Monthly Statement Template");
        statementTab.setContent(createTemplatePanel("MONTHLY_STATEMENT"));
        statementTab.setClosable(false);

        Tab reminder1Tab = new Tab("First Reminder Template");
        reminder1Tab.setContent(createTemplatePanel("FIRST_REMINDER"));
        reminder1Tab.setClosable(false);

        Tab reminder2Tab = new Tab("Second Reminder Template");
        reminder2Tab.setContent(createTemplatePanel("SECOND_REMINDER"));
        reminder2Tab.setClosable(false);

        getTabs().addAll(merchantTab, invoiceTab, statementTab, reminder1Tab, reminder2Tab);
    }

    /**
     * Create Merchant Settings Panel with Logo Upload
     */
    private VBox createMerchantSettingsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        MerchantSettings settings = controller.getMerchantSettings();

        // Title
        Label titleLabel = new Label("Merchant/Pharmacy Identity Settings");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Separator separator1 = new Separator();

        // Logo Section
        Label logoLabel = new Label("Company Logo:");
        logoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox logoBox = new HBox(15);
        logoBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Logo preview
        logoImageView = new ImageView();
        logoImageView.setFitWidth(150);
        logoImageView.setFitHeight(100);
        logoImageView.setPreserveRatio(true);
        logoImageView.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: white;");

        // Load existing logo if exists
        String logoPath = settings.getLogoPath();
        if (logoPath != null && !logoPath.isEmpty()) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                logoImageView.setImage(new Image(logoFile.toURI().toString()));
            }
        }

        VBox logoPreviewBox = new VBox(10, logoLabel, logoImageView);

        VBox logoButtonBox = new VBox(10);
        logoPathField = new TextField(settings.getLogoPath());
        logoPathField.setPromptText("Logo file path");
        logoPathField.setPrefWidth(300);
        logoPathField.setEditable(false);

        Button uploadLogoButton = new Button("Upload Logo");
        uploadLogoButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        uploadLogoButton.setPadding(new Insets(10, 20, 10, 20));
        uploadLogoButton.setOnAction(e -> handleUploadLogo());

        Button removeLogoButton = new Button("Remove Logo");
        removeLogoButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        removeLogoButton.setPadding(new Insets(10, 20, 10, 20));
        removeLogoButton.setOnAction(e -> handleRemoveLogo());

        logoButtonBox.getChildren().addAll(logoPathField, uploadLogoButton, removeLogoButton);
        logoBox.getChildren().addAll(logoPreviewBox, logoButtonBox);

        Separator separator2 = new Separator();

        // Company Information Section
        Label companyLabel = new Label("Company Information");
        companyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane companyGrid = new GridPane();
        companyGrid.setHgap(10);
        companyGrid.setVgap(10);

        companyNameField = createTextField(settings.getCompanyName(), 300);
        address1Field = createTextField(settings.getAddressLine1(), 300);
        address2Field = createTextField(settings.getAddressLine2(), 300);
        cityField = createTextField(settings.getCity(), 150);
        postcodeField = createTextField(settings.getPostcode(), 150);

        companyGrid.add(createLabel("Company Name:"), 0, 0);
        companyGrid.add(companyNameField, 1, 0);
        companyGrid.add(createLabel("Address Line 1:"), 0, 1);
        companyGrid.add(address1Field, 1, 1);
        companyGrid.add(createLabel("Address Line 2:"), 0, 2);
        companyGrid.add(address2Field, 1, 2);
        companyGrid.add(createLabel("City:"), 0, 3);
        companyGrid.add(cityField, 1, 3);
        companyGrid.add(createLabel("Postcode:"), 0, 4);
        companyGrid.add(postcodeField, 1, 4);

        // Contact Information Section
        Label contactLabel = new Label("Contact Information");
        contactLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane contactGrid = new GridPane();
        contactGrid.setHgap(10);
        contactGrid.setVgap(10);

        phoneField = createTextField(settings.getPhone(), 200);
        faxField = createTextField(settings.getFax(), 200);
        emailField = createTextField(settings.getEmail(), 250);
        websiteField = createTextField(settings.getWebsite(), 250);
        directorNameField = createTextField(settings.getDirectorName(), 200);

        contactGrid.add(createLabel("Phone:"), 0, 0);
        contactGrid.add(phoneField, 1, 0);
        contactGrid.add(createLabel("Fax:"), 0, 1);
        contactGrid.add(faxField, 1, 1);
        contactGrid.add(createLabel("Email:"), 0, 2);
        contactGrid.add(emailField, 1, 2);
        contactGrid.add(createLabel("Website:"), 0, 3);
        contactGrid.add(websiteField, 1, 3);
        contactGrid.add(createLabel("Director Name:"), 0, 4);
        contactGrid.add(directorNameField, 1, 4);

        // Registration Information
        Label regLabel = new Label("Registration Information");
        regLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        GridPane regGrid = new GridPane();
        regGrid.setHgap(10);
        regGrid.setVgap(10);

        regNumberField = createTextField(settings.getRegistrationNumber(), 200);
        vatNumberField = createTextField(settings.getVatNumber(), 200);

        regGrid.add(createLabel("Registration Number:"), 0, 0);
        regGrid.add(regNumberField, 1, 0);
        regGrid.add(createLabel("VAT Number:"), 0, 1);
        regGrid.add(vatNumberField, 1, 1);

        // Save Button
        Button saveButton = new Button("Save Merchant Settings");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        saveButton.setOnAction(e -> handleSaveMerchantSettings());

        // Info Label
        infoLabel = new Label();
        infoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        // Help Text
        Label helpText = new Label("These settings will be used in all invoices, reminders, and statements.");
        helpText.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        panel.getChildren().addAll(
                titleLabel,
                separator1,
                logoBox,
                separator2,
                companyLabel,
                companyGrid,
                contactLabel,
                contactGrid,
                regLabel,
                regGrid,
                saveButton,
                helpText,
                infoLabel
        );

        return panel;
    }

    /**
     * Handle logo upload
     */
    private void handleUploadLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Company Logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) logoImageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Copy logo to application directory
                String appDir = System.getProperty("user.dir") + "/logos/";
                File appLogoDir = new File(appDir);
                if (!appLogoDir.exists()) {
                    appLogoDir.mkdirs();
                }

                String newLogoPath = appDir + "company_logo_" + System.currentTimeMillis() +
                        selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                File newLogoFile = new File(newLogoPath);

                Files.copy(selectedFile.toPath(), newLogoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Update UI and settings
                logoPathField.setText(newLogoPath);
                logoImageView.setImage(new Image(newLogoFile.toURI().toString()));

                infoLabel.setText("Logo uploaded successfully: " + newLogoPath);
                infoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

            } catch (IOException e) {
                infoLabel.setText("Error uploading logo: " + e.getMessage());
                infoLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle logo removal
     */
    private void handleRemoveLogo() {
        logoPathField.clear();
        logoImageView.setImage(null);
        infoLabel.setText("Logo removed");
        infoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
    }

    /**
     * Create Template Editor Panel
     */
    private VBox createTemplatePanel(String templateType) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #f8f9fa;");

        DocumentTemplate template = controller.getTemplateByType(templateType);
        if (template == null) {
            template = new DocumentTemplate("Default " + templateType, templateType, "", "", "");
        }

        // Title
        Label titleLabel = new Label("Template: " + template.getTemplateName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Separator separator1 = new Separator();

        // Available Placeholders
        Label placeholdersLabel = new Label("Available Placeholders:");
        placeholdersLabel.setStyle("-fx-font-weight: bold;");

        String placeholders = "{company_name}, {company_phone}, {company_email}, {company_address}, " +
                "{customer_title}, {customer_last_name}, {customer_name}, {account_number}, " +
                "{invoice_number}, {invoice_date}, {total_amount}, {payment_due_date}, " +
                "{director_name}, {items_table}, {statement_month}, {first_reminder_date}";

        Label placeholdersText = new Label(placeholders);
        placeholdersText.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        placeholdersText.setWrapText(true);

        // Subject
        Label subjectLabel = new Label("Subject:");
        subjectLabel.setStyle("-fx-font-weight: bold;");
        subjectArea = createTextArea(template.getSubjectTemplate(), 400, 2);

        // Body
        Label bodyLabel = new Label("Body:");
        bodyLabel.setStyle("-fx-font-weight: bold;");
        bodyArea = createTextArea(template.getBodyTemplate(), 500, 12);

        // Footer
        Label footerLabel = new Label("Footer/Sign-off:");
        footerLabel.setStyle("-fx-font-weight: bold;");
        footerArea = createTextArea(template.getFooterTemplate(), 400, 5);

        // Save Button
        Button saveButton = new Button("Save Template");
        saveButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        saveButton.setOnAction(e -> handleSaveTemplate(templateType));

        // Preview Button
        Button previewButton = new Button("Preview");
        previewButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        previewButton.setOnAction(e -> handlePreviewTemplate(templateType));

        HBox buttonBox = new HBox(10, saveButton, previewButton);

        // Info Label
        Label templateInfoLabel = new Label();
        templateInfoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        panel.getChildren().addAll(
                titleLabel,
                separator1,
                placeholdersLabel,
                placeholdersText,
                subjectLabel,
                subjectArea,
                bodyLabel,
                bodyArea,
                footerLabel,
                footerArea,
                buttonBox,
                templateInfoLabel
        );

        return panel;
    }

    private TextField createTextField(String text, double width) {
        TextField field = new TextField(text);
        field.setPrefWidth(width);
        field.setStyle("-fx-padding: 8px;");
        return field;
    }

    private TextArea createTextArea(String text, double width, int rows) {
        TextArea area = new TextArea(text);
        area.setPrefWidth(width);
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        area.setStyle("-fx-padding: 8px;");
        return area;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    /**
     * Method to handle saving merchant settings changes
     */
    private void handleSaveMerchantSettings() {
        MerchantSettings settings = controller.getMerchantSettings();
        settings.setCompanyName(companyNameField.getText());
        settings.setAddressLine1(address1Field.getText());
        settings.setAddressLine2(address2Field.getText());
        settings.setCity(cityField.getText());
        settings.setPostcode(postcodeField.getText());
        settings.setPhone(phoneField.getText());
        settings.setFax(faxField.getText());
        settings.setEmail(emailField.getText());
        settings.setWebsite(websiteField.getText());
        settings.setRegistrationNumber(regNumberField.getText());
        settings.setVatNumber(vatNumberField.getText());
        settings.setDirectorName(directorNameField.getText());
        settings.setLogoPath(logoPathField.getText());

        boolean success = controller.saveMerchantSettings(settings);

        if (success) {
            infoLabel.setText("Merchant settings saved successfully!");
            infoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            infoLabel.setText("Failed to save merchant settings");
            infoLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    private void handleSaveTemplate(String templateType) {
        DocumentTemplate template = controller.getTemplateByType(templateType);
        if (template == null) {
            template = new DocumentTemplate("Default " + templateType, templateType, "", "", "");
        }

        template.setSubjectTemplate(subjectArea.getText());
        template.setBodyTemplate(bodyArea.getText());
        template.setFooterTemplate(footerArea.getText());

        boolean success = controller.saveTemplate(template);

        if (success) {
            infoLabel.setText("Template saved successfully!");
            infoLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else {
            infoLabel.setText("Failed to save template");
            infoLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    /**
     * Shows template preview in styled dialog
     */
    private void handlePreviewTemplate(String templateType) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Template Preview");
        alert.setHeaderText("Preview: " + templateType);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setPrefHeight(500);

        String preview = "Subject: " + subjectArea.getText() + "\n\n" +
                "Body:\n" + bodyArea.getText() + "\n\n" +
                "Footer:\n" + footerArea.getText();

        TextArea previewArea = new TextArea(preview);
        previewArea.setPrefWidth(580);
        previewArea.setPrefHeight(400);
        previewArea.setEditable(false);
        previewArea.setWrapText(true);

        alert.getDialogPane().setContent(previewArea);
        alert.showAndWait();
    }
}