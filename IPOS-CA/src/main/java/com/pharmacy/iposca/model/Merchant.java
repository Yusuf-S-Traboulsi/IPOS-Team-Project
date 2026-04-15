package com.pharmacy.iposca.model;

import javafx.beans.property.*;

/**
 * Class representing a merchant in the IPOS-CA system.
 */
public class Merchant {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty companyName = new SimpleStringProperty();
    private final StringProperty contactInfo = new SimpleStringProperty();

    public Merchant(int id, String name, String contact) {
        this.id.set(id);
        this.companyName.set(name);
        this.contactInfo.set(contact);
    }

    //Getter methods
    public String getCompanyName() { return companyName.get(); }
    public String getContactInfo() { return contactInfo.get(); }
}