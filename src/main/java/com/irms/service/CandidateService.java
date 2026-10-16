package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.CVExtractionResult;
import com.irms.model.Candidate;
import com.irms.model.CandidateWithdrawal;
import com.irms.model.SharedCatalogType;
import com.irms.util.SecurityUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ Hồ sơ Ứng viên & Pipeline tuyển dụng
 */
public class CandidateService {
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final SharedCatalogDAO catalogDAO = new SharedCatalogDAO();
    private static final List<String> CANDIDATE_STATUSES =
            Arrays.asList("APPLIED", "SCREENING", "INTERVIEWING", "OFFER", "HIRED", "REJECTED");
    private static final String INTERNAL_REFERRAL_SOURCE_ID = "cat-src-referral";

    public List<Candidate> getCandidates(String search, String status) {
        return candidateDAO.findAll(search, status, null);
    }

    /**
     * [US 5]: Lấy danh sách ứng viên có kiểm tra quyền hạn của Recruiter
     */
    public List<Candidate> getCandidates(String search, String status, String recruiterId) {
        return candidateDAO.findAll(search, status, recruiterId);
    }

    public List<Candidate> getCandidates(String search, String status, String recruiterId, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return candidateDAO.findAll(search, status, recruiterId, offset, size);
    }

    public int countCandidates(String search, String status, String recruiterId) {
        return candidateDAO.countAll(search, status, recruiterId);
    }

    public List<Candidate> getReferralsByUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin người giới thiệu.");
        }
        return candidateDAO.findReferralsByUser(userId.trim());
    }

    public boolean createReferral(Candidate candidate, String referrerId) {
        if (candidate == null) {
            throw new IllegalArgumentException("Thông tin hồ sơ giới thiệu không được để trống.");
        }
        if (referrerId == null || referrerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin người giới thiệu.");
        }
        if (candidate.getFullName() == null || candidate.getFullName().trim().isEmpty()
                || candidate.getFullName().trim().length() > 255) {
            throw new IllegalArgumentException("Họ và tên ứng viên là bắt buộc và không được vượt quá 255 ký tự.");
        }
        String email = candidate.getEmail();
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                || email.trim().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ email ứng viên không hợp lệ.");
        }
        if (candidate.getNotes() == null || candidate.getNotes().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập ghi chú giới thiệu.");
        }
        if (candidate.getNotes().trim().length() > 5000) {
            throw new IllegalArgumentException("Ghi chú giới thiệu không được vượt quá 5000 ký tự.");
        }
        if (candidate.getPhone() != null && candidate.getPhone().trim().length() > 50) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 50 ký tự.");
        }
        validateReferralCvUrl(candidate.getCvUrl());

        candidate.setFullName(candidate.getFullName().trim());
        candidate.setEmail(email.trim().toLowerCase(Locale.ROOT));
        candidate.setPhone(candidate.getPhone() == null || candidate.getPhone().trim().isEmpty()
                ? null : candidate.getPhone().trim());
        candidate.setNotes(candidate.getNotes().trim());
        candidate.setCvUrl(candidate.getCvUrl() == null || candidate.getCvUrl().trim().isEmpty()
                ? null : candidate.getCvUrl().trim());
        candidate.setRequisitionId(candidate.getRequisitionId() == null || candidate.getRequisitionId().trim().isEmpty()
                ? null : candidate.getRequisitionId().trim());
        candidate.setApplicationSourceId(INTERNAL_REFERRAL_SOURCE_ID);
        candidate.setReferredByUserId(referrerId.trim());
        candidate.setStatus("APPLIED");

        if (!catalogDAO.isValueInType(INTERNAL_REFERRAL_SOURCE_ID, SharedCatalogType.APPLICATION_SOURCE)) {
            throw new IllegalStateException("Danh mục nguồn giới thiệu nội bộ chưa được cấu hình.");
        }

        candidate.setId(SecurityUtil.generateUUID());
        return candidateDAO.insert(candidate);
    }

    private void validateReferralCvUrl(String cvUrl) {
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            return;
        }
        if (cvUrl.trim().length() > 2048) {
            throw new IllegalArgumentException("Đường dẫn CV không được vượt quá 2048 ký tự.");
        }
        try {
            URI uri = new URI(cvUrl.trim());
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Đường dẫn CV phải là URL bắt đầu bằng http:// hoặc https://.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Đường dẫn CV không hợp lệ.", e);
        }
    }

    public boolean createCandidate(Candidate c) {
        if (!catalogDAO.isValueInType(c.getApplicationSourceId(), SharedCatalogType.APPLICATION_SOURCE)) {
            throw new IllegalArgumentException("Nguồn ứng viên không hợp lệ.");
        }
        c.setStatus(normalizeStatus(c.getStatus()));
        if ("REJECTED".equals(c.getStatus())) {
            validateRejectionReason(c.getRejectionReasonId());
        } else {
            c.setRejectionReasonId(null);
        }
        if (c.getRejectionReasonId() != null
                && !catalogDAO.isValueInType(c.getRejectionReasonId(), SharedCatalogType.REJECTION_REASON)) {
            throw new IllegalArgumentException("Lý do loại hồ sơ không hợp lệ.");
        }
        c.setId(SecurityUtil.generateUUID());
        boolean ok = candidateDAO.insert(c);
        if (ok) {
            try {
                new CandidateDuplicateService().detectAndRecordDuplicates(c, null);
            } catch (Exception ignored) {}
        }
        return ok;
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        return updateStatus(candidateId, newStatus, null);
    }

    public boolean updateStatus(String candidateId, String newStatus, String rejectionReasonId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        String normalizedStatus = normalizeStatus(newStatus);
        if ("REJECTED".equals(normalizedStatus)) {
            validateRejectionReason(rejectionReasonId);
        } else {
            rejectionReasonId = null;
        }
        return candidateDAO.updateStatus(candidateId.trim(), normalizedStatus, rejectionReasonId);
    }

    public int getTotalCandidates() {
        return candidateDAO.countTotal();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.trim().isEmpty() ? "APPLIED" : status.trim();
        if (!CANDIDATE_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái hồ sơ không hợp lệ.");
        }
        return normalized;
    }

    private void validateRejectionReason(String rejectionReasonId) {
        if (rejectionReasonId == null || rejectionReasonId.trim().isEmpty()
                || !catalogDAO.isValueInType(rejectionReasonId, SharedCatalogType.REJECTION_REASON)) {
            throw new IllegalArgumentException("Hãy chọn lý do loại hồ sơ hợp lệ.");
        }
    }

    /**
     * [KN-231]: Kiểm tra xem ứng viên đã nộp đơn vào vị trí này chưa
     */
    public boolean hasAlreadyApplied(String email, String requisitionId) {
        if (email == null || requisitionId == null) return false;
        return candidateDAO.existsByEmailAndRequisition(email.trim().toLowerCase(), requisitionId.trim());
    }

    /**
     * [KN-233]: Sinh mã tra cứu hồ sơ duy nhất toàn hệ thống (APP-YYYYMMDD-XXXX)
     */
    public String generateUniqueTrackingCode() {
        String datePrefix = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Loại bỏ ký tự dễ nhầm lẫn như 0, O, 1, I
        Random random = new Random();
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("APP-");
            sb.append(datePrefix).append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
            attempts++;
            if (attempts > 100) {
                code = "APP-" + datePrefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                break;
            }
        } while (candidateDAO.existsTrackingCode(code));

        return code;
    }

    /**
     * [KN-233]: Tiếp nhận và xử lý nộp hồ sơ ứng tuyển công khai
     */
    public Candidate applyPublicJob(Candidate candidate) {
        return applyPublicJob(candidate, null);
    }

    public Candidate applyPublicJobWithWithdrawalToken(Candidate candidate, String withdrawalToken) {
        if (withdrawalToken == null || !withdrawalToken.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Token rút hồ sơ không hợp lệ.");
        }
        return applyPublicJob(candidate, SecurityUtil.sha256Hex(withdrawalToken));
    }

    private Candidate applyPublicJob(Candidate candidate, String withdrawalTokenHash) {
        if (candidate == null) {
            throw new IllegalArgumentException("Dữ liệu hồ sơ ứng tuyển không được để trống.");
        }

        // Validate Full Name
        if (candidate.getFullName() == null || candidate.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên ứng viên không được để trống.");
        }

        // Validate Email (KN-230)
        String email = candidate.getEmail();
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Địa chỉ email không đúng định dạng hợp lệ.");
        }
        candidate.setEmail(email.trim().toLowerCase());

        // Validate Phone (KN-230: Định dạng số điện thoại Việt Nam: 10 chữ số bắt đầu bằng 0)
        String phone = candidate.getPhone();
        if (phone == null || !phone.trim().replaceAll("[\\s.-]", "").matches("^0[3|5|7|8|9][0-9]{8}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại di động Việt Nam gồm 10 chữ số (bắt đầu bằng 03, 05, 07, 08, 09).");
        }
        candidate.setPhone(phone.trim().replaceAll("[\\s.-]", ""));

        // Validate Requisition ID
        if (candidate.getRequisitionId() == null || candidate.getRequisitionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin vị trí tuyển dụng ứng tuyển.");
        }

        // [KN-231]: Kiểm tra nộp trùng
        if (hasAlreadyApplied(candidate.getEmail(), candidate.getRequisitionId())) {
            throw new IllegalStateException("Bạn đã nộp hồ sơ ứng tuyển vào vị trí này trước đó với email " + candidate.getEmail() + ". Hệ thống không cho phép nộp lại nhiều lần.");
        }

        // Validate CV URL
        if (candidate.getCvUrl() == null || candidate.getCvUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng tải lên tệp CV đính kèm.");
        }

        // Xử lý nguồn ứng tuyển (mặc định Trang tuyển dụng nếu rỗng)
        if (candidate.getApplicationSourceId() == null || candidate.getApplicationSourceId().trim().isEmpty()) {
            candidate.setApplicationSourceId("cat-src-career-site");
        }

        // Sinh ID và Tracking Code độc nhất (KN-233)
        candidate.setId(SecurityUtil.generateUUID());
        if (candidate.getTrackingCode() == null || candidate.getTrackingCode().trim().isEmpty()) {
            candidate.setTrackingCode(generateUniqueTrackingCode());
        }
        candidate.setStatus("APPLIED");

        boolean inserted = withdrawalTokenHash == null
                ? candidateDAO.insertApplication(candidate)
                : candidateDAO.insertApplication(candidate, withdrawalTokenHash);
        if (!inserted) {
            throw new RuntimeException("Không thể lưu thông tin hồ sơ ứng tuyển vào hệ thống.");
        }

        try {
            new CandidateDuplicateService().detectAndRecordDuplicates(candidate, null);
        } catch (Exception ignored) {}

        return candidate;
    }

    public CandidateWithdrawal getCandidateForWithdrawal(String withdrawalToken) {
        if (withdrawalToken == null || !withdrawalToken.matches("[A-Za-z0-9_-]{43}")) {
            return null;
        }
        return candidateDAO.findByValidWithdrawalTokenHash(SecurityUtil.sha256Hex(withdrawalToken));
    }

    public CandidateWithdrawal withdrawCandidate(String withdrawalToken, String ipAddress, String userAgent) {
        if (withdrawalToken == null || !withdrawalToken.matches("[A-Za-z0-9_-]{43}")) {
            return null;
        }
        return candidateDAO.withdrawByTokenHash(
                SecurityUtil.sha256Hex(withdrawalToken), ipAddress, userAgent);
    }

    /**
     * [KN-233]: Tra cứu hồ sơ ứng viên theo mã tra cứu
     */
    public Candidate getCandidateByTrackingCode(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            return null;
        }
        return candidateDAO.findByTrackingCode(trackingCode.trim());
    }

    private final CVDataExtractionService cvExtractionService = new CVDataExtractionService();

    /**
     * [KN-213]: Lấy hồ sơ ứng viên theo ID
     */
    public Candidate getCandidateById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return candidateDAO.findById(id.trim());
    }

    /**
     * [KN-213]: Recruiter kích hoạt bóc tách lại thông tin từ tệp CV của ứng viên
     */
    public CVExtractionResult reExtractCVData(String candidateId, String uploadRootPath) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            return CVExtractionResult.failed("Thiếu mã hồ sơ ứng viên.");
        }
        Candidate c = candidateDAO.findById(candidateId.trim());
        if (c == null) {
            return CVExtractionResult.failed("Không tìm thấy hồ sơ ứng viên.");
        }
        String cvUrl = c.getCvUrl();
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            return CVExtractionResult.failed("Ứng viên này chưa có tệp CV đính kèm.");
        }

        CVExtractionResult result;
        try {
            if (cvUrl.startsWith("http://") || cvUrl.startsWith("https://")) {
                try {
                    java.net.URLConnection conn = new java.net.URL(cvUrl).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);
                    try (java.io.InputStream in = conn.getInputStream()) {
                        result = cvExtractionService.extract(in);
                    }
                } catch (Throwable e) {
                    result = CVExtractionResult.failed("Không thể tải tệp CV từ URL từ xa: " + e.getMessage());
                }
            } else {
                File pdfFile = null;
                if (cvUrl.startsWith("/") || cvUrl.startsWith("\\")) {
                    pdfFile = new File(uploadRootPath, cvUrl.substring(1).replace("/", File.separator));
                } else {
                    pdfFile = new File(uploadRootPath, cvUrl.replace("/", File.separator));
                }

                if (pdfFile != null && pdfFile.exists()) {
                    result = cvExtractionService.extract(pdfFile);
                } else {
                    result = CVExtractionResult.failed("Không tìm thấy tệp CV tại đường dẫn lưu trữ hệ thống: " + cvUrl);
                }
            }
        } catch (Throwable t) {
            result = CVExtractionResult.failed("Lỗi bóc tách CV: " + t.getMessage());
        }

        // Lưu kết quả trích xuất (KHÔNG tự ghi đè họ tên, email, SĐT gốc của ứng viên)
        candidateDAO.updateExtractionData(candidateId, result.getExtractedName(),
                result.getExtractedEmail(), result.getExtractedPhone(), result.getStatus());

        return result;
    }

    /**
     * [KN-213]: Recruiter xác nhận hoặc sửa thông tin đã bóc tách từ CV để cập nhật vào hồ sơ
     */
    public boolean confirmAndApplyExtraction(String candidateId, String confirmedName, String confirmedEmail, String confirmedPhone) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        if (confirmedName == null || confirmedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống khi xác nhận.");
        }
        if (confirmedEmail == null || !confirmedEmail.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Địa chỉ email xác nhận không hợp lệ.");
        }

        String cleanPhone = null;
        if (confirmedPhone != null && !confirmedPhone.trim().isEmpty()) {
            cleanPhone = confirmedPhone.trim().replaceAll("[\\s.-]", "");
        }

        boolean ok = candidateDAO.updateConfirmExtraction(candidateId.trim(), confirmedName.trim(), confirmedEmail.trim().toLowerCase(Locale.ROOT), cleanPhone);
        if (ok) {
            try {
                Candidate updated = candidateDAO.findById(candidateId.trim());
                if (updated != null) {
                    new CandidateDuplicateService().detectAndRecordDuplicates(updated, null);
                }
            } catch (Exception ignored) {}
        }
        return ok;
    }

    /**
     * [KN-218]: Lấy toàn bộ lịch sử ứng tuyển của cùng một người (theo email)
     */
    public List<Candidate> getApplicationHistory(String email) {
        if (email == null || email.trim().isEmpty()) return new java.util.ArrayList<>();
        return candidateDAO.findAllByEmail(email.trim().toLowerCase());
    }

    /**
     * Lấy toàn bộ lịch sử ứng tuyển của cả hai hồ sơ được bảo toàn sau hợp nhất (theo email và các hồ sơ đã gộp)
     */
    public List<Candidate> getApplicationHistory(Candidate candidate) {
        if (candidate == null) return new java.util.ArrayList<>();
        return candidateDAO.findAllApplicationsForCandidate(candidate);
    }

    /**
     * [KN-218]: Cập nhật ghi chú cho hồ sơ ứng viên
     */
    public boolean updateNotes(String candidateId, String notes) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        return candidateDAO.updateNotes(candidateId.trim(), notes);
    }

    /**
     * [KN-274]: Cập nhật nhãn phân loại (tags) cho hồ sơ ứng viên
     */
    public boolean updateTags(String candidateId, String tags) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        return candidateDAO.updateTags(candidateId.trim(), tags);
    }

    /**
     * [KN-269]: Kiểm tra quyền truy cập hồ sơ ứng viên:
     * - ADMIN và HR_MANAGER được xem tất cả.
     * - RECRUITER: Phải là recruiter chính hoặc recruiter hỗ trợ của vị trí đó.
     * - HIRING_MANAGER: Phải là hiring manager của yêu cầu tuyển dụng đó.
     */
    public boolean canAccessCandidateProfile(com.irms.model.User user, Candidate candidate) {
        if (user == null || candidate == null) return false;
        if (user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")) return true;

        String requisitionId = candidate.getRequisitionId();
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            return false;
        }

        if (user.hasRole("RECRUITER")) {
            com.irms.dao.RecruiterAssignmentDAO assignmentDAO = new com.irms.dao.RecruiterAssignmentDAO();
            if (assignmentDAO.isRecruiterAssigned(requisitionId, user.getId())) {
                return true;
            }
        }

        if (user.hasRole("HIRING_MANAGER")) {
            return isHiringManagerOfRequisition(requisitionId, user.getId());
        }

        return false;
    }

    private boolean isHiringManagerOfRequisition(String requisitionId, String userId) {
        if (requisitionId == null || userId == null) return false;
        try (java.sql.Connection conn = com.irms.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM recruitment_requisitions WHERE id = ? AND (hiring_manager_id = ? OR created_by = ?) LIMIT 1")) {
            ps.setString(1, requisitionId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
