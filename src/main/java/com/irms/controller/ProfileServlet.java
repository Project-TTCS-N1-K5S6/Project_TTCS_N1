package com.irms.controller;

import com.irms.model.User;
import com.irms.service.ProfileService;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "ProfileServlet", urlPatterns = "/profile")
public class ProfileServlet extends HttpServlet {
    private final ProfileService profileService = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        User profile = profileService.getProfile(currentUser.getId());
        if (profile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("profile", profile);
        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        User currentUser = getCurrentUser(session);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String email = request.getParameter("email");

        try {
            User updatedUser = profileService.updateProfile(
                    currentUser.getId(), fullName, phone, email,
                    SecurityUtil.getClientIp(request), request.getHeader("User-Agent"));
            currentUser.setFullName(updatedUser.getFullName());
            currentUser.setPhone(updatedUser.getPhone());
            currentUser.setEmail(updatedUser.getEmail());
            session.setAttribute("currentUser", currentUser);
            session.setAttribute("flashSuccess", "Hồ sơ cá nhân đã được cập nhật.");
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (Exception e) {
            User profile = new User();
            profile.setFullName(fullName);
            profile.setPhone(phone);
            profile.setEmail(email);
            request.setAttribute("profile", profile);
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
        }
    }

    private User getCurrentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}