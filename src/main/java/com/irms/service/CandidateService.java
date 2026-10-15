package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.Candidate;
import com.irms.model.SharedCatalogType;
import com.irms.util.SecurityUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
        return candidateDAO.insert(c);
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

        boolean inserted = candidateDAO.insertApplication(candidate);
        if (!inserted) {
            throw new RuntimeException("Không thể lưu thông tin hồ sơ ứng tuyển vào hệ thống.");
        }

        return candidate;
    }

    /**
     * [KN-233]: Tra cứu hồ sơ ứng tuyển theo mã tra cứu
     */
    public Candidate getCandidateByTrackingCode(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            return null;
        }
        return candidateDAO.findByTrackingCode(trackingCode.trim());
    }
}
