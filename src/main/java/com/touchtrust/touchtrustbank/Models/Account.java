package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.util.UUID;

public abstract class Account {
    private final StringProperty owner;
    private final StringProperty accountNumber;
    private final DoubleProperty balance;
    private final ObjectProperty<LocalDate> openDate;
    private final BooleanProperty active;
    private final StringProperty accountId;
    private final DoubleProperty interestRate;
    private final ObjectProperty<LocalDate> lastInterestApplied;

    public Account(String owner, String accountNumber, double balance, LocalDate openDate, 
                   boolean active, double interestRate) {
        this.owner = new SimpleStringProperty(this, "Owner", owner);
        this.accountNumber = new SimpleStringProperty(this, "Account Number", accountNumber);
        this.balance = new SimpleDoubleProperty(this, "Balance", balance);
        this.openDate = new SimpleObjectProperty<>(this, "Open Date", openDate);
        this.active = new SimpleBooleanProperty(this, "Active", active);
        this.accountId = new SimpleStringProperty(this, "Account ID", UUID.randomUUID().toString());
        this.interestRate = new SimpleDoubleProperty(this, "Interest Rate", interestRate);
        this.lastInterestApplied = new SimpleObjectProperty<>(this, "Last Interest Applied", openDate);
    }

    // Existing properties
    public StringProperty ownerProperty() { return owner; }
    public StringProperty accountNumberProperty() { return accountNumber; }
    public DoubleProperty balanceProperty() { return balance; }
    
    // New properties
    public ObjectProperty<LocalDate> openDateProperty() { return openDate; }
    public BooleanProperty activeProperty() { return active; }
    public StringProperty accountIdProperty() { return accountId; }
    public DoubleProperty interestRateProperty() { return interestRate; }
    public ObjectProperty<LocalDate> lastInterestAppliedProperty() { return lastInterestApplied; }

    // Method to apply interest
    public void applyInterest() {
        double newBalance = balance.get() * (1 + interestRate.get());
        balance.set(newBalance);
        lastInterestApplied.set(LocalDate.now());
    }

    // Method to handle withdrawals with validation
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (amount > balance.get()) {
            return false;
        }
        balance.set(balance.get() - amount);
        return true;
    }

    // Method to handle deposits with validation
    public boolean deposit(double amount) {
        if (amount <= 0) {
            return false;
        }
        balance.set(balance.get() + amount);
        return true;
    }

    public void setBalance(double newBalance) {
        this.balance.set(newBalance);
    }
}