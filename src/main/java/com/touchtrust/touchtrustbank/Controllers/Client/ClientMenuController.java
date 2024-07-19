package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Views.ClientMenuOptions;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientMenuController implements Initializable {
    public Button dashboard_btn;
    public Button transaction_btn;
    public Button accounts_btn;
    public Button profile_btn;
    public Button logout_btn;
    public Button report_btn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("ClientMenuController initialized.");
        addListeners();
    }

    private void addListeners(){
        dashboard_btn.setOnAction(event -> onDashboard());
        transaction_btn.setOnAction(event -> onTransactions());
        accounts_btn.setOnAction(event -> onAccounts());
        logout_btn.setOnAction(event -> onLogout());


    }

    private void onDashboard(){
        Model.getInstance().getViewFactory().getClientSelectedMenu().set(ClientMenuOptions.DASHBOARD);
    }

    private void onTransactions(){
        System.out.println("Transaction button clicked");
        Model.getInstance().getViewFactory().getClientSelectedMenu().set(ClientMenuOptions.TRANSACTIONS);
        System.out.println("clientSelectedMenu set to Transaction");

    }

    private void onAccounts(){
        System.out.println("Accounts button clicked");
        Model.getInstance().getViewFactory().getClientSelectedMenu().set(ClientMenuOptions.ACCOUNTS);
        System.out.println("clientSelectedMenu set to Accounts");
    }

    private void onLogout(){
//        Get Stage
        Stage stage = (Stage) dashboard_btn.getScene().getWindow();
//        Close the client window
        Model.getInstance().getViewFactory().closeStage(stage);
//        Show Login window
        Model.getInstance().getViewFactory().showLoginWindow();
//        Set client login succes flag to false
        Model.getInstance().setClientLoginSuccessFlag(false);
    }

}
