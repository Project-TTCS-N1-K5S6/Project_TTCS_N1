package com.irms.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * Filter thiết lập mã hóa ký tự UTF-8 cho toàn bộ request và response trong hệ thống
 */
@WebFilter(filterName = "EncodingFilter", urlPatterns = {"/*"})
public class EncodingFilter implements Filter {

    /**
     * Chức năng: Khởi tạo cấu hình cho EncodingFilter khi ứng dụng khởi chạy
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /**
     * Chức năng: Thiết lập bảng mã UTF-8 cho toàn bộ request và response, chống lỗi font chữ tiếng Việt
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }

    /**
     * Chức năng: Giải phóng tài nguyên khi bộ lọc EncodingFilter bị hủy
     */
    @Override
    public void destroy() {}
}
