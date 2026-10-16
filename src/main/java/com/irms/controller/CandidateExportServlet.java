package com.irms.controller;

import com.irms.dao.CandidateDAO;
import com.irms.service.CandidateService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servlet xuất danh sách ứng viên ra file Excel (.xlsx).
 *
 * <p>Endpoint: GET /api/candidates/export
 *
 * <p>Tham số query (tất cả đều tuỳ chọn):
 * <ul>
 *   <li>{@code position}  – lọc theo vị trí ứng tuyển (chứa chuỗi)</li>
 *   <li>{@code source}    – lọc theo nguồn ứng viên (khớp chính xác)</li>
 *   <li>{@code fromDate}  – ngày bắt đầu, định dạng YYYY-MM-DD</li>
 *   <li>{@code toDate}    – ngày kết thúc, định dạng YYYY-MM-DD</li>
 * </ul>
 *
 * <p>Tệp trả về không chứa các trường nhạy cảm nội bộ:
 * id, email_normalized, phone_normalized, cv_storage_name, cv_content_type.
 *
 * <p>Tên tệp có dạng: {@code candidates_<mãVịTrí>_<ngàyXuất>.xlsx}
 */
@WebServlet("/api/candidates/export")
public class CandidateExportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private CandidateService candidateService;

    @Override
    public void init() throws ServletException {
        candidateService = new CandidateService(new CandidateDAO());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");

        String position = emptyToNull(request.getParameter("position"));
        String source   = emptyToNull(request.getParameter("source"));
        String fromDate = emptyToNull(request.getParameter("fromDate"));
        String toDate   = emptyToNull(request.getParameter("toDate"));

        // Validate định dạng ngày đơn giản (YYYY-MM-DD) để ngăn injection
        if (!isValidDateOrNull(fromDate) || !isValidDateOrNull(toDate)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Ngày tháng không đúng định dạng YYYY-MM-DD.");
            return;
        }

        String fileName = CandidateService.buildExportFileName(position);
        // RFC 5987 encoding cho tên tệp chứa tiếng Việt
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20");

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedName);
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");

        try {
            candidateService.exportToExcel(position, source, fromDate, toDate,
                    response.getOutputStream());
        } catch (Exception exception) {
            getServletContext().log("Failed to export candidates to Excel.", exception);
            // Nếu chưa commit response, gửi lỗi 500
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Không thể xuất danh sách ứng viên. Vui lòng thử lại.");
            }
        }
    }

    /** Trả về null nếu chuỗi null hoặc chỉ chứa khoảng trắng. */
    private static String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Kiểm tra chuỗi ngày có dạng YYYY-MM-DD (hoặc null).
     * Chỉ kiểm tra cú pháp, không kiểm tra tính hợp lệ của ngày tháng.
     */
    private static boolean isValidDateOrNull(String date) {
        return date == null || date.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
