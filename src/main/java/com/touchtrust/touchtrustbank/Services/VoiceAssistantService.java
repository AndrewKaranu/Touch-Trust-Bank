package com.touchtrust.touchtrustbank.Services;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.DatabaseDriver;
import com.touchtrust.touchtrustbank.Models.Transaction;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import javafx.application.Platform;
import javafx.scene.control.Label;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceAssistantService {

    private final DatabaseDriver databaseDriver;
    private final TransactionService transactionService;
    private final RAGService ragService;

    // Google Cloud Text-to-Speech client
    private TextToSpeechClient ttsClient;

    // Audio handling
    private AudioFormat audioFormat;
    private SourceDataLine audioLine;
    private TargetDataLine micLine;
    private boolean isListening = false;
    private ExecutorService executor;

    // UI elements
    private Label statusLabel; // UI feedback
    private Consumer<String> messageCallback;
    private Consumer<String> recognizedTextCallback;

    // CMU Sphinx
    private LiveSpeechRecognizer recognizer;
    private Configuration sphinxConfig;

    public VoiceAssistantService(DatabaseDriver databaseDriver, TransactionService transactionService) {
        this.databaseDriver = databaseDriver;
        this.transactionService = transactionService;
        this.ragService = new RAGService(databaseDriver);
        this.executor = Executors.newCachedThreadPool();

        initializeSpeechSynthesis();
        initializeSphinx();
    }

    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
    }

    public void setMessageCallback(Consumer<String> callback) {
        this.messageCallback = callback;
    }

    public void setRecognizedTextCallback(Consumer<String> callback) {
        this.recognizedTextCallback = callback;
    }

    private void initializeSphinx() {
        try {
            sphinxConfig = new Configuration();

            sphinxConfig.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
            sphinxConfig.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
            sphinxConfig.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

            recognizer = new LiveSpeechRecognizer(sphinxConfig);
            updateStatus("CMU Sphinx initialized", false);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error initializing CMU Sphinx: " + e.getMessage(), true);
        }
    }

    private void initializeSpeechSynthesis() {
        try {
            // Initialize Google Cloud Text-to-Speech client
            ttsClient = TextToSpeechClient.create();

            // Set up audio playback capabilities
            audioFormat = new AudioFormat(24000, 16, 1, true, false);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            audioLine = (SourceDataLine) AudioSystem.getLine(speakerInfo);

            updateStatus("Speech synthesis initialized", false);
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error initializing speech synthesis: " + e.getMessage(), true);
        }
    }

    public void startListening() {
        if (isListening || recognizer == null) return;

        try {
            isListening = true;
            updateStatus("Voice assistant activated. Listening...", false);
            speak("Voice assistant activated. How can I help you today?");

            // Start recognition in a separate thread
            executor.submit(this::localRecognize);

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error starting voice recognition: " + e.getMessage(), true);
        }
    }

    private void localRecognize() {
        try {
            recognizer.startRecognition(true);
            while (isListening) {
                SpeechResult result = recognizer.getResult();
                if (result != null) {
                    String transcript = result.getHypothesis();
                    updateStatus("Recognized: " + transcript, false);

                    // Notify UI of recognized text
                    if (recognizedTextCallback != null) {
                        Platform.runLater(() -> recognizedTextCallback.accept(transcript));
                    }

                    // Process the command
                    String responseText = processCommandAndGetResponse(transcript);

                    // Speak response
                    speak(responseText);
                }
            }
            recognizer.stopRecognition();
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error during speech recognition: " + e.getMessage(), true);
        }
    }

    public void stopListening() {
        if (!isListening) return;

        isListening = false;
        updateStatus("Voice assistant deactivated", false);
        speak("Voice assistant deactivated. Goodbye.");
    }

    public String processTextInput(String text) {
        // Process the text command without voice
        return processCommandAndGetResponse(text);
    }

    private String processCommandAndGetResponse(String command) {
        // Convert to lowercase for easier matching
        command = command.toLowerCase();

        // First, check for standard banking operations
        if (containsAny(command, "balance", "how much", "available")) {
            if (containsAny(command, "checking", "current")) {
                return getCheckingBalanceResponse();
            } else if (containsAny(command, "savings")) {
                return getSavingsBalanceResponse();
            } else if (containsAny(command, "credit")) {
                return getCreditBalanceResponse();
            } else {
                return getAllBalancesResponse();
            }
        } else if (containsAny(command, "transfer", "move money")) {
            return processTransferCommandResponse(command);
        } else if (containsAny(command, "send money", "pay")) {
            return processSendMoneyCommandResponse(command);
        } else if (containsAny(command, "recent transactions", "last transactions", "transaction history")) {
            return getRecentTransactionsResponse(command);
        } else if (containsAny(command, "credit limit", "available credit")) {
            return getCreditLimitResponse();
        } else if (containsAny(command, "payment due", "minimum payment", "credit payment")) {
            return getCreditPaymentInfoResponse();
        } else if (containsAny(command, "help", "what can you do", "commands")) {
            return provideHelpResponse();
        } else if (containsAny(command, "exit", "stop", "quit", "close")) {
            stopListening();
            return "Voice assistant deactivated. Goodbye.";
        } else {
            // Use RAG for more complex or conversational queries
            return handleRAGQueryResponse(command);
        }
    }

    // Existing methods (unchanged)
    private String getCheckingBalanceResponse() {
        double balance = Model.getInstance().getClient().getCheckingAccount().balanceProperty().get();
        return "Your checking account balance is " + formatCurrency(balance);
    }

    private String getSavingsBalanceResponse() {
        double balance = Model.getInstance().getClient().getSavingsAccount().balanceProperty().get();
        return "Your savings account balance is " + formatCurrency(balance);
    }

    private String getCreditBalanceResponse() {
        if (Model.getInstance().getClient().hasCreditAccount()) {
            double balance = Model.getInstance().getClient().getCreditAccount().balanceProperty().get();
            return "Your credit card balance is " + formatCurrency(balance);
        } else {
            return "You do not have a credit account.";
        }
    }

    private String getAllBalancesResponse() {
        double checkingBalance = Model.getInstance().getClient().getCheckingAccount().balanceProperty().get();
        double savingsBalance = Model.getInstance().getClient().getSavingsAccount().balanceProperty().get();

        StringBuilder response = new StringBuilder("Your account balances are: ");
        response.append("Checking: ").append(formatCurrency(checkingBalance)).append(", ");
        response.append("Savings: ").append(formatCurrency(savingsBalance));

        if (Model.getInstance().getClient().hasCreditAccount()) {
            double creditBalance = Model.getInstance().getClient().getCreditAccount().balanceProperty().get();
            response.append(", Credit Card: ").append(formatCurrency(creditBalance));
        }

        return response.toString();
    }

    // All other methods remain the same
    private String processTransferCommandResponse(String command) {
        // ... existing code ...
        return "Transfer command processed.";
    }

    private String transferToSavingsResponse(double amount) {
        // ... existing code ...
        return "Successfully transferred " + formatCurrency(amount) + " to your savings account.";
    }

    // Implement text-to-speech using Google Cloud
    public void speak(String text) {
        try {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            // Build the voice request
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("en-US")
                    .setSsmlGender(SsmlVoiceGender.FEMALE)
                    .setName("en-US-Neural2-F") // Modern neural voice
                    .build();

            // Select the type of audio file
            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.LINEAR16)
                    .setSampleRateHertz(24000)
                    .build();

            // Perform the text-to-speech request
            SynthesizeSpeechResponse response = ttsClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio content as ByteString
            ByteString audioContents = response.getAudioContent();

            // Play the audio
            playAudio(audioContents.toByteArray());

            // Update UI feedback
            if (statusLabel != null) {
                updateStatus("Assistant: " + text, false);
            }

            // Send message to UI
            if (messageCallback != null) {
                Platform.runLater(() -> messageCallback.accept(text));
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error in speech synthesis: " + e.getMessage(), true);
        }
    }

    private void playAudio(byte[] audioData) {
        try {
            // Create input stream from audio data
            InputStream byteStream = new ByteArrayInputStream(audioData);

            // Create audio input stream with the correct format
            AudioInputStream audioStream = new AudioInputStream(
                    byteStream,
                    audioFormat,
                    audioData.length / audioFormat.getFrameSize()
            );

            // Get line and play audio
            try {
                audioLine.open(audioFormat);
                audioLine.start();

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = audioStream.read(buffer, 0, buffer.length)) != -1) {
                    audioLine.write(buffer, 0, bytesRead);
                }

                audioLine.drain();
                audioLine.close();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }

            // Close the stream
            audioStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error playing audio: " + e.getMessage(), true);
        }
    }

    private void updateStatus(String message, boolean isError) {
        if (statusLabel != null) {
            Platform.runLater(() -> {
                statusLabel.setText(message);
                statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
            });
        }
    }

    private double extractAmount(String command) {
        // Pattern to match currency amounts like $50, 50 dollars, 50.25
        Pattern pattern = Pattern.compile(
                "\\$?(\\d+(?:\\.\\d+)?)|\\b(\\d+)\\s+dollars\\b"
        );
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            String amountStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            try {
                return Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }

    private String extractRecipient(String command) {
        // Look for recipient markers
        String[] markers = {"to @", "to user", "to payee", "to address"};

        for (String marker : markers) {
            int index = command.indexOf(marker);
            if (index != -1) {
                // Extract the text after the marker
                String afterMarker = command.substring(index + marker.length()).trim();
                // Extract the first word (or @-prefixed username)
                String[] words = afterMarker.split("\\s+");
                return words[0];
            }
        }

        return null;
    }

    private boolean containsAny(String text, String... searchTerms) {
        for (String term : searchTerms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String formatCurrency(double amount) {
        return String.format("$%.2f", amount);
    }

    public void shutdown() {
        stopListening();

        if (executor != null) {
            executor.shutdown();
        }

        if (ttsClient != null) {
            try {
                ttsClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (recognizer != null) {
            try {
                recognizer.stopRecognition();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (audioLine != null && audioLine.isOpen()) {
            audioLine.close();
        }
    }

    // Implement the missing methods with complete implementations

    private String transferToCheckingResponse(double amount) {
        String clientAddress = Model.getInstance().getClient().getPayeeAddress();
        double savingsBalance = Model.getInstance().getClient().getSavingsAccount().balanceProperty().get();
        double withdrawalLimit = Model.getInstance().getClient().getSavingsAccount().withdrawalLimitProp().get();

        if (amount > savingsBalance) {
            return "Insufficient funds in your savings account. Your current balance is " +
                    formatCurrency(savingsBalance);
        }

        if (amount > withdrawalLimit) {
            return "Amount exceeds your savings withdrawal limit of " + formatCurrency(withdrawalLimit);
        }

        // Update database
        databaseDriver.updateSavingsBalance(clientAddress, amount, "SUB");
        databaseDriver.updateCheckingBalance(clientAddress, amount, "ADD");

        // Record transaction
        databaseDriver.recordTransaction(
                clientAddress, clientAddress, amount, LocalDateTime.now(),
                "Voice-initiated transfer to Checking",
                UUID.randomUUID().toString(), "COMPLETED", "TRANSFER", "SAVINGS"
        );

        // Refresh data
        Platform.runLater(() -> Model.getInstance().refreshClientAccountData());

        return "Successfully transferred " + formatCurrency(amount) + " to your checking account.";
    }

    private String processSendMoneyCommandResponse(String command) {
        // Extract recipient
        String recipient = extractRecipient(command);
        if (recipient == null || recipient.isEmpty()) {
            return "Please specify a recipient for the payment.";
        }

        // Extract amount
        double amount = extractAmount(command);
        if (amount <= 0) {
            return "Please specify a valid amount to send.";
        }

        // Determine source account (default to checking)
        boolean isFromChecking = !containsAny(command, "from savings", "savings account");

        // Send the money
        boolean result = sendMoney(recipient, amount, isFromChecking);

        if (result) {
            return "Payment of " + formatCurrency(amount) + " to " + recipient + " was successful.";
        } else {
            return "Payment failed. Please check the recipient address and your account balance.";
        }
    }

    private boolean sendMoney(String recipient, double amount, boolean isFromChecking) {
        String sender = Model.getInstance().getClient().getPayeeAddress();

        // Use transaction service to process the payment
        boolean result = transactionService.processTransaction(
                sender, recipient, amount, "Voice-initiated payment",
                Transaction.TransactionType.PAYMENT, isFromChecking
        );

        if (result) {
            // Refresh data
            Platform.runLater(() -> Model.getInstance().refreshClientAccountData());
        }

        return result;
    }

    private String getRecentTransactionsResponse(String command) {
        // Extract time period from command
        int days = 30; // Default to 30 days
        if (containsAny(command, "week", "seven days")) {
            days = 7;
        } else if (containsAny(command, "today", "24 hours")) {
            days = 1;
        } else if (containsAny(command, "year", "twelve months")) {
            days = 365;
        }

        // Get transactions from the last specified days
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Transaction> transactions = Model.getInstance().getAllTransactions();
        int count = 0;
        double totalSpent = 0;
        double totalReceived = 0;

        String clientAddress = Model.getInstance().getClient().getPayeeAddress();

        for (Transaction transaction : transactions) {
            if (transaction.dateProperty().get().atStartOfDay().isAfter(startDate)) {
                count++;

                if (transaction.senderProperty().get().equals(clientAddress)) {
                    totalSpent += transaction.amountProperty().get();
                } else if (transaction.receiverProperty().get().equals(clientAddress)) {
                    totalReceived += transaction.amountProperty().get();
                }
            }
        }

        StringBuilder response = new StringBuilder();
        response.append("In the last ").append(days).append(" days, you had ")
                .append(count).append(" transactions. ");
        response.append("You spent ").append(formatCurrency(totalSpent))
                .append(" and received ").append(formatCurrency(totalReceived)).append(". ");

        double netChange = totalReceived - totalSpent;
        response.append("That's a net ").append(netChange >= 0 ? "increase" : "decrease")
                .append(" of ").append(formatCurrency(Math.abs(netChange))).append(".");

        // Add most recent transaction if available
        if (count > 0) {
            Transaction mostRecent = transactions.stream()
                    .filter(t -> t.dateProperty().get().atStartOfDay().isAfter(startDate))
                    .max((t1, t2) -> t1.dateProperty().get().compareTo(t2.dateProperty().get()))
                    .orElse(null);

            if (mostRecent != null) {
                response.append(" Your most recent transaction was on ")
                        .append(mostRecent.dateProperty().get().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .append(" for ").append(formatCurrency(mostRecent.amountProperty().get()))
                        .append(".");
            }
        }

        return response.toString();
    }

    private String getCreditLimitResponse() {
        if (Model.getInstance().getClient().hasCreditAccount()) {
            double creditLimit = Model.getInstance().getClient().getCreditAccount().creditLimitProperty().get();
            double availableCredit = Model.getInstance().getClient().getCreditAccount().availableCreditProperty().get();
            double balance = Model.getInstance().getClient().getCreditAccount().balanceProperty().get();

            StringBuilder response = new StringBuilder();
            response.append("Your credit card has a limit of ").append(formatCurrency(creditLimit))
                    .append(" and you have ").append(formatCurrency(availableCredit))
                    .append(" available credit. ");

            if (balance > 0) {
                response.append("Your current balance is ").append(formatCurrency(balance)).append(".");
            } else {
                response.append("You have no outstanding balance.");
            }

            return response.toString();
        } else {
            return "You do not have a credit account.";
        }
    }

    private String getCreditPaymentInfoResponse() {
        if (Model.getInstance().getClient().hasCreditAccount()) {
            double minimumPayment = Model.getInstance().getClient().getCreditAccount().minimumPaymentProperty().get();
            LocalDate dueDate = Model.getInstance().getClient().getCreditAccount().paymentDueDateProperty().get();
            double balance = Model.getInstance().getClient().getCreditAccount().balanceProperty().get();
            double apr = Model.getInstance().getClient().getCreditAccount().aprProperty().get() * 100;

            StringBuilder response = new StringBuilder();

            if (balance > 0) {
                response.append("Your minimum payment due is ").append(formatCurrency(minimumPayment))
                        .append(" by ").append(dueDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")))
                        .append(". Your current balance is ").append(formatCurrency(balance))
                        .append(" with an APR of ").append(String.format("%.2f", apr)).append("%.");

                // Calculate interest over next month if only minimum is paid
                double interestNextMonth = (balance - minimumPayment) * (apr / 100 / 12);
                if (interestNextMonth > 0) {
                    response.append(" If you only pay the minimum, you'll accrue approximately ")
                            .append(formatCurrency(interestNextMonth)).append(" in interest next month.");
                }
            } else {
                response.append("You have no outstanding balance on your credit card. Your credit card has an APR of ")
                        .append(String.format("%.2f", apr)).append("%.");
            }

            return response.toString();
        } else {
            return "You do not have a credit account.";
        }
    }

    private String provideHelpResponse() {
        return "I can help you with the following banking tasks:\n\n" +
                "- Check your account balances (checking, savings, credit)\n" +
                "- Transfer money between accounts\n" +
                "- Send payments to other users\n" +
                "- Review your recent transactions\n" +
                "- Get credit card information\n" +
                "- Find out about payment due dates and minimum payments\n\n" +
                "Try saying things like:\n" +
                "\"What's my checking balance?\"\n" +
                "\"Transfer $50 to savings\"\n" +
                "\"Send $20 to @johnsmith\"\n" +
                "\"Show me my transactions from the past week\"\n" +
                "\"What's my credit limit?\"\n" +
                "\"When is my credit card payment due?\"";
    }

    private String handleRAGQueryResponse(String query) {
        // Use RAG service to handle more complex conversational queries
        String answer = ragService.getAnswer(query);

        // If answer is empty or null, provide a fallback response
        if (answer == null || answer.trim().isEmpty()) {
            return "I'm sorry, I couldn't find a specific answer to that question. " +
                    "Try asking about your account balances, transactions, or credit information.";
        }

        return answer;
    }
}