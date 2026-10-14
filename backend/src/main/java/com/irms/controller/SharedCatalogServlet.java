package com.irms.controller;

import com.irms.model.SharedCatalogType;
import com.irms.service.SharedCatalogService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;

@WebServlet(name = "SharedCatalogServlet", urlPatterns = {
        "/admin/shared-catalogs",
        "/admin/shared-catalogs/create",
        "/admin/shared-catalogs/edit",
        "/admin/shared-catalogs/delete",
        "/admin/shared-catalogs/move"
})
public class SharedCatalogServlet extends HttpServlet {
    private final SharedCatalogService catalogService = new SharedCatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String typeCode = request.getParameter("type");
        SharedCatalogType type = SharedCatalogType.APPLICATION_SOURCE;
        if (typeCode != null) {
            try {
                type = SharedCatalogType.fromCode(typeCode);
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                return;
            }
        }
        request.setAttribute("catalogTypes", SharedCatalogType.values());
        request.setAttribute("selectedType", type);
        request.setAttribute("catalogItems", catalogService.getItems(type));
        request.setAttribute("itemCount", catalogService.countItems(type));
        request.getRequestDispatcher("/WEB-INF/views/shared-catalogs/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();
        HttpSession session = request.getSession();
        String typeCode = request.getParameter("type");

        try {
            SharedCatalogType type = SharedCatalogType.fromCode(typeCode);
            switch (path) {
                case "/admin/shared-catalogs/create":
                    requireSuccess(catalogService.create(type, request.getParameter("value")),
                            "Không thể thêm giá trị. Có thể tên này đã tồn tại.");
                    session.setAttribute("flashSuccess", "Đã thêm giá trị danh mục.");
                    break;
                case "/admin/shared-catalogs/edit":
                    requireSuccess(catalogService.update(request.getParameter("id"), type, request.getParameter("value")),
                            "Không thể cập nhật giá trị. Có thể tên này đã tồn tại.");
                    session.setAttribute("flashSuccess", "Đã cập nhật giá trị danh mục.");
                    break;
                case "/admin/shared-catalogs/delete":
                    requireSuccess(catalogService.delete(request.getParameter("id"), type),
                            "Không tìm thấy giá trị danh mục cần xóa.");
                    session.setAttribute("flashSuccess", "Đã xóa giá trị danh mục.");
                    break;
                case "/admin/shared-catalogs/move":
                    requireSuccess(catalogService.move(request.getParameter("id"), type,
                                    request.getParameter("direction")),
                            "Không thể thay đổi thứ tự giá trị danh mục.");
                    session.setAttribute("flashSuccess", "Đã cập nhật thứ tự danh mục.");
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("flashError", e.getMessage());
        }

        String redirectType;
        try {
            redirectType = SharedCatalogType.fromCode(typeCode).getCode();
        } catch (IllegalArgumentException e) {
            redirectType = SharedCatalogType.APPLICATION_SOURCE.getCode();
        }
        String encodedType = URLEncoder.encode(redirectType, "UTF-8");
        response.sendRedirect(request.getContextPath() + "/admin/shared-catalogs?type=" + encodedType);
    }

    private void requireSuccess(boolean success, String message) {
        if (!success) {
            throw new IllegalStateException(message);
        }
    }
}
