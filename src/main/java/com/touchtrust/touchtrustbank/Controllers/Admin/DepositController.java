package com.touchtrust.touchtrustbank.Controllers.Admin;

import com.touchtrust.touchtrustbank.Models.Client;
import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Views.ClientCellFactory;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class DepositController implements Initializable {
    public TextField pAddress_fld;
    public Button search_btn;
    public ListView result_listview;
    public TextField ammount_fld;
    public Button deposit_btn;

    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        search_btn.setOnAction(event -> onClientSearch());
        deposit_btn.setOnAction(event -> onDeposit());
    }

    private void onClientSearch() {
        ObservableList<Client> searchResults = Model.getInstance().searchClient(pAddress_fld.getText());
        result_listview.setItems(searchResults);
        result_listview.setCellFactory(e -> new ClientCellFactory());
        client = searchResults.get(0);
    }

    private void onDeposit() {
        double amount = Double.parseDouble(ammount_fld.getText());
        double newBalance = amount + client.savingsAccountProperty().get().balanceProperty().get();
        if (ammount_fld.getText() != null){
            Model.getInstance().getDatabaseDriver().depositSavings(client.pAddressProperty().get(), newBalance);
        }
        emptyFields();
    }

    private void emptyFields() {
        pAddress_fld.setText("");
        ammount_fld.setText("");
    }
}
