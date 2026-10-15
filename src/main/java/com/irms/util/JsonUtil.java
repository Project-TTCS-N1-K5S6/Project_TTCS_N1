package com.irms.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Tiện ích chuyển đổi đối tượng Java sang JSON và gửi phản hồi AJAX
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls()
            .create();

    /**
     * Chức năng: Chuyển đổi đối tượng Java thành chuỗi JSON với định dạng ngày giờ chuẩn yyyy-MM-dd HH:mm:ss
     *
     * @param obj Đối tượng Java cần tuần tự hóa
     * @return Chuỗi định dạng JSON
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Chức năng: Giải tuần tự hóa chuỗi JSON thành đối tượng Java theo kiểu class chỉ định
     *
     * @param json     Chuỗi JSON đầu vào
     * @param classOfT Lớp đối tượng đích
     * @return Thực thể đối tượng Java
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * Chức năng: Thiết lập mã HTTP status, header application/json và gửi phản hồi JSON về Client
     *
     * @param response   Đối tượng HttpServletResponse
     * @param statusCode Mã trạng thái HTTP (200, 400, 404, 500...)
     * @param data       Đối tượng dữ liệu cần trả về dạng JSON
     */
    public static void sendJsonResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);
        PrintWriter out = response.getWriter();
        out.print(toJson(data));
        out.flush();
    }
}
