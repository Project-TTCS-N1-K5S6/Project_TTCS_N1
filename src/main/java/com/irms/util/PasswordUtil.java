package com.irms.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích mã hóa và đối chiếu mật khẩu theo chuẩn bảo mật BCrypt
 */
public class PasswordUtil {
    private static final int BCRYPT_LOG_ROUNDS = 10;

    /**
     * Băm mật khẩu thô với Salt ngẫu nhiên
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * So sánh mật khẩu thô với chuỗi hash đã lưu trong DB
     */
    public static boolean check(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
