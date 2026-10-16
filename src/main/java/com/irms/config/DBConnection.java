package com.irms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối Cơ sở dữ liệu MySQL sử dụng HikariCP Connection Pool
 */
public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static HikariDataSource dataSource;

    static {
        try {
            String driver = AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver");
            Class.forName(driver);

            HikariConfig config = new HikariConfig();
            config.setDriverClassName(driver);
            config.setJdbcUrl(AppConfig.get("db.url", "jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"));
            config.setUsername(AppConfig.get("db.user", "root"));
            config.setPassword(AppConfig.get("db.password", ""));

            config.setMinimumIdle(AppConfig.getInt("db.pool.minIdle", 5));
            config.setMaximumPoolSize(AppConfig.getInt("db.pool.maximumPoolSize", 20));
            config.setIdleTimeout(AppConfig.getInt("db.pool.idleTimeout", 30000));
            config.setConnectionTimeout(AppConfig.getInt("db.pool.connectionTimeout", 10000));
            config.setMaxLifetime(AppConfig.getInt("db.pool.maxLifetime", 1800000));

            // Tối ưu hóa hiệu năng kết nối MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");

            dataSource = new HikariDataSource(config);
            LOGGER.info("[DBConnection] Đã khởi tạo thành công HikariCP Connection Pool kết nối MySQL.");
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
        String url = AppConfig.get("db.url", "jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8");
        String user = AppConfig.get("db.user", "root");
        String pass = AppConfig.get("db.password", "");
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
