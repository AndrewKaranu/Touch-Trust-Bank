package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.CheckingAccount;
import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.SavingsAccount;
import com.touchtrust.touchtrustbank.Models.Transaction;
import com.touchtrust.touchtrustbank.Services.TransactionService;
import com.touchtrust.touchtrustbank.Views.TransactionCellFactory;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // Existing UI components
    public Text user_name;
    public Label login_date;
    public Label checking_bal;
    public Label checking_acc_num;
    public Label savings_bal;
    public Label savings_acc_num;
    public Label income_lbl;
    public Label expense_lbl;
    public ListView<Transaction> transaction_listview;
    public TextField payee_fld;
    public TextField amount_fld;
    public TextArea message_fld;
    public Button send_money_btn;
    
    // New UI components (add these to your FXML)
    public VBox account_summary_container; // For charts
    public ComboBox<String> transaction_filter;
    public Label transaction_status;
    public Label checking_interest_lbl;
    public Label savings_interest_lbl;
    public ComboBox<String> source_account_selector;
    
    private TransactionService transactionService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize transaction service
        transactionService = new TransactionService(Model.getInstance().getDatabaseDriver());
        
        // Setup UI
        bindData();
        initLatestTransactionsList();
        setupTransactionFilters();
        setupAccountSummaryCharts();
        setupSourceAccountSelector();
        transaction_listview.setItems(Model.getInstance().getLatestTransactions());
        transaction_listview.setCellFactory(e -> new TransactionCellFactory());
        
        // Add event handlers
        send_money_btn.setOnAction(event -> onSendMoney());
        
        // Calculate account summary
        accountSummary();
    }
    
    private void bindData() {
        // Existing bindings
        user_name.textProperty().bind(Bindings.concat("Hi, ").concat(Model.getInstance().getClient().firstNameProperty()));
        login_date.setText("Today, " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        checking_bal.textProperty().bind(Bindings.format("$%.2f", Model.getInstance().getClient().checkingAccountProperty().get().balanceProperty()));
        checking_acc_num.textProperty().bind(Model.getInstance().getClient().checkingAccountProperty().get().accountNumberProperty());
        savings_bal.textProperty().bind(Bindings.format("$%.2f", Model.getInstance().getClient().savingsAccountProperty().get().balanceProperty()));
        savings_acc_num.textProperty().bind(Model.getInstance().getClient().savingsAccountProperty().get().accountNumberProperty());
        
        // New bindings for interest rates
        if (checking_interest_lbl != null) {
            checking_interest_lbl.textProperty().bind(
                Bindings.format("%.2f%%", 
                    Bindings.multiply(Model.getInstance().getClient().checkingAccountProperty().get().interestRateProperty(), 100))
            );
        }
        
        if (savings_interest_lbl != null) {
            savings_interest_lbl.textProperty().bind(
                Bindings.format("%.2f%%", 
                    Bindings.multiply(Model.getInstance().getClient().savingsAccountProperty().get().interestRateProperty(), 100))
            );
        }
    }

    private void initLatestTransactionsList() {
        if (Model.getInstance().getLatestTransactions().isEmpty()){
            Model.getInstance().setLatestTransactions();
        }
    }
    
    private void setupTransactionFilters() {
        if (transaction_filter != null) {
            ObservableList<String> filters = FXCollections.observableArrayList(
                "All Transactions", 
                "Incoming", 
                "Outgoing", 
                "Last 7 Days", 
                "Last 30 Days"
            );
            transaction_filter.setItems(filters);
            transaction_filter.setValue("All Transactions");
            
            transaction_filter.setOnAction(event -> {
                String selected = transaction_filter.getValue();
                switch (selected) {
                    case "Incoming":
                        filterIncomingTransactions();
                        break;
                    case "Outgoing":
                        filterOutgoingTransactions();
                        break;
                    case "Last 7 Days":
                        filterRecentTransactions(7);
                        break;
                    case "Last 30 Days":
                        filterRecentTransactions(30);
                        break;
                    default:
                        transaction_listview.setItems(Model.getInstance().getAllTransactions());
                        break;
                }
            });
        }
    }
    
    private void filterIncomingTransactions() {
        String clientAddress = Model.getInstance().getClient().pAddressProperty().get();
        ObservableList<Transaction> filtered = Model.getInstance().getAllTransactions().filtered(
            transaction -> transaction.receiverProperty().get().equals(clientAddress)
        );
        transaction_listview.setItems(filtered);
    }
    
    private void filterOutgoingTransactions() {
        String clientAddress = Model.getInstance().getClient().pAddressProperty().get();
        ObservableList<Transaction> filtered = Model.getInstance().getAllTransactions().filtered(
            transaction -> transaction.senderProperty().get().equals(clientAddress)
        );
        transaction_listview.setItems(filtered);
    }
    
    private void filterRecentTransactions(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        ObservableList<Transaction> filtered = Model.getInstance().getAllTransactions().filtered(
            transaction -> transaction.dateProperty().get().isAfter(cutoffDate) || 
                           transaction.dateProperty().get().isEqual(cutoffDate)
        );
        transaction_listview.setItems(filtered);
    }
    
    private void setupAccountSummaryCharts() {
        if (account_summary_container != null) {
            // Create a pie chart for account balances
            PieChart balanceChart = new PieChart();
            
            double checkingBalance = Model.getInstance().getClient().checkingAccountProperty().get().balanceProperty().get();
            double savingsBalance = Model.getInstance().getClient().savingsAccountProperty().get().balanceProperty().get();
            
            PieChart.Data checkingData = new PieChart.Data("Checking", checkingBalance);
            PieChart.Data savingsData = new PieChart.Data("Savings", savingsBalance);
            
            balanceChart.getData().addAll(checkingData, savingsData);
            balanceChart.setTitle("Account Distribution");
            
            // Add the chart to the UI
            account_summary_container.getChildren().add(balanceChart);
        }
    }

    private void setupSourceAccountSelector() {
        // Add checking and savings options to the dropdown
        source_account_selector.setItems(FXCollections.observableArrayList("Checking", "Savings"));
        // Set checking as default
        source_account_selector.setValue("Checking");
    }

    private void onSendMoney() {
        // Get selected account type
        String sourceAccountType = source_account_selector.getValue();

        // Validate the form as before
        if (!validateTransferForm()) {
            return;
        }

        // Get form data
        String receiver = payee_fld.getText();
        double amount = Double.parseDouble(amount_fld.getText());
        String message = message_fld.getText();
        String sender = Model.getInstance().getClient().pAddressProperty().get();

        // Determine which account to use
        boolean isFromChecking = "Checking".equals(sourceAccountType);

        // Process the transaction with the account type
        boolean result = Model.getInstance().getDatabaseDriver().createTransaction(
                sender, receiver, amount, message, isFromChecking);

        if (result) {
            updateTransactionStatus("Transaction completed successfully!", false);
            clearTransferForm();

            // Refresh UI after transaction
            refreshAccountData();

            // Refresh latest transactions list
            Model.getInstance().setLatestTransactions();

            // Also refresh the transaction list view
            transaction_listview.refresh();
        } else {
            updateTransactionStatus("Transaction failed! Check account balance and try again.", true);
        }
    }

    // Add this method to refresh account data from database
    private void refreshAccountData() {
        // Refresh checking account from database
        CheckingAccount updatedChecking = Model.getInstance().getCheckingAccount(
                Model.getInstance().getClient().pAddressProperty().get()
        );
        Model.getInstance().getClient().checkingAccountProperty().set(updatedChecking);

        // Refresh savings account from database
        SavingsAccount updatedSavings = Model.getInstance().getSavingsAccount(
                Model.getInstance().getClient().pAddressProperty().get()
        );
        Model.getInstance().getClient().savingsAccountProperty().set(updatedSavings);

        // Update account summary
        accountSummary();
    }
    
    private boolean validateTransferForm() {
        // Validate payee field
        if (payee_fld.getText() == null || payee_fld.getText().trim().isEmpty()) {
            updateTransactionStatus("Please enter a recipient address", true);
            return false;
        }
        
        // Validate amount field - must be numeric and > 0
        try {
            double amount = Double.parseDouble(amount_fld.getText().trim());
            if (amount <= 0) {
                updateTransactionStatus("Amount must be greater than zero", true);
                return false;
            }
            
            // Check if sender has sufficient funds
            double availableBalance = Model.getInstance().getClient().savingsAccountProperty().get().balanceProperty().get();
            if (amount > availableBalance) {
                updateTransactionStatus("Insufficient funds in your account", true);
                return false;
            }
        } catch (NumberFormatException e) {
            updateTransactionStatus("Please enter a valid amount", true);
            return false;
        }
        
        return true;
    }
    
    private void updateTransactionStatus(String message, boolean isError) {
        if (transaction_status != null) {
            transaction_status.setText(message);
            
            if (isError) {
                transaction_status.setTextFill(Color.RED);
            } else {
                transaction_status.setTextFill(Color.GREEN);
            }
        }
    }
    
    private void clearTransferForm() {
        payee_fld.setText("");
        amount_fld.setText("");
        message_fld.setText("");
    }

    // Method calculates all expenses and income
    private void accountSummary() {
        double income = 0;
        double expenses = 0;
        
        if (Model.getInstance().getAllTransactions().isEmpty()){
            Model.getInstance().setAllTransactions();
        }
        
        for (Transaction transaction: Model.getInstance().getAllTransactions()) {
            // Only count completed transactions
            if (transaction.statusProperty().get() == Transaction.TransactionStatus.COMPLETED) {
                if (transaction.senderProperty().get().equals(Model.getInstance().getClient().pAddressProperty().get())){
                    expenses = expenses + transaction.amountProperty().get();
                } else if (transaction.receiverProperty().get().equals(Model.getInstance().getClient().pAddressProperty().get())) {
                    income = income + transaction.amountProperty().get();
                }
            }
        }
        
        income_lbl.setText(String.format("+ $%.2f", income));
        expense_lbl.setText(String.format("- $%.2f", expenses));
    }
}