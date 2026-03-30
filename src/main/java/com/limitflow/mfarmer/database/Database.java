package com.limitflow.mfarmer.database;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Database {

    private final MFarmer plugin;
    private Connection connection;

    public Database(MFarmer plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        try {
            File file = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS backpack_stats (uuid TEXT PRIMARY KEY, extra_slots INTEGER)");
            s.execute("CREATE TABLE IF NOT EXISTS local_boosts (uuid TEXT PRIMARY KEY, multiplier REAL, end_time LONG)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveExtraSlots(UUID uuid, int slots) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO backpack_stats (uuid, extra_slots) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, slots);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка: " + e.getMessage());
            }
        });
    }

    public int loadExtraSlots(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT extra_slots FROM backpack_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("extra_slots");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void saveLocalBoost(UUID uuid, double mult, long endTime) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO local_boosts (uuid, multiplier, end_time) VALUES (?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, mult);
                ps.setLong(3, endTime);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка: " + e.getMessage());
            }
        });
    }

    public void cleanupOldBoosts() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM local_boosts WHERE end_time < ?")) {
                ps.setLong(1, System.currentTimeMillis());
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Очистка базы: удалено " + deleted + " истекших бустов.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Ошибка: " + e.getMessage());
            }
        });
    }

    public Map<UUID, Double[]> loadAllActiveBoosts() {
        Map<UUID, Double[]> boosts = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, multiplier, end_time FROM local_boosts WHERE end_time > ?")) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boosts.put(
                            UUID.fromString(rs.getString("uuid")),
                            new Double[]{rs.getDouble("multiplier"), (double) rs.getLong("end_time")}
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return boosts;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
