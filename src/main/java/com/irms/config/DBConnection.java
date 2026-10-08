package com.irms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối Cơ sở dữ liệu (PostgreSQL / MySQL) sử dụng HikariCP Connection Pool
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static HikariDataSource dataSource;

    static {
        try {
            String driver = AppConfig.get("db.driver", "org.postgresql.Driver");
            Class.forName(driver);

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(AppConfig.get("db.url", "jdbc:postgresql://localhost:5432/myapp"));
            config.setUsername(AppConfig.get("db.user", "postgres"));
            config.setPassword(AppConfig.get("db.password", "180506"));

            config.setMinimumIdle(AppConfig.getInt("db.pool.minIdle", 5));
            config.setMaximumPoolSize(AppConfig.getInt("db.pool.maximumPoolSize", 20));
            config.setIdleTimeout(AppConfig.getInt("db.pool.idleTimeout", 30000));
            config.setConnectionTimeout(AppConfig.getInt("db.pool.connectionTimeout", 10000));
            config.setMaxLifetime(AppConfig.getInt("db.pool.maxLifetime", 1800000));

            // Cấu hình tối ưu nếu là MySQL
            if (driver.contains("mysql")) {
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
            }

            dataSource = new HikariDataSource(config);
            LOGGER.info("[DBConnection] Đã khởi tạo thành công HikariCP Connection Pool kết nối CSDL (" + driver + ").");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[DBConnection] Lỗi khởi tạo HikariCP, sẽ sử dụng DriverManager dự phòng", e);
        }
    }

    /**
     * Lấy một kết nối từ Connection Pool
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            return dataSource.getConnection();
        }
        // Dự phòng kết nối trực tiếp nếu pool gặp sự cố
        String url = AppConfig.get("db.url", "jdbc:postgresql://localhost:5432/myapp");
        String user = AppConfig.get("db.user", "postgres");
        String pass = AppConfig.get("db.password", "180506");
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Đóng Connection Pool khi ứng dụng tắt
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("[DBConnection] Đã đóng HikariCP Connection Pool.");
        }
    }
}
