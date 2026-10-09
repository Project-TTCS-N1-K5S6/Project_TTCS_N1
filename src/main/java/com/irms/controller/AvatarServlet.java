package com.irms.controller;

import com.irms.dao.UserDAO;
import com.irms.model.User;
import com.irms.util.ImageUtil;

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
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * Servlet xử lý cập nhật & tải lên ảnh đại diện người dùng (AvatarServlet)
 * ==============================================================================
 * Tiêu chí nghiệm thu:
 * - Chấp nhận JPG/PNG tối đa 2MB.
 * - Ảnh được cắt vuông và tạo bản thu nhỏ.
 * ==============================================================================
 */
@WebServlet(name = "AvatarServlet", urlPatterns = {"/user/avatar"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 512,      // 512 KB
        maxFileSize = 1024 * 1024 * 2,       // Tối đa 2MB theo đúng tiêu chí
        maxRequestSize = 1024 * 1024 * 4     // 4MB
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
                // ==============================================================
                // [Tiêu chí 1 & 2]: Xử lý tải ảnh đại diện từ máy tính
                // ==============================================================
                Part filePart = null;
                try {
                    filePart = request.getPart("avatarFile");
                } catch (Exception ex) {
                    session.setAttribute("flashError", "Dung lượng ảnh vượt quá giới hạn tối đa 2MB! Vui lòng chọn ảnh nhỏ hơn.");
                    redirectBack(request, response);
                    return;
                }

                if (filePart != null && filePart.getSize() > 0) {
                    // 1. Kiểm tra kích thước tệp tối đa 2MB
                    if (filePart.getSize() > ImageUtil.MAX_FILE_SIZE) {
                        session.setAttribute("flashError", "Dung lượng ảnh vượt quá giới hạn tối đa 2MB! Vui lòng chọn ảnh nhỏ hơn.");
                        redirectBack(request, response);
                        return;
                    }

                    // 2. Kiểm tra định dạng tệp (chỉ chấp nhận JPG và PNG)
                    String fileName = extractFileName(filePart);
                    String ext = ".jpg";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) {
                        ext = fileName.substring(i).toLowerCase();
                    }

                    if (!ImageUtil.isValidImageFormat(ext)) {
                        session.setAttribute("flashError", "Định dạng tệp không hợp lệ! Hệ thống chỉ chấp nhận ảnh định dạng JPG hoặc PNG.");
                        redirectBack(request, response);
                        return;
                    }

                    // Thư mục lưu trữ ảnh đại diện
                    String uploadDir = getServletContext().getRealPath("") + File.separator + "uploads" + File.separator + "avatars";
                    File dir = new File(uploadDir);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    // Chuẩn hóa đuôi lưu trữ
                    String saveExt = ext.equals(".png") ? ".png" : ".jpg";
                    String formatName = ext.equals(".png") ? "png" : "jpg";
                    String baseFileName = "avatar_" + currentUser.getId() + "_" + System.currentTimeMillis();
                    String newFileName = baseFileName + saveExt;
                    String thumbFileName = baseFileName + "_thumb" + saveExt;

                    File avatarFile = new File(dir, newFileName);
                    File thumbFile = new File(dir, thumbFileName);

                    // 3. Cắt vuông và tạo bản thu nhỏ (Thumbnail)
                    try (InputStream in = filePart.getInputStream()) {
                        ImageUtil.processAndSaveAvatar(in, avatarFile, thumbFile, formatName);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Lỗi đọc hoặc xử lý ảnh đại diện", e);
                        session.setAttribute("flashError", "Tệp tải lên không phải là ảnh hợp lệ hoặc dữ liệu ảnh bị lỗi.");
                        redirectBack(request, response);
                        return;
                    }

                    avatarUrl = request.getContextPath() + "/uploads/avatars/" + newFileName;
                }
            }

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                boolean updated = userDAO.updateAvatar(currentUser.getId(), avatarUrl);
                if (updated) {
                    currentUser.setAvatarUrl(avatarUrl);
                    session.setAttribute("currentUser", currentUser);
                    session.setAttribute("flashSuccess", "Cập nhật ảnh đại diện thành công (ảnh đã được cắt vuông và tạo bản thu nhỏ)!");
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
