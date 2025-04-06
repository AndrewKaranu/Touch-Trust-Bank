package com.touchtrust.touchtrustbank.Services;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import com.touchtrust.touchtrustbank.Models.DatabaseDriver;
import com.touchtrust.touchtrustbank.Models.Transaction;

public class TransactionService {
    private final DatabaseDriver databaseDriver;
    
    public TransactionService(DatabaseDriver databaseDriver) {
        this.databaseDriver = databaseDriver;
    }
    
    public boolean processTransaction(String sender, String receiver, double amount, String message,
                                     Transaction.TransactionType type, boolean isFromChecking) {
        // Validate the transaction
        if (amount <= 0) {
            return false; // Negative or zero amounts not allowed
        }
        
        // Check if sender exists and has sufficient funds from the selected account
        double senderBalance;
        if (isFromChecking) {
            senderBalance = databaseDriver.getCheckingAccountBalance(sender);
        } else {
            senderBalance = databaseDriver.getSavingsAccountBalance(sender);
        }
        
        if (senderBalance < amount) {
            // Create a failed transaction record
            recordTransaction(sender, receiver, amount, message, 
                             Transaction.TransactionStatus.FAILED, type, isFromChecking);
            return false;
        }
        
        // Check if receiver exists
        if (!receiverExists(receiver)) {
            recordTransaction(sender, receiver, amount, message, 
                             Transaction.TransactionStatus.FAILED, type, isFromChecking);
            return false;
        }
        
        // Process the transaction - update the correct account
        if (isFromChecking) {
            databaseDriver.updateCheckingBalance(sender, amount, "SUB");
        } else {
            databaseDriver.updateSavingsBalance(sender, amount, "SUB");
        }
        
        // For receiver, always deposit to checking account
        databaseDriver.updateCheckingBalance(receiver, amount, "ADD");
        
        // Record successful transaction
        recordTransaction(sender, receiver, amount, message, 
                         Transaction.TransactionStatus.COMPLETED, type, isFromChecking);
        
        return true;
    }
    
    private boolean receiverExists(String receiver) {
        try {
            return databaseDriver.searchClient(receiver).isBeforeFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Assume receiver does not exist if an exception occurs
        }
    }
    
    private void recordTransaction(String sender, String receiver, double amount,
                              String message, Transaction.TransactionStatus status,
                              Transaction.TransactionType type, boolean isFromChecking) {
    String transactionId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    
    // Include the source account information in the transaction record
    String sourceAccount = isFromChecking ? "CHECKING" : "SAVINGS";
    
    // Use the enhanced method in DatabaseDriver
    databaseDriver.recordTransaction(
        sender, receiver, amount, now, message, 
        transactionId, status.toString(), type.toString(), sourceAccount
    );
}
    // Add this method to process credit card payments
    public boolean processCreditPayment(String clientAddress, double amount, boolean isFromChecking) {
        // Validate the transaction
        if (amount <= 0) {
            return false; // Negative or zero amounts not allowed
        }

        // Check if client has sufficient funds from the selected account
        double balance;
        if (isFromChecking) {
            balance = databaseDriver.getCheckingAccountBalance(clientAddress);
        } else {
            balance = databaseDriver.getSavingsAccountBalance(clientAddress);
        }

        if (balance < amount) {
            // Create a failed transaction record
            recordCreditPayment(clientAddress, amount, Transaction.TransactionStatus.FAILED, isFromChecking);
            return false;
        }

        // Process the payment - update accounts
        if (isFromChecking) {
            databaseDriver.updateCheckingBalance(clientAddress, amount, "SUB");
        } else {
            databaseDriver.updateSavingsBalance(clientAddress, amount, "SUB");
        }

        // Reduce credit balance
        databaseDriver.updateCreditBalance(clientAddress, amount, "SUB");

        // Record successful transaction
        recordCreditPayment(clientAddress, amount, Transaction.TransactionStatus.COMPLETED, isFromChecking);

        return true;
    }

    private void recordCreditPayment(String clientAddress, double amount,
                                     Transaction.TransactionStatus status, boolean isFromChecking) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        // Include the source account information in the transaction record
        String sourceAccount = isFromChecking ? "CHECKING" : "SAVINGS";

        // Use the enhanced method in DatabaseDriver
        databaseDriver.recordTransaction(
                clientAddress, "CREDIT-PAYMENT", amount, now, "Credit Card Payment",
                transactionId, status.toString(), "PAYMENT", sourceAccount
        );
    }

    // Add method to process credit purchases
    public boolean processCreditPurchase(String clientAddress, String merchant, double amount, String description) {
        // Get current credit limit and available credit
        double availableCredit = databaseDriver.getCreditAvailableCredit(clientAddress);

        if (amount <= 0 || amount > availableCredit) {
            // Record failed transaction
            recordCreditPurchase(clientAddress, merchant, amount, description, Transaction.TransactionStatus.FAILED);
            return false;
        }

        // Update credit balance - increase balance (debt)
        databaseDriver.updateCreditBalance(clientAddress, amount, "ADD");

        // Record successful transaction
        recordCreditPurchase(clientAddress, merchant, amount, description, Transaction.TransactionStatus.COMPLETED);

        return true;
    }

    private void recordCreditPurchase(String clientAddress, String merchant, double amount,
                                      String description, Transaction.TransactionStatus status) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        databaseDriver.recordTransaction(
                clientAddress, merchant, amount, now, description,
                transactionId, status.toString(), "PURCHASE", "CREDIT"
        );
    }
}