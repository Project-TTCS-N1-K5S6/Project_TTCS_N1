package com.irms.controller;

import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.User;
import com.irms.service.CandidateService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "ReferralServlet", urlPatterns = {"/referrals", "/referrals/create"})
public class ReferralServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = session == null ? null : (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        request.setAttribute("referrals", candidateService.getReferralsByUser(currentUser.getId()));
        request.setAttribute("openRequisitions", requisitionDAO.findAll(null, "OPEN", null));
        request.getRequestDispatcher("/WEB-INF/views/referrals/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = session == null ? null : (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        Candidate candidate = new Candidate();
        candidate.setFullName(request.getParameter("fullName"));
        candidate.setEmail(request.getParameter("email"));
        candidate.setPhone(request.getParameter("phone"));
        candidate.setRequisitionId(request.getParameter("requisitionId"));
        candidate.setCvUrl(request.getParameter("cvUrl"));
        candidate.setNotes(request.getParameter("notes"));

        try {
            if (candidateService.createReferral(candidate, currentUser.getId())) {
                session.setAttribute("flashSuccess", "Đã gửi hồ sơ giới thiệu thành công.");
            } else {
                session.setAttribute("flashError", "Không thể lưu hồ sơ giới thiệu. Vui lòng thử lại.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            session.setAttribute("flashError", e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/referrals");
    }
}
