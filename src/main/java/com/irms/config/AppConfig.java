package com.irms.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp cấu hình nạp các thuộc tính hệ thống từ application.properties và .env
 */
public class AppConfig {
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();
    private static final Map<String, String> envMap = new HashMap<>();

    static {
        // 1. Nạp application.properties từ classpath
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi tải cấu hình application.properties", e);
        }

        // 2. Tìm và nạp file .env từ thư mục gốc dự án
        loadDotEnv();
    }

    private static void loadDotEnv() {
        String[] potentialPaths = {
            ".env",
            "../.env",
            System.getProperty("user.dir") + File.separator + ".env"
        };
        for (String p : potentialPaths) {
            File f = new File(p);
            if (f.exists() && f.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int idx = line.indexOf('=');
                        if (idx > 0) {
                            String k = line.substring(0, idx).trim();
                            String v = line.substring(idx + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            envMap.put(k, v);
                        }
                    }
                    LOGGER.info("[AppConfig] Đã nạp thành công biến môi trường từ tệp .env tại: " + f.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Không thể đọc file .env tại " + p, e);
                }
            }
        }
    }

    public static String get(String key, String defaultValue) {
        String envKey = key.replace('.', '_').toUpperCase();

        // Ưu tiên 1: Tệp .env
        if (envMap.containsKey(envKey)) {
            return envMap.get(envKey);
        }
        if (envMap.containsKey(key)) {
            return envMap.get(key);
        }

        // Ưu tiên 2: Biến môi trường hệ thống
        String sysVal = System.getenv(envKey);
        if (sysVal != null && !sysVal.trim().isEmpty()) {
            return sysVal;
        }

        // Tra cứu alias cho cơ sở dữ liệu
        if ("db.url".equals(key)) {
            String host = get("DB_HOST", null);
            String port = get("DB_PORT", null);
            String db = get("DB_NAME", null);
            if (host != null && port != null && db != null) {
                if ("5432".equals(port)) {
                    return "jdbc:postgresql://" + host + ":" + port + "/" + db;
                } else {
                    return "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8";
                }
            }
        } else if ("db.driver".equals(key)) {
            String port = get("DB_PORT", null);
            String url = get("db.url", "");
            if ("5432".equals(port) || url.startsWith("jdbc:postgresql:")) {
                return "org.postgresql.Driver";
            }
        } else if ("db.user".equals(key)) {
            String user = get("DB_USER", null);
            if (user != null) return user;
        } else if ("db.password".equals(key)) {
            String pass = get("DB_PASSWORD", null);
            if (pass != null) return pass;
        } else if ("mail.smtp.host".equals(key)) {
            String alias = get("SMTP_HOST", null);
            if (alias != null) return alias;
        } else if ("mail.smtp.port".equals(key)) {
            String alias = get("SMTP_PORT", null);
            if (alias != null) return alias;
        } else if ("mail.smtp.username".equals(key)) {
            String alias = get("SMTP_USER", null);
            if (alias != null) return alias;
        } else if ("mail.smtp.password".equals(key)) {
            String alias = get("SMTP_PASSWORD", null);
            if (alias != null) return alias;
        } else if ("mail.from".equals(key)) {
            String alias = get("EMAIL_FROM", null);
            if (alias != null) return alias;
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
