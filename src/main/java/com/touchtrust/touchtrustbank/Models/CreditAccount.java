package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.time.LocalDate;

public class CreditAccount extends Account {
    // Credit specific properties
    private final DoubleProperty creditLimit;
    private final DoubleProperty availableCredit;
    private final DoubleProperty apr; // Annual Percentage Rate
    private final DoubleProperty minimumPayment;
    private final DoubleProperty lastStatementBalance;
    private final ObjectProperty<LocalDate> paymentDueDate;

    // Legacy constructor for backward compatibility
    public CreditAccount(String owner, String accountNumber, double balance, double creditLimit) {
        this(owner, accountNumber, balance, LocalDate.now(), true, 0.1899, creditLimit);
    }

    // Full constructor
    public CreditAccount(String owner, String accountNumber, double balance,
                         LocalDate openDate, boolean active, double interestRate, double creditLimit) {
        super(owner, accountNumber, balance, openDate, active, interestRate);
        this.creditLimit = new SimpleDoubleProperty(this, "Credit Limit", creditLimit);
        this.availableCredit = new SimpleDoubleProperty(this, "Available Credit", creditLimit - balance);
        this.apr = new SimpleDoubleProperty(this, "APR", interestRate);
        this.minimumPayment = new SimpleDoubleProperty(this, "Minimum Payment", Math.max(25.0, balance * 0.02));
        this.lastStatementBalance = new SimpleDoubleProperty(this, "Last Statement Balance", 0.0);
        this.paymentDueDate = new SimpleObjectProperty<>(this, "Payment Due Date", LocalDate.now().plusDays(30));
    }

    // Property getters
    public DoubleProperty creditLimitProperty() { return creditLimit; }
    public DoubleProperty availableCreditProperty() { return availableCredit; }
    public DoubleProperty aprProperty() { return apr; }
    public DoubleProperty minimumPaymentProperty() { return minimumPayment; }
    public DoubleProperty lastStatementBalanceProperty() { return lastStatementBalance; }
    public ObjectProperty<LocalDate> paymentDueDateProperty() { return paymentDueDate; }

    // Override balance setter to update available credit
    @Override
    public void setBalance(double newBalance) {
        super.setBalance(newBalance);
        // Update available credit whenever balance changes
        availableCredit.set(creditLimit.get() - newBalance);
        // Update minimum payment
        minimumPayment.set(Math.max(25.0, newBalance * 0.02));
    }

    // Credit-specific methods
    public boolean makePurchase(double amount) {
        if (amount <= 0) {
            return false;
        }

        if (balanceProperty().get() + amount > creditLimit.get()) {
            return false; // Exceeds credit limit
        }

        // For credit accounts, increasing balance means increasing debt
        setBalance(balanceProperty().get() + amount);
        return true;
    }

    public boolean makePayment(double amount) {
        if (amount <= 0) {
            return false;
        }

        // For credit accounts, reducing balance means reducing debt
        setBalance(Math.max(0, balanceProperty().get() - amount));
        return true;
    }

    // Generate monthly statement and calculate interest
    public double generateStatement() {
        // Save current balance as last statement balance
        lastStatementBalance.set(balanceProperty().get());

        // Calculate monthly interest (APR / 12)
        double monthlyInterest = balanceProperty().get() * (apr.get() / 12);

        // Add interest to balance
        setBalance(balanceProperty().get() + monthlyInterest);

        // Set new payment due date
        paymentDueDate.set(LocalDate.now().plusDays(30));

        return minimumPayment.get();
    }

    @Override
    public String toString() {
        return accountNumberProperty().get();
    }
}