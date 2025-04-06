package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.CheckingAccount;
import com.touchtrust.touchtrustbank.Models.CreditAccount;
import com.touchtrust.touchtrustbank.Models.Model;
import javafx.beans.binding.Bindings;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CreditAccountController implements Initializable {
    public Label credit_acc_num;
    public Label credit_limit;
    public Label available_credit;
    public Label credit_balance;
    public Label apr_label;
    public Label due_date;
    public Label minimum_payment;
    public TextField payment_amount;
    public Button make_payment_btn;
    public Label payment_status;
    public VBox transactions_container;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Check if client has a credit account
        if (Model.getInstance().getClient().hasCreditAccount()) {
            // Bind credit account data to UI
            bindCreditAccountData();

            // Set up payment button event handler
            make_payment_btn.setOnAction(event -> makePayment());
        } else {
            // Show message that client doesn't have a credit account
            credit_balance.setText("No credit account available");
            make_payment_btn.setDisable(true);
        }
    }

    private void bindCreditAccountData() {
        CreditAccount creditAccount = Model.getInstance().getClient().creditAccountProperty().get();
        if (creditAccount != null) {
            credit_acc_num.textProperty().bind(creditAccount.accountNumberProperty());
            credit_limit.textProperty().bind(Bindings.format("$%.2f", creditAccount.creditLimitProperty()));
            available_credit.textProperty().bind(Bindings.format("$%.2f", creditAccount.availableCreditProperty()));
            credit_balance.textProperty().bind(Bindings.format("$%.2f", creditAccount.balanceProperty()));
            apr_label.textProperty().bind(Bindings.format("%.2f%%", Bindings.multiply(creditAccount.aprProperty(), 100)));
            due_date.textProperty().bind(Bindings.createStringBinding(
                    () -> creditAccount.paymentDueDateProperty().get().format(DateTimeFormatter.ISO_DATE),
                    creditAccount.paymentDueDateProperty()
            ));
            minimum_payment.textProperty().bind(Bindings.format("$%.2f", creditAccount.minimumPaymentProperty()));
        }
    }

    private void makePayment() {
        try {
            // Reset status
            payment_status.setText("");

            double amount = Double.parseDouble(payment_amount.getText().trim());
            if (amount <= 0) {
                showPaymentError("Amount must be greater than zero");
                return;
            }

            // Get accounts
            CheckingAccount checkingAccount = Model.getInstance().getClient().checkingAccountProperty().get();
            CreditAccount creditAccount = Model.getInstance().getClient().creditAccountProperty().get();

            // Check if checking account has sufficient funds
            if (amount > checkingAccount.balanceProperty().get()) {
                showPaymentError("Insufficient funds in your checking account");
                return;
            }

            // Perform the payment
            String clientAddress = Model.getInstance().getClient().pAddressProperty().get();

            // Update balances in database
            Model.getInstance().getDatabaseDriver().updateCheckingBalance(clientAddress, amount, "SUB");
            Model.getInstance().getDatabaseDriver().updateCreditBalance(clientAddress, amount, "SUB");

            // Record the transaction
            Model.getInstance().getDatabaseDriver().recordTransaction(
                    clientAddress, "CREDIT-" + creditAccount.accountNumberProperty().get(),
                    amount, java.time.LocalDateTime.now(), "Credit Card Payment",
                    java.util.UUID.randomUUID().toString(), "COMPLETED", "PAYMENT", "CHECKING"
            );

            // Show success message
            showPaymentSuccess("Payment completed successfully");

            // Clear the amount field
            payment_amount.clear();

            // Refresh account data
            Model.getInstance().refreshClientAccountData();

        } catch (NumberFormatException e) {
            showPaymentError("Please enter a valid amount");
        }
    }

    private void showPaymentError(String message) {
        payment_status.setText(message);
        payment_status.setTextFill(Color.RED);
    }

    private void showPaymentSuccess(String message) {
        payment_status.setText(message);
        payment_status.setTextFill(Color.GREEN);
    }
}