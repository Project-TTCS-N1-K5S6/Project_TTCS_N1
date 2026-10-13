package com.irms.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.irms.dao.JobPostingDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ Quản lý Xuất bản bài đăng (KN-120)
 * 1. Tin phải duyệt trước khi show công khai
 * 2. Xem trước đúng như giao diện khai báo trước khi xuất bản
 * 3. Ghi lại bản xuất và thời điểm
 */
public class JobPublishingService {

    private final JobPostingDAO jobPostingDAO = new JobPostingDAO();
    private final RecruitmentRequestDAO requestDAO = new RecruitmentRequestDAO();
    private final AuditService auditService = new AuditService();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Tiêu chí 1: Tin phải duyệt trước khi show công khai
     */
    public boolean isEligibleForPublish(RecruitmentRequest req) {
        if (req == null || req.getStatus() == null) return false;
        String status = req.getStatus().trim().toUpperCase();
        return "APPROVED".equals(status) || "OPEN".equals(status);
    }

    public void validateCanPublish(RecruitmentRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin tin tuyển dụng!");
        }
        if (!isEligibleForPublish(req)) {
            throw new IllegalStateException("Tin phải duyệt trước khi show công khai! Tin này hiện đang ở trạng thái \"" 
                    + req.getStatusLabel() + "\". Chỉ các tin đã được phê duyệt mới được phép xuất bản công khai.");
        }
    }

    /**
     * Tiêu chí 3: Xuất bản bài đăng, ghi lại bản xuất và thời điểm kèm snapshot nội dung
     */
    public JobPostPublication publishPost(String requisitionId, String channel, String note,
                                         User actor, String ip, String userAgent) throws SQLException {
        RecruitmentRequest req = requestDAO.findById(requisitionId);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng ID: " + requisitionId);
        }

        // Kiểm tra điều kiện bắt buộc: Tin phải duyệt trước khi show công khai
        validateCanPublish(req);

        String actorId = (actor != null) ? actor.getId() : "system";
        String actorName = (actor != null) ? actor.getFullName() : "Hệ thống";

        // Tạo JSON snapshot toàn bộ nội dung khai báo tại thời điểm xuất bản này
        String snapshotJson = createSnapshotJson(req);

        // Lưu vào DB
        JobPostPublication pub = jobPostingDAO.publish(requisitionId, channel, note, actorId, actorName, snapshotJson);

        // Ghi nhật ký kiểm toán
        String desc = "Xuất bản bài đăng " + req.getCode() + " - " + req.getTitle() + 
                      " (" + pub.getVersionLabel() + ") tại thời điểm " + pub.getFormattedPublishedAt();
        auditService.log(actorId, "JOB_POST_PUBLISHED", "JOB_POSTING", pub.getId(), desc, ip, userAgent);

        return pub;
    }

    /**
     * Duyệt yêu cầu tuyển dụng (nếu người dùng có thẩm quyền)
     */
    public boolean approveRequisition(String requisitionId, User actor, String ip, String userAgent) {
        RecruitmentRequest req = requestDAO.findById(requisitionId);
        if (req == null) return false;

        String actorId = (actor != null) ? actor.getId() : "system";
        boolean ok = jobPostingDAO.approveRequisition(requisitionId, actorId);
        if (ok) {
            auditService.log(actorId, "REQUISITION_APPROVED", "REQUISITION", requisitionId,
                    "Phê duyệt yêu cầu tuyển dụng: " + req.getCode() + " - " + req.getTitle(), ip, userAgent);
        }
        return ok;
    }

    /**
     * Gỡ bài đăng (Unpublish)
     */
    public boolean unpublishPost(String requisitionId, String reason, User actor, String ip, String userAgent) {
        RecruitmentRequest req = requestDAO.findById(requisitionId);
        if (req == null) return false;

        String actorId = (actor != null) ? actor.getId() : "system";
        String actorName = (actor != null) ? actor.getFullName() : "Hệ thống";

        boolean ok = jobPostingDAO.unpublish(requisitionId, actorId, actorName, reason);
        if (ok) {
            String desc = "Gỡ bài đăng tuyển dụng: " + req.getCode() + " - " + req.getTitle() + 
                          ". Lý do: " + (reason != null ? reason : "Không có lý do");
            auditService.log(actorId, "JOB_POST_UNPUBLISHED", "JOB_POSTING", requisitionId, desc, ip, userAgent);
        }
        return ok;
    }

    /**
     * Tiêu chí 3: Lấy lịch sử tất cả các bản xuất và thời điểm của một bài đăng
     */
    public List<JobPostPublication> getPublicationHistory(String requisitionId) {
        return jobPostingDAO.getPublicationHistory(requisitionId);
    }

    public JobPostPublication getPublicationById(String publicationId) {
        return jobPostingDAO.getPublicationById(publicationId);
    }

    public List<JobPosting> getPostings(String search, String deptId, String approvalStatus, String publishStatus, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return jobPostingDAO.findAllPosts(search, deptId, approvalStatus, publishStatus, offset, size);
    }

    public int countPostings(String search, String deptId, String approvalStatus, String publishStatus) {
        return jobPostingDAO.countAllPosts(search, deptId, approvalStatus, publishStatus);
    }

    public JobPosting getPostingByRequisitionId(String requisitionId) {
        return jobPostingDAO.findByRequisitionId(requisitionId);
    }

    public List<JobPosting> getPublicPublishedPosts(String search, String deptId, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return jobPostingDAO.findPublicPublishedPosts(search, deptId, offset, size);
    }

    /**
     * Tạo dữ liệu snapshot chi tiết chuẩn hóa để lưu trữ lịch sử
     */
    public String createSnapshotJson(RecruitmentRequest req) {
        Map<String, Object> map = new HashMap<>();
        map.put("requisitionId", req.getId());
        map.put("code", req.getCode());
        map.put("title", req.getTitle());
        map.put("positionTitle", req.getPositionTitle());
        map.put("departmentId", req.getDepartmentId());
        map.put("departmentName", req.getDepartmentName() != null ? req.getDepartmentName() : "Toàn công ty");
        map.put("headcount", req.getHeadcount());
        map.put("recruitmentReason", req.getRecruitmentReason());
        map.put("recruitmentReasonLabel", req.getRecruitmentReasonLabel());
        map.put("minSalary", req.getMinSalary());
        map.put("maxSalary", req.getMaxSalary());
        map.put("currency", req.getCurrency());
        map.put("formattedSalaryRange", req.getFormattedSalaryRange());
        map.put("deadline", req.getDeadline() != null ? req.getDeadline().toString() : null);
        map.put("formattedDeadline", req.getDeadline() != null ? new SimpleDateFormat("dd/MM/yyyy").format(req.getDeadline()) : "--");
        map.put("jobDescription", req.getJobDescription());
        map.put("jobRequirements", req.getJobRequirements());
        map.put("salaryExplanation", req.getSalaryExplanation());
        map.put("recruiterName", req.getRecruiterName());
        map.put("snapshotCreatedAt", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        return gson.toJson(map);
    }
}
