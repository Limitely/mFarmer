package com.limitflow.mfarmer.database;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private synchronized void connect() {
        try {
            File file = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private synchronized void createTables() {
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS backpack_stats " +
                      "(uuid TEXT PRIMARY KEY, extra_slots INTEGER DEFAULT 0, total_earned REAL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS local_boosts " +
                      "(uuid TEXT PRIMARY KEY, multiplier REAL, end_time LONG)");
            s.execute("CREATE TABLE IF NOT EXISTS global_boost " +
                      "(id INTEGER PRIMARY KEY CHECK(id=1), multiplier REAL, end_time LONG)");
            try { s.execute("ALTER TABLE backpack_stats ADD COLUMN total_earned REAL DEFAULT 0"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE backpack_stats ADD COLUMN extra_slots INTEGER DEFAULT 0"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveExtraSlots(UUID uuid, int slots) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO backpack_stats (uuid, extra_slots) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET extra_slots = excluded.extra_slots")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, slots);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("saveExtraSlots: " + e.getMessage());
                }
            }
        });
    }

    public synchronized int loadExtraSlots(UUID uuid) {
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

    public void addEarned(UUID uuid, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO backpack_stats (uuid, extra_slots, total_earned) VALUES (?, 0, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET total_earned = total_earned + excluded.total_earned")) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("addEarned: " + e.getMessage());
                }
            }
        });
    }

    public synchronized List<Object[]> getTopEntries(int limit) {
        List<Object[]> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, total_earned FROM backpack_stats ORDER BY total_earned DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                        UUID.fromString(rs.getString("uuid")),
                        rs.getDouble("total_earned")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void saveLocalBoost(UUID uuid, double mult, long endTime) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO local_boosts (uuid, multiplier, end_time) VALUES (?, ?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, mult);
                    ps.setLong(3, endTime);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("saveLocalBoost: " + e.getMessage());
                }
            }
        });
    }

    public synchronized Map<UUID, Double[]> loadAllActiveBoosts() {
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

    public void cleanupOldBoosts() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM local_boosts WHERE end_time < ?")) {
                    ps.setLong(1, System.currentTimeMillis());
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) plugin.getLogger().info("Очистка БД: удалено " + deleted + " истекших бустов.");
                } catch (SQLException e) {
                    plugin.getLogger().severe("cleanupOldBoosts: " + e.getMessage());
                }
            }
        });
    }

    public void saveGlobalBoost(double multiplier, long endTime) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO global_boost (id, multiplier, end_time) VALUES (1, ?, ?)")) {
                    ps.setDouble(1, multiplier);
                    ps.setLong(2, endTime);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("saveGlobalBoost: " + e.getMessage());
                }
            }
        });
    }

    public synchronized double[] loadGlobalBoost() {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT multiplier, end_time FROM global_boost WHERE id = 1 AND end_time > ?")) {
            ps.setLong(1, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble("multiplier"), rs.getLong("end_time")};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
