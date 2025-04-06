package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.Transaction;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TransactionCellController implements Initializable {
    public FontAwesomeIconView in_icon;
    public FontAwesomeIconView out_icon;
    public Label trans_date_lbl;
    public Label trans_time_lbl;
    public Label sender_lbl;
    public Label receiver_lbl;
    public Button message_btn;
    public Label amount_lbl;
    public Label status_lbl;
    public Label type_lbl;

    private final Transaction transaction;

    public TransactionCellController(Transaction transaction){
        this.transaction = transaction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Bind existing properties
        sender_lbl.textProperty().bind(transaction.senderProperty());
        receiver_lbl.textProperty().bind(transaction.receiverProperty());
        amount_lbl.textProperty().bind(transaction.amountProperty().asString("$%.2f"));
        
        // Format date and time separately if we have a LocalDateTime
        if (transaction.dateTimeProperty() != null && transaction.dateTimeProperty().get() != null) {
            trans_date_lbl.setText(transaction.dateTimeProperty().get().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                
            if (trans_time_lbl != null) {
                trans_time_lbl.setText(transaction.dateTimeProperty().get().format(
                    DateTimeFormatter.ofPattern("HH:mm")));
            }
        } else {
            // Fallback to old date format
            trans_date_lbl.textProperty().bind(transaction.dateProperty().asString());
        }
        
        // Bind new properties
        if (status_lbl != null && transaction.statusProperty() != null) {
            status_lbl.setText(transaction.statusProperty().get().toString());
            
            // Set color based on status
            switch (transaction.statusProperty().get()) {
                case COMPLETED:
                    status_lbl.setTextFill(Color.GREEN);
                    break;
                case PENDING:
                    status_lbl.setTextFill(Color.ORANGE);
                    break;
                case FAILED:
                case CANCELLED:
                    status_lbl.setTextFill(Color.RED);
                    break;
                default:
                    status_lbl.setTextFill(Color.BLACK);
            }
        }
        
        if (type_lbl != null && transaction.typeProperty() != null) {
            type_lbl.setText(transaction.typeProperty().get().toString());
        }
        
        // Set up message button
        message_btn.setOnAction(event -> 
            Model.getInstance().getViewFactory().showMessageWindow(
                transaction.senderProperty().get(), 
                transaction.messageProperty().get()
            )
        );
        
        // Setup transaction icons
        transactionIcons();
    }

    private void transactionIcons() {
        if (transaction.senderProperty().get().equals(Model.getInstance().getClient().pAddressProperty().get())){
            in_icon.setFill(Color.rgb(240, 240, 240));
            out_icon.setFill(Color.RED);
        } else {
            in_icon.setFill(Color.GREEN);
            out_icon.setFill(Color.rgb(240, 240, 240));
        }
    }
}