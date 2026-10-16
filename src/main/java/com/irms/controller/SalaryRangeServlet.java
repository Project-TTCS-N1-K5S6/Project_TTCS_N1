package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.model.SalaryRange;
import com.irms.service.SalaryRangeService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Controller quản lý Dải lương vị trí tuyển dụng
 */
@WebServlet(name = "SalaryRangeServlet", urlPatterns = {
        "/salary-ranges",
        "/salary-ranges/create"
})
public class SalaryRangeServlet extends HttpServlet {
    private final SalaryRangeService salaryRangeService = new SalaryRangeService();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<SalaryRange> list = salaryRangeService.getAllSalaryRanges();
        request.setAttribute("salaryRanges", list);
        request.setAttribute("departments", departmentDAO.findAll());
        request.getRequestDispatcher("/WEB-INF/views/salary/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        try {
            SalaryRange sr = new SalaryRange();
            sr.setDepartmentId(request.getParameter("departmentId"));
            sr.setPositionTitle(request.getParameter("positionTitle"));
            sr.setMinSalary(new BigDecimal(request.getParameter("minSalary")));
            sr.setMaxSalary(new BigDecimal(request.getParameter("maxSalary")));
            sr.setCurrency(request.getParameter("currency"));

            salaryRangeService.createSalaryRange(sr);
            if (session != null) session.setAttribute("flashSuccess", "Thêm mới dải lương vị trí thành công!");
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/salary-ranges");
    }
}
