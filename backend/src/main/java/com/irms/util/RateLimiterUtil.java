package com.irms.util;

import com.irms.config.AppConfig;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ==============================================================================
 * TIỆN ÍCH GIỚI HẠN TẦN SUẤT & CHỐNG SPAM (RateLimiterUtil)
 * ==============================================================================
 * - Subtask KN-234: Giới hạn tần suất nộp hồ sơ theo địa chỉ IP/Client trong vòng 1 giờ.
 * - Sử dụng thuật toán Sliding Window (Cửa sổ trượt) theo thời gian thực (in-memory).
 * ==============================================================================
 */
public class RateLimiterUtil {

    // Khoảng thời gian cửa sổ tính toán (1 giờ = 3600 giây = 3,600,000 ms)
    public static final long ONE_HOUR_MS = 60L * 60L * 1000L;

    // Số lần nộp hồ sơ tối đa cho phép từ một địa chỉ IP trong 1 giờ (mặc định 5 lần)
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    // Bộ nhớ lưu trữ lịch sử nộp theo từng địa chỉ IP: IP -> Danh sách timestamps (ms)
    private static final ConcurrentHashMap<String, List<Long>> IP_REQUESTS = new ConcurrentHashMap<>();

    /**
     * Lấy cấu hình giới hạn số lần nộp từ file cấu hình hoặc biến môi trường
     */
    public static int getMaxAttemptsPerHour() {
        return AppConfig.getInt("rate.limit.apply.per_hour", DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Kiểm tra xem địa chỉ IP có được phép nộp hồ sơ tiếp hay không
     *
     * @param clientIp Địa chỉ IP của client
     * @return true nếu còn trong hạn mức; false nếu vượt quá tần suất
     */
    public static boolean isAllowed(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return true;
        }

        String ip = clientIp.trim();
        long now = System.currentTimeMillis();
        long windowStart = now - ONE_HOUR_MS;
        int maxAllowed = getMaxAttemptsPerHour();

        IP_REQUESTS.compute(ip, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            // Loại bỏ các mốc thời gian ngoài cửa sổ 1 giờ
            list.removeIf(timestamp -> timestamp < windowStart);
            return list;
        });

        List<Long> attempts = IP_REQUESTS.get(ip);
        return attempts == null || attempts.size() < maxAllowed;
    }

    /**
     * Ghi nhận một lần nộp hồ sơ thành công từ IP
     */
    public static void recordAttempt(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return;
        }
        String ip = clientIp.trim();
        long now = System.currentTimeMillis();
        long windowStart = now - ONE_HOUR_MS;

        IP_REQUESTS.compute(ip, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.removeIf(timestamp -> timestamp < windowStart);
            list.add(now);
            return list;
        });
    }

    /**
     * Lấy số lượt nộp hồ sơ còn lại của IP trong giờ hiện tại
     */
    public static int getRemainingAttempts(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return getMaxAttemptsPerHour();
        }
        String ip = clientIp.trim();
        long now = System.currentTimeMillis();
        long windowStart = now - ONE_HOUR_MS;

        List<Long> list = IP_REQUESTS.get(ip);
        if (list == null) {
            return getMaxAttemptsPerHour();
        }
        synchronized (list) {
            long count = list.stream().filter(t -> t >= windowStart).count();
            return (int) Math.max(0, getMaxAttemptsPerHour() - count);
        }
    }

    /**
     * Tính toán số giây cần chờ trước khi có thể nộp tiếp (Retry-After)
     */
    public static long getRetryAfterSeconds(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return 0;
        }
        String ip = clientIp.trim();
        long now = System.currentTimeMillis();
        long windowStart = now - ONE_HOUR_MS;

        List<Long> list = IP_REQUESTS.get(ip);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        synchronized (list) {
            Optional<Long> earliest = list.stream().filter(t -> t >= windowStart).min(Long::compareTo);
            if (!earliest.isPresent()) {
                return 0;
            }
            long expireTime = earliest.get() + ONE_HOUR_MS;
            return Math.max(1, (expireTime - now) / 1000L);
        }
    }

    /**
     * Trích xuất địa chỉ IP thực tế của Client từ Header hoặc RemoteAddr
     */
    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String[] headers = {
                "CF-Connecting-IP",    // Cloudflare
                "X-Forwarded-For",     // Standard Reverse Proxy
                "X-Real-IP",           // Nginx
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa danh sách IP ngăn cách bởi dấu phẩy: client, proxy1, proxy2...
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    /**
     * Xóa lịch sử của một địa chỉ IP (dùng cho Unit Test hoặc quản trị)
     */
    public static void reset(String clientIp) {
        if (clientIp != null) {
            IP_REQUESTS.remove(clientIp.trim());
        }
    }

    /**
     * Xóa toàn bộ lịch sử (dùng cho Unit Test)
     */
    public static void resetAll() {
        IP_REQUESTS.clear();
    }
}
