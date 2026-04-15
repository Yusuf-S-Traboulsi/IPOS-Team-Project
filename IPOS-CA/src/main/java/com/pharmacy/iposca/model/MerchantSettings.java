package com.pharmacy.iposca.model;

import javafx.beans.property.*;

/**
 * This class stores the merchant settings.
 * */
public class MerchantSettings {

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty companyName = new SimpleStringProperty();
    private final StringProperty addressLine1 = new SimpleStringProperty();
    private final StringProperty addressLine2 = new SimpleStringProperty();
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty postcode = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty fax = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty website = new SimpleStringProperty();
    private final StringProperty logoPath = new SimpleStringProperty();
    private final StringProperty registrationNumber = new SimpleStringProperty();
    private final StringProperty vatNumber = new SimpleStringProperty();
    private final StringProperty directorName = new SimpleStringProperty("Mr. Lancaster");

    public MerchantSettings() {
        //Default constructor
    }

    //Getters and Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public String getCompanyName() { return companyName.get(); }
    public void setCompanyName(String name) { this.companyName.set(name); }

    public String getAddressLine1() { return addressLine1.get(); }
    public void setAddressLine1(String address) { this.addressLine1.set(address); }

    public String getAddressLine2() { return addressLine2.get(); }
    public void setAddressLine2(String address) { this.addressLine2.set(address); }

    public String getCity() { return city.get(); }
    public void setCity(String city) { this.city.set(city); }

    public String getPostcode() { return postcode.get(); }
    public void setPostcode(String postcode) { this.postcode.set(postcode); }

    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone); }

    public String getFax() { return fax.get(); }
    public void setFax(String fax) { this.fax.set(fax); }

    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }

    public String getWebsite() { return website.get(); }
    public void setWebsite(String website) { this.website.set(website); }

    public String getLogoPath() { return logoPath.get(); }
    public void setLogoPath(String path) { this.logoPath.set(path); }

    public String getRegistrationNumber() { return registrationNumber.get(); }
    public void setRegistrationNumber(String reg) { this.registrationNumber.set(reg); }

    public String getVatNumber() { return vatNumber.get(); }
    public void setVatNumber(String vat) { this.vatNumber.set(vat); }

    public String getDirectorName() { return directorName.get(); }
    public void setDirectorName(String name) { this.directorName.set(name); }

    //Property getters for JavaFX binding
    public IntegerProperty idProperty() { return id; }
    public StringProperty companyNameProperty() { return companyName; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty emailProperty() { return email; }
    public StringProperty logoPathProperty() { return logoPath; }
}