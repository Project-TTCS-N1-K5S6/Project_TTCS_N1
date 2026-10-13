package com.irms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Database {
    private static volatile HikariDataSource dataSource;

    private Database() {
    }

    public static DataSource getDataSource() {
        HikariDataSource current = dataSource;
        if (current == null) {
            synchronized (Database.class) {
                current = dataSource;
                if (current == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(environmentOrDefault(
                        "DB_URL",
                        "jdbc:mysql://localhost:3306/ttcs_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
                    ));
                    config.setUsername(environmentOrDefault("DB_USER", "root"));
                    config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", ""));
                    config.setMaximumPoolSize(10);
                    config.setConnectionTimeout(10000);
                    config.setInitializationFailTimeout(-1);
                    config.setPoolName("recruitment-pool");
                    dataSource = new HikariDataSource(config);
                    current = dataSource;
                }
            }
        }
        return current;
    }

    public static void close() {
        HikariDataSource current = dataSource;
        if (current != null) {
            current.close();
            dataSource = null;
        }
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
