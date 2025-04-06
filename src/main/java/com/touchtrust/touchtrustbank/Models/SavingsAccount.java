package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.time.LocalDate;

public class SavingsAccount extends Account{
    // The withdrawal limit from the savings
    private final DoubleProperty withdrawalLimit;

    // Legacy constructor for backward compatibility
    public SavingsAccount(String owner, String accountNumber, double balance, double withdrawalLimit) {
        this(owner, accountNumber, balance, LocalDate.now(), true, 0.025, withdrawalLimit);
    }
    
    // New constructor with all parameters
    public SavingsAccount(String owner, String accountNumber, double balance, 
                         LocalDate openDate, boolean active, double interestRate, double withdrawalLimit) {
        super(owner, accountNumber, balance, openDate, active, interestRate);
        this.withdrawalLimit = new SimpleDoubleProperty(this, "Withdrawal Limit", withdrawalLimit);
    }

    public DoubleProperty withdrawalLimitProp() {return withdrawalLimit;}

    @Override
    public String toString() {
        return accountNumberProperty().get();
    }
}