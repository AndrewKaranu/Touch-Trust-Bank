package com.touchtrust.touchtrustbank.Models;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.touchtrust.touchtrustbank.Services.TransactionService;

public class DatabaseDriver {
    private Connection conn;
    public DatabaseDriver(){
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:touchtrust.db");
            initializeDatabase();
        }catch (SQLDataException e){
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void initializeDatabase() {
        try (Statement statement = conn.createStatement()) {
            // Create Clients table
            statement.execute("CREATE TABLE IF NOT EXISTS Clients (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "FirstName TEXT NOT NULL, " +
                    "LastName TEXT NOT NULL, " +
                    "PayeeAddress TEXT NOT NULL UNIQUE, " +
                    "Password TEXT NOT NULL, " +
                    "Date TEXT NOT NULL)");
    
            // Create CheckingAccounts table
            statement.execute("CREATE TABLE IF NOT EXISTS CheckingAccounts (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Owner TEXT NOT NULL UNIQUE, " +
                    "AccountNumber TEXT NOT NULL UNIQUE, " +
                    "TransactionLimit REAL NOT NULL, " +
                    "Balance REAL NOT NULL)");
    
            // Add columns to CheckingAccounts table safely
            addColumnIfNotExists("CheckingAccounts", "OpenDate", "TEXT");
            addColumnIfNotExists("CheckingAccounts", "Active", "INTEGER DEFAULT 1");
            addColumnIfNotExists("CheckingAccounts", "AccountId", "TEXT");
            addColumnIfNotExists("CheckingAccounts", "InterestRate", "REAL DEFAULT 0.0");
            addColumnIfNotExists("CheckingAccounts", "LastInterestApplied", "TEXT");
    
            // Create SavingsAccounts table
            statement.execute("CREATE TABLE IF NOT EXISTS SavingsAccounts (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Owner TEXT NOT NULL UNIQUE, " +
                    "AccountNumber TEXT NOT NULL UNIQUE, " +
                    "WithdrawalLimit REAL NOT NULL, " +
                    "Balance REAL NOT NULL)");
            
            // Add columns to SavingsAccounts table safely
            addColumnIfNotExists("SavingsAccounts", "OpenDate", "TEXT");
            addColumnIfNotExists("SavingsAccounts", "Active", "INTEGER DEFAULT 1");
            addColumnIfNotExists("SavingsAccounts", "AccountId", "TEXT");
            addColumnIfNotExists("SavingsAccounts", "InterestRate", "REAL DEFAULT 0.025");
            addColumnIfNotExists("SavingsAccounts", "LastInterestApplied", "TEXT");

//            Create CreditAccounts table
            // Create CreditAccounts table
            statement.execute("CREATE TABLE IF NOT EXISTS CreditAccounts (" +
                    "AccountId TEXT PRIMARY KEY, " +
                    "Owner TEXT NOT NULL, " +
                    "AccountNumber TEXT NOT NULL, " +
                    "Balance REAL DEFAULT 0.0, " +
                    "OpenDate TEXT NOT NULL, " +
                    "Active INTEGER DEFAULT 1, " +
                    "InterestRate REAL DEFAULT 0.1899, " +
                    "CreditLimit REAL DEFAULT 1000.0, " +
                    "AvailableCredit REAL DEFAULT 1000.0, " +
                    "MinimumPayment REAL DEFAULT 25.0, " +
                    "LastStatementBalance REAL DEFAULT 0.0, " +
                    "PaymentDueDate TEXT, " +
                    "LastInterestApplied TEXT)");

            // Create Transactions table
            statement.execute("CREATE TABLE IF NOT EXISTS Transactions (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Sender TEXT NOT NULL, " +
                    "Receiver TEXT NOT NULL, " +
                    "Amount REAL NOT NULL, " +
                    "Date TEXT NOT NULL, " +
                    "Message TEXT)");
    
            // Add columns to Transactions table safely
            addColumnIfNotExists("Transactions", "TransactionId", "TEXT");
            addColumnIfNotExists("Transactions", "Status", "TEXT");
            addColumnIfNotExists("Transactions", "Type", "TEXT");
            addColumnIfNotExists("Transactions", "DateTime", "TEXT");
            addColumnIfNotExists("Transactions", "SourceAccount", "TEXT DEFAULT 'CHECKING'");
            
            // Create Admins table
            statement.execute("CREATE TABLE IF NOT EXISTS Admins (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Username TEXT NOT NULL UNIQUE, " +
                    "Password TEXT NOT NULL)");
    
            // Create facial recognition table
            statement.execute("CREATE TABLE IF NOT EXISTS facial_recognition (" +
                    "UserId TEXT PRIMARY KEY, " +
                    "FacePath TEXT NOT NULL, " +
                    "DateRegistered TEXT DEFAULT (datetime('now')), " +
                    "LastUpdated TEXT DEFAULT (datetime('now')))");
    
            // Create new tables for enhanced features
            statement.execute("CREATE TABLE IF NOT EXISTS AccountInterestHistory (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "AccountId TEXT NOT NULL, " +
                    "Amount REAL NOT NULL, " +
                    "Date TEXT NOT NULL)");
    
            statement.execute("CREATE TABLE IF NOT EXISTS TransactionFees (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "TransactionId TEXT NOT NULL, " +
                    "Amount REAL NOT NULL)");
    
            statement.execute("CREATE TABLE IF NOT EXISTS ScheduledTransactions (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "Sender TEXT NOT NULL, " +
                    "Receiver TEXT NOT NULL, " +
                    "Amount REAL NOT NULL, " +
                    "ScheduleDate TEXT NOT NULL, " +
                    "RecurringType TEXT, " + // ONCE, DAILY, WEEKLY, MONTHLY
                    "Message TEXT, " +
                    "Active INTEGER DEFAULT 1)");
                    
            // Add default admin if none exists
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM Admins");
            if (rs.next() && rs.getInt(1) == 0) {
                statement.execute("INSERT INTO Admins (Username, Password) VALUES ('admin', 'admin')");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Helper method to safely add a column if it doesn't exist
    private void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        try {
            // Check if the column already exists
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet columns = dbm.getColumns(null, null, tableName, columnName);
            
            if (!columns.next()) {
                // Column doesn't exist, so add it
                Statement statement = conn.createStatement();
                statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


//    Client Section
    public ResultSet getClientData(String pAddress, String password){
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Clients WHERE payeeAddress='"+pAddress+"' AND Password='"+password+"';");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;

    }

    public ResultSet getTransactions(String pAddress, int limit) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Transactions WHERE Sender='"+pAddress+"' OR Receiver='"+pAddress+"' LIMIT "+limit+";");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;
    }

    // Method returns savings account balance
    public double getSavingsAccountBalance(String pAddress) {
        Statement statement;
        ResultSet resultSet;
        double balance = 0;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM SavingsAccounts WHERE Owner='"+pAddress+"';");
            balance = resultSet.getDouble("Balance");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return balance;
    }

    // Method to either add or subtract from balance given operation
    public void updateBalance(String pAddress, double amount, String operation) {
        Statement statement;
        ResultSet resultSet;
        try{
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM SavingsAccounts WHERE Owner='"+pAddress+"';");
            double newBalance;
            if (operation.equals("ADD")){
                newBalance = resultSet.getDouble("Balance") + amount;
                statement.executeUpdate("UPDATE SavingsAccounts SET Balance="+newBalance+" WHERE Owner='"+pAddress+"';");
            } else {
                if (resultSet.getDouble("Balance") >= amount) {
                    newBalance = resultSet.getDouble("Balance") - amount;
                    statement.executeUpdate("UPDATE SavingsAccounts SET Balance="+newBalance+" WHERE Owner='"+pAddress+"';");
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    // Creates and records new transaction
    public void newTransaction(String sender, String receiver, double amount, String message) {
    Statement statement;
    try {
        statement = this.conn.createStatement();
        LocalDateTime now = LocalDateTime.now();
        String transactionId = UUID.randomUUID().toString();
        String status = "COMPLETED";
        String type = "TRANSFER";
        
        statement.executeUpdate("INSERT INTO " +
                "Transactions(Sender, Receiver, Amount, Date, Message, TransactionId, Status, Type, DateTime) " +
                "VALUES ('"+sender+"', '"+receiver+"', "+amount+", '"+LocalDate.now()+"', '"+message+"', '" +
                transactionId + "', '" + status + "', '" + type + "', '" + now + "');");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



//    Admin Section
    public ResultSet getAdminData(String username, String password){
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Admins WHERE Username='"+username+"' AND Password='"+password+"';");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;

}

    public  void createClient(String fname, String lname, String pAddress, String password, LocalDate date){
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("INSERT INTO " +
                    "Clients (FirstName, LastName, PayeeAddress, Password, Date)" +
                    "VALUES ('"+fname+"', '"+lname+"', '"+pAddress+"', '"+password+"', '"+date.toString()+"');");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void createCheckingAccount(String owner, String number, double tLimit, double balance) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("INSERT INTO " +
                    "CheckingAccounts (Owner, AccountNumber, TransactionLimit, Balance)" +
                    " VALUES ('"+owner+"', '"+number+"', "+tLimit+", "+balance+")");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void createSavingsAccount(String owner, String number, double wLimit, double balance) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("INSERT INTO " +
                    "SavingsAccounts (Owner, AccountNumber, WithdrawalLimit, Balance)" +
                    " VALUES ('"+owner+"', '"+number+"', "+wLimit+", "+balance+")");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public ResultSet getAllClientsData() {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Clients;");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;
    }

    public void depositSavings(String pAddress, double amount) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("UPDATE SavingsAccounts SET Balance="+amount+" WHERE Owner='"+pAddress+"';");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

//    Utility Methods
public ResultSet searchClient(String pAddress) {
    Statement statement;
    ResultSet resultSet = null;
    try {
        statement = this.conn.createStatement();
        resultSet = statement.executeQuery("SELECT * FROM Clients WHERE PayeeAddress='"+pAddress+"';");
    }catch (SQLException e){
        e.printStackTrace();
    }
    return resultSet;
}

    public int getLastClientsId() {
        Statement statement;
        ResultSet resultSet;
        int id = 0;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM sqlite_sequence WHERE name='Clients';");
            id = resultSet.getInt("seq");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return id;
    }

    public ResultSet getCheckingAccountData(String pAddress) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CheckingAccounts WHERE Owner='"+pAddress+"';");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;
    }

    public ResultSet getSavingsAccountData(String pAddress) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM SavingsAccounts WHERE Owner='"+pAddress+"';");
        }catch (SQLException e){
            e.printStackTrace();
        }
        return resultSet;
    }


    public ResultSet getClientDataByPayeeAddress(String payeeAddress) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Clients WHERE PayeeAddress='" + payeeAddress + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public ResultSet getAdminDataByUsername(String username) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Admins WHERE Username='" + username + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }
    // Add these methods to your DatabaseDriver class

public ResultSet getAllSavingsAccounts() {
    Statement statement;
    ResultSet resultSet = null;
    try {
        statement = this.conn.createStatement();
        resultSet = statement.executeQuery("SELECT * FROM SavingsAccounts;");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return resultSet;
}

public ResultSet getAllCheckingAccounts() {
    Statement statement;
    ResultSet resultSet = null;
    try {
        statement = this.conn.createStatement();
        resultSet = statement.executeQuery("SELECT * FROM CheckingAccounts;");
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return resultSet;
}

public void addInterestToAccount(String accountTable, String accountId, double amount, LocalDate date) {
    Statement statement;
    try {
        statement = this.conn.createStatement();
        // First update the balance
        statement.executeUpdate("UPDATE " + accountTable + 
                               " SET Balance = Balance + " + amount + 
                               ", LastInterestApplied = '" + date.toString() + "'" +
                               " WHERE AccountId = '" + accountId + "'");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

public void logInterestApplication(String accountId, double amount, LocalDate date) {
    Statement statement;
    try {
        statement = this.conn.createStatement();
        statement.executeUpdate("INSERT INTO AccountInterestHistory " +
                               "(AccountId, Amount, Date) VALUES " +
                               "('" + accountId + "', " + amount + ", '" + date.toString() + "')");
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    // Tracking facial recognition data
    public void saveFacialRecognitionData(String userId, String facePath) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            // SQLite doesn't support ON DUPLICATE KEY, let's use INSERT OR REPLACE instead
            statement.executeUpdate("INSERT OR REPLACE INTO facial_recognition (UserId, FacePath) " +
                    "VALUES('" + userId + "', '" + facePath + "')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get face data for a user
    public String getFacialRecognitionPath(String userId) {
        Statement statement;
        ResultSet resultSet;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT FacePath FROM facial_recognition WHERE UserId='" + userId + "'");
            if (resultSet.next()) {
                return resultSet.getString("FacePath");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void updateFacialRecognitionTimestamp(String userId) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("UPDATE facial_recognition SET LastUpdated = datetime('now') WHERE UserId='" + userId + "'");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get checking account balance
    public double getCheckingAccountBalance(String pAddress) {
        Statement statement;
        ResultSet resultSet;
        double balance = 0;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CheckingAccounts WHERE Owner='"+pAddress+"';");
            balance = resultSet.getDouble("Balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }
    
    // Update checking account balance
    public void updateCheckingBalance(String pAddress, double amount, String operation) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            String sql = "";
            if (operation.equals("ADD")) {
                sql = "UPDATE CheckingAccounts SET Balance = Balance + " + amount + 
                      " WHERE Owner = '" + pAddress + "'";
            } else {
                sql = "UPDATE CheckingAccounts SET Balance = Balance - " + amount + 
                      " WHERE Owner = '" + pAddress + "'";
            }
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Update savings account balance
    public void updateSavingsBalance(String pAddress, double amount, String operation) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            String sql = "";
            if (operation.equals("ADD")) {
                sql = "UPDATE SavingsAccounts SET Balance = Balance + " + amount + 
                      " WHERE Owner = '" + pAddress + "'";
            } else {
                sql = "UPDATE SavingsAccounts SET Balance = Balance - " + amount + 
                      " WHERE Owner = '" + pAddress + "'";
            }
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Updated recordTransaction to include source account
        // Updated recordTransaction to include source account AND Date
    public void recordTransaction(String sender, String receiver, double amount, 
                             LocalDateTime dateTime, String message, String transactionId, 
                             String status, String type, String sourceAccount) {
    try {
        Statement statement = this.conn.createStatement();
        
        // Extract the date part for the Date column
        LocalDate date = dateTime.toLocalDate();
        
        String sql = "INSERT INTO Transactions (Sender, Receiver, Amount, Date, DateTime, " +
                    "Message, TransactionId, Status, Type, SourceAccount) VALUES ('" +
                    sender + "', '" + receiver + "', " + amount + ", '" + date.toString() + "', '" + 
                    dateTime.toString() + "', '" + message + "', '" + transactionId + 
                    "', '" + status + "', '" + type + "', '" + sourceAccount + "')";
        
        try {
            statement.executeUpdate(sql);
            System.out.println("Transaction recorded successfully: " + transactionId);
        } catch (SQLException e) {
            System.err.println("Failed to record transaction. SQL: " + sql);
            System.err.println("Error: " + e.getMessage());
            throw e; // Rethrow to maintain the original behavior
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
    
    // Handle legacy use of createTransaction
    public boolean createTransaction(String sender, String receiver, double amount, String message, boolean isFromChecking) {
        // This can call a TransactionService instance to handle the process
        TransactionService service = new TransactionService(this);
        boolean success = service.processTransaction(
                sender, receiver, amount, message,
                Transaction.TransactionType.TRANSFER, isFromChecking
        );

        if (success) {
            // Notify our application model that account data has changed
            Model.getInstance().refreshClientAccountData();
        }

        return success;
    }

    // Add these methods to your DatabaseDriver class

    public void createCreditAccount(String owner, String number, double creditLimit, double initialBalance) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            double availableCredit = creditLimit - initialBalance;
            double minimumPayment = Math.max(25.0, initialBalance * 0.02);
            LocalDate dueDate = LocalDate.now().plusDays(30);

            statement.executeUpdate("INSERT INTO " +
                    "CreditAccounts (Owner, AccountNumber, Balance, OpenDate, Active, " +
                    "InterestRate, CreditLimit, AvailableCredit, MinimumPayment, PaymentDueDate) " +
                    "VALUES ('"+owner+"', '"+number+"', "+initialBalance+", '"+LocalDate.now()+"', 1, " +
                    "0.1899, "+creditLimit+", "+availableCredit+", "+minimumPayment+", '"+dueDate+"')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet getCreditAccountData(String pAddress) {
        Statement statement;
        ResultSet resultSet = null;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CreditAccounts WHERE Owner='"+pAddress+"';");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultSet;
    }

    public double getCreditAccountBalance(String pAddress) {
        Statement statement;
        ResultSet resultSet;
        double balance = 0;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CreditAccounts WHERE Owner='"+pAddress+"';");
            balance = resultSet.getDouble("Balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public void updateCreditBalance(String pAddress, double amount, String operation) {
        Statement statement;
        ResultSet resultSet;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CreditAccounts WHERE Owner='"+pAddress+"'");

            if(resultSet.isBeforeFirst()) {
                double oldBalance = resultSet.getDouble("Balance");
                double creditLimit = resultSet.getDouble("CreditLimit");
                double newBalance;

                if(operation.equals("ADD")) {
                    newBalance = oldBalance + amount;
                } else {
                    newBalance = oldBalance - amount;
                }

                double availableCredit = creditLimit - newBalance;
                double minimumPayment = Math.max(25.0, newBalance * 0.02);

                statement.executeUpdate("UPDATE CreditAccounts SET " +
                        "Balance="+newBalance+", " +
                        "AvailableCredit="+availableCredit+", " +
                        "MinimumPayment="+minimumPayment+" " +
                        "WHERE Owner='"+pAddress+"'");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void applyCreditInterest() {
        Statement statement;
        ResultSet resultSet;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CreditAccounts");

            while(resultSet.next()) {
                String owner = resultSet.getString("Owner");
                double balance = resultSet.getDouble("Balance");
                double interestRate = resultSet.getDouble("InterestRate");
                double creditLimit = resultSet.getDouble("CreditLimit");

                // Calculate monthly interest
                double monthlyInterest = balance * (interestRate / 12);
                double newBalance = balance + monthlyInterest;
                double availableCredit = creditLimit - newBalance;
                double minimumPayment = Math.max(25.0, newBalance * 0.02);

                // Update the account
                statement.executeUpdate("UPDATE CreditAccounts SET " +
                        "Balance="+newBalance+", " +
                        "AvailableCredit="+availableCredit+", " +
                        "MinimumPayment="+minimumPayment+", " +
                        "LastInterestApplied='"+LocalDate.now()+"', " +
                        "PaymentDueDate='"+LocalDate.now().plusDays(30)+"' " +
                        "WHERE Owner='"+owner+"'");

                // Log interest application
                logCreditInterestApplication(owner, monthlyInterest);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void logCreditInterestApplication(String owner, double amount) {
        Statement statement;
        try {
            statement = this.conn.createStatement();
            statement.executeUpdate("INSERT INTO CreditInterestHistory " +
                    "(Owner, Amount, Date) VALUES " +
                    "('"+owner+"', "+amount+", '"+LocalDate.now()+"')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the available credit for a client's credit account
     * @param pAddress The client's payee address
     * @return The amount of available credit, or 0 if no credit account exists
     */
    public double getCreditAvailableCredit(String pAddress) {
        Statement statement;
        ResultSet resultSet;
        double availableCredit = 0;
        try {
            statement = this.conn.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM CreditAccounts WHERE Owner='"+pAddress+"'");

            if (resultSet.next()) {
                // Two ways to get available credit:
                // 1. If you store it directly in the database
                availableCredit = resultSet.getDouble("AvailableCredit");

                // 2. Or calculate it from credit limit and current balance
                // double creditLimit = resultSet.getDouble("CreditLimit");
                // double balance = resultSet.getDouble("Balance");
                // availableCredit = creditLimit - balance;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return availableCredit;
    }
}
