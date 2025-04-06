package com.touchtrust.touchtrustbank;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Views.ViewFactory;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        System.out.println(System.getProperty("java.library.path"));
        Model.getInstance().getViewFactory().showLoginWindow();
    }
}
