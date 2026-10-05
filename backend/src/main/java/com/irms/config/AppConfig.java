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
