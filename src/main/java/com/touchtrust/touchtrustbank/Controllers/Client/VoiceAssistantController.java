package com.touchtrust.touchtrustbank.Controllers.Client;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Services.VoiceAssistantService;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

public class VoiceAssistantController implements Initializable {
    public ScrollPane conversation_scroll;
    public VBox conversation_container;
    public Label status_label;
    public Button start_listening_btn;
    public Button stop_listening_btn;
    public Button help_btn;
    public TextArea text_input;
    public Button send_btn;
    
    private VoiceAssistantService voiceAssistant;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the voice assistant service
        voiceAssistant = new VoiceAssistantService(
            Model.getInstance().getDatabaseDriver(),
            Model.getInstance().getTransactionService()
        );
        
        // Set the status label for feedback
        voiceAssistant.setStatusLabel(status_label);
        
        // Add initial welcome message
        addAssistantMessage("Hello " + Model.getInstance().getClient().getFirstName() + 
                          ", I'm your Touch Trust Bank assistant. How can I help you today?");
        
        // Set up the button actions
        setupButtonActions();
        
        // Disable stop button initially
        stop_listening_btn.setDisable(true);
        
        // Set up voice assistant message listener
        voiceAssistant.setMessageCallback(this::addAssistantMessage);
        
        // Set up recognized text listener
        voiceAssistant.setRecognizedTextCallback(this::addUserMessage);
    }
    
    private void setupButtonActions() {
        start_listening_btn.setOnAction(event -> {
            voiceAssistant.startListening();
            start_listening_btn.setDisable(true);
            stop_listening_btn.setDisable(false);
            status_label.setText("Listening...");
        });
        
        stop_listening_btn.setOnAction(event -> {
            voiceAssistant.stopListening();
            start_listening_btn.setDisable(false);
            stop_listening_btn.setDisable(true);
            status_label.setText("Voice assistant stopped");
        });
        
        help_btn.setOnAction(event -> {
            showHelpDialog();
        });
        
        send_btn.setOnAction(event -> {
            String text = text_input.getText().trim();
            if (!text.isEmpty()) {
                processTextInput(text);
                text_input.clear();
            }
        });
        
        // Also process input when Enter is pressed (Shift+Enter for new line)
        text_input.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && !event.isShiftDown()) {
                event.consume(); // Prevent default behavior (new line)
                String text = text_input.getText().trim();
                if (!text.isEmpty()) {
                    processTextInput(text);
                    text_input.clear();
                }
            }
        });
    }
    
    private void processTextInput(String text) {
        addUserMessage(text);
        
        // Process the text through the voice assistant
        String response = voiceAssistant.processTextInput(text);
        addAssistantMessage(response);
    }
    
    private void addUserMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox();
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setPrefWidth(conversation_container.getWidth() - 20);
            
            TextFlow textFlow = new TextFlow();
            textFlow.getStyleClass().add("user_message");
            
            Text text = new Text(message);
            textFlow.getChildren().add(text);
            
            messageBox.getChildren().add(textFlow);
            conversation_container.getChildren().add(messageBox);
            
            // Scroll to bottom
            scrollToBottom();
        });
    }
    
    private void addAssistantMessage(String message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox();
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.setPrefWidth(conversation_container.getWidth() - 20);
            
            TextFlow textFlow = new TextFlow();
            textFlow.getStyleClass().add("assistant_message");
            
            Text text = new Text(message);
            textFlow.getChildren().add(text);
            
            messageBox.getChildren().add(textFlow);
            conversation_container.getChildren().add(messageBox);
            
            // Scroll to bottom
            scrollToBottom();
        });
    }
    
    private void scrollToBottom() {
        conversation_scroll.applyCss();
        conversation_scroll.layout();
        conversation_scroll.setVvalue(1.0);
    }
    
    private void showHelpDialog() {
        // Display available voice commands in assistant message
        String helpText = "Here are some things you can ask me:\n\n" +
                "- What's my checking/savings/credit balance?\n" +
                "- How much have I spent in the last week?\n" +
                "- Transfer $100 from checking to savings\n" +
                "- Send $50 to @johndoe\n" +
                "- What's my credit limit?\n" +
                "- When is my credit card payment due?\n" +
                "- Show me my recent transactions\n" +
                "- What's my savings interest rate?\n\n" +
                "You can also use the buttons below to start and stop voice recognition.";
        
        addAssistantMessage(helpText);
    }
}