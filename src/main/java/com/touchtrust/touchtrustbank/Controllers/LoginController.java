package com.touchtrust.touchtrustbank.Controllers;

import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Views.AccountType;
import javafx.collections.FXCollections;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    public ChoiceBox<AccountType> acc_selector;
    public ChoiceBox<String> login_method_selector;
    public TextField payee_address_fld;
    public PasswordField password_fld;
    public Button login_btn;
    public Label error_lbl;
    public HBox login_options_box;

    // Face recognition UI
    public VBox credentials_login_container;
    public VBox face_recognition_container;
    public VBox register_face_container;
    public Button register_face_btn;
    public Button start_face_scan_btn;
    public ImageView face_preview;
    public Label face_recognition_status;
    public TextField register_payee_address_fld;
    public PasswordField register_password_fld;
    public Button verify_credentials_btn;

    private FaceRecognitionService faceRecognitionService;
    private boolean isFaceRecognitionInitialized = false;
    private boolean isCapturingFace = false;
    private String currentPayeeAddress = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        acc_selector.setItems(FXCollections.observableArrayList(AccountType.CLIENT, AccountType.ADMIN));
        acc_selector.setValue(AccountType.CLIENT);

        login_method_selector.setItems(FXCollections.observableArrayList("Credentials", "Face Recognition"));
        login_method_selector.setValue("Credentials");

        // Handle login method change
        login_method_selector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.equals("Face Recognition")) {
                credentials_login_container.setVisible(false);
                credentials_login_container.setManaged(false);
                face_recognition_container.setVisible(true);
                face_recognition_container.setManaged(true);
                initializeFaceRecognition();
            } else {
                credentials_login_container.setVisible(true);
                credentials_login_container.setManaged(true);
                face_recognition_container.setVisible(false);
                face_recognition_container.setManaged(false);
                stopFaceRecognition();
            }
        });

        // Show/hide the login method selector based on account type
        acc_selector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == AccountType.CLIENT) {
                login_options_box.setVisible(true);
                login_options_box.setManaged(true);
                register_face_btn.setVisible(true);
                register_face_btn.setManaged(true);
            } else {
                login_options_box.setVisible(false);
                login_options_box.setManaged(false);
                register_face_btn.setVisible(false);
                register_face_btn.setManaged(false);

                // Reset to credentials when switching to admin
                login_method_selector.setValue("Credentials");
                credentials_login_container.setVisible(true);
                credentials_login_container.setManaged(true);
                face_recognition_container.setVisible(false);
                face_recognition_container.setManaged(false);
            }
        });

        login_btn.setOnAction(event -> onLogin());

        start_face_scan_btn.setOnAction(event -> {
            if (!isCapturingFace) {
                isCapturingFace = true;
                face_recognition_status.setText("Looking for your face...");
                startFaceRecognition();
            } else {
                isCapturingFace = false;
                face_recognition_status.setText("Face scan stopped");
                stopFaceCapture();
            }
        });

        register_face_btn.setOnAction(event -> {
            credentials_login_container.setVisible(false);
            credentials_login_container.setManaged(false);
            face_recognition_container.setVisible(false);
            face_recognition_container.setManaged(false);
            register_face_container.setVisible(true);
            register_face_container.setManaged(true);
        });

        verify_credentials_btn.setOnAction(event -> verifyCredentialsForFaceRegistration());
    }

    private void initializeFaceRecognition() {
        if (faceRecognitionService == null) {
            faceRecognitionService = new FaceRecognitionService();
        }

        if (!isFaceRecognitionInitialized) {
            boolean initialized = faceRecognitionService.initialize();
            if (!initialized) {
                face_recognition_status.setText("Error: Could not initialize camera");
                return;
            }
            isFaceRecognitionInitialized = true;
        }
    }

    private void startFaceRecognition() {
        initializeFaceRecognition();

        // Start a thread to capture frames and update the preview
        Thread captureThread = new Thread(() -> {
            while (isCapturingFace) {
                try {
                    // Capture frame and show in preview
                    Image frame = faceRecognitionService.captureFrameAsImage();
                    if (frame != null) {
                        Image highlightedFrame = faceRecognitionService.highlightFaceInImage(frame);

                        // Update UI on JavaFX thread
                        javafx.application.Platform.runLater(() -> face_preview.setImage(highlightedFrame));

                        // Try to recognize face
                        String recognizedUserId = faceRecognitionService.recognizeFace();
                        if (recognizedUserId != null) {
                            // Face recognized, update UI and login
                            javafx.application.Platform.runLater(() -> {
                                face_recognition_status.setText("Face recognized: " + recognizedUserId);
                                isCapturingFace = false;
                                currentPayeeAddress = recognizedUserId;
                                loginWithFace(recognizedUserId);
                            });
                            break;
                        }
                    }

                    Thread.sleep(100); // Small delay to reduce CPU usage
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void stopFaceCapture() {
        isCapturingFace = false;
    }

    private void stopFaceRecognition() {
        if (faceRecognitionService != null) {
            isCapturingFace = false;
            faceRecognitionService.release();
            isFaceRecognitionInitialized = false;
        }
    }

    private void loginWithFace(String payeeAddress) {
        // Use Model's evaluateClientCredentials method with null password
        // to indicate facial recognition login
        Model.getInstance().evaluateClientCredentials(payeeAddress, null);

        if (Model.getInstance().getClientLoginSuccessFlag()) {
            // Login succeeded
            stopFaceRecognition();
            Stage stage = (Stage) error_lbl.getScene().getWindow();
            Model.getInstance().getViewFactory().closeStage(stage);
            Model.getInstance().getViewFactory().showClientWindow();
        } else {
            // Login failed
            face_recognition_status.setText("Login failed: User not found");
        }
    }

    private void verifyCredentialsForFaceRegistration() {
        String payeeAddress = register_payee_address_fld.getText();
        String password = register_password_fld.getText();

        if (payeeAddress.isEmpty() || password.isEmpty()) {
            error_lbl.setText("Please enter both payee address and password");
            return;
        }

        // Verify credentials against the database
        ResultSet resultSet = Model.getInstance().getDatabaseDriver().getClientData(payeeAddress, password);
        try {
            if (resultSet != null && resultSet.isBeforeFirst()) {
                // Credentials are valid, proceed with face registration
                error_lbl.setText("");
                register_face_container.setVisible(false);
                register_face_container.setManaged(false);
                face_recognition_container.setVisible(true);
                face_recognition_container.setManaged(true);

                currentPayeeAddress = payeeAddress;

                // Initialize face recognition
                initializeFaceRecognition();

                // Start face registration process
                face_recognition_status.setText("Look at the camera to register your face");

                // Register the face
                boolean registered = faceRecognitionService.registerFace(payeeAddress);

                if (registered) {
                    face_recognition_status.setText("Face registered successfully! You can now login with your face.");

                    // Update database to track that this user has registered their face
                    Model.getInstance().getDatabaseDriver().saveFacialRecognitionData(
                            payeeAddress,
                            "resources/faces/" + payeeAddress + "/"
                    );
                } else {
                    face_recognition_status.setText("Failed to register face. Please try again.");
                }
            } else {
                error_lbl.setText("Invalid credentials. Please check and try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            error_lbl.setText("An error occurred: " + e.getMessage());
        }
    }

    private void onLogin() {
        if (login_method_selector.getValue().equals("Credentials")) {
            String payeeAddressText = payee_address_fld.getText();
            String passwordText = password_fld.getText();

            if (payeeAddressText.isEmpty() || passwordText.isEmpty()) {
                error_lbl.setText("Please enter both payee address and password");
                return;
            }

            if (acc_selector.getValue() == AccountType.CLIENT) {
                // Client login logic
                Model.getInstance().evaluateClientCredentials(payeeAddressText, passwordText);
                if (Model.getInstance().getClientLoginSuccessFlag()) {
                    Model.getInstance().getViewFactory().showClientWindow();
                    // Close the login stage
                    Stage stage = (Stage) error_lbl.getScene().getWindow();
                    Model.getInstance().getViewFactory().closeStage(stage);
                } else {
                    error_lbl.setText("Invalid Credentials");
                }
            } else {
                // Admin login logic
                Model.getInstance().evaluateAdminCredentials(payeeAddressText, passwordText);
                if (Model.getInstance().getAdminLoginSuccessFlag()) {
                    Model.getInstance().getViewFactory().showAdminWindow();
                    // Close the login stage
                    Stage stage = (Stage) error_lbl.getScene().getWindow();
                    Model.getInstance().getViewFactory().closeStage(stage);
                } else {
                    error_lbl.setText("Invalid Credentials");
                }
            }
        }
    }
}