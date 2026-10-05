package com.irms.controller;

import com.irms.model.Candidate;
import com.irms.service.CandidateService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * Controller quản lý Hồ sơ ứng viên và Pipeline tuyển dụng
 */
@WebServlet(name = "CandidateServlet", urlPatterns = {
        "/candidates",
        "/candidates/create",
        "/candidates/update-status"
})
public class CandidateServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        List<Candidate> list = candidateService.getCandidates(search, status);
        request.setAttribute("candidates", list);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);

        request.getRequestDispatcher("/WEB-INF/views/candidates/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);

        try {
            if ("/candidates/create".equals(path)) {
                Candidate c = new Candidate();
                c.setFullName(request.getParameter("fullName"));
                c.setEmail(request.getParameter("email"));
                c.setPhone(request.getParameter("phone"));
                c.setStatus(request.getParameter("status"));
                c.setCvUrl(request.getParameter("cvUrl"));
                c.setNotes(request.getParameter("notes"));
                candidateService.createCandidate(c);
                if (session != null) session.setAttribute("flashSuccess", "Thêm hồ sơ ứng viên thành công!");
            } else if ("/candidates/update-status".equals(path)) {
                String id = request.getParameter("candidateId");
                String newStatus = request.getParameter("status");
                candidateService.updateStatus(id, newStatus);
                if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }
}
