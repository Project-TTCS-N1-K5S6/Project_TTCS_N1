package com.irms.service;

import com.irms.dao.RecruitmentRequestDAO;
import com.irms.dao.SalaryRangeDAO;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SalaryRange;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Xử lý nghiệp vụ Yêu cầu tuyển dụng (Job Requisitions)
 */
public class RecruitmentRequestService {
    private final RecruitmentRequestDAO requestDAO = new RecruitmentRequestDAO();
    private final SalaryRangeDAO salaryRangeDAO = new SalaryRangeDAO();
    private final AuditService auditService = new AuditService();

    public List<RecruitmentRequest> getAllRequests(String search, String status, String departmentId) {
        return requestDAO.findAll(search, status, departmentId);
    }

    public RecruitmentRequest getRequestById(String id) {
        return requestDAO.findById(id);
    }

    public String getNextCode() {
        return requestDAO.generateNextCode();
    }

    public SalaryRange getStandardSalaryRange(String departmentId, String positionTitle) {
        return salaryRangeDAO.findByDepartmentAndPosition(departmentId, positionTitle);
    }

    /**
     * Kiểm tra xem dải lương đề xuất có nằm ngoài dải chuẩn của chức danh hay không
     */
    public boolean isSalaryOutOfStandardRange(String departmentId, String positionTitle, BigDecimal proposedMin, BigDecimal proposedMax) {
        if (positionTitle == null || positionTitle.trim().isEmpty()) {
            return false;
        }
        SalaryRange standard = salaryRangeDAO.findByDepartmentAndPosition(departmentId, positionTitle);
        if (standard == null) {
            return false;
        }

        boolean out = false;
        if (proposedMin != null && standard.getMinSalary() != null) {
            if (proposedMin.compareTo(standard.getMinSalary()) < 0) {
                out = true;
            }
        }
        if (proposedMax != null && standard.getMaxSalary() != null) {
            if (proposedMax.compareTo(standard.getMaxSalary()) > 0) {
                out = true;
            }
        }
        return out;
    }

    /**
     * Xác thực các tiêu chí theo yêu cầu nghiệp vụ
     */
    public void validateRequest(RecruitmentRequest req, boolean isDraft) {
        // 1. Chức danh không được để trống
        if (req.getPositionTitle() == null || req.getPositionTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hoặc nhập chức danh cần tuyển!");
        }

        // Tự động sinh tiêu đề nếu người dùng chưa nhập
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            req.setTitle("Tuyển dụng " + req.getPositionTitle().trim());
        }

        // 2. Ngày cần người không được ở quá khứ
        if (req.getDeadline() != null) {
            LocalDate today = LocalDate.now();
            LocalDate reqDate = req.getDeadline().toLocalDate();
            if (reqDate.isBefore(today)) {
                throw new IllegalArgumentException("Ngày cần người không được ở quá khứ (phải từ hôm nay trở đi)!");
            }
        }

        // 3. Kiểm tra lương tối thiểu <= lương tối đa
        if (req.getMinSalary() != null && req.getMaxSalary() != null) {
            if (req.getMinSalary().compareTo(req.getMaxSalary()) > 0) {
                throw new IllegalArgumentException("Dải lương đề xuất không hợp lệ: Lương tối thiểu không được lớn hơn lương tối đa!");
            }
            if (req.getMinSalary().compareTo(BigDecimal.ZERO) < 0 || req.getMaxSalary().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Dải lương đề xuất không được là số âm!");
            }
        }

        // 4. Kiểm tra dải lương đề xuất so với dải chuẩn của chức danh
        SalaryRange standard = salaryRangeDAO.findByDepartmentAndPosition(req.getDepartmentId(), req.getPositionTitle());
        boolean outOfRange = false;
        if (standard != null) {
            if (req.getMinSalary() != null && standard.getMinSalary() != null && req.getMinSalary().compareTo(standard.getMinSalary()) < 0) {
                outOfRange = true;
            }
            if (req.getMaxSalary() != null && standard.getMaxSalary() != null && req.getMaxSalary().compareTo(standard.getMaxSalary()) > 0) {
                outOfRange = true;
            }
        }

        // Nếu ngoài dải chuẩn thì BẮT BUỘC nhập giải trình (ngay cả khi gửi duyệt, và khuyến khích khi lưu nháp)
        if (outOfRange && !isDraft) {
            if (req.getSalaryExplanation() == null || req.getSalaryExplanation().trim().isEmpty()) {
                String rangeText = (standard != null) ? standard.getFormattedRange() : "khung chuẩn";
                throw new IllegalArgumentException("Dải lương đề xuất nằm ngoài dải chuẩn của chức danh (" + rangeText + "). Bạn bắt buộc phải nhập nội dung giải trình ngân sách!");
            }
        }

        // 5. Nếu không phải lưu nháp (Gửi duyệt), kiểm tra nghiêm ngặt toàn bộ tiêu chí
        if (!isDraft) {
            if (req.getDepartmentId() == null || req.getDepartmentId().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn phòng ban cần tuyển!");
            }
            if (req.getHeadcount() <= 0) {
                throw new IllegalArgumentException("Số lượng tuyển dụng phải lớn hơn 0!");
            }
            if (req.getRecruitmentReason() == null || req.getRecruitmentReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn lý do tuyển dụng (Thay thế hoặc Tăng mới)!");
            }
            if (req.getDeadline() == null) {
                throw new IllegalArgumentException("Vui lòng nhập ngày cần người (không được ở quá khứ)!");
            }
            if (req.getJobDescription() == null || req.getJobDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng soạn bản mô tả công việc (Job Description)!");
            }
            if (req.getJobRequirements() == null || req.getJobRequirements().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng soạn yêu cầu ứng viên (Candidate Requirements)!");
            }
            if (req.getMinSalary() == null || req.getMaxSalary() == null) {
                throw new IllegalArgumentException("Vui lòng nhập đầy đủ dải lương đề xuất (từ - đến)!");
            }
        }
    }

    public boolean createRequest(RecruitmentRequest req, boolean isDraft, String actorId, String ip, String userAgent) {
        if (req.getId() == null || req.getId().trim().isEmpty()) {
            req.setId(SecurityUtil.generateUUID());
        }
        if (req.getCode() == null || req.getCode().trim().isEmpty()) {
            req.setCode(requestDAO.generateNextCode());
        }
        req.setStatus(isDraft ? "DRAFT" : "PENDING_APPROVAL");
        req.setCreatedBy(actorId);

        validateRequest(req, isDraft);

        boolean success = requestDAO.insert(req);
        if (success) {
            String action = isDraft ? "REQUISITION_DRAFT_CREATED" : "REQUISITION_SUBMITTED";
            String desc = (isDraft ? "Lưu nháp yêu cầu tuyển dụng: " : "Gửi phê duyệt yêu cầu tuyển dụng: ") 
                          + req.getCode() + " - " + req.getTitle();
            auditService.log(actorId, action, "REQUISITION", req.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public boolean updateRequest(RecruitmentRequest req, boolean isDraft, String actorId, String ip, String userAgent) {
        RecruitmentRequest current = requestDAO.findById(req.getId());
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng có mã ID: " + req.getId());
        }

        req.setStatus(isDraft ? "DRAFT" : "PENDING_APPROVAL");
        validateRequest(req, isDraft);

        boolean success = requestDAO.update(req);
        if (success) {
            String action = isDraft ? "REQUISITION_DRAFT_UPDATED" : "REQUISITION_SUBMITTED";
            String desc = "Cập nhật yêu cầu tuyển dụng: " + req.getCode() + " - " + req.getTitle();
            auditService.log(actorId, action, "REQUISITION", req.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public boolean deleteRequest(String id, String actorId, String ip, String userAgent) {
        RecruitmentRequest current = requestDAO.findById(id);
        if (current == null) return false;

        boolean success = requestDAO.delete(id);
        if (success) {
            auditService.log(actorId, "REQUISITION_DELETED", "REQUISITION", id, 
                    "Xóa yêu cầu tuyển dụng: " + current.getCode(), ip, userAgent);
        }
        return success;
    }
}
