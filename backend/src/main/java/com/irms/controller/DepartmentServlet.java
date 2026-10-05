package com.irms.controller;

import com.irms.model.Department;
import com.irms.service.DepartmentService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Controller quản lý danh mục phòng ban và cơ cấu tổ chức
 */
@WebServlet(name = "DepartmentServlet", urlPatterns = {
        "/admin/departments",
        "/admin/departments/create",
        "/admin/departments/edit",
        "/admin/departments/delete"
})
public class DepartmentServlet extends HttpServlet {
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Department> list = departmentService.getAllDepartments();
        request.setAttribute("departments", list);
        request.getRequestDispatcher("/WEB-INF/views/departments/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);

        try {
            switch (path) {
                case "/admin/departments/create": {
                    String code = request.getParameter("code");
                    String name = request.getParameter("name");
                    String desc = request.getParameter("description");
                    departmentService.createDepartment(code, name, desc);
                    if (session != null) session.setAttribute("flashSuccess", "Thêm mới phòng ban thành công!");
                    break;
                }
                case "/admin/departments/edit": {
                    String id = request.getParameter("id");
                    String code = request.getParameter("code");
                    String name = request.getParameter("name");
                    String desc = request.getParameter("description");
                    departmentService.updateDepartment(id, code, name, desc);
                    if (session != null) session.setAttribute("flashSuccess", "Cập nhật thông tin phòng ban thành công!");
                    break;
                }
                case "/admin/departments/delete": {
                    String id = request.getParameter("id");
                    departmentService.deleteDepartment(id);
                    if (session != null) session.setAttribute("flashSuccess", "Đã xóa phòng ban thành công!");
                    break;
                }
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/departments");
    }
}
