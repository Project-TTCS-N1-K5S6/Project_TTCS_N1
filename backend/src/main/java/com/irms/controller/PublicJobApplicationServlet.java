package com.irms.controller;

import com.irms.dao.JobPostingDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.service.CVFileStorageService;
import com.irms.service.CandidateService;
import com.irms.service.EmailService;
import com.irms.util.CaptchaValidatorUtil;
import com.irms.util.JsonUtil;
import com.irms.util.RateLimiterUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * CONTROLLER TIẾP NHẬN HỒ SƠ ỨNG TUYỂN CÔNG KHAI KÈM CV
 * ==============================================================================
 * - KN-230: Nhận dữ liệu biểu mẫu: Họ tên, Email, SĐT, Nguồn, Thư giới thiệu, File CV
 * - KN-231: Từ chối nộp trùng cùng vị trí với cùng email (HTTP 409 Conflict)
 * - KN-232: Kiểm tra MIME type, đuôi .pdf/.docx, dung lượng <= 10MB, lưu trữ an toàn
 * - KN-233: API endpoint POST /api/v1/public/jobs/apply, sinh Tracking Code duy nhất
 * - KN-234: Chống spam và giới hạn tần suất nộp theo IP trong vòng 1 giờ
 * - KN-235: Gửi email xác nhận kèm mã tra cứu bất đồng bộ (Non-blocking)
 * - KN-236: Kiểm tra token xác thực người dùng / CAPTCHA chống spam
 * ==============================================================================
 */
@WebServlet(name = "PublicJobApplicationServlet", urlPatterns = {
        "/api/v1/public/jobs/apply",
        "/api/public/jobs/apply",
        "/careers/apply"
})
@MultipartConfig(
        maxFileSize = 10485760L,        // 10MB
        maxRequestSize = 20971520L,     // 20MB
        fileSizeThreshold = 1048576     // 1MB
)
public class PublicJobApplicationServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PublicJobApplicationServlet.class.getName());

    private final CandidateService candidateService = new CandidateService();
    private final CVFileStorageService cvStorageService = new CVFileStorageService();
    private final JobPostingDAO jobPostingDAO = new JobPostingDAO();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Hỗ trợ tra cứu nhanh trạng thái hồ sơ theo Tracking Code
        String trackingCode = request.getParameter("trackingCode");
        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            trackingCode = request.getParameter("code");
        }

        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Vui lòng cung cấp mã tra cứu hồ sơ (trackingCode)."));
            return;
        }

        Candidate c = candidateService.getCandidateByTrackingCode(trackingCode.trim());
        if (c == null) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    Map.of("success", false, "error", "Không tìm thấy hồ sơ ứng tuyển với mã tra cứu: " + trackingCode));
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("trackingCode", c.getTrackingCode());
        data.put("fullName", c.getFullName());
        data.put("positionTitle", c.getRequisitionTitle() != null ? c.getRequisitionTitle() : "Vị trí tuyển dụng");
        data.put("departmentName", c.getDepartmentName() != null ? c.getDepartmentName() : "Toàn công ty");
        data.put("status", c.getStatus());
        data.put("statusLabel", c.getStatusLabel());
        data.put("submittedAt", c.getCreatedAt());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("data", data);
        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String clientIp = RateLimiterUtil.resolveClientIp(request);

        // ======================================================================
        // 1. [KN-234]: KIỂM TRA GIỚI HẠN TẦN SUẤT THEO ĐỊA CHỈ IP (RATE LIMIT)
        // ======================================================================
        if (!RateLimiterUtil.isAllowed(clientIp)) {
            long retryAfter = RateLimiterUtil.getRetryAfterSeconds(clientIp);
            LOGGER.warning("[ApplicationAPI] IP " + clientIp + " bị chặn do vượt quá giới hạn nộp hồ sơ/giờ.");
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", "Bạn đã nộp hồ sơ quá số lần cho phép từ địa chỉ mạng này trong vòng 1 giờ (tối đa "
                    + RateLimiterUtil.getMaxAttemptsPerHour() + " lần/giờ). Vui lòng thử lại sau " + retryAfter + " giây.");
            err.put("retryAfterSeconds", retryAfter);
            JsonUtil.sendJsonResponse(response, 429, err);
            return;
        }

        // ======================================================================
        // 2. [KN-236]: XÁC THỰC CHỐNG SPAM & NGƯỜI MÁY (CAPTCHA / TURNSTILE)
        // ======================================================================
        String captchaToken = request.getParameter("captchaToken");
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            captchaToken = request.getParameter("g-recaptcha-response");
        }
        if (captchaToken == null || captchaToken.trim().isEmpty()) {
            captchaToken = request.getParameter("cf-turnstile-response");
        }

        if (!CaptchaValidatorUtil.verifyToken(captchaToken, clientIp)) {
            LOGGER.warning("[ApplicationAPI] Xác thực CAPTCHA thất bại từ IP: " + clientIp);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Xác thực chống spam không thành công. Vui lòng xác nhận bạn không phải người máy."));
            return;
        }

        // ======================================================================
        // 3. [KN-230]: TRÍCH XUẤT VÀ VALIDATE CÁC TRƯỜNG THÔNG TIN BIỂU MẪU
        // ======================================================================
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String sourceId = request.getParameter("applicationSourceId");
        String coverLetter = request.getParameter("coverLetter");
        String requisitionId = request.getParameter("requisitionId");
        String jobId = request.getParameter("jobId");

        // Validate Họ và tên
        if (fullName == null || fullName.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Vui lòng nhập đầy đủ họ và tên ứng viên."));
            return;
        }
        fullName = fullName.trim();

        // Validate Email (KN-230: client & server validation)
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Địa chỉ email không đúng định dạng hợp lệ (ví dụ: ungvien@domain.com)."));
            return;
        }
        email = email.trim().toLowerCase();

        // Validate Số điện thoại (KN-230: Định dạng số điện thoại Việt Nam: 10 số bắt đầu bằng 0)
        String cleanPhone = (phone != null) ? phone.trim().replaceAll("[\\s.-]", "") : "";
        if (!cleanPhone.matches("^0[3|5|7|8|9][0-9]{8}$")) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Số điện thoại không hợp lệ. Vui lòng nhập số di động Việt Nam gồm 10 chữ số (bắt đầu bằng 03, 05, 07, 08, 09)."));
            return;
        }

        // ======================================================================
        // 4. XÁC ĐỊNH VÀ KIỂM TRA TỒN TẠI VỊ TRÍ TUYỂN DỤNG
        // ======================================================================
        JobPosting posting = null;
        RecruitmentRequest req = null;

        if (requisitionId != null && !requisitionId.trim().isEmpty()) {
            req = requisitionDAO.findById(requisitionId.trim());
            posting = jobPostingDAO.findByRequisitionId(requisitionId.trim());
        } else if (jobId != null && !jobId.trim().isEmpty()) {
            posting = jobPostingDAO.findById(jobId.trim());
            if (posting != null && posting.getRequisitionId() != null) {
                req = requisitionDAO.findById(posting.getRequisitionId());
                requisitionId = posting.getRequisitionId();
            }
        }

        if (posting == null && req != null) {
            posting = jobPostingDAO.findByRequisitionId(req.getId());
        }

        if (req == null && posting == null) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    Map.of("success", false, "error", "Vị trí tuyển dụng không tồn tại hoặc đã bị gỡ bỏ."));
            return;
        }

        String targetReqId = (requisitionId != null && !requisitionId.trim().isEmpty())
                ? requisitionId.trim()
                : (posting != null ? posting.getRequisitionId() : null);

        String positionTitle = (posting != null && posting.getTitle() != null)
                ? posting.getTitle()
                : (req != null ? req.getPositionTitle() : "Vị trí tuyển dụng");

        String departmentName = (posting != null && posting.getDepartmentName() != null)
                ? posting.getDepartmentName()
                : (req != null ? req.getDepartmentName() : "Toàn công ty");

        // 4.1. Kiểm tra trạng thái phê duyệt
        if (posting != null && !posting.isApproved()) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    Map.of("success", false, "error", "Vị trí tuyển dụng này chưa được phê duyệt để tiếp nhận hồ sơ."));
            return;
        }
        if (req != null && !"APPROVED".equalsIgnoreCase(req.getStatus()) && !"OPEN".equalsIgnoreCase(req.getStatus())) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    Map.of("success", false, "error", "Yêu cầu tuyển dụng chưa ở trạng thái hợp lệ để tiếp nhận hồ sơ."));
            return;
        }

        // 4.2. Kiểm tra trạng thái xuất bản & đóng tin tuyển dụng
        if (posting != null) {
            String pStatus = posting.getStatus() != null ? posting.getStatus().trim().toUpperCase() : "DRAFT";
            if ("CLOSED".equals(pStatus)) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("success", false, "error", "Vị trí tuyển dụng này đã đóng. Hệ thống đã ngừng tiếp nhận hồ sơ ứng tuyển mới."));
                return;
            }
            if (!"PUBLISHED".equals(pStatus)) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        Map.of("success", false, "error", "Vị trí tuyển dụng hiện chưa xuất bản hoặc đã tạm dừng nhận hồ sơ."));
                return;
            }
        }

        // 4.3. Kiểm tra hạn nộp hồ sơ ứng tuyển
        boolean isExpired = false;
        String deadlineStr = "--";
        if (posting != null) {
            deadlineStr = posting.getFormattedDeadline();
            if (posting.isExpired()) {
                isExpired = true;
            } else if (posting.getApplicationDeadline() != null) {
                long deadlineEndMs = posting.getApplicationDeadline().getTime() + 86399000L;
                if (deadlineEndMs < System.currentTimeMillis()) {
                    isExpired = true;
                }
            }
        }
        if (!isExpired && req != null && req.getDeadline() != null) {
            if ("--".equals(deadlineStr) || deadlineStr == null) {
                deadlineStr = new SimpleDateFormat("dd/MM/yyyy").format(req.getDeadline());
            }
            long deadlineEndMs = req.getDeadline().getTime() + 86399000L;
            if (deadlineEndMs < System.currentTimeMillis()) {
                isExpired = true;
            }
        }
        if (isExpired) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Vị trí tuyển dụng này đã hết hạn nhận hồ sơ ứng tuyển (Hạn nộp: " + deadlineStr + "). Không thể tiếp nhận thêm hồ sơ mới."));
            return;
        }

        // ======================================================================
        // 5. [KN-231]: TỪ CHỐI NỘP TRÙNG CÙNG VỊ TRÍ VỚI CÙNG EMAIL
        // ======================================================================
        if (candidateService.hasAlreadyApplied(email, targetReqId)) {
            LOGGER.warning("[ApplicationAPI] Từ chối hồ sơ trùng lặp: " + email + " cho vị trí: " + targetReqId);
            Map<String, Object> conflictRes = new LinkedHashMap<>();
            conflictRes.put("success", false);
            conflictRes.put("error", "Bạn đã nộp hồ sơ ứng tuyển vào vị trí này trước đó với email: " + email
                    + ". Hệ thống không cho phép nộp lại nhiều lần cho cùng một vị trí.");
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_CONFLICT, conflictRes);
            return;
        }

        // ======================================================================
        // 6. [KN-232]: KIỂM TRA ĐỊNH DẠNG & DUNG LƯỢNG TỆP CV ĐÍNH KÈM
        // ======================================================================
        Part cvPart = null;
        try {
            cvPart = request.getPart("cvFile");
            if (cvPart == null) {
                cvPart = request.getPart("cv");
            }
            if (cvPart == null) {
                cvPart = request.getPart("file");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Lỗi đọc tệp CV từ request multipart", ex);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", "Dung lượng tệp CV vượt quá giới hạn tối đa cho phép là 10MB!"));
            return;
        }

        CVFileStorageService.ValidationResult valResult = cvStorageService.validate(cvPart);
        if (!valResult.isValid()) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("success", false, "error", valResult.getErrorMessage()));
            return;
        }

        // ======================================================================
        // 7. [KN-233]: SINH MÃ TRA CỨU HỒ SƠ DUY NHẤT & LƯU TỆP CV AN TOÀN
        // ======================================================================
        String trackingCode = candidateService.generateUniqueTrackingCode();
        String originalFileName = CVFileStorageService.extractFileName(cvPart);

        String uploadRoot = getServletContext().getRealPath("");
        if (uploadRoot == null || uploadRoot.trim().isEmpty()) {
            uploadRoot = System.getProperty("user.dir");
        }

        String cvUrl;
        try {
            cvUrl = cvStorageService.storeCV(cvPart, uploadRoot, trackingCode);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi lưu trữ tệp CV trên server", ex);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("success", false, "error", "Không thể lưu trữ tệp CV vào hệ thống. Vui lòng thử lại sau."));
            return;
        }

        // ======================================================================
        // 8. [KN-233]: TẠO BẢN GHI ỨNG VIÊN TRONG CƠ SỞ DỮ LIỆU
        // ======================================================================
        Candidate candidate = new Candidate();
        candidate.setRequisitionId(targetReqId);
        candidate.setFullName(fullName);
        candidate.setEmail(email);
        candidate.setPhone(cleanPhone);
        candidate.setApplicationSourceId((sourceId != null && !sourceId.trim().isEmpty()) ? sourceId.trim() : "cat-src-career-site");
        candidate.setCoverLetter(coverLetter != null ? coverLetter.trim() : "");
        candidate.setNotes(coverLetter != null && !coverLetter.trim().isEmpty() ? "Thư giới thiệu: " + coverLetter.trim() : "Nộp qua Careers Portal");
        candidate.setCvUrl(cvUrl);
        candidate.setTrackingCode(trackingCode);
        candidate.setIpAddress(clientIp);

        try {
            candidate = candidateService.applyPublicJob(candidate);
        } catch (IllegalStateException e) {
            // Trường hợp phát hiện trùng lặp
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_CONFLICT,
                    Map.of("success", false, "error", e.getMessage()));
            return;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi lưu hồ sơ ứng tuyển vào DB", e);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("success", false, "error", "Có lỗi xảy ra khi lưu hồ sơ. Vui lòng thử lại sau: " + e.getMessage()));
            return;
        }

        // Ghi nhận lượt nộp thành công cho IP vào bộ giới hạn tần suất (KN-234)
        RateLimiterUtil.recordAttempt(clientIp);

        // ======================================================================
        // 9. [KN-235]: GỬI EMAIL XÁC NHẬN BẤT ĐỒNG BỘ (NON-BLOCKING)
        // ======================================================================
        String submittedAtStr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        EmailService.sendApplicationConfirmationAsync(
                email,
                fullName,
                positionTitle,
                departmentName,
                trackingCode,
                submittedAtStr,
                originalFileName
        );

        // ======================================================================
        // 10. TRẢ VỀ PHẢN HỒI THÀNH CÔNG VỚI MÃ TRA CỨU HỒ SƠ
        // ======================================================================
        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("success", true);
        responseData.put("message", "Nộp hồ sơ ứng tuyển thành công!");
        responseData.put("trackingCode", trackingCode);
        responseData.put("candidateName", fullName);
        responseData.put("email", email);
        responseData.put("positionTitle", positionTitle);
        responseData.put("departmentName", departmentName);
        responseData.put("submittedAt", submittedAtStr);
        responseData.put("cvUrl", cvUrl);

        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
}
