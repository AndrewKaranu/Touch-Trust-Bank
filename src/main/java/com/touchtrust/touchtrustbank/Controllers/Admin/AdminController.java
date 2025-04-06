package com.touchtrust.touchtrustbank.Controllers.Admin;

import com.touchtrust.touchtrustbank.Models.Model;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
    public BorderPane admin_parent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("adminController initialized, admin_parent: " + (admin_parent != null));
        System.out.println("Current adminSelectedMenu value: " + Model.getInstance().getViewFactory().getAdminSelectedMenu().get());
        Model.getInstance().getViewFactory().getAdminSelectedMenu().addListener((observableValue, oldVal, newVal) -> {
            System.out.println("adminController: Menu changed from " + oldVal + " to " + newVal);
            switch (newVal){
                case CLIENTS -> {
                    System.out.println("Switching to Clients view");
                    admin_parent.setCenter(Model.getInstance().getViewFactory().getClientsView());
                }

                case DEPOSIT -> {
                    System.out.println("Switching to Deposit view");
                    admin_parent.setCenter(Model.getInstance().getViewFactory().getDepositView());
                }

                default -> {
                    System.out.println("Switching to CreateClient view");
                    admin_parent.setCenter(Model.getInstance().getViewFactory().getCreateClientView());
                }
            }
        });
    }
}

