package com.touchtrust.touchtrustbank.Controllers.Admin;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Views.AdminMenuOptions;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminMenuController implements Initializable {
    public Button create_client_btn;
    public Button clients_btn;
    public Button deposit_btn;
    public Button logout_btn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        addListeners();
    }
    private void addListeners(){
        create_client_btn.setOnAction(event-> onCreate());
        clients_btn.setOnAction(event-> onClients());
        deposit_btn.setOnAction(event-> onDeposit());
        logout_btn.setOnAction(event-> onLogout());

    }

    private void onCreate(){
        Model.getInstance().getViewFactory().getAdminSelectedMenu().set(AdminMenuOptions.CREATE_CLIENT);
    }

    private void onClients(){
        Model.getInstance().getViewFactory().getAdminSelectedMenu().set(AdminMenuOptions.CLIENTS);
    }

    private void onDeposit(){
        Model.getInstance().getViewFactory().getAdminSelectedMenu().set(AdminMenuOptions.DEPOSIT);
    }

    private void onLogout(){
//        Get Stage
        Stage stage = (Stage) clients_btn.getScene().getWindow();
//        Close the client window
        Model.getInstance().getViewFactory().closeStage(stage);
//        Show the Login window
        Model.getInstance().getViewFactory().showLoginWindow();
//        Set client login success flag to false
        Model.getInstance().setAdminLoginSuccessFlag(false);
    }
}
