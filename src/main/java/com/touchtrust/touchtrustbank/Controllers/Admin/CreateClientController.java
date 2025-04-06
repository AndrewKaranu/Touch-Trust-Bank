package com.touchtrust.touchtrustbank.Controllers.Admin;

import com.touchtrust.touchtrustbank.Models.Model;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.util.Random;
import java.util.ResourceBundle;

public class CreateClientController implements Initializable {
    public TextField fname_fld;
    public TextField lname_fld;
    public TextField password_fld;
    public CheckBox pAddress_box;
    public Label pAddress_lbl;
    public CheckBox ch_acc_box;
    public TextField ch_amount_fld;
    public CheckBox sv_acc_box;
    public TextField sv_amount_fld;
    public CheckBox cr_acc_box;
    public TextField cr_limit_fld;
    public Button create_client_btn;
    public Label error_lbl;
    private String payeeAddress;
    private boolean createCheckingAccountFlag = false;
    private boolean createSavingsAccountFlag = false;
    private boolean createCreditAccountFlag = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        create_client_btn.setOnAction(event -> createClient());
        pAddress_box.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal){
                payeeAddress = createPayeeAddress();
                onCreatePayeeAddress();
            }
        });
        ch_acc_box.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal){
                createCheckingAccountFlag = true;
            }
        });
        sv_acc_box.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal){
                createSavingsAccountFlag = true;
            }
        });
        cr_acc_box.selectedProperty().addListener((observableValue, oldVal, newVal) -> {
            if (newVal){
                createCreditAccountFlag = true;
            }
        });
    }

    private void createClient() {
        // First validate inputs
        if (!validateInputs()) {
            return;
        }

        // Create Checking account
        if (createCheckingAccountFlag){
            createAccount("Checking");
        }
        // Create Savings Account
        if (createSavingsAccountFlag){
            createAccount("Savings");
        }
        // Create Credit Account
        if (createCreditAccountFlag){
            createAccount("Credit");
        }
        // Create Client
        String fname = fname_fld.getText();
        String lname = lname_fld.getText();
        String password = password_fld.getText();
        Model.getInstance().getDatabaseDriver().createClient(fname, lname, payeeAddress, password, LocalDate.now());
        error_lbl.setStyle("-fx-text-fill: blue; -fx-font-size: 1.3em; -fx-font-weight: bold");
        error_lbl.setText("Client Created Successfully!");
        emptyFields();
    }

    private boolean validateInputs() {
        // Basic validation to ensure required fields are filled
        if (fname_fld.getText().isEmpty() || lname_fld.getText().isEmpty() || password_fld.getText().isEmpty()) {
            error_lbl.setStyle("-fx-text-fill: red; -fx-font-size: 1.3em; -fx-font-weight: bold");
            error_lbl.setText("Please fill all required fields!");
            return false;
        }

        if (createCheckingAccountFlag && (ch_amount_fld.getText().isEmpty() || !isValidAmount(ch_amount_fld.getText()))) {
            error_lbl.setStyle("-fx-text-fill: red; -fx-font-size: 1.3em; -fx-font-weight: bold");
            error_lbl.setText("Please enter a valid amount for checking account!");
            return false;
        }

        if (createSavingsAccountFlag && (sv_amount_fld.getText().isEmpty() || !isValidAmount(sv_amount_fld.getText()))) {
            error_lbl.setStyle("-fx-text-fill: red; -fx-font-size: 1.3em; -fx-font-weight: bold");
            error_lbl.setText("Please enter a valid amount for savings account!");
            return false;
        }

        if (createCreditAccountFlag && (cr_limit_fld.getText().isEmpty() || !isValidAmount(cr_limit_fld.getText()))) {
            error_lbl.setStyle("-fx-text-fill: red; -fx-font-size: 1.3em; -fx-font-weight: bold");
            error_lbl.setText("Please enter a valid credit limit!");
            return false;
        }

        return true;
    }

    private boolean isValidAmount(String amount) {
        try {
            double value = Double.parseDouble(amount);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void createAccount(String accountType) {
        // Generate Account Number
        String firstSection = "3201";
        String lastSection = Integer.toString((new Random()).nextInt(9999) + 1000);
        String accountNumber = firstSection + " " + lastSection;

        // Create the account based on type
        switch (accountType) {
            case "Checking":
                double checkingBalance = Double.parseDouble(ch_amount_fld.getText());
                Model.getInstance().getDatabaseDriver().createCheckingAccount(payeeAddress, accountNumber, 10, checkingBalance);
                break;
            case "Savings":
                double savingsBalance = Double.parseDouble(sv_amount_fld.getText());
                Model.getInstance().getDatabaseDriver().createSavingsAccount(payeeAddress, accountNumber, 2000, savingsBalance);
                break;
            case "Credit":
                double creditLimit = Double.parseDouble(cr_limit_fld.getText());
                Model.getInstance().getDatabaseDriver().createCreditAccount(payeeAddress, accountNumber, creditLimit, 0);
                break;
        }
    }

    private void onCreatePayeeAddress() {
        if (fname_fld.getText() != null & lname_fld.getText() != null){
            pAddress_lbl.setText(payeeAddress);
        }
    }

    private String createPayeeAddress() {
        int id = Model.getInstance().getDatabaseDriver().getLastClientsId() + 1;
        char fChar = Character.toLowerCase(fname_fld.getText().charAt(0));
        return "@"+fChar+lname_fld.getText()+id;
    }

    private void emptyFields() {
        fname_fld.setText("");
        lname_fld.setText("");
        password_fld.setText("");
        pAddress_box.setSelected(false);
        pAddress_lbl.setText("");
        ch_acc_box.setSelected(false);
        ch_amount_fld.setText("");
        sv_acc_box.setSelected(false);
        sv_amount_fld.setText("");
        cr_acc_box.setSelected(false);
        cr_limit_fld.setText("");
        createCheckingAccountFlag = false;
        createSavingsAccountFlag = false;
        createCreditAccountFlag = false;
    }
}