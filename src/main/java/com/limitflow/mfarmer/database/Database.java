package com.limitflow.mfarmer.database;

import com.limitflow.mfarmer.MFarmer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Database {

    private final MFarmer plugin;
    private HikariDataSource dataSource;
    private boolean isMySQL = false;
    private final ExecutorService async = Executors.newFixedThreadPool(2);

    public Database(MFarmer plugin) {
        this.plugin = plugin;
        setupPool();
        createTables();
    }

    private void setupPool() {
        var cfg = plugin.getConfig();
        String type = cfg.getString("database.type", "sqlite").toLowerCase();
        HikariConfig config = new HikariConfig();

        if (type.equals("mysql")) {
            isMySQL = true;
            config.setJdbcUrl("jdbc:mysql://" +
                    cfg.getString("database.host", "localhost") + ":" +
                    cfg.getInt("database.port", 3306) + "/" +
                    cfg.getString("database.name", "mfarmer") +
                    "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
            config.setUsername(cfg.getString("database.user", "root"));
            config.setPassword(cfg.getString("database.password", ""));
            config.setMaximumPoolSize(10);
        } else {
            File file = new File(plugin.getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.setMaximumPoolSize(1);
        }

        config.setConnectionTimeout(10000);
        config.setPoolName("mFarmerPool");
        dataSource = new HikariDataSource(config);
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS backpack_stats (
                    uuid VARCHAR(36) PRIMARY KEY,
                    extra_slots INT DEFAULT 0,
                    total_earned DOUBLE DEFAULT 0
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS local_boosts (
                    uuid VARCHAR(36) PRIMARY KEY,
                    multiplier DOUBLE DEFAULT 1.0,
                    end_time BIGINT DEFAULT 0
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS global_boost (
                    id INT PRIMARY KEY,
                    multiplier DOUBLE DEFAULT 1.0,
                    end_time BIGINT DEFAULT 0
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Table error: " + e.getMessage());
        }
    }

    public void cleanupOldBoosts() {
        async.submit(() -> {
            long now = System.currentTimeMillis();
            try (Connection c = getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM local_boosts WHERE end_time > 0 AND end_time < ?")) {
                    ps.setLong(1, now);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE global_boost SET multiplier=1.0, end_time=0 WHERE end_time > 0 AND end_time < ?")) {
                    ps.setLong(1, now);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("cleanupOldBoosts: " + e.getMessage());
            }
        });
    }

    public int loadExtraSlots(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT extra_slots FROM backpack_stats WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("loadExtraSlots: " + e.getMessage());
        }
        return 0;
    }

    public void saveExtraSlots(UUID uuid, int slots) {
        async.submit(() -> saveExtraSlotsSync(uuid, slots));
    }

    public void saveExtraSlotsSync(UUID uuid, int slots) {
        String sql = isMySQL
                ? "INSERT INTO backpack_stats (uuid, extra_slots, total_earned) VALUES (?, ?, 0) ON DUPLICATE KEY UPDATE extra_slots=?"
                : "INSERT INTO backpack_stats (uuid, extra_slots, total_earned) VALUES (?, ?, 0) ON CONFLICT(uuid) DO UPDATE SET extra_slots=excluded.extra_slots";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, slots);
            if (isMySQL) ps.setInt(3, slots);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("saveExtraSlots: " + e.getMessage());
        }
    }

    public void addEarned(UUID uuid, double amount) {
        String sql = isMySQL
                ? "INSERT INTO backpack_stats (uuid, extra_slots, total_earned) VALUES (?, 0, ?) ON DUPLICATE KEY UPDATE total_earned=total_earned+?"
                : "INSERT INTO backpack_stats (uuid, extra_slots, total_earned) VALUES (?, 0, ?) ON CONFLICT(uuid) DO UPDATE SET total_earned=total_earned+excluded.total_earned";
        async.submit(() -> {
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, amount);
                if (isMySQL) ps.setDouble(3, amount);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("addEarned: " + e.getMessage());
            }
        });
    }

    public double[] loadGlobalBoost() {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT multiplier, end_time FROM global_boost WHERE id=1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble(1), rs.getDouble(2)};
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("loadGlobalBoost: " + e.getMessage());
        }
        return null;
    }

    public void saveGlobalBoost(double multiplier, long endTime) {
        String sql = isMySQL
                ? "INSERT INTO global_boost (id, multiplier, end_time) VALUES (1, ?, ?) ON DUPLICATE KEY UPDATE multiplier=?, end_time=?"
                : "INSERT INTO global_boost (id, multiplier, end_time) VALUES (1, ?, ?) ON CONFLICT(id) DO UPDATE SET multiplier=excluded.multiplier, end_time=excluded.end_time";
        async.submit(() -> {
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setDouble(1, multiplier);
                ps.setLong(2, endTime);
                if (isMySQL) {
                    ps.setDouble(3, multiplier);
                    ps.setLong(4, endTime);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("saveGlobalBoost: " + e.getMessage());
            }
        });
    }

    public double[] loadLocalBoost(UUID uuid) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT multiplier, end_time FROM local_boosts WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble(1), rs.getDouble(2)};
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("loadLocalBoost: " + e.getMessage());
        }
        return null;
    }

    public void saveLocalBoost(UUID uuid, double multiplier, long endTime) {
        String sql = isMySQL
                ? "INSERT INTO local_boosts (uuid, multiplier, end_time) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE multiplier=?, end_time=?"
                : "INSERT INTO local_boosts (uuid, multiplier, end_time) VALUES (?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET multiplier=excluded.multiplier, end_time=excluded.end_time";
        async.submit(() -> {
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setDouble(2, multiplier);
                ps.setLong(3, endTime);
                if (isMySQL) {
                    ps.setDouble(4, multiplier);
                    ps.setLong(5, endTime);
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("saveLocalBoost: " + e.getMessage());
            }
        });
    }

    public List<TopEntry> getTopEntries(int limit) {
        List<TopEntry> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT uuid, total_earned FROM backpack_stats ORDER BY total_earned DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TopEntry(UUID.fromString(rs.getString(1)), rs.getDouble(2)));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("getTopEntries: " + e.getMessage());
        }
        return list;
    }

    public void close() {
        async.shutdown();
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public record TopEntry(UUID uuid, double earned) {}
}
