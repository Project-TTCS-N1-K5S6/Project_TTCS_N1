package com.irms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DBConnection {
    private static HikariDataSource dataSource;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            String jdbcUrl = requiredEnvironment("IRMS_DB_URL");
            String username = requiredEnvironment("IRMS_DB_USER");
            String password = requiredEnvironment("IRMS_DB_PASSWORD");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setPoolName("IRMS-Candidate-Pool");
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Required environment variable is not set: " + name);
        }
        return value;
    }
}
