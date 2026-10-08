package com.irms.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller hiển thị giao diện Placeholder cho các phân hệ theo lộ trình mở rộng
 */
@WebServlet(name = "PlaceholderServlet", urlPatterns = {
        "/job-postings/*",
        "/interviews/*",
        "/offers/*",
        "/onboarding/*",
        "/notifications/*",
        "/reports/*"
})
public class PlaceholderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String moduleName = "Phân hệ nghiệp vụ";

        if (uri.contains("recruitment-requests")) moduleName = "Quản lý Yêu cầu tuyển dụng";
        else if (uri.contains("job-postings")) moduleName = "Quản lý Tin tuyển dụng";
        else if (uri.contains("interviews")) moduleName = "Điều phối Lịch phỏng vấn & Đánh giá";
        else if (uri.contains("offers")) moduleName = "Thư mời nhận việc (Offer Letter)";
        else if (uri.contains("onboarding")) moduleName = "Quy trình Tiếp nhận nhân sự (Onboarding)";
        else if (uri.contains("notifications")) moduleName = "Hộp thư Email & Thông báo tự động";
        else if (uri.contains("reports")) moduleName = "Báo cáo thống kê hiệu quả tuyển dụng";

        request.setAttribute("moduleName", moduleName);
        request.setAttribute("requestUri", uri);
        request.getRequestDispatcher("/WEB-INF/views/placeholder/index.jsp").forward(request, response);
    }
}
