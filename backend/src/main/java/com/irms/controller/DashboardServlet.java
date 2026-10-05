package com.irms.controller;

import com.irms.dao.AuditDAO;
import com.irms.dao.CandidateDAO;
import com.irms.dao.DepartmentDAO;
import com.irms.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller hiển thị Bảng điều khiển Tổng quan (Dashboard)
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", ""})
public class DashboardServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Thống kê số lượng chỉ số KPI
        int totalUsers = userDAO.countAll(null, null, null);
        int totalCandidates = candidateDAO.countTotal();
        int totalDepts = departmentDAO.findAll().size();
        int totalAuditLogs = auditDAO.countAll(null);

        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalCandidates", totalCandidates);
        request.setAttribute("totalDepts", totalDepts);
        request.setAttribute("totalAuditLogs", totalAuditLogs);

        // Danh sách hoạt động gần đây
        request.setAttribute("recentCandidates", candidateDAO.findAll(null, null));
        request.setAttribute("recentLogs", auditDAO.findAll(null, 0, 5));

        request.getRequestDispatcher("/WEB-INF/views/dashboard/index.jsp").forward(request, response);
    }
}
