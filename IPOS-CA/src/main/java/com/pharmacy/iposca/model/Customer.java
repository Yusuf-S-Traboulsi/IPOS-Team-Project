package com.pharmacy.iposca.model;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * This class represents a customer in the system.
 * Includes discount plan, Fixed & Flexible/Variable plans.
 */
public class Customer {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty town = new SimpleStringProperty();
    private final StringProperty postcode = new SimpleStringProperty();
    private final DoubleProperty creditLimit = new SimpleDoubleProperty();
    private final DoubleProperty currentDebt = new SimpleDoubleProperty();
    private final StringProperty accountStatus = new SimpleStringProperty("Normal");
    private final StringProperty status1stReminder = new SimpleStringProperty("no_need");
    private final StringProperty status2ndReminder = new SimpleStringProperty("no_need");
    private final ObjectProperty<LocalDate> date1stReminder = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> date2ndReminder = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> oldestDebtDate = new SimpleObjectProperty<>();

    // Discount Plan Fields
    private final StringProperty discountPlanType = new SimpleStringProperty("NONE");
    private final DoubleProperty discountRate = new SimpleDoubleProperty(0.0);
    private final DoubleProperty monthlyPurchaseTotal = new SimpleDoubleProperty(0.0);

    public Customer(int id, String title, String name, String email, String address,
                    String town, String postcode, double limit, double debt) {
        this.id.set(id);
        this.title.set(title);
        this.name.set(name);
        this.email.set(email);
        this.address.set(address);
        this.town.set(town);
        this.postcode.set(postcode);
        this.creditLimit.set(limit);
        this.currentDebt.set(debt);
        this.accountStatus.set("Normal");
        this.status1stReminder.set("no_need");
        this.status2ndReminder.set("no_need");
        this.discountPlanType.set("NONE");
        this.discountRate.set(0.0);
        this.monthlyPurchaseTotal.set(0.0);
    }

    //Getters and setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String title) { this.title.set(title); }
    public StringProperty titleProperty() { return title; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public StringProperty emailProperty() { return email; }

    public String getAddress() { return address.get(); }
    public void setAddress(String address) { this.address.set(address); }
    public StringProperty addressProperty() { return address; }

    public String getTown() { return town.get(); }
    public void setTown(String town) { this.town.set(town); }
    public StringProperty townProperty() { return town; }

    public String getPostcode() { return postcode.get(); }
    public void setPostcode(String postcode) { this.postcode.set(postcode); }
    public StringProperty postcodeProperty() { return postcode; }

    public double getCreditLimit() { return creditLimit.get(); }
    public void setCreditLimit(double limit) { this.creditLimit.set(limit); }
    public DoubleProperty creditLimitProperty() { return creditLimit; }

    public double getCurrentDebt() { return currentDebt.get(); }
    public void setCurrentDebt(double debt) { this.currentDebt.set(debt); }
    public DoubleProperty currentDebtProperty() { return currentDebt; }

    public String getAccountStatus() { return accountStatus.get(); }
    public void setAccountStatus(String status) { this.accountStatus.set(status); }
    public StringProperty accountStatusProperty() { return accountStatus; }

    public String getStatus1stReminder() { return status1stReminder.get(); }
    public void setStatus1stReminder(String status) { this.status1stReminder.set(status); }
    public StringProperty status1stReminderProperty() { return status1stReminder; }

    public String getStatus2ndReminder() { return status2ndReminder.get(); }
    public void setStatus2ndReminder(String status) { this.status2ndReminder.set(status); }
    public StringProperty status2ndReminderProperty() { return status2ndReminder; }

    public LocalDate getDate1stReminder() { return date1stReminder.get(); }
    public void setDate1stReminder(LocalDate date) { this.date1stReminder.set(date); }
    public ObjectProperty<LocalDate> date1stReminderProperty() { return date1stReminder; }

    public LocalDate getDate2ndReminder() { return date2ndReminder.get(); }
    public void setDate2ndReminder(LocalDate date) { this.date2ndReminder.set(date); }
    public ObjectProperty<LocalDate> date2ndReminderProperty() { return date2ndReminder; }

    public LocalDate getOldestDebtDate() { return oldestDebtDate.get(); }
    public void setOldestDebtDate(LocalDate date) { this.oldestDebtDate.set(date); }
    public ObjectProperty<LocalDate> oldestDebtDateProperty() { return oldestDebtDate; }

    //Discount Plan getters and setters
    public String getDiscountPlanType() { return discountPlanType.get(); }
    public void setDiscountPlanType(String type) { this.discountPlanType.set(type); }
    public StringProperty discountPlanTypeProperty() { return discountPlanType; }

    public double getDiscountRate() { return discountRate.get(); }
    public void setDiscountRate(double rate) { this.discountRate.set(rate); }
    public DoubleProperty discountRateProperty() { return discountRate; }

    public double getMonthlyPurchaseTotal() { return monthlyPurchaseTotal.get(); }
    public void setMonthlyPurchaseTotal(double total) { this.monthlyPurchaseTotal.set(total); }
    public DoubleProperty monthlyPurchaseTotalProperty() { return monthlyPurchaseTotal; }

    public void resetMonthlyPurchaseTotal() {
        this.monthlyPurchaseTotal.set(0.0);
    }

    public void addToMonthlyPurchaseTotal(double amount) {
        this.monthlyPurchaseTotal.set(this.monthlyPurchaseTotal.get() + amount);
    }

    /**
     * This method calculates effective discount rate based on plan type and monthly purchases.
     */
    public double calculateEffectiveDiscountRate() {
        if ("NONE".equals(discountPlanType.get())) {
            return 0.0;
        } else if ("FIXED".equals(discountPlanType.get())) {
            return discountRate.get();
        } else if ("FLEXIBLE".equals(discountPlanType.get())) {
            double monthlyTotal = monthlyPurchaseTotal.get();
            if (monthlyTotal > 2000.0) {
                return 0.03;
            } else if (monthlyTotal >= 1000.0) {
                return 0.02;
            } else {
                return 0.01;
            }
        }
        return 0.0;
    }
}