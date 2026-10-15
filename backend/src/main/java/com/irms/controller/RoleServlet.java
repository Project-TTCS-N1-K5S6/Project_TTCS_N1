package com.irms.controller;

import com.irms.dao.RoleDAO;
import com.irms.model.Role;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Controller quản lý danh mục vai trò hệ thống
 */
@WebServlet(name = "RoleServlet", urlPatterns = {"/admin/roles"})
public class RoleServlet extends HttpServlet {
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Role> roles = roleDAO.findAll();
        request.setAttribute("roles", roles);
        request.getRequestDispatcher("/WEB-INF/views/roles/list.jsp").forward(request, response);
    }
}
