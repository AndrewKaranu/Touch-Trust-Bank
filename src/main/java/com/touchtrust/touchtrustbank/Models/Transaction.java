package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private final StringProperty sender;
    private final StringProperty receiver;
    private final DoubleProperty amount;
    private final ObjectProperty<LocalDateTime> dateTime; 
    private final StringProperty message;
    private final StringProperty transactionId;
    private final ObjectProperty<TransactionStatus> status;
    private final ObjectProperty<TransactionType> type;
    private final StringProperty sourceAccount;

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, CANCELLED
    }
    
    public enum TransactionType {
        TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT, REFUND
    }

    // Add a constructor that handles LocalDate for backward compatibility
    public Transaction(String sender, String receiver, Double amount, LocalDate date, String message) {
        this(
            sender,
            receiver, 
            amount,
            date != null ? date.atStartOfDay() : LocalDateTime.now(),
            message,
            UUID.randomUUID().toString(),
            TransactionStatus.COMPLETED,
            TransactionType.TRANSFER,
            "SAVINGS"  // Default for backward compatibility
        );
    }

    public Transaction(String sender, String receiver, Double amount, LocalDateTime dateTime, 
                      String message, String transactionId, TransactionStatus status, 
                      TransactionType type, String sourceAccount) {
        this.sender = new SimpleStringProperty(this, "sender", sender);
        this.receiver = new SimpleStringProperty(this, "receiver", receiver);
        this.amount = new SimpleDoubleProperty(this, "amount", amount);
        this.dateTime = new SimpleObjectProperty<>(this, "dateTime", dateTime);
        this.message = new SimpleStringProperty(this, "message", message);
        this.transactionId = new SimpleStringProperty(this, "transactionId", transactionId != null ? transactionId : UUID.randomUUID().toString());
        this.status = new SimpleObjectProperty<>(this, "status", status);
        this.type = new SimpleObjectProperty<>(this, "type", type);
        this.sourceAccount = new SimpleStringProperty(this, "sourceAccount", sourceAccount);
    }

    // Add getters/setters for new properties
    public StringProperty transactionIdProperty() { return this.transactionId; }
    public ObjectProperty<TransactionStatus> statusProperty() { return this.status; }
    public ObjectProperty<TransactionType> typeProperty() { return this.type; }
    public ObjectProperty<LocalDateTime> dateTimeProperty() { return this.dateTime; }
    public StringProperty sourceAccountProperty() { return this.sourceAccount; }

    // Keep existing properties
    public StringProperty senderProperty() { return this.sender; }
    public StringProperty receiverProperty() { return this.receiver; }
    public DoubleProperty amountProperty() { return this.amount; }
    public StringProperty messageProperty() { return this.message; }
    
    // Add a compatibility method for the date property that was likely used before
    public ObjectProperty<LocalDate> dateProperty() {
        return new SimpleObjectProperty<>(this, "date", dateTime.get().toLocalDate());
    }
}