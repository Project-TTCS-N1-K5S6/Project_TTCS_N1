package com.irms.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.irms.dao.JobPostingDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // =========================================================================
    // Nghiệp vụ Tạo nhanh Bài đăng Tuyển dụng (KN-204, KN-205, KN-206, KN-207, KN-208)
    // =========================================================================

    /**
     * KN-206: Chuẩn hóa nội dung mô tả công việc
     * - Kế thừa từ yêu cầu tuyển dụng gốc
     * - Làm sạch dòng trống dư thừa, chuẩn hóa bullet points
     * - Giữ nguyên vẹn toàn bộ câu chữ, thông tin nghiệp vụ, không thêm bớt thông tin sai lệch
     */
    public String normalizeJobDescription(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        int consecutiveBlankLines = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                consecutiveBlankLines++;
                if (consecutiveBlankLines <= 1) {
                    sb.append("\n");
                }
                continue;
            }
            consecutiveBlankLines = 0;

            // Chuẩn hóa ký tự đầu dòng thành "- "
            if (trimmed.startsWith("•") || trimmed.startsWith("*") || trimmed.startsWith("+")
                    || trimmed.startsWith("–") || trimmed.startsWith("—")) {
                String itemContent = trimmed.substring(1).trim();
                sb.append("- ").append(itemContent).append("\n");
            } else {
                sb.append(trimmed).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Kiểm tra quyền thực hiện thao tác tạo bài đăng tuyển dụng
     */
    public boolean canManageJobPostings(User actor) {
        if (actor == null) return false;
        return actor.hasRole("ADMIN") || actor.hasRole("HR_MANAGER") || actor.hasRole("RECRUITER")
                || actor.hasPermission("job_postings.manage");
    }

    /**
     * KN-205 & KN-208: Backend kiểm tra dữ liệu bắt buộc & điều kiện phê duyệt
     */
    public List<String> validateQuickPosting(JobPosting posting, RecruitmentRequest req) {
        List<String> errors = new ArrayList<>();
        if (posting == null) {
            errors.add("Dữ liệu bài đăng tuyển dụng không được để trống.");
            return errors;
        }

        // 1. Kiểm tra yêu cầu tuyển dụng nguồn
        if (req == null) {
            errors.add("Yêu cầu tuyển dụng nguồn không tồn tại.");
            return errors;
        }

        // 2. KN-208: Kiểm tra trạng thái Đã duyệt của yêu cầu tuyển dụng nguồn
        if (!isEligibleForPublish(req)) {
            errors.add("Chỉ những yêu cầu tuyển dụng ở trạng thái \"Đã duyệt\" mới được phép tạo bài đăng! Trạng thái hiện tại: " 
                    + req.getStatusLabel() + ".");
        }

        // 3. Tiêu đề bài đăng
        if (posting.getTitle() == null || posting.getTitle().trim().isEmpty()) {
            errors.add("Tiêu đề bài đăng tuyển dụng là bắt buộc và không được để trống.");
        } else if (posting.getTitle().trim().length() > 255) {
            errors.add("Tiêu đề bài đăng không được vượt quá 255 ký tự.");
        }

        // 4. Nội dung mô tả công việc
        if (posting.getJobDescription() == null || posting.getJobDescription().trim().isEmpty()) {
            errors.add("Nội dung mô tả công việc là bắt buộc và không được để trống.");
        }

        // 5. Địa điểm làm việc
        if ((posting.getWorkLocation() == null || posting.getWorkLocation().trim().isEmpty())
                && (posting.getWorkLocationId() == null || posting.getWorkLocationId().trim().isEmpty())) {
            errors.add("Địa điểm làm việc là bắt buộc. Vui lòng chọn hoặc nhập địa điểm làm việc.");
        }

        // 6. Hình thức làm việc
        if ((posting.getWorkMode() == null || posting.getWorkMode().trim().isEmpty())
                && (posting.getWorkModeId() == null || posting.getWorkModeId().trim().isEmpty())) {
            errors.add("Hình thức làm việc là bắt buộc. Vui lòng chọn hình thức làm việc.");
        }

        // 7. Hạn nhận hồ sơ
        if (posting.getApplicationDeadline() == null) {
            errors.add("Hạn nhận hồ sơ là bắt buộc.");
        } else {
            LocalDate deadlineDate = posting.getApplicationDeadline().toLocalDate();
            LocalDate today = LocalDate.now();
            if (deadlineDate.isBefore(today)) {
                errors.add("Hạn nhận hồ sơ không hợp lệ (không được ở quá khứ so với ngày hiện tại).");
            }
        }

        // 8. Kiểm tra khoảng lương
        if (posting.isSalaryVisible()) {
            BigDecimal min = posting.getMinSalary();
            BigDecimal max = posting.getMaxSalary();
            BigDecimal maxAllowed = new BigDecimal("1000000000000");

            if (min != null) {
                if (min.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Mức lương tối thiểu không được là số âm.");
                }
                if (min.compareTo(maxAllowed) > 0) {
                    errors.add("Mức lương tối thiểu vượt quá giới hạn cho phép.");
                }
            }
            if (max != null) {
                if (max.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Mức lương tối đa không được là số âm.");
                }
                if (max.compareTo(maxAllowed) > 0) {
                    errors.add("Mức lương tối đa vượt quá giới hạn cho phép.");
                }
            }
            if (min != null && max != null) {
                if (min.compareTo(max) > 0) {
                    errors.add("Khoảng lương không hợp lệ: Mức lương tối thiểu không được lớn hơn mức lương tối đa.");
                }
            }
        }

        return errors;
    }

    /**
     * KN-205 & KN-208: Xác thực bài đăng tuyển dụng tạo nhanh, ném ngoại lệ nếu vi phạm
     */
    public void assertValidQuickPosting(JobPosting posting, RecruitmentRequest req) {
        List<String> errors = validateQuickPosting(posting, req);
        if (!errors.isEmpty()) {
            if (req != null && !isEligibleForPublish(req)) {
                throw new IllegalStateException(String.join("; ", errors));
            }
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    /**
     * KN-207: Tạo nhanh bài đăng từ yêu cầu tuyển dụng đã duyệt
     */
    public JobPosting createQuickJobPosting(JobPosting posting, User actor, String ip, String userAgent) throws SQLException {
        if (!canManageJobPostings(actor)) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác tạo bài đăng tuyển dụng!");
        }

        if (posting == null || posting.getRequisitionId() == null || posting.getRequisitionId().trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu tuyển dụng nguồn!");
        }

        RecruitmentRequest req = requestDAO.findById(posting.getRequisitionId().trim());
        assertValidQuickPosting(posting, req);

        // Nếu người dùng chọn không hiển thị lương, lưu giá trị theo quy ước không hiển thị (null)
        if (!posting.isSalaryVisible()) {
            posting.setMinSalary(null);
            posting.setMaxSalary(null);
        }

        String actorId = (actor != null) ? actor.getId() : "system";
        String actorName = (actor != null) ? actor.getFullName() : "Hệ thống";

        JobPosting created = jobPostingDAO.createQuickPosting(posting, actorId, actorName);

        // Ghi nhật ký kiểm toán
        String desc = "Tạo nhanh bài đăng tuyển dụng: " + created.getTitle() + 
                      " (Mã yêu cầu: " + req.getCode() + ", Trạng thái: " + created.getStatus() + ")";
        auditService.log(actorId, "JOB_POST_QUICK_CREATED", "JOB_POSTING", created.getId(), desc, ip, userAgent);

        return created;
    }

    public List<RecruitmentRequest> getApprovedRequisitions() {
        return jobPostingDAO.findApprovedRequisitionsForPosting();
    }

    public JobPostingDAO getJobPostingDAO() {
        return jobPostingDAO;
    }
}
