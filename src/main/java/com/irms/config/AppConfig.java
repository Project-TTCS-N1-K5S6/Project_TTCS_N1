package com.irms.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp cấu hình nạp các thuộc tính hệ thống từ application.properties
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                LOGGER.warning("Không tìm thấy tệp application.properties trên classpath, sử dụng cấu hình mặc định.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải cấu hình application.properties", e);
        }
    }

    public static String get(String key, String defaultValue) {
        String sysVal = System.getenv(key.replace('.', '_').toUpperCase());
        if (sysVal != null && !sysVal.trim().isEmpty()) {
            return sysVal;
        }

        // Hỗ trợ tra cứu các biến môi trường phổ biến từ .env
        if ("mail.smtp.host".equals(key)) {
            String alias = System.getenv("SMTP_HOST");
            if (alias != null && !alias.trim().isEmpty()) return alias;
        } else if ("mail.smtp.port".equals(key)) {
            String alias = System.getenv("SMTP_PORT");
            if (alias != null && !alias.trim().isEmpty()) return alias;
        } else if ("mail.smtp.username".equals(key)) {
            String alias = System.getenv("SMTP_USER");
            if (alias != null && !alias.trim().isEmpty()) return alias;
        } else if ("mail.smtp.password".equals(key)) {
            String alias = System.getenv("SMTP_PASSWORD");
            if (alias != null && !alias.trim().isEmpty()) return alias;
        } else if ("mail.from".equals(key)) {
            String alias = System.getenv("EMAIL_FROM");
            if (alias != null && !alias.trim().isEmpty()) return alias;
        }

        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
}
