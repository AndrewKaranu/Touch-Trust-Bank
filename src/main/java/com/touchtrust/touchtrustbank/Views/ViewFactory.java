package com.touchtrust.touchtrustbank.Views;

import com.touchtrust.touchtrustbank.Controllers.Admin.AdminController;
import com.touchtrust.touchtrustbank.Controllers.Client.ClientController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ViewFactory {
    private AccountType loginAccountType;

    //Client Views
    private final ObjectProperty<ClientMenuOptions> clientSelectedMenu;
    private AnchorPane dashboardView;
    private AnchorPane transactionsView;
    private AnchorPane accountsView;
    private AnchorPane creditAccountView;

    //Admin Views
    private final ObjectProperty<AdminMenuOptions> adminSelectedMenu;
    private AnchorPane createClientView;

    private AnchorPane clientsView;
    private AnchorPane depositView;


    public ViewFactory(){
        this.loginAccountType = AccountType.CLIENT;
        this.clientSelectedMenu = new SimpleObjectProperty<>();
        this.adminSelectedMenu = new SimpleObjectProperty<>();
    }

    public AccountType getLoginAccountType() {
        return loginAccountType;
    }

    public void setLoginAccountType(AccountType loginAccountType) {
        this.loginAccountType = loginAccountType;
    }

    //    Client Views

    public ObjectProperty<ClientMenuOptions> getClientSelectedMenu() {
        System.out.println("Getting clientSelectedMenu: " + clientSelectedMenu.get());
        return clientSelectedMenu;
    }

    public AnchorPane getDashboardView() {
        if (dashboardView == null){
            try {
                dashboardView = new FXMLLoader(getClass().getResource("/FXML/Client/Dashboard.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return dashboardView;
    }

    public AnchorPane getTransactionsView() {
        if (transactionsView == null){
            try {
                transactionsView = new FXMLLoader(getClass().getResource("/FXML/Client/Transactions.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return transactionsView;
    }

    public AnchorPane getAccountsView() {
        if (accountsView == null){
            try {
                accountsView = new FXMLLoader(getClass().getResource("/FXML/Client/Accounts.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return accountsView;
    }

    public AnchorPane getCreditAccountView() {
        if (creditAccountView == null) {
            try {
                creditAccountView = new FXMLLoader(getClass().getResource("/FXML/Client/CreditAccount.fxml")).load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return creditAccountView;
    }

//    Admin Views


    public ObjectProperty<AdminMenuOptions> getAdminSelectedMenu() {
        System.out.println("Getting adminSelectedMenu: " + adminSelectedMenu.get());

        return adminSelectedMenu;
    }

    public AnchorPane getCreateClientView() {
        if (createClientView == null){
            try {
                createClientView = new FXMLLoader(getClass().getResource("/FXML/Admin/CreateClient.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return createClientView;
    }

    public AnchorPane getClientsView() {
        if (clientsView == null){
            try {
                clientsView = new FXMLLoader(getClass().getResource("/FXML/Admin/Clients.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return clientsView;
    }

    public AnchorPane getDepositView() {
        if (depositView == null){
            try {
                depositView = new FXMLLoader(getClass().getResource("/FXML/Admin/Deposit.fxml")).load();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return depositView;
    }

    public void createStage(FXMLLoader loader){
        Scene scene = null ;

        try {
            scene = new Scene(loader.load());
        }catch (Exception e){
            e.printStackTrace();
        }
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.getIcons().add(new Image(String.valueOf(getClass().getResource("/Images/Headerlogo.png"))));
        stage.setResizable(false);
        stage.setTitle("Touch Trust Bank");
        stage.show();
    }


    public void showLoginWindow () {
        System.out.println("Showing login window");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Login.fxml"));
        createStage(loader);
    }

    public void showMessageWindow(String pAddress, String messageText) {
        StackPane pane = new StackPane();
        HBox hBox = new HBox(5);
//        hBox.setAlignment(Pos.CENTER);
        Label sender = new Label(pAddress);
        Label message = new Label(messageText);
        hBox.getChildren().addAll(sender, message);
        pane.getChildren().add(hBox);
        Scene scene = new Scene(pane, 300, 100);
        Stage stage = new Stage();
        stage.getIcons().add(new Image(String.valueOf(getClass().getResource("/Images/Headerlogo.png"))));
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Message");
        stage.setScene(scene);
        stage.show();
    }

    public void showClientWindow() {
        System.out.println("Showing client window");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Client/Client.fxml"));
        createStage(loader);
    }

    public void showAdminWindow(){
        System.out.println("Showing admin window");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/Admin/Admin.fxml"));
        createStage(loader);
    }

    public void closeStage(Stage stage){
        stage.close();
    }

    // Add this field to your ViewFactory class
private AnchorPane voiceAssistantView;

// Add getter method for voice assistant view
public AnchorPane getVoiceAssistantView() {
    if (voiceAssistantView == null) {
        try {
            voiceAssistantView = new FXMLLoader(getClass().getResource("/FXML/Client/VoiceAssistant.fxml")).load();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading Voice Assistant view: " + e.getMessage());
        }
    }
    return voiceAssistantView;
}






}
