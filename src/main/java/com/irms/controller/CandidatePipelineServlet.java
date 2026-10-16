package com.irms.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irms.dao.CandidateDAO;
import com.irms.dao.RecruiterAssignmentDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.Candidate;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.StageTransitionResult;
import com.irms.model.User;
import com.irms.service.AuditService;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN PIPELINE TUYỂN DỤNG DẠNG KANBAN (CandidatePipelineServlet)
 * ==============================================================================
 * Phục vụ Jira Issue KN-282 & KN-283 và các Subtasks:
 * - KN-300: [BE] Thiết kế API lấy dữ liệu Pipeline Kanban theo vị trí tuyển dụng
 * - KN-299: [FE] Bộ lọc vị trí tuyển dụng cho Pipeline Kanban
 * - KN-301: [BE] Xử lý cập nhật giai đoạn ứng viên bằng kéo thả
 * - KN-302: [FE] Xây dựng giao diện bảng Kanban pipeline tuyển dụng
 * - KN-303: [FE] Hiển thị số lượng ứng viên theo từng giai đoạn
 * - KN-304: [BE] Xác định phạm vi dữ liệu mặc định theo vị trí recruiter phụ trách
 * - KN-305: [BE] Kiểm tra điều kiện bỏ qua giai đoạn Phỏng vấn trước khi chuyển sang Đề xuất offer
 * - KN-306: [BE] Lưu nhật ký mỗi lần chuyển giai đoạn ứng viên
 * - KN-309: [BE] Ràng buộc chuyển sang trạng thái Loại phải chọn lý do từ danh mục
 * - KN-310: [BE] Xử lý xác nhận của Trưởng phòng Nhân sự cho trường hợp bỏ qua Phỏng vấn
 * - KN-311: [BE] Thiết kế API lấy danh mục lý do loại ứng viên
 *
 * URLs:
 *   GET  /candidates/pipeline               — Giao diện bảng Kanban Pipeline
 *   GET  /api/candidates/pipeline           — API lấy dữ liệu Kanban theo vị trí
 *   GET  /api/candidates/rejection-reasons  — API lấy danh mục lý do loại ứng viên (KN-311)
 *   POST /api/candidates/pipeline/update-stage — API cập nhật giai đoạn khi kéo thả (KN-283)
 *   POST /candidates/pipeline/update-stage  — Alias cập nhật giai đoạn
 * ==============================================================================
 */
@WebServlet(name = "CandidatePipelineServlet", urlPatterns = {
        "/candidates/pipeline",
        "/api/candidates/pipeline",
        "/api/candidates/pipeline/update-stage",
        "/candidates/pipeline/update-stage",
        "/api/candidates/rejection-reasons"
})
public class CandidatePipelineServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CandidatePipelineServlet.class.getName());

    private final CandidateService candidateService = new CandidateService();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();
    private final RecruiterAssignmentDAO assignmentDAO = new RecruiterAssignmentDAO();
    private final SharedCatalogService catalogService = new SharedCatalogService();
    private final SharedCatalogDAO catalogDAO = new SharedCatalogDAO();
    private final AuditService auditService = new AuditService();
    private final Gson gson = new Gson();

    /**
     * Định nghĩa 6 giai đoạn chuẩn của Pipeline tuyển dụng (theo đúng thứ tự nghiệp vụ KN-282):
     * 1. Hồ sơ mới (APPLIED)
     * 2. Sàng lọc (SCREENING)
     * 3. Phỏng vấn (INTERVIEWING)
     * 4. Đề xuất offer (OFFER)
     * 5. Nhận việc (HIRED)
     * 6. Loại (REJECTED)
     */
    public static final List<StageDefinition> PIPELINE_STAGES = Arrays.asList(
            new StageDefinition("APPLIED", "Hồ sơ mới", "primary", "bi-inbox-fill"),
            new StageDefinition("SCREENING", "Sàng lọc", "info", "bi-funnel-fill"),
            new StageDefinition("INTERVIEWING", "Phỏng vấn", "warning", "bi-calendar-event-fill"),
            new StageDefinition("OFFER", "Đề xuất offer", "purple", "bi-envelope-paper-fill"),
            new StageDefinition("HIRED", "Nhận việc", "success", "bi-check-circle-fill"),
            new StageDefinition("REJECTED", "Loại", "danger", "bi-x-circle-fill")
    );

    public static class StageDefinition {
        private final String code;
        private final String name;
        private final String color;
        private final String icon;

        public StageDefinition(String code, String name, String color, String icon) {
            this.code = code;
            this.name = name;
            this.color = color;
            this.icon = icon;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getColor() { return color; }
        public String getIcon() { return icon; }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String servletPath = request.getServletPath();
        boolean isApi = "/api/candidates/pipeline".equals(servletPath)
                || "/api/candidates/pipeline/update-stage".equals(servletPath)
                || "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (currentUser == null) {
            if (isApi) {
                sendErrorJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục.");
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login?redirect=" + request.getRequestURI());
            }
            return;
        }

        // [KN-311]: API lấy danh mục lý do loại ứng viên (REJECTION_REASON)
        if ("/api/candidates/rejection-reasons".equals(servletPath)) {
            handleRejectionReasonsJson(response);
            return;
        }

        // Kiểm tra quyền xem ứng viên
        boolean canView = currentUser.hasPermission("candidates.view")
                || currentUser.hasPermission("candidates.manage")
                || currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER");

        if (!canView) {
            if (isApi) {
                sendErrorJson(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập dữ liệu Pipeline ứng viên.");
            } else {
                request.setAttribute("errorMessage", "Bạn không có quyền truy cập dữ liệu Pipeline ứng viên.");
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            }
            return;
        }

        boolean isRecruiterOnly = currentUser.hasRole("RECRUITER")
                && !currentUser.hasRole("ADMIN")
                && !currentUser.hasRole("HR_MANAGER");

        // [KN-304]: Lấy danh sách vị trí tuyển dụng mà người dùng được phép xem
        List<RecruitmentRequest> availableRequisitions;
        if (isRecruiterOnly) {
            availableRequisitions = requisitionDAO.findAll(null, null, null, currentUser.getId(), null, null, null, 0, 1000);
        } else {
            availableRequisitions = requisitionDAO.findAll(null, null, null, null, null, null, null, 0, 1000);
        }

        // [KN-304]: Xác định vị trí tuyển dụng được chọn (selectedRequisitionId)
        String requisitionIdParam = request.getParameter("requisitionId");
        String selectedRequisitionId = null;

        if (requisitionIdParam != null && !requisitionIdParam.trim().isEmpty() && !"ALL".equalsIgnoreCase(requisitionIdParam.trim())) {
            String targetId = requisitionIdParam.trim();
            // Nếu là Recruiter thuần túy, bắt buộc kiểm tra thẩm quyền phụ trách vị trí
            if (isRecruiterOnly) {
                boolean isAssigned = assignmentDAO.isRecruiterAssigned(targetId, currentUser.getId());
                if (!isAssigned) {
                    if (isApi) {
                        sendErrorJson(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xem ứng viên của vị trí tuyển dụng này.");
                    } else {
                        request.setAttribute("errorMessage", "Bạn không có quyền xem ứng viên của vị trí tuyển dụng này.");
                        request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                    }
                    return;
                }
            }
            selectedRequisitionId = targetId;
        } else if ("ALL".equalsIgnoreCase(requisitionIdParam)) {
            // Chỉ ADMIN và HR_MANAGER được xem tất cả vị trí mà không giới hạn
            if (isRecruiterOnly) {
                selectedRequisitionId = !availableRequisitions.isEmpty() ? availableRequisitions.get(0).getId() : null;
            } else {
                selectedRequisitionId = "ALL";
            }
        } else {
            // [KN-304]: Quy tắc mặc định khi mở Pipeline lần đầu
            if (isRecruiterOnly) {
                if (!availableRequisitions.isEmpty()) {
                    // Ưu tiên chọn vị trí OPEN đầu tiên mà recruiter này phụ trách
                    RecruitmentRequest defaultReq = null;
                    for (RecruitmentRequest r : availableRequisitions) {
                        if ("OPEN".equalsIgnoreCase(r.getStatus())) {
                            defaultReq = r;
                            break;
                        }
                    }
                    if (defaultReq == null) {
                        defaultReq = availableRequisitions.get(0);
                    }
                    selectedRequisitionId = defaultReq.getId();
                } else {
                    selectedRequisitionId = null;
                }
            } else {
                // Admin / HR Manager: mặc định chọn vị trí tuyển dụng đầu tiên (hoặc ALL)
                if (!availableRequisitions.isEmpty()) {
                    RecruitmentRequest defaultReq = null;
                    for (RecruitmentRequest r : availableRequisitions) {
                        if ("OPEN".equalsIgnoreCase(r.getStatus())) {
                            defaultReq = r;
                            break;
                        }
                    }
                    selectedRequisitionId = defaultReq != null ? defaultReq.getId() : availableRequisitions.get(0).getId();
                } else {
                    selectedRequisitionId = "ALL";
                }
            }
        }

        // Lấy thông tin chi tiết vị trí đang chọn (nếu có)
        RecruitmentRequest currentRequisition = null;
        if (selectedRequisitionId != null && !"ALL".equalsIgnoreCase(selectedRequisitionId)) {
            for (RecruitmentRequest r : availableRequisitions) {
                if (r.getId().equals(selectedRequisitionId)) {
                    currentRequisition = r;
                    break;
                }
            }
            if (currentRequisition == null) {
                currentRequisition = requisitionDAO.findById(selectedRequisitionId);
            }
        }

        String search = request.getParameter("search");
        String recruiterFilter = isRecruiterOnly ? currentUser.getId() : null;

        List<Candidate> candidates;
        if (isRecruiterOnly && availableRequisitions.isEmpty()) {
            candidates = new ArrayList<>();
        } else {
            candidates = candidateService.getCandidatesForPipeline(
                    "ALL".equalsIgnoreCase(selectedRequisitionId) ? null : selectedRequisitionId,
                    recruiterFilter,
                    search
            );
        }

        // Danh mục lý do loại ứng viên (shared_catalogs)
        List<SharedCatalogItem> rejectionReasons = catalogService.getItems(SharedCatalogType.REJECTION_REASON);

        if (isApi) {
            handlePipelineJson(response, candidates, availableRequisitions, currentRequisition, selectedRequisitionId, rejectionReasons, isRecruiterOnly);
        } else {
            request.setAttribute("pipelineStages", PIPELINE_STAGES);
            request.setAttribute("availableRequisitions", availableRequisitions);
            request.setAttribute("selectedRequisitionId", selectedRequisitionId);
            request.setAttribute("currentRequisition", currentRequisition);
            request.setAttribute("candidates", candidates);
            request.setAttribute("rejectionReasons", rejectionReasons);
            request.setAttribute("isRecruiterOnly", isRecruiterOnly);
            request.setAttribute("activeMenu", "candidate-pipeline");
            request.setAttribute("pageTitle", "Quản lý Pipeline Tuyển dụng dạng Kanban (KN-282)");
            request.getRequestDispatcher("/WEB-INF/views/candidates/pipeline.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            sendErrorJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện thao tác.");
            return;
        }

        // [KN-301]: Kiểm tra quyền hạn quản lý ứng viên (candidates.manage hoặc vai trò ADMIN / HR_MANAGER / RECRUITER)
        boolean canManage = currentUser.hasPermission("candidates.manage")
                || currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER");

        if (!canManage) {
            sendErrorJson(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền cập nhật trạng thái ứng viên.");
            return;
        }

        String candidateId = null;
        String targetStage = null;
        String rejectionReasonId = null;
        boolean skipInterviewConfirmed = false;
        String confirmationNote = null;

        // Hỗ trợ đọc cả JSON request body và form URL-encoded
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("application/json")) {
            try (BufferedReader reader = request.getReader()) {
                JsonObject jsonObj = gson.fromJson(reader, JsonObject.class);
                if (jsonObj != null) {
                    if (jsonObj.has("candidateId") && !jsonObj.get("candidateId").isJsonNull()) {
                        candidateId = jsonObj.get("candidateId").getAsString();
                    }
                    if (jsonObj.has("targetStage") && !jsonObj.get("targetStage").isJsonNull()) {
                        targetStage = jsonObj.get("targetStage").getAsString();
                    } else if (jsonObj.has("status") && !jsonObj.get("status").isJsonNull()) {
                        targetStage = jsonObj.get("status").getAsString();
                    }
                    if (jsonObj.has("rejectionReasonId") && !jsonObj.get("rejectionReasonId").isJsonNull()) {
                        rejectionReasonId = jsonObj.get("rejectionReasonId").getAsString();
                    }
                    if (jsonObj.has("skipInterviewConfirmed") && !jsonObj.get("skipInterviewConfirmed").isJsonNull()) {
                        skipInterviewConfirmed = jsonObj.get("skipInterviewConfirmed").getAsBoolean();
                    }
                    if (jsonObj.has("confirmationNote") && !jsonObj.get("confirmationNote").isJsonNull()) {
                        confirmationNote = jsonObj.get("confirmationNote").getAsString();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lỗi đọc JSON payload update-stage: " + e.getMessage());
            }
        }

        if (candidateId == null) candidateId = request.getParameter("candidateId");
        if (targetStage == null) targetStage = request.getParameter("targetStage");
        if (targetStage == null) targetStage = request.getParameter("status");
        if (rejectionReasonId == null) rejectionReasonId = request.getParameter("rejectionReasonId");
        if (!skipInterviewConfirmed && "true".equalsIgnoreCase(request.getParameter("skipInterviewConfirmed"))) {
            skipInterviewConfirmed = true;
        }
        if (confirmationNote == null) confirmationNote = request.getParameter("confirmationNote");

        // [KN-283]: Chuyển giao toàn bộ nghiệp vụ kiểm tra cho candidateService.transitionStage
        StageTransitionResult result = candidateService.transitionStage(
                currentUser,
                candidateId,
                targetStage,
                rejectionReasonId,
                skipInterviewConfirmed,
                confirmationNote,
                SecurityUtil.getClientIp(request),
                request.getHeader("User-Agent")
        );

        if (!result.isSuccess()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("statusCode", result.getStatusCode());
            err.put("message", result.getMessage());
            err.put("requiresHrApproval", result.isRequiresHrApproval());
            err.put("canCurrentUserApprove", result.isCanCurrentUserApprove());
            err.put("candidateId", result.getCandidateId());
            err.put("candidateName", result.getCandidateName());
            err.put("oldStage", result.getOldStage());
            err.put("oldStageName", result.getOldStageName());
            err.put("newStage", result.getNewStage());
            err.put("newStageName", result.getNewStageName());
            sendErrorJson(response, result.getStatusCode(), err);
            return;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("statusCode", 200);
        res.put("message", result.getMessage());
        res.put("candidateId", result.getCandidateId());
        res.put("candidateName", result.getCandidateName());
        res.put("oldStage", result.getOldStage());
        res.put("oldStageName", result.getOldStageName());
        res.put("newStage", result.getNewStage());
        res.put("newStageName", result.getNewStageName());
        res.put("skipInterview", result.isSkipInterview());
        res.put("rejectionReasonId", result.getRejectionReasonId());
        res.put("rejectionReasonName", result.getRejectionReasonName());
        sendSuccessJson(response, res);
    }

    /**
     * [KN-300 / KN-303]: Xuất dữ liệu Pipeline Kanban thành định dạng JSON chuẩn
     */
    private void handlePipelineJson(HttpServletResponse response,
                                    List<Candidate> candidates,
                                    List<RecruitmentRequest> availableRequisitions,
                                    RecruitmentRequest currentRequisition,
                                    String selectedRequisitionId,
                                    List<SharedCatalogItem> rejectionReasons,
                                    boolean isRecruiterOnly) throws IOException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        // Nhóm ứng viên theo 6 giai đoạn
        Map<String, List<Map<String, Object>>> stageCandidatesMap = new HashMap<>();
        for (StageDefinition def : PIPELINE_STAGES) {
            stageCandidatesMap.put(def.getCode(), new ArrayList<>());
        }

        for (Candidate c : candidates) {
            String st = c.getStatus() != null ? c.getStatus().trim().toUpperCase(Locale.ROOT) : "APPLIED";
            if (stageCandidatesMap.containsKey(st)) {
                Map<String, Object> candDto = new HashMap<>();
                candDto.put("id", c.getId());
                candDto.put("fullName", c.getFullName());
                candDto.put("email", c.getEmail());
                candDto.put("phone", c.getPhone() != null ? c.getPhone() : "");
                candDto.put("requisitionId", c.getRequisitionId() != null ? c.getRequisitionId() : "");
                candDto.put("requisitionTitle", c.getRequisitionTitle() != null ? c.getRequisitionTitle() : "Chưa gắn vị trí");
                candDto.put("status", st);
                candDto.put("statusLabel", c.getStatusLabel());
                candDto.put("trackingCode", c.getTrackingCode() != null ? c.getTrackingCode() : "");
                candDto.put("cvUrl", c.getCvUrl() != null ? c.getCvUrl() : "");
                candDto.put("applicationSourceName", c.getApplicationSourceName() != null ? c.getApplicationSourceName() : "");
                candDto.put("rejectionReasonName", c.getRejectionReasonName() != null ? c.getRejectionReasonName() : "");
                candDto.put("recruiterName", c.getRecruiterName() != null ? c.getRecruiterName() : "");
                candDto.put("tags", c.getTags() != null ? c.getTags() : "");
                candDto.put("tagList", c.getTagList());
                candDto.put("isSlaOverdue", c.isSlaOverdue());
                candDto.put("daysInCurrentStage", c.getDaysInCurrentStage());
                candDto.put("slaDaysThreshold", c.getSlaDaysThreshold());
                candDto.put("createdAtFormatted", c.getCreatedAt() != null ? dateFormat.format(c.getCreatedAt()) : "");
                stageCandidatesMap.get(st).add(candDto);
            }
        }

        // Tạo mảng stages theo đúng thứ tự 6 giai đoạn quy định
        List<Map<String, Object>> stagesList = new ArrayList<>();
        int totalActiveCandidates = 0;

        for (StageDefinition def : PIPELINE_STAGES) {
            List<Map<String, Object>> list = stageCandidatesMap.get(def.getCode());
            Map<String, Object> stageData = new LinkedHashMap<>();
            stageData.put("code", def.getCode());
            stageData.put("name", def.getName());
            stageData.put("color", def.getColor());
            stageData.put("icon", def.getIcon());
            stageData.put("count", list.size());
            stageData.put("candidates", list);
            stagesList.add(stageData);

            if (!"REJECTED".equals(def.getCode())) {
                totalActiveCandidates += list.size();
            }
        }

        // Tạo danh sách availableRequisitions DTO
        List<Map<String, Object>> reqDtoList = new ArrayList<>();
        for (RecruitmentRequest r : availableRequisitions) {
            Map<String, Object> rDto = new HashMap<>();
            rDto.put("id", r.getId());
            rDto.put("code", r.getCode());
            rDto.put("title", r.getTitle());
            rDto.put("positionTitle", r.getPositionTitle());
            rDto.put("departmentName", r.getDepartmentName() != null ? r.getDepartmentName() : "");
            rDto.put("status", r.getStatus());
            rDto.put("headcount", r.getHeadcount());
            reqDtoList.add(rDto);
        }

        // Tạo danh sách rejectionReasons DTO
        List<Map<String, Object>> rejDtoList = new ArrayList<>();
        for (SharedCatalogItem item : rejectionReasons) {
            Map<String, Object> rejDto = new HashMap<>();
            rejDto.put("id", item.getId());
            rejDto.put("value", item.getValue());
            rejDtoList.add(rejDto);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("success", true);
        root.put("selectedRequisitionId", selectedRequisitionId);
        root.put("isRecruiterOnly", isRecruiterOnly);

        if (currentRequisition != null) {
            Map<String, Object> curDto = new HashMap<>();
            curDto.put("id", currentRequisition.getId());
            curDto.put("code", currentRequisition.getCode());
            curDto.put("title", currentRequisition.getTitle());
            curDto.put("positionTitle", currentRequisition.getPositionTitle());
            curDto.put("departmentName", currentRequisition.getDepartmentName() != null ? currentRequisition.getDepartmentName() : "");
            curDto.put("headcount", currentRequisition.getHeadcount());
            curDto.put("status", currentRequisition.getStatus());
            curDto.put("deadline", currentRequisition.getDeadline() != null ? currentRequisition.getDeadline().toString() : "");
            root.put("currentRequisition", curDto);
        } else {
            root.put("currentRequisition", null);
        }

        root.put("availableRequisitions", reqDtoList);
        root.put("stages", stagesList);
        root.put("rejectionReasons", rejDtoList);
        root.put("totalCandidates", candidates.size());
        root.put("totalActiveCandidates", totalActiveCandidates);

        sendSuccessJson(response, root);
    }

    private void sendSuccessJson(HttpServletResponse response, Map<String, Object> data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.getWriter().write(gson.toJson(data));
    }

    private void sendErrorJson(HttpServletResponse response, int statusCode, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", message);
        sendErrorJson(response, statusCode, err);
    }

    private void sendErrorJson(HttpServletResponse response, int statusCode, Map<String, Object> errData) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.getWriter().write(gson.toJson(errData));
    }

    /**
     * [KN-311]: API lấy danh mục lý do loại hồ sơ ứng viên (REJECTION_REASON)
     */
    private void handleRejectionReasonsJson(HttpServletResponse response) throws IOException {
        List<SharedCatalogItem> reasons = catalogDAO.findAll(SharedCatalogType.REJECTION_REASON);
        List<Map<String, Object>> list = new ArrayList<>();
        for (SharedCatalogItem r : reasons) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("name", r.getValue());
            map.put("value", r.getValue());
            map.put("displayOrder", r.getDisplayOrder());
            list.add(map);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("reasons", list);
        res.put("total", list.size());
        sendSuccessJson(response, res);
    }
}
