package com.pharmacy.iposca.model;

import javafx.beans.property.*;

/**
 * This class represents a document template for the pharmacy inventory system.
 */
public class DocumentTemplate {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty templateName = new SimpleStringProperty();
    private final StringProperty templateType = new SimpleStringProperty(); // INVOICE, MONTHLY_STATEMENT, FIRST_REMINDER, SECOND_REMINDER
    private final StringProperty subjectTemplate = new SimpleStringProperty();
    private final StringProperty bodyTemplate = new SimpleStringProperty();
    private final StringProperty footerTemplate = new SimpleStringProperty();
    private final BooleanProperty isActive = new SimpleBooleanProperty(true);

    public DocumentTemplate() {
        //Default constructor
    }

    public DocumentTemplate(String name, String type, String subject, String body, String footer) {
        this.templateName.set(name);
        this.templateType.set(type);
        this.subjectTemplate.set(subject);
        this.bodyTemplate.set(body);
        this.footerTemplate.set(footer);
    }

    //Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public String getTemplateName() { return templateName.get(); }
    public void setTemplateName(String name) { this.templateName.set(name); } //INVOICE, MONTHLY_STATEMENT, FIRST_REMINDER, SECOND_REMINDER

    public String getTemplateType() { return templateType.get(); }
    public void setTemplateType(String type) { this.templateType.set(type); }

    public String getSubjectTemplate() { return subjectTemplate.get(); }
    public void setSubjectTemplate(String subject) { this.subjectTemplate.set(subject); }

    public String getBodyTemplate() { return bodyTemplate.get(); }
    public void setBodyTemplate(String body) { this.bodyTemplate.set(body); }

    public String getFooterTemplate() { return footerTemplate.get(); }
    public void setFooterTemplate(String footer) { this.footerTemplate.set(footer); }

    public boolean isActive() { return isActive.get(); }
    public void setActive(boolean active) { this.isActive.set(active); }

    //Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty templateNameProperty() { return templateName; }
    public StringProperty templateTypeProperty() { return templateType; }
    public BooleanProperty isActiveProperty() { return isActive; }
}