package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.CompanyProfileDAO;
import com.irms.model.AuditLog;
import com.irms.model.CompanyProfile;
import com.irms.util.SecurityUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý nghiệp vụ cấu hình trang giới thiệu công ty (Company Profile / About Us)
 */
public class CompanyProfileService {

    private static final Logger LOGGER = Logger.getLogger(CompanyProfileService.class.getName());

    private final CompanyProfileDAO profileDAO = new CompanyProfileDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    /**
     * Lấy cấu hình giới thiệu công ty hiện tại
     */
    public CompanyProfile getProfile() {
        return profileDAO.getProfile();
    }

    /**
     * Lưu thông tin cấu hình trang giới thiệu công ty
     */
    public CompanyProfile saveProfile(CompanyProfile profile, String userId, String ip, String userAgent) {
        if (profile == null) {
            throw new IllegalArgumentException("Dữ liệu cấu hình không hợp lệ!");
        }

        if (profile.getCompanyName() == null || profile.getCompanyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên công ty không được để trống!");
        }
        profile.setCompanyName(profile.getCompanyName().trim());

        if (profile.getBrandName() != null) {
            profile.setBrandName(profile.getBrandName().trim());
        }
        if (profile.getSlogan() != null) {
            profile.setSlogan(profile.getSlogan().trim());
        }

        profile.setUpdatedBy(userId);

        boolean success = profileDAO.saveProfile(profile);
        if (!success) {
            throw new RuntimeException("Lỗi lưu cấu hình trang giới thiệu công ty vào cơ sở dữ liệu!");
        }

        logAudit(userId, "COMPANY_PROFILE_UPDATED", "COMPANY_PROFILE", profile.getId(),
                "Cập nhật cấu hình trang giới thiệu công ty: " + profile.getCompanyName(), ip, userAgent);

        return profileDAO.getProfile();
    }

    /**
     * Cập nhật trạng thái xuất bản
     */
    public void updateStatus(String status, String userId, String ip, String userAgent) {
        if (status == null || (!CompanyProfile.STATUS_PUBLISHED.equalsIgnoreCase(status) && !CompanyProfile.STATUS_DRAFT.equalsIgnoreCase(status))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ!");
        }

        boolean success = profileDAO.updateStatus(status.toUpperCase(), userId);
        if (!success) {
            throw new RuntimeException("Lỗi cập nhật trạng thái xuất bản!");
        }

        logAudit(userId, "COMPANY_PROFILE_STATUS_CHANGED", "COMPANY_PROFILE", "current",
                "Chuyển trạng thái trang giới thiệu công ty sang: " + status.toUpperCase(), ip, userAgent);
    }

    /**
     * Xử lý lưu file upload (ảnh logo, banner, gallery) vào thư mục server
     * Trả về đường dẫn URL tương đối để hiển thị trên web
     */
    public String saveUploadedFile(InputStream inputStream, String originalFilename, String uploadDirPath) {
        if (inputStream == null || originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File upload không hợp lệ!");
        }

        try {
            File uploadDir = new File(uploadDirPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Tạo tên file an toàn duy nhất
            String ext = "";
            int dotIdx = originalFilename.lastIndexOf('.');
            if (dotIdx >= 0) {
                ext = originalFilename.substring(dotIdx).toLowerCase();
            }
            if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp") && !ext.equals(".svg")) {
                ext = ".png";
            }

            String safeFileName = UUID.randomUUID().toString() + ext;
            File targetFile = new File(uploadDir, safeFileName);

            try (OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            return "/uploads/company/" + safeFileName;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi lưu file ảnh upload", e);
            throw new RuntimeException("Không thể lưu file ảnh: " + e.getMessage());
        }
    }

    private void logAudit(String userId, String action, String entityType, String entityId, String desc, String ip, String userAgent) {
        try {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(userId);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(desc);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Lỗi ghi audit log cấu hình công ty", e);
        }
    }
}
