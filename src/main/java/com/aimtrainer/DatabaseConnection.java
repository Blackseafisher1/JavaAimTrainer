package com.aimtrainer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    private Connection connection;
    private static final String DB_NAME = "aimtrainer.db";
    private String dbPath;
    private boolean initialized = false;

    public DatabaseConnection() {
        dbPath = DB_NAME;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                if (!initialized) {
                    initializeDatabase();
                    initialized = true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error connecting to database", e);
            throw new RuntimeException("Failed to connect to database", e);
        }
        return connection;
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA busy_timeout = 5000");

            createTables();

            LOGGER.info("Database initialized at: " + new File(dbPath).getAbsolutePath());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // --- Tabelle: Spiele (games) ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS games (" +
                "    game_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "    score INTEGER DEFAULT 0," +
                "    missclicks INTEGER DEFAULT 0," +
                "    accuracy DECIMAL(5,2) DEFAULT 0," +
                "    combo INTEGER DEFAULT 0," +
                "    avg_time DECIMAL(5,3) DEFAULT 0," +
                "    mode TEXT DEFAULT 'SNIPER'," +
                "    target_size TEXT DEFAULT 'Medium'" +
                ")"
            );

            // --- Tabelle: Bestwerte (best_scores) ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS best_scores (" +
                "    mode TEXT PRIMARY KEY," +
                "    best_score INTEGER DEFAULT 0," +
                "    best_accuracy DECIMAL(5,2) DEFAULT 0," +
                "    best_combo INTEGER DEFAULT 0," +
                "    total_games INTEGER DEFAULT 0," +
                "    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            String[] modes = { "SNIPER", "RADIAL", "BOUNCE" };
            for (String mode : modes) {
                stmt.execute("INSERT OR IGNORE INTO best_scores (mode) VALUES ('" + mode + "')");
            }

            // --- Tabelle: Einstellungen (preferences) ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS preferences (" +
                "    mode TEXT DEFAULT 'BOUNCE'," +
                "    speed REAL DEFAULT 1.0," + // Name von ball_speed zu speed geändert
                "    sound TEXT DEFAULT 'CLASSIC'," +
                "    size TEXT DEFAULT 'MEDIUM'," +
                "    effect TEXT DEFAULT 'EXPAND_CONTRACT'," +
                "    custom_sound_path TEXT," +
                "    hit_effects_enabled INTEGER DEFAULT 1," +
                "    use_target INTEGER DEFAULT 0" + // use_target: 0=false, 1=true
                ")"
            );

            // --- Migration: Fehlende Spalten bei Bedarf hinzufügen ---
            try { stmt.execute("ALTER TABLE preferences ADD COLUMN custom_sound_path TEXT"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE preferences ADD COLUMN hit_effects_enabled INTEGER DEFAULT 1"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE preferences ADD COLUMN use_target INTEGER DEFAULT 0"); } catch (SQLException ignored) {}

            // --- Standard-Einstellungen einfügen, falls leer ---
            String checkSql = "SELECT COUNT(*) FROM preferences";
            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute(
                        "INSERT INTO preferences (mode, speed, sound, size, effect, custom_sound_path, hit_effects_enabled, use_target) " +
                        "VALUES ('BOUNCE', 1.0, 'CLASSIC', 'MEDIUM', 'EXPAND_CONTRACT', NULL, 1, 0)"
                    );
                }
            }
        }
    }

    public record Preferences(
        String mode, 
        double speed, 
        String sound, 
        String size, 
        String effect, 
        String customSoundPath, 
        boolean hitEffectsEnabled,
        boolean useTarget // added
    ) {}

    public Preferences getPreferences() {
        String sql = "SELECT * FROM preferences LIMIT 1";
        try (Statement stmt = getConnection().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new Preferences(
                    rs.getString("mode"),
                    rs.getDouble("speed"),
                    rs.getString("sound"),
                    rs.getString("size"),
                    rs.getString("effect"),
                    rs.getString("custom_sound_path"),
                    rs.getInt("hit_effects_enabled") == 1,
                    rs.getInt("use_target") == 1 // int to boolean
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error fetching preferences", e);
        }
        return new Preferences("BOUNCE", 1.0, "CLASSIC", "MEDIUM", "EXPAND_CONTRACT", null, true, false);
    }

  
public void setPreferences(
    String mode, 
    double speed, 
    String sound, 
    String size, 
    String effect, 
    String customSoundPath,
    boolean hitEffectsEnabled,
    boolean useTarget 
) {
    // Use 'speed' column name
    String sql = "UPDATE preferences SET mode=?, speed=?, sound=?, size=?, effect=?, custom_sound_path=?, hit_effects_enabled=?, use_target=?";
    
    try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
        pstmt.setString(1, mode);
        pstmt.setDouble(2, speed);
        pstmt.setString(3, sound);
        pstmt.setString(4, size);
        pstmt.setString(5, effect);
        pstmt.setString(6, customSoundPath);
        pstmt.setInt(7, hitEffectsEnabled ? 1 : 0);
        pstmt.setInt(8, useTarget ? 1 : 0);

        int affected = pstmt.executeUpdate();
        
        if (affected == 0) {
            // Ensure column names match current schema (speed, use_target)
            String insertSql = "INSERT INTO preferences (mode, speed, sound, size, effect, custom_sound_path, hit_effects_enabled, use_target) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertPstmt = getConnection().prepareStatement(insertSql)) {
                insertPstmt.setString(1, mode);
                insertPstmt.setDouble(2, speed);
                insertPstmt.setString(3, sound);
                insertPstmt.setString(4, size);
                insertPstmt.setString(5, effect);
                insertPstmt.setString(6, customSoundPath);
                insertPstmt.setInt(7, hitEffectsEnabled ? 1 : 0);
                insertPstmt.setInt(8, useTarget ? 1 : 0);
                insertPstmt.executeUpdate();
            }
        }
    } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Error saving preferences", e);
    }
}



    public void saveGame(int score, int missclicks, double accuracy, int combo, double avgTime, String mode, String targetSize) {
        String sql = "INSERT INTO games (score, missclicks, accuracy, combo, avg_time, mode, target_size) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, score);
            pstmt.setInt(2, missclicks);
            pstmt.setDouble(3, accuracy);
            pstmt.setInt(4, combo);
            pstmt.setDouble(5, avgTime);
            pstmt.setString(6, mode);
            pstmt.setString(7, targetSize);
            pstmt.executeUpdate();
            updateBestScore(mode, score, accuracy, combo);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error saving game", e);
        }
    }

    private void updateBestScore(String mode, int score, double accuracy, int combo) {
        if (score > getBestScoreForMode(mode)) {
            String sql = "UPDATE best_scores SET best_score = ?, best_accuracy = ?, best_combo = ?, total_games = total_games + 1, last_updated = CURRENT_TIMESTAMP WHERE mode = ?";
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setInt(1, score);
                pstmt.setDouble(2, accuracy);
                pstmt.setInt(3, combo);
                pstmt.setString(4, mode);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error updating best score", e);
            }
        } else {
            String sql = "UPDATE best_scores SET total_games = total_games + 1 WHERE mode = ?";
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                pstmt.setString(1, mode);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error updating game count", e);
            }
        }
    }

    public int getBestScoreForMode(String mode) {
        String sql = "SELECT best_score FROM best_scores WHERE mode = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, mode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("best_score");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error fetching best score", e);
        }
        return 0;
    }

    public boolean deleteGame(long gameId) {
        String sql = "DELETE FROM games WHERE game_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, gameId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error deleting game", e);
            return false;
        }
    }
     
    public void clearHistory() {
  
    String sql = "DELETE FROM games";
    
    // sequenz zurücksetzen ( ids starten von 1)
    String resetSeq = "DELETE FROM sqlite_sequence WHERE name='games'"; 

        try (Statement stmt = getConnection().createStatement()) {
        stmt.execute(sql);
        stmt.execute(resetSeq); // Nur nötig, wenn game_id wieder bei 1 starten soll
        LOGGER.info("Verlauf erfolgreich gelöscht.");
    } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Fehler beim Löschen", e);
    }
}



}