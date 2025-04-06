package com.touchtrust.touchtrustbank.Models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Client {
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty payeeAddress;
    private final ObjectProperty<CheckingAccount> checkingAccount;
    private final ObjectProperty<SavingsAccount> savingsAccount;
    private final ObjectProperty<CreditAccount> creditAccount;
    private final ObjectProperty<LocalDate> dateCreated;

    public Client(String fName, String lName, String pAddress, CheckingAccount cAccount,
                  SavingsAccount sAccount, CreditAccount crAccount, LocalDate date){
        this.firstName = new SimpleStringProperty(this,"FirstName", fName);
        this.lastName = new SimpleStringProperty(this,"LastName", lName);
        this.payeeAddress = new SimpleStringProperty(this,"Payee Address", pAddress);
        this.checkingAccount = new SimpleObjectProperty<>(this, "Checking Account", cAccount);
        this.savingsAccount = new SimpleObjectProperty<>(this, "Savings Account", sAccount);
        this.creditAccount = new SimpleObjectProperty<>(this, "Credit Account", crAccount);
        this.dateCreated = new SimpleObjectProperty<>(this, "Date", date);
    }

    // For backward compatibility - client without credit account
    public Client(String fName, String lName, String pAddress, CheckingAccount cAccount,
                  SavingsAccount sAccount, LocalDate date){
        this(fName, lName, pAddress, cAccount, sAccount, null, date);
    }

    public StringProperty firstNameProperty(){
        return firstName;
    }

    public StringProperty lastNameProperty(){
        return lastName;
    }

    public StringProperty pAddressProperty(){
        return payeeAddress;
    }

    public ObjectProperty<CheckingAccount> checkingAccountProperty(){
        return checkingAccount;
    }

    public ObjectProperty<SavingsAccount> savingsAccountProperty(){
        return savingsAccount;
    }

    public ObjectProperty<CreditAccount> creditAccountProperty(){
        return creditAccount;
    }

    public ObjectProperty<LocalDate> dateProperty(){
        return dateCreated;
    }

    // Getter methods for all properties
    public String getFirstName() {
        return firstName.get();
    }

    public String getLastName() {
        return lastName.get();
    }

    public String getPayeeAddress() {
        return payeeAddress.get();
    }

    public CheckingAccount getCheckingAccount() {
        return checkingAccount.get();
    }

    public SavingsAccount getSavingsAccount() {
        return savingsAccount.get();
    }

    public CreditAccount getCreditAccount() {
        return creditAccount.get();
    }

    public LocalDate getDateCreated() {
        return dateCreated.get();
    }

    public boolean hasCreditAccount() {
        return creditAccount.get() != null;
    }
}