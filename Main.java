package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Application {
    static {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    private ImageView imageView;
    private TextArea logArea;
    private VideoCapture capture;
    private Timer timer;
    private boolean cameraActive = false;
    private boolean isDarkMode = false;

    private BorderPane root;
    private Scene scene;
    private Label statusLabel;
    private Button getCaptionButton;
    private ProgressIndicator progressIndicator;

    // BLIP API Settings
    private static final String BLIP_API_URL = "https://api-inference.huggingface.co/models/Salesforce/blip-image-captioning-base";
    private static final String BLIP_API_TOKEN = "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"; // Replace with your actual token if needed

    // Store the current frame for processing
    private Mat currentFrame;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Object Detection Platform");

        // Image View with rounded corners
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        Rectangle clip = new Rectangle(640, 480);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        // Detection log area
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefWidth(340);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13;");

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(24, 24);

        // Buttons
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        Button captureButton = new Button("Capture");
        getCaptionButton = new Button("Get Caption");
        Button saveButton = new Button("Save Caption");
        Button clearButton = new Button("Clear Output");
        Button themeButton = new Button("Toggle Theme");
        Button exitButton = new Button("Exit");

        startButton.setOnAction(e -> {
            startCamera();
            updateStatus("Camera started");
        });
        stopButton.setOnAction(e -> {
            stopCamera();
            updateStatus("Camera stopped");
        });
        captureButton.setOnAction(e -> captureFrame());
        getCaptionButton.setOnAction(e -> getCaptionForCurrentFrame());
        saveButton.setOnAction(e -> saveCaption());
        clearButton.setOnAction(e -> logArea.clear());
        themeButton.setOnAction(e -> toggleTheme());
        exitButton.setOnAction(e -> {
            stopCamera();
            Platform.exit();
            System.exit(0);
        });

        // Disable caption button initially
        getCaptionButton.setDisable(true);

        HBox buttonBox = new HBox(10, startButton, stopButton, captureButton, getCaptionButton, progressIndicator,
                saveButton, clearButton, themeButton, exitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        HBox mainContent = new HBox(20, imageView, logArea);
        mainContent.setPadding(new Insets(15));

        statusLabel = new Label("Ready");
        statusLabel.setId("status-label");
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-font-size: 12;");

        HBox statusBar = new HBox(statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5, 10, 5, 10));

        root = new BorderPane();
        root.setTop(buttonBox);
        root.setCenter(mainContent);
        root.setBottom(statusBar);

        scene = new Scene(root, 1080, 620);
        scene.getStylesheets().add(getClass().getResource("/application/style.css").toExternalForm());

        applyLightTheme(); // default theme

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startCamera() {
        if (!cameraActive) {
            capture = new VideoCapture(0);
            if (capture.isOpened()) {
                cameraActive = true;
                currentFrame = new Mat();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (capture.read(currentFrame)) {
                            Image fxImage = matToImage(currentFrame);
                            Platform.runLater(() -> {
                                imageView.setImage(fxImage);
                                getCaptionButton.setDisable(false);
                            });
                        }
                    }
                }, 0, 33);
                log("Camera started successfully");
            } else {
                log("Error: Cannot open camera");
            }
        }
    }

    private void stopCamera() {
        if (cameraActive) {
            cameraActive = false;
            if (timer != null) {
                timer.cancel();
            }
            if (capture != null) {
                capture.release();
            }
            Platform.runLater(() -> {
                imageView.setImage(null);
                getCaptionButton.setDisable(true);
                log("Camera stopped");
            });
        }
    }

    private Image matToImage(Mat frame) {
        Mat converted = new Mat();
        Imgproc.cvtColor(frame, converted, Imgproc.COLOR_BGR2RGB);
        byte[] data = new byte[converted.rows() * converted.cols() * (int)(converted.elemSize())];
        converted.get(0, 0, data);
        BufferedImage bufferedImage = new BufferedImage(converted.width(), converted.height(), BufferedImage.TYPE_3BYTE_BGR);
        bufferedImage.getRaster().setDataElements(0, 0, converted.cols(), converted.rows(), data);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void captureFrame() {
        if (cameraActive && capture != null && capture.isOpened()) {
            Mat frame = new Mat();
            if (capture.read(frame)) {
                String filename = "capture_" + System.currentTimeMillis() + ".png";
                Imgcodecs.imwrite(filename, frame);
                log("Captured frame saved: " + filename);
                updateStatus("Frame saved");
            } else {
                log("Failed to capture frame");
            }
        } else {
            log("Camera not active, can't capture");
        }
    }

    private void getCaptionForCurrentFrame() {
        if (!cameraActive || currentFrame == null || currentFrame.empty()) {
            log("No frame available to caption");
            return;
        }

        // Create a copy of the current frame to process
        Mat frameCopy = currentFrame.clone();

        // Show progress indicator
        Platform.runLater(() -> {
            progressIndicator.setVisible(true);
            getCaptionButton.setDisable(true);
            updateStatus("Processing image...");
        });

        // Process frame in background thread
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                try {
                    return processFrameAndGetCaption(frameCopy);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "Error: " + e.getMessage();
                } finally {
                    frameCopy.release(); // Clean up the copied frame
                }
            }
        };

        task.setOnSucceeded(event -> {
            String caption = task.getValue();
            log("BLIP Caption: " + caption);
            progressIndicator.setVisible(false);
            getCaptionButton.setDisable(false);
            updateStatus("Caption received");
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            log("Error getting caption: " + exception.getMessage());
            progressIndicator.setVisible(false);
            getCaptionButton.setDisable(false);
            updateStatus("Caption failed");
        });

        new Thread(task).start();
    }

    // Process a frame and return a caption from the BLIP API
    private String processFrameAndGetCaption(Mat frame) throws IOException {
        BufferedImage image = matToBufferedImage(frame);
        if (image == null) throw new IOException("Failed to convert frame");

        // Convert to Base64
        String base64Image = bufferedImageToBase64(image);

        // Try with prefix first
        String caption = sendToBLIP(base64Image, true);

        // If failed, try without prefix
        if (caption.contains("Error") || caption.equals("No caption generated")) {
            caption = sendToBLIP(base64Image, false);
        }

        return caption;
    }

    // Convert Mat to BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) throws IOException {
        MatOfByte mob = new MatOfByte();
        if (!Imgcodecs.imencode(".jpg", mat, mob)) return null;
        byte[] byteArray = mob.toArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        return ImageIO.read(bis);
    }

    // Convert BufferedImage to Base64 string
    private String bufferedImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // Send the Base64 image to the BLIP API
    private String sendToBLIP(String base64Image, boolean usePrefix) {
        String imageData = usePrefix ? "data:image/jpeg;base64," + base64Image : base64Image;
        String jsonInputString = "{\"inputs\": \"" + imageData + "\"}";

        try {
            URL url = new URL(BLIP_API_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + BLIP_API_TOKEN);
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();

            InputStream is = (responseCode >= 200 && responseCode < 300) ? con.getInputStream() : con.getErrorStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line.trim());
            }
            in.close();

            String jsonResponse = response.toString();

            int index = jsonResponse.indexOf("\"generated_text\":");
            if (index != -1) {
                int start = jsonResponse.indexOf("\"", index + 17) + 1;
                int end = jsonResponse.indexOf("\"", start);
                return jsonResponse.substring(start, end);
            }
            return "No caption generated";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling BLIP API: " + e.getMessage();
        }
    }

    private void saveCaption() {
        String content = logArea.getText();
        if (content.isEmpty()) {
            log("No caption to save");
            return;
        }
        String filename = "caption_" + System.currentTimeMillis() + ".txt";
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), content.getBytes());
            log("Caption saved to: " + filename);
            updateStatus("Caption saved");
        } catch (Exception e) {
            log("Failed to save caption");
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            applyDarkTheme();
        } else {
            applyLightTheme();
        }
    }

    private void applyLightTheme() {
        root.getStyleClass().remove("dark-mode");
    }

    private void applyDarkTheme() {
        if (!root.getStyleClass().contains("dark-mode")) {
            root.getStyleClass().add("dark-mode");
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
}