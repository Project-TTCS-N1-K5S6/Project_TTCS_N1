package com.irms.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irms.dao.CandidateDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.CVExtractionResult;
import com.irms.model.Candidate;
import com.irms.model.CandidateActivity;
import com.irms.model.CandidateWithdrawal;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.StageTransitionResult;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
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
    private final AuditService auditService = new AuditService();
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

    /**
     * [KN-282 / KN-300]: Lấy danh sách ứng viên phục vụ màn hình Kanban Pipeline
     */
    public List<Candidate> getCandidatesForPipeline(String requisitionId, String recruiterId, String search) {
        return candidateDAO.findForPipeline(requisitionId, recruiterId, search);
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

    /**
     * [KN-283]: Chuyển giai đoạn tuyển dụng cho ứng viên với đầy đủ các quy tắc nghiệp vụ:
     * 1. Kiểm tra tính hợp lệ của ứng viên và quyền thao tác (RBAC).
     * 2. [KN-305 / KN-310]: Không cho phép bỏ qua giai đoạn Phỏng vấn để chuyển sang Đề xuất offer/Nhận việc,
     *    trừ khi có sự xác nhận hợp lệ của Trưởng phòng Nhân sự (HR_MANAGER / ADMIN).
     * 3. [KN-309]: Chuyển sang "Loại" (REJECTED) bắt buộc phải chọn lý do hợp lệ từ danh mục shared_catalogs.
     * 4. [KN-306]: Lưu nhật ký chuyển giai đoạn vào candidate_activities và cập nhật trạng thái trong CÙNG MỘT GIAO DỊCH.
     * 5. Ghi nhật ký kiểm toán hệ thống (Audit Log).
     */
    public StageTransitionResult transitionStage(
            User currentUser,
            String candidateId,
            String targetStage,
            String rejectionReasonId,
            boolean skipInterviewConfirmed,
            String confirmationNote,
            String clientIp,
            String userAgent) {

        if (currentUser == null) {
            return new StageTransitionResult(false, 401, "Vui lòng đăng nhập để thực hiện thao tác.");
        }

        if (candidateId == null || candidateId.trim().isEmpty()) {
            return new StageTransitionResult(false, 400, "Thiếu mã hồ sơ ứng viên.");
        }

        Candidate candidate = candidateDAO.findById(candidateId.trim());
        if (candidate == null) {
            return new StageTransitionResult(false, 404, "Hồ sơ ứng viên không tồn tại hoặc đã bị xóa.");
        }

        // Kiểm tra quyền thao tác với ứng viên theo phân quyền RBAC
        if (!canManageCandidate(currentUser, candidate)) {
            return new StageTransitionResult(false, 403,
                    "Bạn không có quyền chuyển giai đoạn cho ứng viên này (chỉ Recruiter phụ trách hoặc Lãnh đạo tuyển dụng mới được phép thao tác).");
        }

        String normalizedTarget = targetStage != null ? targetStage.trim().toUpperCase(Locale.ROOT) : "";
        if (!CANDIDATE_STATUSES.contains(normalizedTarget)) {
            return new StageTransitionResult(false, 400, "Giai đoạn tuyển dụng đích không hợp lệ: " + targetStage);
        }

        String oldStage = candidate.getStatus() != null ? candidate.getStatus().trim().toUpperCase(Locale.ROOT) : "APPLIED";
        String oldStageName = getStageName(oldStage);
        String targetStageName = getStageName(normalizedTarget);

        StageTransitionResult result = new StageTransitionResult();
        result.setCandidateId(candidate.getId());
        result.setCandidateName(candidate.getFullName());
        result.setOldStage(oldStage);
        result.setOldStageName(oldStageName);
        result.setNewStage(normalizedTarget);
        result.setNewStageName(targetStageName);

        // Nếu chuyển vào chính giai đoạn hiện tại
        if (normalizedTarget.equals(oldStage)) {
            result.setSuccess(true);
            result.setStatusCode(200);
            result.setMessage("Ứng viên hiện đã ở giai đoạn " + targetStageName + ".");
            return result;
        }

        // [KN-305 / KN-310]: Kiểm tra điều kiện bỏ qua giai đoạn Phỏng vấn
        boolean isSkippingInterview = isSkippingInterview(oldStage, normalizedTarget);
        boolean isHrManagerOrAdmin = currentUser.hasRole("HR_MANAGER") || currentUser.hasRole("ADMIN");

        if (isSkippingInterview) {
            if (!isHrManagerOrAdmin) {
                // Người dùng không có thẩm quyền đặc cách (ví dụ: Recruiter)
                result.setSuccess(false);
                result.setStatusCode(403);
                result.setRequiresHrApproval(true);
                result.setCanCurrentUserApprove(false);
                result.setMessage("Quy trình tuyển dụng yêu cầu ứng viên phải qua giai đoạn Phỏng vấn trước khi Đề xuất offer. " +
                        "Bạn không có quyền xác nhận bỏ qua phỏng vấn. Vui lòng liên hệ Trưởng phòng Nhân sự (HR Manager).");
                return result;
            } else {
                // Người dùng là HR Manager hoặc Admin: Yêu cầu xác nhận rõ ràng
                if (!skipInterviewConfirmed) {
                    result.setSuccess(false);
                    result.setStatusCode(400);
                    result.setRequiresHrApproval(true);
                    result.setCanCurrentUserApprove(true);
                    result.setMessage("Ứng viên chưa qua giai đoạn Phỏng vấn. Cần có sự xác nhận đặc cách của Trưởng phòng Nhân sự để tiếp tục.");
                    return result;
                }
                result.setSkipInterview(true);
                result.setConfirmationNote(confirmationNote != null ? confirmationNote.trim() : "");
            }
        }

        // [KN-309]: Ràng buộc bắt buộc chọn lý do khi chuyển sang trạng thái "Loại" (REJECTED)
        String finalRejectionReasonId = null;
        String rejectionReasonName = null;
        if ("REJECTED".equals(normalizedTarget)) {
            if (rejectionReasonId == null || rejectionReasonId.trim().isEmpty()) {
                result.setSuccess(false);
                result.setStatusCode(400);
                result.setMessage("Khi chuyển ứng viên sang giai đoạn Loại, bắt buộc phải chọn lý do loại từ danh mục hợp lệ.");
                return result;
            }
            SharedCatalogItem reasonItem = catalogDAO.findById(rejectionReasonId.trim());
            if (reasonItem == null || reasonItem.getType() != SharedCatalogType.REJECTION_REASON) {
                result.setSuccess(false);
                result.setStatusCode(400);
                result.setMessage("Lý do loại hồ sơ không hợp lệ hoặc không tồn tại trong danh mục hệ thống.");
                return result;
            }
            finalRejectionReasonId = reasonItem.getId();
            rejectionReasonName = reasonItem.getValue();
            result.setRejectionReasonId(finalRejectionReasonId);
            result.setRejectionReasonName(rejectionReasonName);
        }

        // [KN-306]: Tạo bản ghi CandidateActivity để lưu nhật ký chuyển giai đoạn
        CandidateActivity activity = new CandidateActivity();
        activity.setId(SecurityUtil.generateUUID());
        activity.setCandidateId(candidate.getId());
        activity.setActivityType("STATUS_CHANGE");
        activity.setTitle("Chuyển giai đoạn: " + oldStageName + " → " + targetStageName);
        activity.setPerformedBy(currentUser.getId());
        activity.setPerformedByName(currentUser.getFullName());
        activity.setActivityDate(new Timestamp(System.currentTimeMillis()));

        StringBuilder desc = new StringBuilder();
        if ("REJECTED".equals(normalizedTarget)) {
            desc.append("Chuyển sang Loại hồ sơ. Lý do: ").append(rejectionReasonName);
        } else if (isSkippingInterview) {
            desc.append("Đặc cách bỏ qua giai đoạn Phỏng vấn bởi Trưởng phòng Nhân sự: ")
                .append(currentUser.getFullName());
            if (confirmationNote != null && !confirmationNote.trim().isEmpty()) {
                desc.append(". Lý do đặc cách: ").append(confirmationNote.trim());
            }
        } else {
            desc.append("Chuyển giai đoạn tuyển dụng từ [").append(oldStageName)
                .append("] sang [").append(targetStageName).append("]");
        }
        activity.setDescription(desc.toString());

        JsonObject meta = new JsonObject();
        meta.addProperty("oldStage", oldStage);
        meta.addProperty("oldStageName", oldStageName);
        meta.addProperty("newStage", normalizedTarget);
        meta.addProperty("newStageName", targetStageName);
        if (rejectionReasonName != null) {
            meta.addProperty("rejectionReasonId", finalRejectionReasonId);
            meta.addProperty("rejectionReasonName", rejectionReasonName);
        }
        if (isSkippingInterview) {
            meta.addProperty("skipInterview", true);
            meta.addProperty("skippedInterview", true);
            meta.addProperty("confirmedBy", currentUser.getId());
            meta.addProperty("confirmedByName", currentUser.getFullName());
            meta.addProperty("confirmationNote", confirmationNote != null ? confirmationNote.trim() : "");
        }
        activity.setMetadataJson(meta.toString());

        // Cập nhật trạng thái và lưu nhật ký trong CÙNG MỘT GIAO DỊCH
        boolean ok = candidateDAO.updateStatusAndRecordActivity(candidate.getId(), normalizedTarget, finalRejectionReasonId, activity);
        if (!ok) {
            result.setSuccess(false);
            result.setStatusCode(500);
            result.setMessage("Không thể cập nhật trạng thái ứng viên và ghi nhận nhật ký vào cơ sở dữ liệu.");
            return result;
        }

        // Ghi Audit Log
        try {
            auditService.log(
                    currentUser.getId(),
                    "UPDATE_CANDIDATE_STAGE",
                    "CANDIDATE",
                    candidate.getId(),
                    "Chuyển giai đoạn ứng viên " + candidate.getFullName() + " (" + candidate.getEmail() + ") từ [" + oldStage + "] sang [" + normalizedTarget + "]"
                            + (rejectionReasonName != null ? " - Lý do loại: " + rejectionReasonName : "")
                            + (isSkippingInterview ? " - Đặc cách bỏ qua phỏng vấn bởi: " + currentUser.getFullName() : ""),
                    clientIp,
                    userAgent
            );
        } catch (Exception e) {
            // Không làm gián đoạn luồng chính nếu audit log gặp cảnh báo
        }

        result.setSuccess(true);
        result.setStatusCode(200);
        result.setMessage("Đã chuyển ứng viên \"" + candidate.getFullName() + "\" sang giai đoạn " + targetStageName + " thành công!");
        return result;
    }

    /**
     * [KN-305]: Kiểm tra hành động chuyển giai đoạn có bỏ qua giai đoạn Phỏng vấn hay không
     */
    public boolean isSkippingInterview(String oldStage, String targetStage) {
        if (oldStage == null || targetStage == null) return false;
        String oldNorm = oldStage.trim().toUpperCase(Locale.ROOT);
        String targetNorm = targetStage.trim().toUpperCase(Locale.ROOT);
        return ("APPLIED".equals(oldNorm) || "SCREENING".equals(oldNorm))
                && ("OFFER".equals(targetNorm) || "HIRED".equals(targetNorm));
    }

    /**
     * Lấy tên hiển thị tiếng Việt của từng giai đoạn Pipeline
     */
    public String getStageName(String stageCode) {
        if (stageCode == null) return "Chưa xác định";
        switch (stageCode.trim().toUpperCase(Locale.ROOT)) {
            case "APPLIED": return "Hồ sơ mới";
            case "SCREENING": return "Sàng lọc";
            case "INTERVIEWING": return "Phỏng vấn";
            case "OFFER": return "Đề xuất offer";
            case "HIRED": return "Nhận việc";
            case "REJECTED": return "Loại";
            default: return stageCode;
        }
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

    /**
     * [KN-282 / KN-301]: Kiểm tra quyền chuyển giai đoạn / cập nhật ứng viên:
     * - ADMIN và HR_MANAGER có toàn quyền.
     * - RECRUITER: Phải có quyền candidates.manage hoặc vai trò RECRUITER VÀ phải được phân công phụ trách vị trí đó.
     */
    public boolean canManageCandidate(com.irms.model.User user, Candidate candidate) {
        if (user == null || candidate == null) return false;
        if (user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")) return true;

        if (!user.hasPermission("candidates.manage") && !user.hasRole("RECRUITER")) {
            return false;
        }

        String requisitionId = candidate.getRequisitionId();
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            return user.hasRole("ADMIN") || user.hasRole("HR_MANAGER");
        }

        if (user.hasRole("RECRUITER")) {
            com.irms.dao.RecruiterAssignmentDAO assignmentDAO = new com.irms.dao.RecruiterAssignmentDAO();
            return assignmentDAO.isRecruiterAssigned(requisitionId, user.getId());
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

    // =========================================================================
    // [KN-288]: CÁC CHỨC NĂNG THÊM ỨNG VIÊN THỦ CÔNG & KIỂM TRA TRÙNG
    // =========================================================================

    public static class DuplicateMatch {
        private final Candidate candidate;
        private final List<String> matchedFields;
        private final boolean sameName;
        private final boolean samePosition;

        public DuplicateMatch(Candidate candidate, List<String> matchedFields, boolean sameName, boolean samePosition) {
            this.candidate = candidate;
            this.matchedFields = matchedFields;
            this.sameName = sameName;
            this.samePosition = samePosition;
        }

        public Candidate getCandidate() { return candidate; }
        public List<String> getMatchedFields() { return matchedFields; }
        public boolean isSameName() { return sameName; }
        public boolean isSamePosition() { return samePosition; }
    }

    public static class DuplicateCandidateException extends Exception {
        private final List<DuplicateMatch> matches;

        public DuplicateCandidateException(List<DuplicateMatch> matches) {
            super("Đã tồn tại hồ sơ có cùng email hoặc số điện thoại trong hệ thống.");
            this.matches = matches;
        }

        public List<DuplicateMatch> getMatches() {
            return matches;
        }
    }

    /**
     * [KN-346, KN-352]: Rà soát và đối sánh dữ liệu trùng theo hồ sơ hiện có
     */
    public List<DuplicateMatch> checkDuplicates(String email, String phone, String fullName, String requisitionId) {
        List<Candidate> duplicates = candidateDAO.findDuplicates(email, phone);
        List<DuplicateMatch> matches = new java.util.ArrayList<>();
        String normEmail = (email != null) ? email.trim().toLowerCase(Locale.ROOT) : "";
        String normPhone = CandidateDAO.normalizePhone(phone);
        String normName = (fullName != null) ? fullName.trim() : "";

        for (Candidate existing : duplicates) {
            List<String> fields = new java.util.ArrayList<>();
            if (!normEmail.isEmpty() && existing.getEmail() != null && normEmail.equalsIgnoreCase(existing.getEmail().trim())) {
                fields.add("email");
            }
            if (!normPhone.isEmpty() && existing.getPhone() != null && normPhone.equals(CandidateDAO.normalizePhone(existing.getPhone()))) {
                fields.add("phone");
            }
            boolean sameName = !normName.isEmpty() && existing.getFullName() != null && normName.equalsIgnoreCase(existing.getFullName().trim());
            boolean samePosition = requisitionId != null && !requisitionId.trim().isEmpty() && requisitionId.trim().equals(existing.getRequisitionId());

            matches.add(new DuplicateMatch(existing, fields, sameName, samePosition));
        }
        return matches;
    }

    /**
     * [KN-347, KN-350, KN-353]: Tạo mới ứng viên thủ công kèm lưu trữ CV và ghi nhận lịch sử
     */
    public Candidate createManualCandidate(Candidate candidate, javax.servlet.http.Part cvPart, String uploadRootPath,
                                           String clientIp, String userAgent, com.irms.model.User currentUser,
                                           boolean allowDuplicate)
            throws DuplicateCandidateException, java.io.IOException, IllegalArgumentException {
        if (candidate == null) {
            throw new IllegalArgumentException("Dữ liệu ứng viên không được để trống.");
        }
        String fullName = (candidate.getFullName() != null) ? candidate.getFullName().trim() : "";
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Họ và tên ứng viên là bắt buộc.");
        }
        if (fullName.length() > 120) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 120 ký tự.");
        }
        String email = (candidate.getEmail() != null) ? candidate.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        if (email.isEmpty() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không đúng định dạng chuẩn.");
        }
        String phone = (candidate.getPhone() != null) ? candidate.getPhone().trim() : "";
        String normPhone = CandidateDAO.normalizePhone(phone);
        if (normPhone.length() < 8 || normPhone.length() > 15) {
            throw new IllegalArgumentException("Số điện thoại phải có từ 8 đến 15 chữ số.");
        }

        // 1. Kiểm tra trùng lặp (KN-346, KN-352)
        List<DuplicateMatch> matches = checkDuplicates(email, phone, fullName, candidate.getRequisitionId());
        if (!matches.isEmpty() && !allowDuplicate) {
            recordHistory("DUPLICATE_BLOCKED", null, fullName, email, phone,
                    candidate.getApplicationSourceName(), cvPart != null && cvPart.getSize() > 0,
                    "Phát hiện trùng với " + matches.size() + " hồ sơ hiện có.", clientIp);
            throw new DuplicateCandidateException(matches);
        }

        // 2. Xử lý lưu trữ CV nếu có tải lên (KN-349, KN-350)
        String trackingCode = (candidate.getTrackingCode() != null && !candidate.getTrackingCode().trim().isEmpty())
                ? candidate.getTrackingCode().trim()
                : generateUniqueTrackingCode();
        candidate.setTrackingCode(trackingCode);

        if (cvPart != null && cvPart.getSize() > 0) {
            CVFileStorageService storageService = new CVFileStorageService();
            CVFileStorageService.ValidationResult validation = storageService.validate(cvPart);
            if (!validation.isValid()) {
                throw new IllegalArgumentException(validation.getErrorMessage());
            }
            String cvPath = storageService.storeCV(cvPart, uploadRootPath, trackingCode);
            candidate.setCvUrl(cvPath);
        }

        if (candidate.getId() == null || candidate.getId().trim().isEmpty()) {
            candidate.setId(SecurityUtil.generateUUID());
        }
        if (candidate.getStatus() == null || candidate.getStatus().trim().isEmpty()) {
            candidate.setStatus("APPLIED");
        }

        // 3. Lưu vào cơ sở dữ liệu (KN-347)
        boolean ok = candidateDAO.insertApplication(candidate);
        if (!ok) {
            throw new IllegalStateException("Không thể lưu thông tin ứng viên vào cơ sở dữ liệu.");
        }

        // 4. Ghi nhận lịch sử xử lý (KN-353)
        String action = allowDuplicate && !matches.isEmpty() ? "CREATED_DESPITE_DUPLICATE" : "MANUAL_CREATED";
        String detail = allowDuplicate && !matches.isEmpty()
                ? "Người dùng xác nhận tạo mới dù trùng thông tin với " + matches.size() + " hồ sơ hiện có."
                : "Thêm ứng viên thủ công thành công.";

        recordHistory(action, candidate.getId(), fullName, email, phone,
                candidate.getApplicationSourceName(), candidate.getCvUrl() != null, detail, clientIp);

        if (currentUser != null) {
            try {
                com.irms.dao.AuditDAO auditDAO = new com.irms.dao.AuditDAO();
                com.irms.model.AuditLog log = new com.irms.model.AuditLog();
                log.setId(SecurityUtil.generateUUID());
                log.setUserId(currentUser.getId());
                log.setAction(action);
                log.setEntityType("CANDIDATE");
                log.setEntityId(candidate.getId());
                log.setDescription(detail + " Ứng viên: " + fullName + " (" + email + ")");
                log.setIpAddress(clientIp);
                log.setUserAgent(userAgent);
                auditDAO.insert(log);
            } catch (Exception ignored) {}
        }

        return candidate;
    }

    public void recordHistory(String action, String candidateId, String fullName, String email,
                              String phone, String sourceName, boolean hasCv, String details,
                              String clientIp) {
        candidateDAO.insertHistory(action, candidateId, fullName, email, phone, sourceName, hasCv, details, clientIp);
    }
}

