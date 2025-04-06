package com.touchtrust.touchtrustbank.Controllers;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.face.FaceRecognizer;
import org.opencv.core.MatOfInt;
import org.bytedeco.opencv.opencv_core.MatVector;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.DataBufferByte;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class FaceRecognitionService {
    private CascadeClassifier faceDetector;
    private FaceRecognizer recognizer;
    private VideoCapture videoCapture;
    private boolean initialized = false;
    private Map<Integer, String> labelToUserIdMap = new HashMap<>();
    private Map<String, Integer> userIdToLabelMap = new HashMap<>();
    private int currentLabel = 0;

    private final String CASCADE_FILE = "resources/haarcascade_frontalface_alt.xml";
    private final String FACE_DATA_DIR = "resources/faces/";
    private final String MODEL_FILE = "resources/model.xml";

    public FaceRecognitionService() {
        // Initialize OpenCV (make sure to load the OpenCV library)
        Loader.load(opencv_java.class);

        // Create directory for storing face images if it doesn't exist
        createDirectory(FACE_DATA_DIR);

        // Initialize face detector
        faceDetector = new CascadeClassifier();
        File cascadeFile = new File(CASCADE_FILE);
        if (!cascadeFile.exists()) {
            // Extract from resources if needed
            try {
                Files.copy(getClass().getResourceAsStream("/haarcascade_frontalface_alt.xml"),
                        cascadeFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!faceDetector.load(CASCADE_FILE)) {
            System.err.println("Error loading face cascade classifier");
        }

        // Initialize face recognizer
        recognizer = LBPHFaceRecognizer.create();

        // Load existing model if available
        File modelFile = new File(MODEL_FILE);
        if (modelFile.exists()) {
            recognizer.read(MODEL_FILE);
            loadUserIdMappings();
        }
    }

    private void createDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void loadUserIdMappings() {
        // Load user ID to label mappings from a file
        try {
            List<String> lines = Files.readAllLines(Paths.get(FACE_DATA_DIR + "mappings.txt"));
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    int label = Integer.parseInt(parts[0]);
                    String userId = parts[1];
                    labelToUserIdMap.put(label, userId);
                    userIdToLabelMap.put(userId, label);
                    if (label >= currentLabel) {
                        currentLabel = label + 1;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No mappings file found, will create one when needed");
        }
    }

    private void saveUserIdMappings() {
        // Save user ID to label mappings to a file
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : labelToUserIdMap.entrySet()) {
            lines.add(entry.getKey() + "," + entry.getValue());
        }

        try {
            Files.write(Paths.get(FACE_DATA_DIR + "mappings.txt"), lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean initialize() {
        if (!initialized) {
            videoCapture = new VideoCapture(0);
            if (!videoCapture.isOpened()) {
                System.err.println("Error opening webcam");
                return false;
            }
            initialized = true;
        }
        return true;
    }

    public void release() {
        if (videoCapture != null) {
            videoCapture.release();
        }
        initialized = false;
    }

    public Mat captureFrame() {
        if (!initialized) {
            if (!initialize()) {
                return null;
            }
        }

        Mat frame = new Mat();
        if (videoCapture.read(frame)) {
            return frame;
        }
        return null;
    }

    public Image captureFrameAsImage() {
        Mat frame = captureFrame();
        if (frame != null) {
            return matToImage(frame);
        }
        return null;
    }

    public Mat detectFaceInFrame(Mat frame) {
        // Convert frame to grayscale
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(grayFrame, faces);

        // Return the first face found
        Rect[] facesArray = faces.toArray();
        if (facesArray.length > 0) {
            // Extract face region
            Rect faceRect = facesArray[0];
            Mat face = new Mat(grayFrame, faceRect);

            // Resize to standard size
            Mat resizedFace = new Mat();
            Size size = new Size(100, 100);
            Imgproc.resize(face, resizedFace, size);

            return resizedFace;
        }

        return null;
    }

    public boolean registerFace(String userId) {
        if (!initialized) {
            if (!initialize()) {
                return false;
            }
        }

        // Create user directory
        String userDir = FACE_DATA_DIR + userId + "/";
        createDirectory(userDir);

        // Get user label or assign a new one
        int userLabel;
        if (userIdToLabelMap.containsKey(userId)) {
            userLabel = userIdToLabelMap.get(userId);
        } else {
            userLabel = currentLabel++;
            labelToUserIdMap.put(userLabel, userId);
            userIdToLabelMap.put(userId, userLabel);
            saveUserIdMappings();
        }

        // Capture multiple faces for training
        List<Mat> faceImages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Mat frame = captureFrame();
            if (frame != null) {
                Mat face = detectFaceInFrame(frame);
                if (face != null) {
                    // Save face image
                    String filename = userDir + "face_" + i + ".jpg";
                    Imgcodecs.imwrite(filename, face);
                    faceImages.add(face);

                    // Wait a bit between captures
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    i--; // retry if no face detected
                }
            }
        }

        // Train recognizer with the new faces
        if (!faceImages.isEmpty()) {
            trainRecognizer();
            return true;
        }

        return false;
    }

    private void trainRecognizer() {
        // Collect all face images and labels from the faces directory
        List<Mat> faces = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        File facesDir = new File(FACE_DATA_DIR);
        for (File userDir : facesDir.listFiles(File::isDirectory)) {
            String userId = userDir.getName();
            if (userIdToLabelMap.containsKey(userId)) {
                int label = userIdToLabelMap.get(userId);

                for (File imageFile : userDir.listFiles(f -> f.getName().endsWith(".jpg"))) {
                    Mat face = Imgcodecs.imread(imageFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                    faces.add(face);
                    labels.add(label);
                }
            }
        }

        // Convert to format for training
        if (!faces.isEmpty()) {
            // Create a Mat for labels
            Mat labelsMat = new Mat(labels.size(), 1, CvType.CV_32SC1);
            for (int i = 0; i < labels.size(); i++) {
                labelsMat.put(i, 0, labels.get(i));
            }

            // Create a list of Mats for LBPHFaceRecognizer
            List<Mat> trainFaces = new ArrayList<>(faces);

            // Train the recognizer
            recognizer.train(trainFaces, labelsMat);

            // Save the model
            recognizer.save(MODEL_FILE);
            System.out.println("Model trained with " + faces.size() + " images");
        } else {
            System.err.println("No face images found for training");
        }
    }

    public String recognizeFace() {
        if (!initialized) {
            if (!initialize()) {
                return null;
            }
        }

        Mat frame = captureFrame();
        if (frame == null) return null;

        Mat face = detectFaceInFrame(frame);
        if (face == null) return null;

        // Perform recognition
        int[] label = new int[1];
        double[] confidence = new double[1];
        recognizer.predict(face, label, confidence);

        // Check confidence threshold (lower value means more confident)
        if (confidence[0] < 80.0) {
            return labelToUserIdMap.get(label[0]);
        }

        return null;
    }

    public Image matToImage(Mat mat) {
        // Convert Mat to JavaFX Image
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    public Image highlightFaceInImage(Image image) {
        try {
            // Convert JavaFX Image to BufferedImage
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

            // Convert BufferedImage to Mat - more robust method
            Mat mat;
            if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
                mat = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC1);
            } else {
                // Convert to 3-channel BGR (OpenCV standard format)
                mat = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC3);
            }

            // Create a temporary BufferedImage with correct format for OpenCV
            BufferedImage convertedImg = new BufferedImage(
                    bufferedImage.getWidth(), bufferedImage.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(bufferedImage, 0, 0, null);

            // Get the data buffer
            byte[] data = ((DataBufferByte) convertedImg.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);

            // Detect faces
            MatOfRect faces = new MatOfRect();
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);
            faceDetector.detectMultiScale(grayMat, faces);

            // Draw rectangle around faces
            for (Rect rect : faces.toArray()) {
                Imgproc.rectangle(
                        mat,
                        new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0),
                        2
                );
            }

            return matToImage(mat);
        } catch (Exception e) {
            System.err.println("Error highlighting faces: " + e.getMessage());
            e.printStackTrace();
            return image;
        }}}