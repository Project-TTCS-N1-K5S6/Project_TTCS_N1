package com.irms.controller;

import com.irms.model.CompetencyCriterion;
import com.irms.model.CompetencyFramework;
import com.irms.model.CompetencyFrameworkCriterion;
import com.irms.model.PositionCompetencyFramework;
import com.irms.model.User;
import com.irms.service.CompetencyCriterionService;
import com.irms.service.CompetencyFrameworkService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Controller quản lý Khai báo Khung Năng Lực & Gán chức danh (CompetencyFrameworkServlet)
 */
@WebServlet(name = "CompetencyFrameworkServlet", urlPatterns = {
        "/competency-frameworks",
        "/competency-frameworks/create",
        "/competency-frameworks/edit",
        "/competency-frameworks/detail",
        "/competency-frameworks/status",
        "/competency-frameworks/delete",
        "/competency-frameworks/assign-positions",
        "/competency-frameworks/api/positions",
        "/competency-frameworks/api/criteria",
        "/competency-frameworks/api/quick-create-criterion"
})
public class CompetencyFrameworkServlet extends HttpServlet {

    private final CompetencyFrameworkService frameworkService = new CompetencyFrameworkService();
    private final CompetencyCriterionService criterionService = new CompetencyCriterionService();

    private boolean canView(User user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")
                || user.hasPermission("competencies.view")
                || user.hasPermission("evaluations.manage");
    }

    private boolean canManage(User user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")
                || user.hasPermission("competencies.create")
                || user.hasPermission("competencies.update")
                || user.hasPermission("evaluations.manage");
    }

    private boolean canChangeStatus(User user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")
                || user.hasPermission("competencies.status")
                || user.hasPermission("evaluations.manage");
    }

    private boolean canAssign(User user) {
        if (user == null) return false;
        return user.hasRole("ADMIN") || user.hasRole("HR_MANAGER")
                || user.hasPermission("competencies.assign")
                || user.hasPermission("evaluations.manage");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        if (!canView(currentUser)) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String servletPath = request.getServletPath();
        String action = request.getParameter("action");

        // 1. API AJAX: Lấy danh sách chức danh và trạng thái gắn khung
        if ("/competency-frameworks/api/positions".equals(servletPath) || "get-positions".equals(action)) {
            List<PositionCompetencyFramework> positions = frameworkService.getAllPositionsWithFrameworkInfo();
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, positions);
            return;
        }

        // 2. API AJAX: Lấy danh mục tiêu chí
        if ("/competency-frameworks/api/criteria".equals(servletPath) || "get-criteria".equals(action)) {
            List<CompetencyCriterion> criteria = criterionService.getActiveCriteria();
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, criteria);
            return;
        }

        // 3. API hoặc View: Chi tiết khung năng lực
        if ("/competency-frameworks/detail".equals(servletPath) || "detail".equals(action)) {
            String id = request.getParameter("id");
            CompetencyFramework framework = frameworkService.getFrameworkById(id);
            if (framework == null) {
                if (isAjax(request)) {
                    sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy khung năng lực!");
                } else {
                    session.setAttribute("flashError", "Không tìm thấy khung năng lực yêu cầu!");
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                }
                return;
            }

            if (isAjax(request)) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, framework);
                return;
            }

            request.setAttribute("framework", framework);
            request.getRequestDispatcher("/WEB-INF/views/competencies/detail.jsp").forward(request, response);
            return;
        }

        // 4. Màn hình Tạo mới khung năng lực
        if ("/competency-frameworks/create".equals(servletPath)) {
            if (!canManage(currentUser)) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
            List<CompetencyCriterion> criteriaCatalog = criterionService.getActiveCriteria();
            request.setAttribute("criteriaCatalog", criteriaCatalog);
            request.setAttribute("mode", "create");
            request.getRequestDispatcher("/WEB-INF/views/competencies/form.jsp").forward(request, response);
            return;
        }

        // 5. Màn hình Chỉnh sửa khung năng lực
        if ("/competency-frameworks/edit".equals(servletPath)) {
            if (!canManage(currentUser)) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
            String id = request.getParameter("id");
            CompetencyFramework framework = frameworkService.getFrameworkById(id);
            if (framework == null) {
                session.setAttribute("flashError", "Không tìm thấy khung năng lực cần chỉnh sửa!");
                response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                return;
            }

            List<CompetencyCriterion> criteriaCatalog = criterionService.getActiveCriteria();
            request.setAttribute("framework", framework);
            request.setAttribute("criteriaCatalog", criteriaCatalog);
            request.setAttribute("mode", "edit");
            request.getRequestDispatcher("/WEB-INF/views/competencies/form.jsp").forward(request, response);
            return;
        }

        // 6. Danh sách khung năng lực mặc định (/competency-frameworks)
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        List<CompetencyFramework> frameworks = frameworkService.getAllFrameworks(search, status);
        List<CompetencyFramework> all = frameworkService.getAllFrameworks(null, null);

        int totalCount = all.size();
        int activeCount = 0;
        int draftCount = 0;
        int inactiveCount = 0;
        for (CompetencyFramework cf : all) {
            if (cf.isActive()) activeCount++;
            else if (cf.isDraft()) draftCount++;
            else if (cf.isInactive()) inactiveCount++;
        }

        request.setAttribute("frameworks", frameworks);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("activeCount", activeCount);
        request.setAttribute("draftCount", draftCount);
        request.setAttribute("inactiveCount", inactiveCount);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);

        request.getRequestDispatcher("/WEB-INF/views/competencies/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            if (isAjax(request)) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Phiên đăng nhập đã hết hạn!");
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login");
            }
            return;
        }

        String servletPath = request.getServletPath();
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 1. API Tạo nhanh tiêu chí mới vào ngân hàng
        if ("/competency-frameworks/api/quick-create-criterion".equals(servletPath)) {
            if (!canManage(currentUser)) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Không có quyền thêm tiêu chí!");
                return;
            }
            try {
                String name = request.getParameter("name");
                String code = request.getParameter("code");
                String description = request.getParameter("description");
                String evaluationGuideline = request.getParameter("evaluationGuideline");

                CompetencyCriterion created = criterionService.createCriterion(code, name, description, evaluationGuideline);
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("message", "Thêm mới tiêu chí thành công!");
                res.put("data", created);
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            } catch (Exception e) {
                sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
            return;
        }

        // 2. Kích hoạt / Ngừng áp dụng trạng thái
        if ("/competency-frameworks/status".equals(servletPath)) {
            if (!canChangeStatus(currentUser)) {
                if (isAjax(request)) sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thay đổi trạng thái khung năng lực!");
                else {
                    session.setAttribute("flashError", "Bạn không có quyền thay đổi trạng thái khung năng lực!");
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                }
                return;
            }

            String id = request.getParameter("id");
            String newStatus = request.getParameter("status");
            try {
                frameworkService.changeStatus(id, newStatus, currentUser.getId(), ip, userAgent);
                String msg = CompetencyFramework.STATUS_ACTIVE.equalsIgnoreCase(newStatus)
                        ? "Đã kích hoạt áp dụng khung năng lực thành công!"
                        : (CompetencyFramework.STATUS_INACTIVE.equalsIgnoreCase(newStatus)
                        ? "Đã ngừng áp dụng khung năng lực!"
                        : "Đã chuyển khung năng lực về trạng thái Bản nháp!");

                if (isAjax(request)) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", msg);
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                } else {
                    session.setAttribute("flashSuccess", msg);
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks/detail?id=" + id);
                }
            } catch (Exception e) {
                if (isAjax(request)) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                } else {
                    session.setAttribute("flashError", e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks/detail?id=" + id);
                }
            }
            return;
        }

        // 3. Gán chức danh sử dụng Khung năng lực
        if ("/competency-frameworks/assign-positions".equals(servletPath)) {
            if (!canAssign(currentUser)) {
                if (isAjax(request)) sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền gán chức danh cho khung năng lực!");
                else {
                    session.setAttribute("flashError", "Bạn không có quyền gán chức danh cho khung năng lực!");
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                }
                return;
            }

            String frameworkId = request.getParameter("frameworkId");
            String[] positionIdsArr = request.getParameterValues("positionIds");
            List<String> positionIds = positionIdsArr != null ? Arrays.asList(positionIdsArr) : Collections.emptyList();

            try {
                frameworkService.assignPositions(frameworkId, positionIds, currentUser.getId(), ip, userAgent);
                String msg = "Đã cập nhật danh sách chức danh áp dụng khung năng lực thành công! (" + positionIds.size() + " chức danh)";
                if (isAjax(request)) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", msg);
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                } else {
                    session.setAttribute("flashSuccess", msg);
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks/detail?id=" + frameworkId);
                }
            } catch (Exception e) {
                if (isAjax(request)) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                } else {
                    session.setAttribute("flashError", e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks/detail?id=" + frameworkId);
                }
            }
            return;
        }

        // 4. Xóa khung năng lực
        if ("/competency-frameworks/delete".equals(servletPath)) {
            if (!canManage(currentUser)) {
                session.setAttribute("flashError", "Bạn không có quyền xóa khung năng lực!");
                response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                return;
            }

            String id = request.getParameter("id");
            try {
                frameworkService.deleteFramework(id, currentUser.getId(), ip, userAgent);
                session.setAttribute("flashSuccess", "Đã xóa khung năng lực thành công!");
            } catch (Exception e) {
                session.setAttribute("flashError", e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/competency-frameworks");
            return;
        }

        // 5. Tạo mới hoặc Chỉnh sửa khung năng lực (Lưu form)
        if ("/competency-frameworks/create".equals(servletPath) || "/competency-frameworks/edit".equals(servletPath)) {
            if (!canManage(currentUser)) {
                if (isAjax(request)) sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thao tác khung năng lực!");
                else {
                    session.setAttribute("flashError", "Bạn không có quyền thao tác khung năng lực!");
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks");
                }
                return;
            }

            boolean isEdit = "/competency-frameworks/edit".equals(servletPath);
            String id = request.getParameter("id");
            String code = request.getParameter("code");
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            String status = request.getParameter("status");

            String[] criterionIds = request.getParameterValues("criterionId[]");
            if (criterionIds == null) {
                criterionIds = request.getParameterValues("criterionId");
            }
            String[] weights = request.getParameterValues("weight[]");
            if (weights == null) {
                weights = request.getParameterValues("weight");
            }
            String[] displayOrders = request.getParameterValues("displayOrder[]");
            if (displayOrders == null) {
                displayOrders = request.getParameterValues("displayOrder");
            }

            try {
                CompetencyFramework framework = new CompetencyFramework();
                if (isEdit) {
                    framework.setId(id);
                }
                framework.setCode(code);
                framework.setName(name);
                framework.setDescription(description);
                framework.setStatus(status != null ? status : CompetencyFramework.STATUS_DRAFT);

                List<CompetencyFrameworkCriterion> criteriaList = new ArrayList<>();
                if (criterionIds != null) {
                    for (int i = 0; i < criterionIds.length; i++) {
                        String cId = criterionIds[i];
                        if (cId == null || cId.trim().isEmpty()) continue;

                        String wStr = (weights != null && i < weights.length) ? weights[i] : "0";
                        BigDecimal weightVal;
                        try {
                            weightVal = new BigDecimal(wStr.trim());
                        } catch (Exception ex) {
                            throw new IllegalArgumentException("Trọng số không hợp lệ ở dòng " + (i + 1) + ": " + wStr);
                        }

                        int orderVal = i + 1;
                        if (displayOrders != null && i < displayOrders.length) {
                            try {
                                orderVal = Integer.parseInt(displayOrders[i].trim());
                            } catch (Exception ignored) {}
                        }

                        CompetencyFrameworkCriterion item = new CompetencyFrameworkCriterion();
                        item.setCriterionId(cId.trim());
                        item.setWeight(weightVal);
                        item.setDisplayOrder(orderVal);
                        criteriaList.add(item);
                    }
                }

                CompetencyFramework saved;
                if (isEdit) {
                    saved = frameworkService.updateFramework(framework, criteriaList, currentUser.getId(), ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã cập nhật khung năng lực '" + saved.getCode() + "' thành công!");
                } else {
                    saved = frameworkService.createFramework(framework, criteriaList, currentUser.getId(), ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã tạo mới khung năng lực '" + saved.getCode() + "' thành công!");
                }

                if (isAjax(request)) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", isEdit ? "Cập nhật thành công!" : "Tạo mới thành công!");
                    res.put("redirectUrl", request.getContextPath() + "/competency-frameworks/detail?id=" + saved.getId());
                    res.put("frameworkId", saved.getId());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                } else {
                    response.sendRedirect(request.getContextPath() + "/competency-frameworks/detail?id=" + saved.getId());
                }

            } catch (Exception e) {
                if (isAjax(request)) {
                    sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                } else {
                    session.setAttribute("flashError", e.getMessage());
                    if (isEdit) {
                        response.sendRedirect(request.getContextPath() + "/competency-frameworks/edit?id=" + id);
                    } else {
                        response.sendRedirect(request.getContextPath() + "/competency-frameworks/create");
                    }
                }
            }
        }
    }

    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(xrw) || (accept != null && accept.contains("application/json"));
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("error", message);
        err.put("message", message);
        JsonUtil.sendJsonResponse(response, status, err);
    }
}
