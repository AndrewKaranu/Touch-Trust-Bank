package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.CheckingAccount;
import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.SavingsAccount;
import com.touchtrust.touchtrustbank.Models.Transaction;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class AccountsController implements Initializable {
    public Label ch_acc_num;
    public Label transaction_limit;
    public Label ch_acc_date;
    public Label ch_acc_bal;
    public Label sv_acc_num;
    public Label withdraw_limit;
    public Label sv_acc_date;
    public Label sv_acc_bal;
    public TextField amount_to_sv;
    public Button trans_to_sv_btn;
    public TextField amount_to_ch;
    public Button trans_to_ch_btn;
    public Label transfer_to_sv_status;
    public Label transfer_to_ch_status;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Bind account data to UI
        bindAccountData();

        // Set up transaction event handlers
        trans_to_sv_btn.setOnAction(event -> transferToSavings());
        trans_to_ch_btn.setOnAction(event -> transferToChecking());

        // Add listener for account changes to ensure UI stays updated
        setupAccountChangeListeners();
    }

    private void bindAccountData() {
        // Bind checking account data
        CheckingAccount checkingAccount =  Model.getInstance().getClient().checkingAccountProperty().get();
        bindCheckingAccountData(checkingAccount);

        // Bind savings account data
        SavingsAccount savingsAccount =  Model.getInstance().getClient().savingsAccountProperty().get();
        bindSavingsAccountData(savingsAccount);
    }

    private void bindCheckingAccountData(CheckingAccount account) {
        if (account != null) {
            ch_acc_num.textProperty().bind(account.accountNumberProperty());
            transaction_limit.textProperty().bind(account.transactionLimitProp().asString());
            ch_acc_bal.textProperty().bind(Bindings.format("$%.2f", account.balanceProperty()));

            // Set the date created
            if (account.openDateProperty() != null && account.openDateProperty().get() != null) {
                ch_acc_date.setText(account.openDateProperty().get().format(DateTimeFormatter.ISO_DATE));
            } else {
                ch_acc_date.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            }
        }
    }

    private void bindSavingsAccountData(SavingsAccount account) {
        if (account != null) {
            sv_acc_num.textProperty().bind(account.accountNumberProperty());
            withdraw_limit.textProperty().bind(Bindings.format("$%.2f", account.withdrawalLimitProp()));
            sv_acc_bal.textProperty().bind(Bindings.format("$%.2f", account.balanceProperty()));

            // Set the date created
            if (account.openDateProperty() != null && account.openDateProperty().get() != null) {
                sv_acc_date.setText(account.openDateProperty().get().format(DateTimeFormatter.ISO_DATE));
            } else {
                sv_acc_date.setText(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            }
        }
    }

    private void setupAccountChangeListeners() {
        Model.getInstance().getClient().checkingAccountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                bindCheckingAccountData((CheckingAccount) newValue);
            }
        });

        Model.getInstance().getClient().savingsAccountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                bindSavingsAccountData((SavingsAccount) newValue);
            }
        });
    }

    private void transferToSavings() {
        try {
            // Reset status
            transfer_to_sv_status.setText("");

            double amount = Double.parseDouble(amount_to_sv.getText().trim());
            if (amount <= 0) {
                showTransferError("Amount must be greater than zero", transfer_to_sv_status);
                return;
            }

            CheckingAccount checkingAccount = Model.getInstance().getClient().checkingAccountProperty().get();
            if (amount > checkingAccount.balanceProperty().get()) {
                showTransferError("Insufficient funds in your checking account", transfer_to_sv_status);
                return;
            }

            // Perform the transfer
            String clientAddress = Model.getInstance().getClient().pAddressProperty().get();

            // Update database
            Model.getInstance().getDatabaseDriver().updateCheckingBalance(clientAddress, amount, "SUB");
            Model.getInstance().getDatabaseDriver().updateSavingsBalance(clientAddress, amount, "ADD");

            // Record the transaction
            recordInternalTransfer(clientAddress, clientAddress, amount, "Transfer from Checking to Savings", true);

            // Show success message
            showTransferSuccess("Transfer completed successfully", transfer_to_sv_status);

            // Clear the amount field
            amount_to_sv.clear();

            // Refresh account data
            Model.getInstance().refreshClientAccountData();

        } catch (NumberFormatException e) {
            showTransferError("Please enter a valid amount", transfer_to_sv_status);
        }
    }

    private void transferToChecking() {
        try {
            // Reset status
            transfer_to_ch_status.setText("");

            double amount = Double.parseDouble(amount_to_ch.getText().trim());
            if (amount <= 0) {
                showTransferError("Amount must be greater than zero", transfer_to_ch_status);
                return;
            }

            SavingsAccount savingsAccount = Model.getInstance().getClient().savingsAccountProperty().get();
            if (amount > savingsAccount.balanceProperty().get()) {
                showTransferError("Insufficient funds in your savings account", transfer_to_ch_status);
                return;
            }

            // Check if amount exceeds withdrawal limit
            if (amount > savingsAccount.withdrawalLimitProp().get()) {
                showTransferError("Amount exceeds your savings withdrawal limit of $" +
                        savingsAccount.withdrawalLimitProp().get(), transfer_to_ch_status);
                return;
            }

            // Perform the transfer
            String clientAddress = Model.getInstance().getClient().pAddressProperty().get();

            // Update database
            Model.getInstance().getDatabaseDriver().updateSavingsBalance(clientAddress, amount, "SUB");
            Model.getInstance().getDatabaseDriver().updateCheckingBalance(clientAddress, amount, "ADD");

            // Record the transaction
            recordInternalTransfer(clientAddress, clientAddress, amount, "Transfer from Savings to Checking", false);

            // Show success message
            showTransferSuccess("Transfer completed successfully", transfer_to_ch_status);

            // Clear the amount field
            amount_to_ch.clear();

            // Refresh account data
            Model.getInstance().refreshClientAccountData();

        } catch (NumberFormatException e) {
            showTransferError("Please enter a valid amount", transfer_to_ch_status);
        }
    }

    private void showTransferError(String message, Label statusLabel) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.RED);
    }

    private void showTransferSuccess(String message, Label statusLabel) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.GREEN);
    }

    private void recordInternalTransfer(String sender, String receiver, double amount,
                                        String message, boolean isFromChecking) {
        // Create a transaction to record the internal transfer
        Model.getInstance().getDatabaseDriver().createTransaction(
                sender, receiver, amount, message, isFromChecking
        );
    }


}