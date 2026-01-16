package com.aimtrainer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SoundManager {

    // Static instance für Singleton Pattern
    private static SoundManager instance;

    // Public method um die einzige Instanz zu bekommen
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    private SoundOption currentSoundOption = SoundOption.CLASSIC;
    private AudioClip wallHitSound;
    
    // NEU: Speichert den Pfad zur Custom-Datei
    private String customSoundPath = null;
    
    // NEU: Speichert den geladenen Custom Clip
    private AudioClip customAudioClip = null;
    
    // NEU: Volume Variable (da wir stop() entfernen, müssen wir volume hier verwalten)
    private double volume = 0.5;

    public String getCustomSoundPath() {
        return customSoundPath;
    }

    // Private constructor (Singleton)
    private SoundManager() {
        try {
            URL wallHitUrl = SoundManager.class.getResource("/sounds/wood.mp3");
            if (wallHitUrl != null) {
                wallHitSound = new AudioClip(wallHitUrl.toString());
                wallHitSound.setVolume(volume);
            } else {
                System.err.println("Wall hit sound not found: /sounds/wood.mp3");
            }
        } catch (Exception e) {
            System.err.println("Could not load wall hit sound: " + e.getMessage());
        }
    }

    // ... (FileChooser Methoden bleiben gleich) ...
    public boolean chooseCustomSound(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wähle einen Sound");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aiff", "*.m4a")
        );

        File selectedFile = fileChooser.showOpenDialog(ownerStage);
        if (selectedFile != null) {
            try {
                String fileUrl = selectedFile.toURI().toURL().toString();
                setCustomSoundPath(fileUrl);
                return true;
            } catch (MalformedURLException e) {
                System.err.println("Fehler beim Laden der Datei: " + e.getMessage());
            }
        }
        return false;
    }

    public void setCustomSoundPath(String url) {
        this.customSoundPath = url;
        this.customAudioClip = null; 
        System.out.println("Custom Sound gesetzt auf: " + url);
    }
    
    public boolean hasCustomSound() {
        return customSoundPath != null;
    }

    // ... (Volume Methoden - Empfohlen hinzuzufügen) ...
    public void setVolume(double v) {
        this.volume = Math.max(0.0, Math.min(1.0, v));
        if (wallHitSound != null) wallHitSound.setVolume(volume);
        if (customAudioClip != null) customAudioClip.setVolume(volume);
        // Für Enums:
        for(SoundOption so : SoundOption.values()) {
            AudioClip c = so.getAudioClip();
            if(c != null) c.setVolume(volume);
        }
    }
    
     public void changeVolume(double delta) {
        setVolume(volume + delta);
    }
    
    public double getVolume() { return volume; }


    // === PLAY LOGIK (KORRIGIERT) ===

    public void setCurrentSound(SoundOption soundOption) {
        this.currentSoundOption = soundOption;
    }

    public SoundOption getCurrentSound() {
        return currentSoundOption;
    }

    public void playCurrentSound() {
        if (currentSoundOption != null) {
            if (currentSoundOption == SoundOption.CUSTOM) {
                playCustomSound();
            } else {
                currentSoundOption.play(this.volume); // Volume übergeben
            }
        }
    }

    public void playSound(SoundOption soundOption) {
        if (soundOption != null) {
            if (soundOption == SoundOption.CUSTOM) {
                playCustomSound();
            } else {
                soundOption.play(this.volume);
            }
        }
    }

    public void playHit() {
        playCurrentSound();
    }

    public void playWallHit() {
        if (wallHitSound != null) {
            wallHitSound.play();
        }
    }
    
    private void playCustomSound() {
        if (customSoundPath == null) {
            System.out.println("Kein Custom Sound ausgewählt!");
            return;
        }
        
        if (customAudioClip == null) {
            try {
                customAudioClip = new AudioClip(customSoundPath);
                customAudioClip.setVolume(volume);
            } catch (Exception e) {
                System.err.println("Fehler beim Abspielen des Custom Sounds: " + e.getMessage());
                return;
            }
        }
        
        if (customAudioClip != null) {
            // FIX: KEIN stop() hier!
            // if (customAudioClip.isPlaying()) customAudioClip.stop(); <--- GELÖSCHT
            
            // AudioClip unterstützt paralleles Abspielen desselben Clips automatisch
            customAudioClip.play();
        }
    }

    public enum SoundOption {
        CLASSIC("Classic", "/sounds/classic.mp3"),
        BEEP("Beep", "/sounds/beep.mp3"),
        POP("Pop", "/sounds/pop.mp3"),
        LASER("Laser", "/sounds/laser.mp3"),
        CUSTOM("Custom", null);

        private final String displayName;
        private final String soundPath;
        private AudioClip audioClip;

        SoundOption(String displayName, String soundPath) {
            this.displayName = displayName;
            this.soundPath = soundPath;
        }

        public String getDisplayName() { return displayName; }
        public String getSoundPath() { return soundPath; }

        public AudioClip getAudioClip() {
            if (this == CUSTOM) return null;
            if (audioClip == null) {
                try {
                    URL soundUrl = SoundManager.class.getResource(soundPath);
                    if (soundUrl != null) {
                        audioClip = new AudioClip(soundUrl.toString());
                        // Volume wird jetzt zentral gesetzt, aber initial ok
                    }
                } catch (Exception e) {
                    System.err.println("Could not load sound: " + e.getMessage());
                }
            }
            return audioClip;
        }

        // Neue play Methode mit Volume-Support und OHNE stop()
        public void play(double vol) {
            if (this == CUSTOM) return;

            AudioClip clip = getAudioClip();
            if (clip != null) {
                clip.setVolume(vol);
                // FIX: KEIN stop() hier!
                // if (clip.isPlaying()) clip.stop(); <--- GELÖSCHT
                clip.play();
            }
        }
        
        // Alte play Methode für Kompatibilität (falls nötig)
        public void play() {
            play(0.5);
        }
    }
}