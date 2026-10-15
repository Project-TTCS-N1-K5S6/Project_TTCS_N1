package com.irms.controller;

import com.irms.model.CompanyProfile;
import com.irms.model.User;
import com.irms.service.CompanyProfileService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller Cấu hình trang giới thiệu công ty & Trang công khai
 * - Quản trị: /admin/company-profile
 * - Công khai (Public About Us): /about-us, /company/about
 */
@WebServlet(name = "CompanyProfileServlet", urlPatterns = {
        "/admin/company-profile",
        "/admin/company-profile/save",
        "/admin/company-profile/upload",
        "/admin/company-profile/api/preview",
        "/about-us",
        "/company/about"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2, // 2MB
        maxFileSize = 1024 * 1024 * 10,      // 10MB
        maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class CompanyProfileServlet extends HttpServlet {

    private final CompanyProfileService profileService = new CompanyProfileService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. Màn hình công khai (Public About Us) cho ứng viên & người xem tự do
        if ("/about-us".equals(path) || "/company/about".equals(path)) {
            CompanyProfile profile = profileService.getProfile();
            request.setAttribute("profile", profile);
            request.getRequestDispatcher("/WEB-INF/views/company/about_public.jsp").forward(request, response);
            return;
        }

        // 2. API AJAX lấy dữ liệu cấu hình phục vụ Live Preview
        if ("/admin/company-profile/api/preview".equals(path)) {
            CompanyProfile profile = profileService.getProfile();
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, profile);
            return;
        }

        // 3. Màn hình quản trị cấu hình trang giới thiệu công ty
        CompanyProfile profile = profileService.getProfile();
        request.setAttribute("profile", profile);
        request.getRequestDispatcher("/WEB-INF/views/company/config.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 1. API AJAX Upload ảnh / logo
        if ("/admin/company-profile/upload".equals(path)) {
            handleAjaxUpload(request, response);
            return;
        }

        // 2. Lưu cấu hình trang giới thiệu công ty
        if ("/admin/company-profile/save".equals(path)) {
            handleSaveProfile(request, response);
            return;
        }

        // Mặc định chuyển hướng về trang cấu hình
        response.sendRedirect(request.getContextPath() + "/admin/company-profile");
    }

    private void handleAjaxUpload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> resp = new HashMap<>();
        try {
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                resp.put("success", false);
                resp.put("message", "Không tìm thấy file tải lên!");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, resp);
                return;
            }

            String submittedName = filePart.getSubmittedFileName();
            String uploadDir = getUploadDirectory(request);

            String fileUrl = profileService.saveUploadedFile(filePart.getInputStream(), submittedName, uploadDir);
            String fullUrl = request.getContextPath() + fileUrl;

            resp.put("success", true);
            resp.put("url", fullUrl);
            resp.put("relativePath", fileUrl);
            resp.put("message", "Tải ảnh lên thành công!");
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "Lỗi tải ảnh: " + e.getMessage());
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resp);
        }
    }

    private void handleSaveProfile(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String userId = (currentUser != null) ? currentUser.getId() : "system";
        String clientIp = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            CompanyProfile profile = profileService.getProfile();
            if (profile == null) {
                profile = new CompanyProfile();
            }

            // Đọc các trường text
            profile.setCompanyName(request.getParameter("companyName"));
            profile.setBrandName(request.getParameter("brandName"));
            profile.setSlogan(request.getParameter("slogan"));
            profile.setTagline(request.getParameter("tagline"));

            profile.setOverview(request.getParameter("overview"));
            profile.setHistoryStory(request.getParameter("historyStory"));
            profile.setMission(request.getParameter("mission"));
            profile.setVision(request.getParameter("vision"));
            profile.setCultureDesc(request.getParameter("cultureDesc"));

            profile.setHeadquartersAddress(request.getParameter("headquartersAddress"));
            profile.setContactEmail(request.getParameter("contactEmail"));
            profile.setContactPhone(request.getParameter("contactPhone"));
            profile.setWebsiteUrl(request.getParameter("websiteUrl"));
            profile.setFacebookUrl(request.getParameter("facebookUrl"));
            profile.setLinkedinUrl(request.getParameter("linkedinUrl"));
            profile.setYoutubeUrl(request.getParameter("youtubeUrl"));
            profile.setStatus(request.getParameter("status"));

            // Đọc JSON cấu trúc
            String coreValuesJson = request.getParameter("coreValues");
            if (coreValuesJson != null && !coreValuesJson.trim().isEmpty()) {
                profile.setCoreValues(coreValuesJson.trim());
            }

            String perksJson = request.getParameter("perks");
            if (perksJson != null && !perksJson.trim().isEmpty()) {
                profile.setPerks(perksJson.trim());
            }

            String galleryUrlsJson = request.getParameter("galleryUrls");
            if (galleryUrlsJson != null && !galleryUrlsJson.trim().isEmpty()) {
                profile.setGalleryUrls(galleryUrlsJson.trim());
            }

            // Xử lý upload trực tiếp file Logo nếu có đính kèm
            try {
                Part logoPart = request.getPart("logoFile");
                if (logoPart != null && logoPart.getSize() > 0) {
                    String uploadDir = getUploadDirectory(request);
                    String url = profileService.saveUploadedFile(logoPart.getInputStream(), logoPart.getSubmittedFileName(), uploadDir);
                    profile.setLogoUrl(request.getContextPath() + url);
                } else if (request.getParameter("logoUrl") != null && !request.getParameter("logoUrl").trim().isEmpty()) {
                    profile.setLogoUrl(request.getParameter("logoUrl").trim());
                }
            } catch (Exception ignored) {
                if (request.getParameter("logoUrl") != null && !request.getParameter("logoUrl").trim().isEmpty()) {
                    profile.setLogoUrl(request.getParameter("logoUrl").trim());
                }
            }

            // Xử lý upload trực tiếp file Banner nếu có đính kèm
            try {
                Part bannerPart = request.getPart("bannerFile");
                if (bannerPart != null && bannerPart.getSize() > 0) {
                    String uploadDir = getUploadDirectory(request);
                    String url = profileService.saveUploadedFile(bannerPart.getInputStream(), bannerPart.getSubmittedFileName(), uploadDir);
                    profile.setBannerUrl(request.getContextPath() + url);
                } else if (request.getParameter("bannerUrl") != null && !request.getParameter("bannerUrl").trim().isEmpty()) {
                    profile.setBannerUrl(request.getParameter("bannerUrl").trim());
                }
            } catch (Exception ignored) {
                if (request.getParameter("bannerUrl") != null && !request.getParameter("bannerUrl").trim().isEmpty()) {
                    profile.setBannerUrl(request.getParameter("bannerUrl").trim());
                }
            }

            profileService.saveProfile(profile, userId, clientIp, userAgent);

            if (session != null) {
                session.setAttribute("flashSuccess", "Đã lưu cấu hình trang giới thiệu công ty thành công!");
            }
        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("flashError", "Lỗi lưu cấu hình: " + e.getMessage());
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/company-profile");
    }

    private String getUploadDirectory(HttpServletRequest request) {
        String realPath = request.getServletContext().getRealPath("/uploads/company");
        if (realPath == null) {
            realPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "company";
        }
        File dir = new File(realPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return realPath;
    }
}
