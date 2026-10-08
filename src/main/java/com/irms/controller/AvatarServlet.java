package com.irms.controller;

import com.irms.dao.UserDAO;
import com.irms.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet xử lý cập nhật & tải lên ảnh đại diện người dùng
 */
@WebServlet(name = "AvatarServlet", urlPatterns = {"/user/avatar"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2, // 2MB
        maxFileSize = 1024 * 1024 * 10,      // 10MB
        maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class AvatarServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AvatarServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        String avatarType = request.getParameter("avatarType"); // "file", "url", "preset"
        String avatarUrl = null;

        try {
            if ("url".equals(avatarType)) {
                String inputUrl = request.getParameter("avatarUrlInput");
                if (inputUrl != null && !inputUrl.trim().isEmpty()) {
                    avatarUrl = inputUrl.trim();
                }
            } else if ("preset".equals(avatarType)) {
                String presetUrl = request.getParameter("presetAvatarUrl");
                if (presetUrl != null && !presetUrl.trim().isEmpty()) {
                    avatarUrl = presetUrl.trim();
                }
            } else {
                // File Upload
                Part filePart = request.getPart("avatarFile");
                if (filePart != null && filePart.getSize() > 0) {
                    String fileName = extractFileName(filePart);
                    String ext = ".jpg";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) {
                        ext = fileName.substring(i).toLowerCase();
                    }

                    // Kiểm tra định dạng hợp lệ
                    if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".gif") && !ext.equals(".webp") && !ext.equals(".svg")) {
                        session.setAttribute("flashError", "Định dạng tệp không hợp lệ! Vui lòng chọn ảnh JPG, PNG, WEBP hoặc SVG.");
                        redirectBack(request, response);
                        return;
                    }

                    String uploadDir = getServletContext().getRealPath("") + File.separator + "uploads" + File.separator + "avatars";
                    File dir = new File(uploadDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    String newFileName = "avatar_" + currentUser.getId() + "_" + System.currentTimeMillis() + ext;
                    String filePath = uploadDir + File.separator + newFileName;
                    filePart.write(filePath);

                    avatarUrl = request.getContextPath() + "/uploads/avatars/" + newFileName;
                }
            }

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                boolean updated = userDAO.updateAvatar(currentUser.getId(), avatarUrl);
                if (updated) {
                    currentUser.setAvatarUrl(avatarUrl);
                    session.setAttribute("currentUser", currentUser);
                    session.setAttribute("flashSuccess", "Cập nhật ảnh đại diện thành công!");
                } else {
                    session.setAttribute("flashError", "Không thể lưu thông tin ảnh đại diện vào cơ sở dữ liệu!");
                }
            } else {
                session.setAttribute("flashError", "Vui lòng chọn tệp ảnh hoặc nhập liên kết ảnh hợp lệ.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật ảnh đại diện người dùng", e);
            session.setAttribute("flashError", "Đã xảy ra lỗi khi tải ảnh: " + e.getMessage());
        }

        redirectBack(request, response);
    }

    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp != null) {
            String[] items = contentDisp.split(";");
            for (String s : items) {
                if (s.trim().startsWith("filename")) {
                    String clientFileName = s.substring(s.indexOf('=') + 1).trim().replace("\"", "");
                    int slashIdx = Math.max(clientFileName.lastIndexOf('/'), clientFileName.lastIndexOf('\\'));
                    if (slashIdx >= 0) {
                        return clientFileName.substring(slashIdx + 1);
                    }
                    return clientFileName;
                }
            }
        }
        return "avatar.jpg";
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}
