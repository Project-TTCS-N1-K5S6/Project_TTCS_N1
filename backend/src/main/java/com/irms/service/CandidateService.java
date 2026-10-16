package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.Candidate;
import com.irms.model.SharedCatalogType;
import com.irms.util.SecurityUtil;

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

    /**
     * Lấy hồ sơ ứng viên theo ID
     */
    public Candidate getCandidateById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return candidateDAO.findById(id.trim());
    }

    /**
     * Lấy danh sách Kho ứng viên tiềm năng có phân trang & lọc theo chức danh quan tâm, kỹ năng... (KN-286)
     */
    public List<Candidate> getTalentPool(String search, String targetPosition, String skill, Integer minRating, String departmentId, String recruiterId, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return candidateDAO.findTalentPool(search, targetPosition, skill, minRating, departmentId, recruiterId, offset, size);
    }

    public List<Candidate> getTalentPool(String search, Integer minRating, String skill, String departmentId, String recruiterId, int page, int pageSize) {
        return getTalentPool(search, null, skill, minRating, departmentId, recruiterId, page, pageSize);
    }

    /**
     * Đếm tổng số lượng ứng viên trong Kho tiềm năng theo bộ lọc bao gồm chức danh quan tâm (KN-286)
     */
    public int countTalentPool(String search, String targetPosition, String skill, Integer minRating, String departmentId, String recruiterId) {
        return candidateDAO.countTalentPool(search, targetPosition, skill, minRating, departmentId, recruiterId);
    }

    public int countTalentPool(String search, Integer minRating, String skill, String departmentId, String recruiterId) {
        return countTalentPool(search, null, skill, minRating, departmentId, recruiterId);
    }

    /**
     * [KN-286]: Gợi ý ứng viên phù hợp từ kho tiềm năng khi mở vị trí mới
     */
    public List<Candidate> getSuggestedTalents(String requisitionId, int limit) {
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return candidateDAO.findSuggestedTalents(requisitionId.trim(), limit);
    }

    /**
     * [KN-286]: Ứng viên bị loại ở vòng cuối được đánh dấu đưa vào kho tiềm năng kèm lý do
     */
    public boolean rejectAndAddToTalentPool(String candidateId, String rejectionReasonId, int potentialRating, String potentialNotes, String potentialSkills, String targetPosition, String targetDeptId, String userId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ứng viên không được để trống.");
        }
        boolean statusUpdated = updateStatus(candidateId.trim(), "REJECTED", rejectionReasonId);
        if (!statusUpdated) {
            return false;
        }
        int rating = Math.max(1, Math.min(5, potentialRating));
        return addToTalentPool(candidateId.trim(), rating, potentialSkills, potentialNotes, targetDeptId, targetPosition, userId);
    }

    /**
     * Đưa ứng viên hiện có vào Kho ứng viên tiềm năng
     */
    public boolean addToTalentPool(String candidateId, int rating, String skills, String notes, String targetDeptId, String targetPosition, String userId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ứng viên không được để trống.");
        }
        int clampedRating = Math.max(1, Math.min(5, rating));
        return candidateDAO.addToTalentPool(candidateId.trim(), clampedRating, skills, notes, targetDeptId, targetPosition, userId);
    }

    /**
     * Cập nhật thông tin đánh giá tiềm năng, kỹ năng, phòng ban mong muốn
     */
    public boolean updateTalentPool(String candidateId, int rating, String skills, String notes, String targetDeptId, String targetPosition) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ứng viên không được để trống.");
        }
        int clampedRating = Math.max(1, Math.min(5, rating));
        return candidateDAO.updateTalentPool(candidateId.trim(), clampedRating, skills, notes, targetDeptId, targetPosition);
    }

    /**
     * Loại bỏ ứng viên khỏi Kho tiềm năng
     */
    public boolean removeFromTalentPool(String candidateId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ứng viên không được để trống.");
        }
        return candidateDAO.removeFromTalentPool(candidateId.trim());
    }

    /**
     * Tái sử dụng ứng viên từ kho tiềm năng: Gán vào một Yêu cầu tuyển dụng đang mở
     */
    public boolean assignCandidateToRequisition(String candidateId, String requisitionId, String notes, String userId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ứng viên không được để trống.");
        }
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn yêu cầu tuyển dụng cần gán.");
        }
        return candidateDAO.assignCandidateToRequisition(candidateId.trim(), requisitionId.trim(), notes, userId);
    }

    /**
     * Thêm mới trực tiếp một hồ sơ vào Kho ứng viên tiềm năng
     */
    public boolean createPotentialCandidate(Candidate c, String userId) {
        if (c == null) {
            throw new IllegalArgumentException("Thông tin ứng viên không được để trống.");
        }
        if (c.getFullName() == null || c.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên ứng viên là bắt buộc.");
        }
        String email = c.getEmail();
        if (email == null || !email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Địa chỉ email ứng viên không đúng định dạng.");
        }
        c.setId(SecurityUtil.generateUUID());
        c.setEmail(email.trim().toLowerCase());
        c.setFullName(c.getFullName().trim());
        if (c.getPhone() != null && !c.getPhone().trim().isEmpty()) {
            c.setPhone(c.getPhone().trim());
        }
        if (c.getTrackingCode() == null || c.getTrackingCode().trim().isEmpty()) {
            c.setTrackingCode(generateUniqueTrackingCode());
        }
        c.setAddedToPoolBy(userId);
        c.setPotential(true);
        if (c.getPotentialRating() < 1 || c.getPotentialRating() > 5) {
            c.setPotentialRating(3);
        }
        return candidateDAO.insertPotentialCandidate(c);
    }

    /**
     * Lấy thống kê KPI của Kho ứng viên tiềm năng
     */
    public int[] getTalentPoolStats(String recruiterId) {
        return candidateDAO.getTalentPoolStats(recruiterId);
    }
}
