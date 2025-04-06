package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.LocalDate;

public class CheckingAccount extends Account {
    // The number of transactions a client is allowed to do per day.
    private final IntegerProperty transactionLimit;

    // Legacy constructor for backward compatibility
    public CheckingAccount(String owner, String accountNumber, double balance, int tLimit) {
        this(owner, accountNumber, balance, LocalDate.now(), true, 0.0005, tLimit);
    }
    
    // New constructor with all parameters
    public CheckingAccount(String owner, String accountNumber, double balance, 
                          LocalDate openDate, boolean active, double interestRate, int tLimit) {
        super(owner, accountNumber, balance, openDate, active, interestRate);
        this.transactionLimit = new SimpleIntegerProperty(this, "Transaction Limit", tLimit);
    }

    public IntegerProperty transactionLimitProp() {return transactionLimit;}

    @Override
    public String toString() {
        return accountNumberProperty().get();
    }
}