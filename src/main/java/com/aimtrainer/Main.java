package com.aimtrainer;


import com.aimtrainer.Controller;

import java.io.IOException;
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

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Globalen Exception Handler setzen, damit wir Fehler auf dem Handy SEHEN
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Platform.runLater(() -> showError("Uncaught Exception", e));
        });

        try {

            Class.forName("com.aimtrainer.Controller");
            Class.forName("com.aimtrainer.Controller$TargetSize"); // Innere Enums
            Class.forName("com.aimtrainer.Controller$HitEffect");

            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/AimTrainer.fxml"));
            Parent root = fxmlLoader.load();



            // FIX: Keine feste Größe! Nutze die Bildschirmgröße oder lass JavaFX es berechnen.
            // Auf Android wird das Fenster automatisch maximiert.
            // Für Desktop können wir eine Standardgröße als Fallback angeben, aber nicht erzwingen.
            Scene scene;
            
            // Prüfen, ob wir auf Android sind (Gluon Property)
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("android") || os.contains("ios")) {
                Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
                scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
            } else {
                // Desktop: Startgröße, aber skalierbar
                scene = new Scene(root, 1700, 1100); 
            }

            stage.setTitle("Aim Trainer");
            stage.setScene(scene);
            
            // Auf mobilen Geräten wichtig: Maximieren
            /* if (os.contains("android")) { stage.setFullScreen(true); } */ // Gluon macht das meist automatisch
            
            stage.show();

        } catch (Throwable e) {
            // Fehler beim Laden (z.B. FXML nicht gefunden, DB Fehler im Controller)
            e.printStackTrace(); // Logcat
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
