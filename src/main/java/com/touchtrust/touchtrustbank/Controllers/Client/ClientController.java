package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.Model;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    @FXML
    public BorderPane client_parent;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("ClientController initialized, client_parent: " + (client_parent != null));
        System.out.println("Current clientSelectedMenu value: " + Model.getInstance().getViewFactory().getClientSelectedMenu().get());
        Model.getInstance().getViewFactory().getClientSelectedMenu().addListener((observableValue, oldVal, newVal) -> {
            System.out.println("ClientController: Menu changed from " + oldVal + " to " + newVal);
            switch (newVal){
                case TRANSACTIONS -> {
                    System.out.println("Switching to Transactions view");
                    client_parent.setCenter(Model.getInstance().getViewFactory().getTransactionsView());
                }
                case ACCOUNTS -> {
                    System.out.println("Switching to Accounts view");
                    client_parent.setCenter(Model.getInstance().getViewFactory().getAccountsView());
                }
                default -> {
                    System.out.println("Switching to Dashboard view");
                    client_parent.setCenter(Model.getInstance().getViewFactory().getDashboardView());
                }
            }
        });
    }
}
