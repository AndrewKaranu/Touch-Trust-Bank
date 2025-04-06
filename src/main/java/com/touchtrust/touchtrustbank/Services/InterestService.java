package com.touchtrust.touchtrustbank.Services;


import com.touchtrust.touchtrustbank.Models.DatabaseDriver;

import java.time.LocalDate;

public class InterestService {
    private final DatabaseDriver databaseDriver;
    
    public InterestService(DatabaseDriver databaseDriver) {
        this.databaseDriver = databaseDriver;
    }
    
    public void applyMonthlyInterest() {
        // Get all accounts
        var savingsAccounts = databaseDriver.getAllSavingsAccounts();
        var checkingAccounts = databaseDriver.getAllCheckingAccounts();
        
        LocalDate today = LocalDate.now();
        
        // Apply interest to savings accounts (higher rate)
        try {
            while (savingsAccounts.next()) {
                String accountId = savingsAccounts.getString("AccountId");
                double balance = savingsAccounts.getDouble("Balance");
                double interestRate = savingsAccounts.getDouble("InterestRate");
                
                // Calculate interest
                double interestAmount = balance * interestRate;
                
                // Apply interest
                databaseDriver.addInterestToAccount("SavingsAccounts", 
                                                 accountId, interestAmount, today);
                
                // Log interest application
                databaseDriver.logInterestApplication(accountId, interestAmount, today);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Apply interest to checking accounts (lower rate)
        try {
            while (checkingAccounts.next()) {
                String accountId = checkingAccounts.getString("AccountId");
                double balance = checkingAccounts.getDouble("Balance");
                double interestRate = checkingAccounts.getDouble("InterestRate");
                
                // Calculate interest
                double interestAmount = balance * interestRate;
                
                // Apply interest
                databaseDriver.addInterestToAccount("CheckingAccounts", 
                                                 accountId, interestAmount, today);
                
                // Log interest application
                databaseDriver.logInterestApplication(accountId, interestAmount, today);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
