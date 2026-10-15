package com.irms.controller;

import com.irms.model.ApplicationLookupResult;
import com.irms.service.ApplicationLookupService;
import com.irms.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ==============================================================================
 * [KN-244] [KN-245] [KN-247] [KN-248] [KN-249]: Controller tra cứu trạng thái hồ sơ
 * ==============================================================================
 * Endpoint công khai - KHÔNG yêu cầu đăng nhập.
 * URL patterns:
 *   GET  /application-lookup          -> Hiển thị form tra cứu [KN-244]
 *   POST /application-lookup          -> Xử lý tra cứu, redirect về GET với kết quả
 *   GET  /application-lookup/api      -> API JSON trả kết quả tra cứu [KN-247]
 *
 * Bảo mật:
 *   - [KN-245]: Xác thực bằng cả id VÀ email (trong Service + DAO)
 *   - [KN-246]: Chỉ trả về thông tin tổng quát - KHÔNG tiết lộ nhận xét nội bộ,
 *               tên người phỏng vấn, lý do từ chối chi tiết
 *   - [KN-249]: Xử lý đầy đủ các edge case: mã không tồn tại, email sai, input rỗng
 * ==============================================================================
 */
@WebServlet(name = "ApplicationLookupServlet", urlPatterns = {
        "/application-lookup",
        "/application-lookup/api"
})
public class ApplicationLookupServlet extends HttpServlet {

    private final ApplicationLookupService lookupService = new ApplicationLookupService();

    /**
     * [KN-244] GET: Hiển thị form tra cứu hoặc kết quả sau redirect.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // [KN-247]: API JSON endpoint - dùng cho AJAX frontend
        if ("/application-lookup/api".equals(path)) {
            handleApiLookup(request, response);
            return;
        }

        // [KN-244] [KN-248] [KN-249]: Form page - hiển thị UI tra cứu
        // Lấy flash result nếu vừa redirect từ POST
        String lookupCode = request.getParameter("code");
        String errorCode  = request.getParameter("error");

        if (lookupCode != null && !lookupCode.trim().isEmpty()) {
            // Đã submit form - lấy kết quả từ session rồi xoá đi (PRG pattern)
            ApplicationLookupResult result =
                (ApplicationLookupResult) request.getSession().getAttribute("lookupResult_" + lookupCode);
            if (result != null) {
                request.getSession().removeAttribute("lookupResult_" + lookupCode);
                request.setAttribute("lookupResult", result);
                request.setAttribute("pageMode", "RESULT");
            } else {
                request.setAttribute("pageMode", "NOT_FOUND");
                request.setAttribute("lookupCode", lookupCode);
            }
        } else if (errorCode != null) {
            // [KN-249]: Hiển thị lỗi cụ thể
            request.setAttribute("pageMode", "ERROR");
            request.setAttribute("errorCode", errorCode);
        } else {
            // Form trống ban đầu
            request.setAttribute("pageMode", "FORM");
        }

        request.getRequestDispatcher("/WEB-INF/views/public/application-lookup.jsp")
               .forward(request, response);
    }

    /**
     * [KN-247] POST: Xử lý tra cứu, lưu kết quả vào session, redirect (PRG pattern).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String candidateId = request.getParameter("candidateId");
        String email       = request.getParameter("email");
        String contextPath = request.getContextPath();

        // [KN-249]: Validate đầu vào bắt buộc
        if (isBlank(candidateId) || isBlank(email)) {
            response.sendRedirect(contextPath + "/application-lookup?error=MISSING_FIELDS");
            return;
        }

        // [KN-245] [KN-247]: Gọi service - xác thực kép và tra cứu DB
        ApplicationLookupResult result = lookupService.lookup(candidateId, email);

        if (result == null) {
            // [KN-249]: Không tìm thấy hoặc email không khớp - trả về thông báo chung
            // (tránh tiết lộ thông tin: không phân biệt "mã không tồn tại" vs "email sai")
            response.sendRedirect(contextPath + "/application-lookup?error=NOT_FOUND");
            return;
        }

        // Lưu tạm kết quả vào session (xoá ngay sau khi đọc - PRG pattern)
        String sessionKey = "lookupResult_" + result.getCandidateId();
        request.getSession().setAttribute(sessionKey, result);
        response.sendRedirect(contextPath + "/application-lookup?code=" + result.getCandidateId());
    }

    /**
     * [KN-247]: API JSON - trả kết quả tra cứu dưới dạng JSON cho AJAX call.
     * [KN-246]: Chỉ trả các trường công khai, KHÔNG bao gồm rawStatus hay thông tin nội bộ.
     */
    private void handleApiLookup(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String candidateId = request.getParameter("id");
        String email       = request.getParameter("email");

        // [KN-249]: Validate
        if (isBlank(candidateId) || isBlank(email)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("errorCode", "MISSING_FIELDS");
            err.put("message", "Vui lòng nhập đầy đủ mã tra cứu và email.");
            JsonUtil.sendJsonResponse(response, 400, err);
            return;
        }

        // [KN-245] [KN-247]: Tra cứu
        ApplicationLookupResult result = lookupService.lookup(candidateId, email);

        if (result == null) {
            // [KN-249]: Không tìm thấy - thông báo chung, không tiết lộ nguyên nhân cụ thể
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("errorCode", "NOT_FOUND");
            err.put("message", "Không tìm thấy hồ sơ. Vui lòng kiểm tra lại mã tra cứu và email.");
            JsonUtil.sendJsonResponse(response, 404, err);
            return;
        }

        // [KN-243] [KN-246]: Chỉ trả về thông tin tổng quát - build response thủ công
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("candidateId", result.getCandidateId());
        data.put("trackingCode", result.getTrackingCode());
        data.put("displayCode", result.getDisplayCode());
        data.put("fullName", result.getFullName());
        data.put("positionTitle", result.getPositionTitle());
        // Trạng thái tổng quát - KHÔNG trả rawStatus
        data.put("publicStatus", result.getPublicStatus());
        data.put("publicStatusLabel", result.getPublicStatusLabel());
        data.put("publicStatusStep", result.getPublicStatusStep());
        data.put("isSuccessfulEnd", result.isSuccessfulEnd());
        // Thời gian chung
        data.put("submittedAt", result.getSubmittedAt() != null ? result.getSubmittedAt().toString() : null);
        data.put("lastUpdatedAt", result.getLastUpdatedAt() != null ? result.getLastUpdatedAt().toString() : null);

        JsonUtil.sendJsonResponse(response, 200, data);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
