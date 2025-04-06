package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.Transaction;
import com.touchtrust.touchtrustbank.Views.TransactionCellFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class TransactionsController implements Initializable {
    public ListView<Transaction> transactions_listview;
    public ComboBox<String> transaction_filter;
    public Label transaction_count;
    
    private FilteredList<Transaction> filteredTransactions;
    

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize transactions list
        initAllTransactionsList();

        // Set up filtered list
        filteredTransactions = new FilteredList<>(Model.getInstance().getAllTransactions());
        transactions_listview.setItems(filteredTransactions);
        transactions_listview.setCellFactory(e -> new TransactionCellFactory());

        // Set up filter dropdown
        setupTransactionFilters();

        // Update the transaction count
        updateTransactionCount();

        // Listen for changes to all transactions list
        Model.getInstance().getAllTransactions().addListener((javafx.collections.ListChangeListener<Transaction>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    updateTransactionCount();
                    transactions_listview.refresh();
                }
            }
        });
    }

    private void initAllTransactionsList() {
        if (Model.getInstance().getAllTransactions().isEmpty()){
            Model.getInstance().setAllTransactions();
        }
    }
    
    private void setupTransactionFilters() {
        ObservableList<String> filters = FXCollections.observableArrayList(
            "All Transactions", 
            "Incoming", 
            "Outgoing", 
            "Last 7 Days", 
            "Last 30 Days",
            "Completed",
            "Pending",
            "Failed"
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
                case "Completed":
                    filterByStatus(Transaction.TransactionStatus.COMPLETED);
                    break;
                case "Pending":
                    filterByStatus(Transaction.TransactionStatus.PENDING);
                    break;
                case "Failed":
                    filterByStatus(Transaction.TransactionStatus.FAILED);
                    break;
                default:
                    resetTransactionFilters();
                    break;
            }
            updateTransactionCount();
        });
    }
    
    private void filterIncomingTransactions() {
        String clientAddress = Model.getInstance().getClient().pAddressProperty().get();
        filteredTransactions.setPredicate(transaction -> 
            transaction.receiverProperty().get().equals(clientAddress)
        );
    }
    
    private void filterOutgoingTransactions() {
        String clientAddress = Model.getInstance().getClient().pAddressProperty().get();
        filteredTransactions.setPredicate(transaction -> 
            transaction.senderProperty().get().equals(clientAddress)
        );
    }
    
    private void filterRecentTransactions(int days) {
        LocalDateTime cutoffDate = LocalDate.now().minusDays(days).atStartOfDay();
        filteredTransactions.setPredicate(transaction -> 
            transaction.dateTimeProperty().get().isAfter(cutoffDate)
        );
    }
    
    private void filterByStatus(Transaction.TransactionStatus status) {
        filteredTransactions.setPredicate(transaction -> 
            transaction.statusProperty().get() == status
        );
    }
    
    private void filterByType(Transaction.TransactionType type) {
        filteredTransactions.setPredicate(transaction -> 
            transaction.typeProperty().get() == type
        );
    }
    
    private void resetTransactionFilters() {
        filteredTransactions.setPredicate(transaction -> true);
    }
    
    private void updateTransactionCount() {
        int count = filteredTransactions.size();
        String filterType = transaction_filter.getValue();
        transaction_count.setText("Showing " + count + " " + filterType.toLowerCase());
    }
}