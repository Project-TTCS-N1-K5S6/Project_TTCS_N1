package com.irms.controller;

import com.irms.model.CandidateWithdrawal;
import com.irms.service.CandidateService;
import com.irms.service.EmailService;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "PublicCandidateWithdrawalServlet", urlPatterns = "/careers/withdraw")
public class PublicCandidateWithdrawalServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(PublicCandidateWithdrawalServlet.class.getName());
    private final CandidateService candidateService = new CandidateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setSecurityHeaders(response);
        if ("true".equals(request.getParameter("done"))) {
            request.setAttribute("withdrawalCompleted", true);
            request.getRequestDispatcher("/WEB-INF/views/candidates/withdraw.jsp").forward(request, response);
            return;
        }

        String token = request.getParameter("token");
        try {
            CandidateWithdrawal withdrawal = candidateService.getCandidateForWithdrawal(token);
            if (withdrawal == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("withdrawalError",
                        "Liên kết không hợp lệ, đã hết hạn, đã được sử dụng hoặc hồ sơ không còn trong quy trình.");
            } else {
                request.setAttribute("withdrawal", withdrawal);
                request.setAttribute("withdrawalToken", token);
            }
            request.getRequestDispatcher("/WEB-INF/views/candidates/withdraw.jsp").forward(request, response);
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải trang xác nhận rút hồ sơ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("withdrawalError", "Không thể xác thực liên kết lúc này. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/candidates/withdraw.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        setSecurityHeaders(response);
        String token = request.getParameter("token");
        try {
            CandidateWithdrawal withdrawal = candidateService.withdrawCandidate(
                    token, SecurityUtil.getClientIp(request), request.getHeader("User-Agent"));
            if (withdrawal == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                request.setAttribute("withdrawalError",
                        "Liên kết không hợp lệ, đã hết hạn, đã được sử dụng hoặc hồ sơ không còn trong quy trình.");
                request.getRequestDispatcher("/WEB-INF/views/candidates/withdraw.jsp").forward(request, response);
                return;
            }

            EmailService.triggerOutboxProcessing();
            response.sendRedirect(request.getContextPath() + "/careers/withdraw?done=true");
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Không thể hoàn tất thao tác rút hồ sơ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("withdrawalError", "Không thể hoàn tất yêu cầu lúc này. Vui lòng thử lại sau.");
            request.getRequestDispatcher("/WEB-INF/views/candidates/withdraw.jsp").forward(request, response);
        }
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentType("text/html;charset=UTF-8");
    }
}
