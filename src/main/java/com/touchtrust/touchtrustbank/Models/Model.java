package com.touchtrust.touchtrustbank.Models;

import com.touchtrust.touchtrustbank.Services.TransactionService;
import com.touchtrust.touchtrustbank.Services.VoiceAssistantService;
import com.touchtrust.touchtrustbank.Views.AccountType;
import com.touchtrust.touchtrustbank.Views.ViewFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;


    public class Model {
        private static Model model;
        private final ViewFactory viewFactory;
        private final DatabaseDriver databaseDriver;
        // Client Data Section
        private final Client client;
        private boolean clientLoginSuccessFlag;
        private final ObservableList<Transaction> latestTransactions;
        private final ObservableList<Transaction> allTransactions;
        // Admin Data Section
        private boolean adminLoginSuccessFlag;
        private final ObservableList<Client> clients;

        private Model() {
            this.viewFactory = new ViewFactory();
            this.databaseDriver = new DatabaseDriver();
            // Client Data Section
            this.clientLoginSuccessFlag = false;
            this.client = new Client("", "", "", null, null, null);
            this.latestTransactions = FXCollections.observableArrayList();
            this.allTransactions = FXCollections.observableArrayList();
            // Admin Data Section
            this.adminLoginSuccessFlag = false;
            this.clients = FXCollections.observableArrayList();
        }

        public static synchronized Model getInstance() {
            if (model == null) {
                model = new Model();
            }
            return model;
        }

        public ViewFactory getViewFactory() {
            return viewFactory;
        }

        public DatabaseDriver getDatabaseDriver() {
            return databaseDriver;
        }

        /*
         * Client Method Section
         * */
        public boolean getClientLoginSuccessFlag() {
            return this.clientLoginSuccessFlag;
        }

        public void setClientLoginSuccessFlag(boolean flag) {
            this.clientLoginSuccessFlag = flag;
        }

        public Client getClient() {
            return client;
        }

        // Add to the evaluateClientCredentials method to support face-only login

        public void evaluateClientCredentials(String pAddress, String password) {
            // Reset login flag at the beginning
            this.clientLoginSuccessFlag = false;

            // First check if this is a facial recognition login (password will be null)
            if (password == null) {
                // This is a face recognition login
                ResultSet resultSet = databaseDriver.getClientDataByPayeeAddress(pAddress);
                try {
                    if (resultSet.isBeforeFirst() && resultSet.next()) {
                        this.client.firstNameProperty().set(resultSet.getString("FirstName"));
                        this.client.lastNameProperty().set(resultSet.getString("LastName"));
                        this.client.pAddressProperty().set(resultSet.getString("PayeeAddress"));

                        String datePart = resultSet.getString("Date");
                        if (datePart != null && !datePart.isEmpty()) {
                            String[] dateParts = datePart.split("-");
                            LocalDate date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                            this.client.dateProperty().set(date);
                        }

                        // Load all accounts
                        CheckingAccount checkingAccount = getCheckingAccount(pAddress);
                        SavingsAccount savingsAccount = getSavingsAccount(pAddress);
                        CreditAccount creditAccount = getCreditAccount(pAddress);

                        // Set accounts in client object
                        this.client.checkingAccountProperty().set(checkingAccount);
                        this.client.savingsAccountProperty().set(savingsAccount);
                        this.client.creditAccountProperty().set(creditAccount);

                        this.clientLoginSuccessFlag = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // This is a regular password-based login
                ResultSet resultSet = databaseDriver.getClientData(pAddress, password);
                try {
                    if (resultSet.isBeforeFirst() && resultSet.next()) {
                        this.client.firstNameProperty().set(resultSet.getString("FirstName"));
                        this.client.lastNameProperty().set(resultSet.getString("LastName"));
                        this.client.pAddressProperty().set(resultSet.getString("PayeeAddress"));

                        String datePart = resultSet.getString("Date");
                        if (datePart != null && !datePart.isEmpty()) {
                            String[] dateParts = datePart.split("-");
                            LocalDate date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                            this.client.dateProperty().set(date);
                        }

                        // Load all accounts
                        CheckingAccount checkingAccount = getCheckingAccount(pAddress);
                        SavingsAccount savingsAccount = getSavingsAccount(pAddress);
                        CreditAccount creditAccount = getCreditAccount(pAddress);

                        // Set accounts in client object
                        this.client.checkingAccountProperty().set(checkingAccount);
                        this.client.savingsAccountProperty().set(savingsAccount);
                        this.client.creditAccountProperty().set(creditAccount);

                        this.clientLoginSuccessFlag = true;
                    } else {
                        this.clientLoginSuccessFlag = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    this.clientLoginSuccessFlag = false;
                }
            }
        }

// Similarly modify evaluateAdminCredentials for admin facial login

        // Update your prepareTransactions method to handle the new Transaction fields

        private void prepareTransactions(ObservableList<Transaction> transactions, int limit) {
            ResultSet resultSet = databaseDriver.getTransactions(this.client.pAddressProperty().get(), limit);
            try {
                while (resultSet.next()) {
                    String sender = resultSet.getString("Sender");
                    String receiver = resultSet.getString("Receiver");
                    double amount = resultSet.getDouble("Amount");
                    String[] dateParts = resultSet.getString("Date").split("-");
                    LocalDate date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                    String message = resultSet.getString("Message");

                    // Use existing columns if they're available, otherwise use default values
                    String transactionId = null;
                    Transaction.TransactionStatus status = Transaction.TransactionStatus.COMPLETED;
                    Transaction.TransactionType type = Transaction.TransactionType.TRANSFER;
                    LocalDateTime dateTime = date.atStartOfDay();
                    String sourceAccount = "SAVINGS"; // Default for backward compatibility

                    try {
                        transactionId = resultSet.getString("TransactionId");
                        if (resultSet.getString("Status") != null) {
                            status = Transaction.TransactionStatus.valueOf(resultSet.getString("Status"));
                        }
                        if (resultSet.getString("Type") != null) {
                            type = Transaction.TransactionType.valueOf(resultSet.getString("Type"));
                        }
                        if (resultSet.getString("DateTime") != null) {
                            dateTime = LocalDateTime.parse(resultSet.getString("DateTime"));
                        }
                        if (resultSet.getString("SourceAccount") != null) {
                            sourceAccount = resultSet.getString("SourceAccount");
                        }
                    } catch (Exception e) {
                        // Columns might not exist yet, that's fine - we'll use defaults
                    }

                    transactions.add(new Transaction(
                            sender, receiver, amount, dateTime, message,
                            transactionId, status, type, sourceAccount
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setLatestTransactions() {
            prepareTransactions(this.latestTransactions, 4);
        }

        public ObservableList<Transaction> getLatestTransactions() {
            return latestTransactions;
        }

        public void setAllTransactions() {
            prepareTransactions(this.allTransactions, -1);
        }

        public ObservableList<Transaction> getAllTransactions() {
            return allTransactions;
        }

        /*
         * Admin Method Section
         * */

        public boolean getAdminLoginSuccessFlag() {
            return this.adminLoginSuccessFlag;
        }

        public void setAdminLoginSuccessFlag(boolean adminLoginSuccessFlag) {
            this.adminLoginSuccessFlag = adminLoginSuccessFlag;
        }

        public void evaluateAdminCredentials(String username, String password) {
            ResultSet resultSet = databaseDriver.getAdminData(username, password);
            try {
                if (resultSet.isBeforeFirst()) {
                    this.adminLoginSuccessFlag = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public ObservableList<Client> getClients() {
            return clients;
        }

        public void setClients() {
            CheckingAccount checkingAccount;
            SavingsAccount savingsAccount;
            ResultSet resultSet = databaseDriver.getAllClientsData();
            try {
                while (resultSet.next()) {
                    String fName = resultSet.getString("FirstName");
                    String lName = resultSet.getString("LastName");
                    String pAddress = resultSet.getString("PayeeAddress");
                    String[] dateParts = resultSet.getString("Date").split("-");
                    LocalDate date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                    checkingAccount = getCheckingAccount(pAddress);
                    savingsAccount = getSavingsAccount(pAddress);
                    clients.add(new Client(fName, lName, pAddress, checkingAccount, savingsAccount, date));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public ObservableList<Client> searchClient(String pAddress) {
            ObservableList<Client> searchResults = FXCollections.observableArrayList();
            ResultSet resultSet = databaseDriver.searchClient(pAddress);
            try {
                CheckingAccount checkingAccount = getCheckingAccount(pAddress);
                SavingsAccount savingsAccount = getSavingsAccount(pAddress);
                String fName = resultSet.getString("FirstName");
                String lName = resultSet.getString("LastName");
                String[] dateParts = resultSet.getString("Date").split("-");
                LocalDate date = LocalDate.of(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
                searchResults.add(new Client(fName, lName, pAddress, checkingAccount, savingsAccount, date));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return searchResults;
        }

        /*
         * Utility Methods Section
         * */
        public CheckingAccount getCheckingAccount(String pAddress) {
            CheckingAccount account = null;
            ResultSet resultSet = databaseDriver.getCheckingAccountData(pAddress);
            try {
                String num = resultSet.getString("AccountNumber");
                int tLimit = (int) resultSet.getDouble("TransactionLimit");
                double balance = resultSet.getDouble("Balance");
                account = new CheckingAccount(pAddress, num, balance, tLimit);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return account;
        }

        public SavingsAccount getSavingsAccount(String pAddress) {
            SavingsAccount account = null;
            ResultSet resultSet = databaseDriver.getSavingsAccountData(pAddress);
            try {
                String num = resultSet.getString("AccountNumber");
                double wLimit = resultSet.getDouble("WithdrawalLimit");
                double balance = resultSet.getDouble("Balance");
                account = new SavingsAccount(pAddress, num, balance, wLimit);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return account;
        }

        public CreditAccount getCreditAccount(String pAddress) {
            CreditAccount account = null;
            ResultSet resultSet = databaseDriver.getCreditAccountData(pAddress);
            try {
                if (resultSet.next()) {
                    String num = resultSet.getString("AccountNumber");
                    double balance = resultSet.getDouble("Balance");
                    double creditLimit = resultSet.getDouble("CreditLimit");
                    double interestRate = resultSet.getDouble("InterestRate");

                    String dueDateString = resultSet.getString("PaymentDueDate");
                    LocalDate dueDate = dueDateString != null ?
                            LocalDate.parse(dueDateString) : LocalDate.now().plusDays(30);

                    String openDateString = resultSet.getString("OpenDate");
                    LocalDate openDate = openDateString != null ?
                            LocalDate.parse(openDateString) : LocalDate.now();

                    boolean active = resultSet.getInt("Active") == 1;

                    account = new CreditAccount(pAddress, num, balance, openDate, active, interestRate, creditLimit);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return account;
        }

        public void refreshClientAccountData() {
            if (this.client != null && this.client.pAddressProperty().get() != null) {
                String pAddress = this.client.pAddressProperty().get();

                // Refresh checking account
                CheckingAccount checkingAccount = getCheckingAccount(pAddress);
                this.client.checkingAccountProperty().set(checkingAccount);

                // Refresh savings account
                SavingsAccount savingsAccount = getSavingsAccount(pAddress);
                this.client.savingsAccountProperty().set(savingsAccount);

                // Refresh credit account if exists
                CreditAccount creditAccount = getCreditAccount(pAddress);
                this.client.creditAccountProperty().set(creditAccount);

                // Refresh transaction lists
                setLatestTransactions();
                setAllTransactions();
            }
        }

                // Add this field to your Model class
        private TransactionService transactionService;
        private VoiceAssistantService voiceAssistantService;

        // Update getInstance() to initialize the transaction service if needed
        public TransactionService getTransactionService() {
            if (transactionService == null) {
                transactionService = new TransactionService(this.databaseDriver);
            }
            return transactionService;
        }

        // Add getter method for the voice assistant service
        public VoiceAssistantService getVoiceAssistantService() {
            if (voiceAssistantService == null) {
                voiceAssistantService = new VoiceAssistantService(this.databaseDriver, getTransactionService());
            }
            return voiceAssistantService;
        }
    }
