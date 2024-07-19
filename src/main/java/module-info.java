module com.touchtrust.touchtrustbank {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

//    requires de.jensd.fx.glyphs.fontawesome; // Font Awesome
    requires java.sql; // For SQLite
    requires org.xerial.sqlitejdbc;
    requires jbcrypt;
    requires de.jensd.fx.glyphs.fontawesome;


    opens com.touchtrust.touchtrustbank to javafx.fxml;
    exports com.touchtrust.touchtrustbank;
    exports com.touchtrust.touchtrustbank.Controllers;
    exports com.touchtrust.touchtrustbank.Controllers.Admin;
    exports com.touchtrust.touchtrustbank.Controllers.Client;
    exports com.touchtrust.touchtrustbank.Models;
    exports com.touchtrust.touchtrustbank.Views;
}
