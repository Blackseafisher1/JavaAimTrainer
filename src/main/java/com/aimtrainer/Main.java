package com.aimtrainer;


// Removed unused imports
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) {
        // Globaler Exception-Handler: Fehler im UI anzeigen
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Platform.runLater(() -> showError("Uncaught Exception", e));
        });

        try {

            Class.forName("com.aimtrainer.Controller");
            Class.forName("com.aimtrainer.Controller$TargetSize"); // Innere Enums
            Class.forName("com.aimtrainer.Controller$HitEffect");

            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/AimTrainer.fxml"));
            Parent root = fxmlLoader.load();



            // Mobile: Bildschirmgröße, Desktop: Fallback
            Scene scene;
            
            // Prüfen, ob wir auf Android sind (Gluon Property)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("android") || os.contains("ios")) {
                Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
                scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
            } else {
                // Desktop-Fallback-Größe
                scene = new Scene(root, 1700, 1100);
            }

            stage.setTitle("Aim Trainer");
            stage.setScene(scene);
            
            // Mobile: evtl. Vollbild (auskommentiert)
            /* if (os.contains("android")) { stage.setFullScreen(true); } */
            
            stage.show();

        } catch (Throwable e) {
            // Fehler beim Laden (z.B. FXML nicht gefunden, DB Fehler im Controller)
            LOGGER.log(Level.SEVERE, "Startup error", e);
            showError("Start Error", e);
        }
    }

    private void showError(String title, Throwable e) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(e.getClass().getSimpleName() + ": " + e.getMessage());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
